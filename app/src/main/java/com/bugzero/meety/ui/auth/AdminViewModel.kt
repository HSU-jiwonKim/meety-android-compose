package com.bugzero.meety.ui.admin

import androidx.lifecycle.ViewModel
import com.bugzero.meety.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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

    init {
        fetchPendingRequests()
        fetchUsers()
        fetchReports()
    }

    fun fetchPendingRequests() {
        adminRepository.fetchPendingRequests { _requests.value = it }
    }

    fun fetchUsers() {
        adminRepository.fetchUsers { _users.value = it }
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

    fun resetActionState() {
        _actionState.value = AdminActionState.Idle
    }
}