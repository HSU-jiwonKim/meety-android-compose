package com.bugzero.meety.ui.admin

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
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

    private val db = FirebaseFirestore.getInstance()

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
        db.collection("adminQueue")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = snapshot?.documents?.map { doc ->
                    VerificationRequest(
                        requestId = doc.id,
                        userId = doc.getString("userId") ?: "",
                        userName = doc.getString("userName") ?: "",
                        userEmail = doc.getString("userEmail") ?: "",
                        studentIdImageUrl = doc.getString("studentIdImageUrl") ?: "",
                        status = doc.getString("status") ?: "pending"
                    )
                } ?: emptyList()
                _requests.value = list
            }
    }

    fun fetchUsers() {
        db.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = snapshot?.documents?.map { doc ->
                    UserInfo(
                        userId = doc.id,
                        name = doc.getString("name") ?: "",
                        email = doc.getString("email") ?: "",
                        isVerified = doc.getBoolean("isVerified") ?: false,
                        isAdmin = doc.getBoolean("isAdmin") ?: false,
                        isBanned = doc.getBoolean("isBanned") ?: false,
                        department = doc.getString("department") ?: "",
                        profileImages = (doc.get("profileImages") as? List<String>) ?: emptyList()
                    )
                } ?: emptyList()
                _users.value = list
            }
    }

    fun fetchReports() {
        db.collection("reports")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = snapshot?.documents?.map { doc ->
                    ReportInfo(
                        reportId = doc.id,
                        reporterId = doc.getString("reporterId") ?: "",
                        reporterName = doc.getString("reporterName") ?: "",
                        reportedId = doc.getString("reportedId") ?: "",
                        reportedName = doc.getString("reportedName") ?: "",
                        reason = doc.getString("reason") ?: "",
                        status = doc.getString("status") ?: "pending"
                    )
                } ?: emptyList()
                _reports.value = list
            }
    }

    fun approveRequest(requestId: String, userId: String) {
        _actionState.value = AdminActionState.Loading
        db.collection("users").document(userId)
            .update(mapOf("isVerified" to true, "verificationStatus" to "approved"))
            .addOnSuccessListener {
                db.collection("adminQueue").document(requestId)
                    .update("status", "approved")
                    .addOnSuccessListener {
                        _actionState.value = AdminActionState.Success("✅ 승인 완료했습니다")
                    }
                    .addOnFailureListener {
                        _actionState.value = AdminActionState.Error("상태 업데이트에 실패했습니다")
                    }
            }
            .addOnFailureListener {
                _actionState.value = AdminActionState.Error("승인에 실패했습니다")
            }
    }

    fun rejectRequest(requestId: String, userId: String) {
        _actionState.value = AdminActionState.Loading
        db.collection("adminQueue").document(requestId)
            .update("status", "rejected")
            .addOnSuccessListener {
                db.collection("users").document(userId)
                    .update("verificationStatus", "rejected")
                    .addOnSuccessListener {
                        _actionState.value = AdminActionState.Success("❌ 거절 처리했습니다")
                    }
                    .addOnFailureListener {
                        _actionState.value = AdminActionState.Error("거절 처리에 실패했습니다")
                    }
            }
            .addOnFailureListener {
                _actionState.value = AdminActionState.Error("거절 처리에 실패했습니다")
            }
    }

    fun banUser(userId: String) {
        _actionState.value = AdminActionState.Loading
        db.collection("users").document(userId)
            .update("isBanned", true)
            .addOnSuccessListener {
                _actionState.value = AdminActionState.Success("🚫 유저를 차단했습니다")
            }
            .addOnFailureListener {
                _actionState.value = AdminActionState.Error("차단에 실패했습니다")
            }
    }

    fun unbanUser(userId: String) {
        _actionState.value = AdminActionState.Loading
        db.collection("users").document(userId)
            .update("isBanned", false)
            .addOnSuccessListener {
                _actionState.value = AdminActionState.Success("✅ 차단을 해제했습니다")
            }
            .addOnFailureListener {
                _actionState.value = AdminActionState.Error("차단 해제에 실패했습니다")
            }
    }

    fun grantAdmin(userId: String) {
        _actionState.value = AdminActionState.Loading
        db.collection("users").document(userId)
            .update("isAdmin", true)
            .addOnSuccessListener {
                _actionState.value = AdminActionState.Success("👑 관리자 권한을 부여했습니다")
            }
            .addOnFailureListener {
                _actionState.value = AdminActionState.Error("권한 부여에 실패했습니다")
            }
    }

    fun resolveReport(reportId: String, reportedId: String, shouldBan: Boolean) {
        _actionState.value = AdminActionState.Loading
        db.collection("reports").document(reportId)
            .update("status", "resolved")
            .addOnSuccessListener {
                if (shouldBan) {
                    banUser(reportedId)
                } else {
                    _actionState.value = AdminActionState.Success("✅ 신고를 처리했습니다")
                }
            }
            .addOnFailureListener {
                _actionState.value = AdminActionState.Error("신고 처리에 실패했습니다")
            }
    }

    fun resetActionState() {
        _actionState.value = AdminActionState.Idle
    }
}