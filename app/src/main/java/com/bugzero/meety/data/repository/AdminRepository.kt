package com.bugzero.meety.data.repository

import com.bugzero.meety.ui.admin.ReportInfo
import com.bugzero.meety.ui.admin.UserInfo
import com.bugzero.meety.ui.admin.VerificationRequest
import com.google.firebase.firestore.FirebaseFirestore

class AdminRepository {

    private val db = FirebaseFirestore.getInstance()

    // =====================
    // 인증 대기 목록
    // =====================
    fun fetchPendingRequests(onResult: (List<VerificationRequest>) -> Unit) {
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
                onResult(list)
            }
    }

    // =====================
    // 유저 목록
    // =====================
    fun fetchUsers(onResult: (List<UserInfo>) -> Unit) {
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
                onResult(list)
            }
    }

    // =====================
    // 신고 목록
    // =====================
    fun fetchReports(onResult: (List<ReportInfo>) -> Unit) {
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
                onResult(list)
            }
    }

    // =====================
    // 승인
    // =====================
    fun approveRequest(
        requestId: String,
        userId: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("users").document(userId)
            .update(mapOf("isVerified" to true, "verificationStatus" to "approved"))
            .addOnSuccessListener {
                db.collection("adminQueue").document(requestId)
                    .update("status", "approved")
                    .addOnSuccessListener { onSuccess("✅ 승인 완료했습니다") }
                    .addOnFailureListener { onFailure("상태 업데이트에 실패했습니다") }
            }
            .addOnFailureListener { onFailure("승인에 실패했습니다") }
    }

    // =====================
    // 거절
    // =====================
    fun rejectRequest(
        requestId: String,
        userId: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("adminQueue").document(requestId)
            .update("status", "rejected")
            .addOnSuccessListener {
                db.collection("users").document(userId)
                    .update("verificationStatus", "rejected")
                    .addOnSuccessListener { onSuccess("❌ 거절 처리했습니다") }
                    .addOnFailureListener { onFailure("거절 처리에 실패했습니다") }
            }
            .addOnFailureListener { onFailure("거절 처리에 실패했습니다") }
    }

    // =====================
    // 차단
    // =====================
    fun banUser(
        userId: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("users").document(userId)
            .update("isBanned", true)
            .addOnSuccessListener { onSuccess("🚫 유저를 차단했습니다") }
            .addOnFailureListener { onFailure("차단에 실패했습니다") }
    }

    // =====================
    // 차단 해제
    // =====================
    fun unbanUser(
        userId: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("users").document(userId)
            .update("isBanned", false)
            .addOnSuccessListener { onSuccess("✅ 차단을 해제했습니다") }
            .addOnFailureListener { onFailure("차단 해제에 실패했습니다") }
    }

    // =====================
    // 관리자 권한 부여
    // =====================
    fun grantAdmin(
        userId: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("users").document(userId)
            .update("isAdmin", true)
            .addOnSuccessListener { onSuccess("👑 관리자 권한을 부여했습니다") }
            .addOnFailureListener { onFailure("권한 부여에 실패했습니다") }
    }

    // =====================
    // 신고 처리
    // =====================
    fun resolveReport(
        reportId: String,
        reportedId: String,
        shouldBan: Boolean,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("reports").document(reportId)
            .update("status", "resolved")
            .addOnSuccessListener {
                if (shouldBan) {
                    banUser(reportedId, onSuccess, onFailure)
                } else {
                    onSuccess("✅ 신고를 처리했습니다")
                }
            }
            .addOnFailureListener { onFailure("신고 처리에 실패했습니다") }
    }
}