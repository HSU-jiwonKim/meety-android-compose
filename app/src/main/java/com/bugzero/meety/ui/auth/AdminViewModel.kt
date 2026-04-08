package com.bugzero.meety.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugzero.meety.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class VerificationRequest(
    val requestId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val studentIdImageUrl: String = "",
    val status: String = "pending"
)

data class UserInfo(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val isVerified: Boolean = false,
    val isAdmin: Boolean = false,
    val isBanned: Boolean = false,
    val department: String = "",
    val profileImages: List<String> = emptyList()
)

data class ReportInfo(
    val reportId: String = "",
    val reporterId: String = "",
    val reporterName: String = "",
    val reportedId: String = "",
    val reportedName: String = "",
    val reason: String = "",
    val status: String = "pending"
)

sealed class AdminActionState {
    object Idle : AdminActionState()
    object Loading : AdminActionState()
    data class Success(val message: String) : AdminActionState()
    data class Error(val message: String) : AdminActionState()
}

class AdminViewModel : ViewModel() {

    private val adminRepository = AdminRepository()

    private val _requests = MutableStateFlow<List<VerificationRequest>>(emptyList())
    val requests: StateFlow<List<VerificationRequest>> = _requests

    private val _users = MutableStateFlow<List<UserInfo>>(emptyList())
    val users: StateFlow<List<UserInfo>> = _users

    private val _reports = MutableStateFlow<List<ReportInfo>>(emptyList())
    val reports: StateFlow<List<ReportInfo>> = _reports

    private val _actionState = MutableStateFlow<AdminActionState>(AdminActionState.Idle)
    val actionState: StateFlow<AdminActionState> = _actionState

    // ── 자동 수락 모드 ──
    private val _autoAcceptEnabled = MutableStateFlow(false)
    val autoAcceptEnabled: StateFlow<Boolean> = _autoAcceptEnabled

    // ── 시연 계정 목록 (test 계정 + 최근 가입 비더미 유저) ──
    private val _demoUsers = MutableStateFlow<List<UserInfo>>(emptyList())
    val demoUsers: StateFlow<List<UserInfo>> = _demoUsers

    init {
        fetchPendingRequests()
        fetchUsers()
        fetchReports()
    }

    fun fetchPendingRequests() {
        adminRepository.fetchPendingRequests { _requests.value = it }
    }

    fun fetchUsers() {
        adminRepository.fetchUsers { users ->
            _users.value = users
            // 시연 계정 필터: test 계정 + 더미가 아닌 실제 유저
            _demoUsers.value = users.filter { user ->
                user.email.startsWith("test") ||
                        (!user.isAdmin && user.email.endsWith("@hansung.ac.kr") &&
                                !(user.email.startsWith("dummy_")))
            }
        }
    }

    fun fetchReports() {
        adminRepository.fetchReports { _reports.value = it }
    }

    fun approveRequest(requestId: String, userId: String) {
        _actionState.value = AdminActionState.Loading
        adminRepository.approveRequest(
            requestId = requestId,
            userId = userId,
            onSuccess = { _actionState.value = AdminActionState.Success(it) },
            onFailure = { _actionState.value = AdminActionState.Error(it) }
        )
    }

    fun rejectRequest(requestId: String, userId: String) {
        _actionState.value = AdminActionState.Loading
        adminRepository.rejectRequest(
            requestId = requestId,
            userId = userId,
            onSuccess = { _actionState.value = AdminActionState.Success(it) },
            onFailure = { _actionState.value = AdminActionState.Error(it) }
        )
    }

    fun banUser(userId: String) {
        _actionState.value = AdminActionState.Loading
        adminRepository.banUser(
            userId = userId,
            onSuccess = { _actionState.value = AdminActionState.Success(it) },
            onFailure = { _actionState.value = AdminActionState.Error(it) }
        )
    }

    fun unbanUser(userId: String) {
        _actionState.value = AdminActionState.Loading
        adminRepository.unbanUser(
            userId = userId,
            onSuccess = { _actionState.value = AdminActionState.Success(it) },
            onFailure = { _actionState.value = AdminActionState.Error(it) }
        )
    }

    fun grantAdmin(userId: String) {
        _actionState.value = AdminActionState.Loading
        adminRepository.grantAdmin(
            userId = userId,
            onSuccess = { _actionState.value = AdminActionState.Success(it) },
            onFailure = { _actionState.value = AdminActionState.Error(it) }
        )
    }

    fun resolveReport(reportId: String, reportedId: String, shouldBan: Boolean) {
        _actionState.value = AdminActionState.Loading
        adminRepository.resolveReport(
            reportId = reportId,
            reportedId = reportedId,
            shouldBan = shouldBan,
            onSuccess = { _actionState.value = AdminActionState.Success(it) },
            onFailure = { _actionState.value = AdminActionState.Error(it) }
        )
    }

    // ═══════════════════════════════════════
    // 데모 관리 기능
    // ═══════════════════════════════════════

    /**
     * 자동 수락 모드 토글
     * 켜면 likes 컬렉션의 pending 좋아요를 실시간 감시하며 자동 수락
     */
    fun toggleAutoAccept() {
        val newState = !_autoAcceptEnabled.value
        _autoAcceptEnabled.value = newState

        if (newState) {
            adminRepository.startAutoAcceptListener(
                onAccepted = { teamName ->
                    _actionState.value = AdminActionState.Success("✅ 자동 수락: $teamName")
                },
                onFailure = { msg ->
                    _actionState.value = AdminActionState.Error(msg)
                }
            )
        } else {
            adminRepository.stopAutoAcceptListener()
        }
    }

    /**
     * 특정 유저의 시연 데이터 초기화
     * - userPreferences (좋아요/패스 기록, 선호도 점수)
     * - likes (보낸 좋아요)
     * - 해당 유저가 참가한 더미팀 채팅방에서 제거
     */
    fun resetUserDemoData(userId: String) {
        _actionState.value = AdminActionState.Loading
        viewModelScope.launch {
            adminRepository.resetUserDemoData(
                userId = userId,
                onSuccess = {
                    _actionState.value = AdminActionState.Success("🔄 시연 데이터 초기화 완료")
                },
                onFailure = {
                    _actionState.value = AdminActionState.Error(it)
                }
            )
        }
    }

    /**
     * 전체 더미 데이터 초기화
     * - 모든 likes (status: accepted → pending 복원 X, 그냥 삭제)
     * - 모든 userPreferences 중 비관리자/비더미 유저 문서 삭제
     * - 더미팀 채팅방 messages 비우기
     * - 더미팀 participants를 원래 멤버로 복원
     */
    fun resetAllDemoData() {
        _actionState.value = AdminActionState.Loading
        viewModelScope.launch {
            adminRepository.resetAllDemoData(
                onSuccess = {
                    _actionState.value = AdminActionState.Success("🔄 전체 데모 초기화 완료")
                },
                onFailure = {
                    _actionState.value = AdminActionState.Error(it)
                }
            )
        }
    }

    fun resetActionState() {
        _actionState.value = AdminActionState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        adminRepository.stopAutoAcceptListener()
    }
}