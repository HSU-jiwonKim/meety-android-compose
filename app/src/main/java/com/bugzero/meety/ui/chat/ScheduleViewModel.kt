package com.bugzero.meety.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ✨ 팀 공강 추천에서 사용하는 사용자 시간표 모델
data class UserSchedule(
    val schedule: Map<String, List<String>> = emptyMap()
)

class ScheduleViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val currentUserId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val dayNames = listOf("월", "화", "수", "목", "금")
    private val timeSlots = listOf(
        "09:00", "09:30",
        "10:00", "10:30",
        "11:00", "11:30",
        "12:00", "12:30",
        "13:00", "13:30",
        "14:00", "14:30",
        "15:00", "15:30",
        "16:00", "16:30",
        "17:00", "17:30",
        "18:00"
    )

    private val _selectedCells = MutableStateFlow<Set<Pair<Int, Int>>>(emptySet())
    val selectedCells: StateFlow<Set<Pair<Int, Int>>> = _selectedCells.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // --- ✨ [신규 기능] 팀 전체 공강 상태 ---
    private val _mergedBusyTimes = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val mergedBusyTimes: StateFlow<Map<String, List<String>>> = _mergedBusyTimes

    init {
        loadSchedule()
    }

    fun toggleCell(cell: Pair<Int, Int>) {
        _selectedCells.update { current ->
            if (cell in current) current - cell else current + cell
        }
    }

    private fun loadSchedule() {
        viewModelScope.launch {
            try {
                val doc = db.collection("users")
                    .document(currentUserId)
                    .get()
                    .await()

                @Suppress("UNCHECKED_CAST")
                val schedule = doc.get("schedule") as? Map<String, List<String>> ?: return@launch

                val cells = mutableSetOf<Pair<Int, Int>>()
                schedule.forEach { (day, times) ->
                    val dayIdx = dayNames.indexOf(day)
                    if (dayIdx != -1) {
                        times.forEach { timeStr ->
                            val timeIdx = timeSlots.indexOf(timeStr)
                            if (timeIdx != -1) cells.add(Pair(timeIdx, dayIdx))
                        }
                    }
                }
                _selectedCells.value = cells
            } catch (e: Exception) {
                // 첫 설정일 수 있으므로 무시
            }
        }
    }

    fun saveSchedule() {
        viewModelScope.launch {
            _isSaving.value = true
            _errorMessage.value = null
            try {
                val scheduleMap = mutableMapOf<String, List<String>>()
                dayNames.forEachIndexed { dayIdx, dayName ->
                    val times = _selectedCells.value
                        .filter { it.second == dayIdx }
                        .sortedBy { it.first }
                        .map { timeSlots[it.first] } // timeIdx → "09:30" 형식
                    if (times.isNotEmpty()) scheduleMap[dayName] = times
                }

                db.collection("users")
                    .document(currentUserId)
                    .update("schedule", scheduleMap)
                    .await()

                _saveSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "저장에 실패했어요. 다시 시도해주세요."
            } finally {
                _isSaving.value = false
            }
        }
    }

    // --- ✨ [신규 로직] 팀 공강 데이터 합치기 ---
    fun fetchTeamSchedules(memberIds: List<String>) {
        viewModelScope.launch {
            val combinedSchedule = mutableMapOf<String, MutableSet<String>>()
            dayNames.forEach { combinedSchedule[it] = mutableSetOf() }

            memberIds.forEach { uid ->
                try {
                    val document = db.collection("users").document(uid).get().await()
                    @Suppress("UNCHECKED_CAST")
                    val scheduleMap = document.get("schedule") as? Map<String, List<String>>

                    scheduleMap?.forEach { (day, times) ->
                        combinedSchedule[day]?.addAll(times)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ScheduleLog", "데이터 로드 실패: ${e.message}")
                }
            }
            _mergedBusyTimes.value = combinedSchedule.mapValues { it.value.toList().sorted() }
        }
    }
}