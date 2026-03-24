package com.bugzero.meety.ui.feed

import androidx.lifecycle.ViewModel
<<<<<<< HEAD

class FeedViewModel : ViewModel() {
    // TODO: B 담당 - 피드, 모임 상세, 프로필 수정 로직 구현
    // Gemini AI 팀 추천 연동
=======
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// UI 상태를 관리하는 데이터 클래스
data class FeedUiState(
    val viewMode: String = "recommend",
    val currentIndex: Int = 0,
    val teams: List<MockTeam> = mockTeams,
    val history: List<Int> = emptyList(),
    val isLoading: Boolean = false,
    val userPreferences: Map<String, Int> = emptyMap(),

    // 상세보기용: 사용자가 클릭하여 선택한 팀의 상세 정보
    val selectedTeam: MockTeam? = null
)

class FeedViewModel : ViewModel() {
    //private val auth = FirebaseAuth.getInstance()
    //private val db = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    // 1. 탭 전환 (추천 vs 전체 목록)
    fun setViewMode(mode: String) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    // 2. 카드 스와이프 (Like/Pass) 및 AI 분석 연동
    fun onCardSwiped(isLike: Boolean) {
        val state = _uiState.value
        val currentTeam = state.teams.getOrNull(state.currentIndex)

        if (currentTeam != null) {
            // 좋아요를 누른 경우 해당 팀의 속성(학과, 태그)을 분석하여 취향에 반영
            if (isLike) {
                //updateAiPreference(currentTeam)
            }

            _uiState.update {
                it.copy(
                    currentIndex = it.currentIndex + 1,
                    history = it.history + it.currentIndex
                )
            }
        }
    }

    // 3. 되돌리기 (Undo)
    fun undoSwipe() {
        _uiState.update { state ->
            if (state.history.isNotEmpty()) {
                val lastIdx = state.history.last()
                state.copy(
                    currentIndex = lastIdx,
                    history = state.history.dropLast(1)
                )
            } else state
        }
    }

    // 4. 상세보기 전용: 클릭된 팀의 ID를 찾아 selectedTeam 상태 업데이트
    fun selectTeam(teamId: String) {
        val team = _uiState.value.teams.find { it.id == teamId }
        _uiState.update { it.copy(selectedTeam = team) }
    }

    // 5. 피드 초기화 (다시 보기)
    fun resetFeed() {
        _uiState.update { it.copy(currentIndex = 0, history = emptyList()) }
    }

    // =====================
    // AI 취향 분석 및 실시간 추천 로직
    // =====================

    private fun updateAiPreference(team: MockTeam) {
        val currentPrefs = _uiState.value.userPreferences.toMutableMap()

        // 학과 선호도 점수 부여 (+2)
        currentPrefs[team.department] = (currentPrefs[team.department] ?: 0) + 2

        // 태그별 선호도 점수 부여 (+1)
        team.tags.forEach { tag ->
            currentPrefs[tag] = (currentPrefs[tag] ?: 0) + 1
        }

        _uiState.update { it.copy(userPreferences = currentPrefs) }

        // 점수가 업데이트될 때마다 추천 리스트 순서 재정렬
        applyAiRecommendation()
    }

    private fun applyAiRecommendation() {
        val prefs = _uiState.value.userPreferences
        if (prefs.isEmpty()) return

        val allTeams = _uiState.value.teams

        // 사용자의 누적 취향 점수가 높은 순서대로 팀 리스트를 정렬함
        val sortedTeams = allTeams.sortedByDescending { team ->
            val deptScore = prefs[team.department] ?: 0
            val tagScore = team.tags.sumOf { prefs[it] ?: 0 }
            deptScore + tagScore
        }

        _uiState.update { it.copy(teams = sortedTeams) }
    }

    // (추후 기능) Firebase Firestore에서 실제 데이터를 불러올 때 사용
    fun fetchRemoteTeams() {
        _uiState.update { it.copy(isLoading = true) }
        // db.collection("teams").get().addOnSuccessListener { ... }
    }
>>>>>>> ca178ecdf5d6d6fc1948225597641ad0fce1c061
}