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

data class TeamInfo(
    val teamId: String = "",
    val teamName: String = "",
    val leaderId: String = "",
    val memberCount: Int = 0
)

data class DirectChatInfo(
    val chatId: String = "",
    val type: String = "direct",   // "direct" | "group"
    val participantCount: Int = 0
)

data class PendingLikeInfo(
    val likeId: String = "",
    val fromUserId: String = "",
    val fromUserName: String = "",
    val fromUserEmail: String = "",
    val fromUserProfileImage: String = "",
    val toTeamId: String = "",
    val toTeamName: String = ""
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

    // ── 사용자가 만든 팀 목록 (더미팀 제외) ──
    private val _nonDummyTeams = MutableStateFlow<List<TeamInfo>>(emptyList())
    val nonDummyTeams: StateFlow<List<TeamInfo>> = _nonDummyTeams

    // ── 사용자가 만든 개인/그룹 채팅방 목록 (direct / group 타입) ──
    private val _nonDummyDirectChats = MutableStateFlow<List<DirectChatInfo>>(emptyList())
    val nonDummyDirectChats: StateFlow<List<DirectChatInfo>> = _nonDummyDirectChats

    // ── 더미팀으로 들어온 pending 좋아요 목록 ──
    private val _pendingDummyLikes = MutableStateFlow<List<PendingLikeInfo>>(emptyList())
    val pendingDummyLikes: StateFlow<List<PendingLikeInfo>> = _pendingDummyLikes

    init {
        fetchPendingRequests()
        fetchUsers()
        fetchReports()
        fetchNonDummyTeams()
        fetchNonDummyDirectChats()
        fetchPendingDummyLikes()
    }

    fun fetchPendingRequests() {
        adminRepository.fetchPendingRequests { _requests.value = it }
    }

    fun fetchUsers() {
        adminRepository.fetchUsers { users ->
            _users.value = users
            // 시연 계정 필터: 이름/이메일에 "테스터" 또는 "test"(대소문자 무관) 포함된 계정
            _demoUsers.value = users.filter { user ->
                val name = user.name
                val email = user.email
                name.contains("테스터") || email.contains("테스터") ||
                    name.contains("test", ignoreCase = true) ||
                    email.contains("test", ignoreCase = true)
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
     * 더미팀 좋아요 자동 수락 모드 토글
     * 켜면 더미팀(isDummy=true)으로 들어온 pending 좋아요만 실시간 감시하며 자동 수락한다.
     * 사용자가 만든 실제 팀으로 보낸 좋아요는 자동 수락 대상이 아니다.
     */
    fun toggleAutoAccept() {
        val newState = !_autoAcceptEnabled.value
        _autoAcceptEnabled.value = newState

        if (newState) {
            adminRepository.startAutoAcceptDummyListener(
                onAccepted = { teamName ->
                    _actionState.value = AdminActionState.Success("✅ 더미팀 자동 수락: $teamName")
                },
                onFailure = { msg ->
                    _actionState.value = AdminActionState.Error(msg)
                }
            )
        } else {
            adminRepository.stopAutoAcceptDummyListener()
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

    fun fetchNonDummyTeams() {
        adminRepository.fetchNonDummyTeams { _nonDummyTeams.value = it }
    }

    fun fetchNonDummyDirectChats() {
        adminRepository.fetchNonDummyDirectChats { _nonDummyDirectChats.value = it }
    }

    fun fetchPendingDummyLikes() {
        adminRepository.fetchPendingLikesToDummyTeams { _pendingDummyLikes.value = it }
    }

    fun acceptLike(likeId: String, fromUserId: String, toTeamId: String, toTeamName: String) {
        _actionState.value = AdminActionState.Loading
        adminRepository.acceptLike(
            likeId = likeId,
            fromUserId = fromUserId,
            toTeamId = toTeamId,
            toTeamName = toTeamName,
            onSuccess = { _actionState.value = AdminActionState.Success(it) },
            onFailure = { _actionState.value = AdminActionState.Error(it) }
        )
    }

    fun deleteTeam(teamId: String) {
        _actionState.value = AdminActionState.Loading
        viewModelScope.launch {
            adminRepository.deleteTeam(
                teamId = teamId,
                onSuccess = { _actionState.value = AdminActionState.Success(it) },
                onFailure = { _actionState.value = AdminActionState.Error(it) }
            )
        }
    }


    fun deleteAllNonDummyTeams() {
        _actionState.value = AdminActionState.Loading
        viewModelScope.launch {
            adminRepository.deleteAllNonDummyTeams(
                onSuccess = {
                    _actionState.value = AdminActionState.Success(it)
                    fetchNonDummyTeams()          // 팀 목록 즉시 갱신
                    fetchNonDummyDirectChats()    // 직접 채팅방 목록 즉시 갱신
                },
                onFailure = { _actionState.value = AdminActionState.Error(it) }
            )
        }
    }

    fun resetActionState() {
        _actionState.value = AdminActionState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        adminRepository.stopAutoAcceptListener()
        adminRepository.stopAutoAcceptDummyListener()
    }
}