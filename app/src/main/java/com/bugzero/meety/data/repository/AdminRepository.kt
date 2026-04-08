package com.bugzero.meety.data.repository

import com.bugzero.meety.ui.admin.ReportInfo
import com.bugzero.meety.ui.admin.UserInfo
import com.bugzero.meety.ui.admin.VerificationRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class AdminRepository {

    private val db = FirebaseFirestore.getInstance()
    private var autoAcceptListener: ListenerRegistration? = null

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

    // ═══════════════════════════════════════
    // 자동 수락 모드
    // ═══════════════════════════════════════

    /**
     * likes 컬렉션에서 status=pending 문서를 실시간 감시
     * 새로운 좋아요가 들어오면 자동으로 수락 처리 (팀 합류 + 채팅방 참가)
     */
    fun startAutoAcceptListener(
        onAccepted: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        stopAutoAcceptListener()

        autoAcceptListener = db.collection("likes")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onFailure("자동 수락 리스너 에러: ${error.message}")
                    return@addSnapshotListener
                }

                val pendingLikes = snapshot?.documents ?: return@addSnapshotListener

                for (likeDoc in pendingLikes) {
                    val likeId = likeDoc.id
                    val fromUserId = likeDoc.getString("fromUserId") ?: continue
                    val toTeamId = likeDoc.getString("toTeamId") ?: continue
                    val toTeamName = likeDoc.getString("toTeamName") ?: ""

                    // 수락 처리
                    autoAcceptLike(likeId, fromUserId, toTeamId, toTeamName, onAccepted, onFailure)
                }
            }
    }

    private fun autoAcceptLike(
        likeId: String,
        fromUserId: String,
        toTeamId: String,
        toTeamName: String,
        onAccepted: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        // 1. 좋아요 상태 → accepted
        db.collection("likes").document(likeId)
            .update(mapOf("status" to "accepted", "respondedAt" to System.currentTimeMillis()))
            .addOnSuccessListener {
                // 2. 유저 정보 가져오기
                db.collection("users").document(fromUserId).get()
                    .addOnSuccessListener { userDoc ->
                        val mbti = userDoc.getString("mbti") ?: ""
                        val profileImage = userDoc.getString("profileImage") ?: ""
                        val userName = userDoc.getString("name") ?: "새 팀원"

                        // 3. 팀에 멤버 추가
                        db.collection("teams").document(toTeamId)
                            .update(
                                mapOf(
                                    "memberIds" to FieldValue.arrayUnion(fromUserId),
                                    "mbtiTags" to FieldValue.arrayUnion(mbti),
                                    "profileImages" to FieldValue.arrayUnion(profileImage)
                                )
                            )
                            .addOnSuccessListener {
                                // 4. 유저 teamIds에 추가
                                db.collection("users").document(fromUserId)
                                    .update("teamIds", FieldValue.arrayUnion(toTeamId))

                                // 5. 채팅방 참가자 추가
                                db.collection("chats").document(toTeamId)
                                    .update("participants", FieldValue.arrayUnion(fromUserId))

                                // 6. 시스템 메시지
                                val systemMessage = mapOf(
                                    "senderId" to "system",
                                    "senderName" to "system",
                                    "content" to "${userName}님이 입장했습니다.",
                                    "type" to "system",
                                    "createdAt" to com.google.firebase.Timestamp.now()
                                )
                                db.collection("chats").document(toTeamId)
                                    .collection("messages")
                                    .add(systemMessage)

                                onAccepted(toTeamName)
                            }
                    }
            }
            .addOnFailureListener { onFailure("자동 수락 실패: ${it.message}") }
    }

    fun stopAutoAcceptListener() {
        autoAcceptListener?.remove()
        autoAcceptListener = null
    }

    // ═══════════════════════════════════════
    // 특정 유저 시연 데이터 초기화
    // ═══════════════════════════════════════

    /**
     * 특정 유저의 시연 관련 데이터를 초기화한다:
     * 1. userPreferences 문서 삭제 (좋아요/패스 기록 + 선호도 점수)
     * 2. likes 컬렉션에서 해당 유저가 보낸 좋아요 삭제
     * 3. 더미팀에서 해당 유저 제거 (memberIds, participants)
     * 4. 유저의 teamIds에서 더미팀 제거
     */
    suspend fun resetUserDemoData(
        userId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            // 1. userPreferences 삭제
            db.collection("userPreferences").document(userId).delete().await()

            // 2. 해당 유저가 보낸 likes 삭제
            val likesSnapshot = db.collection("likes")
                .whereEqualTo("fromUserId", userId)
                .get().await()
            for (doc in likesSnapshot.documents) {
                doc.reference.delete().await()
            }

            // 3. 더미팀에서 해당 유저 제거
            val dummyTeams = db.collection("teams")
                .whereEqualTo("isDummy", true)
                .get().await()

            val dummyTeamIds = mutableListOf<String>()
            for (teamDoc in dummyTeams.documents) {
                val teamId = teamDoc.id
                val memberIds = (teamDoc.get("memberIds") as? List<String>) ?: emptyList()

                if (memberIds.contains(userId)) {
                    dummyTeamIds.add(teamId)

                    // 팀에서 유저 제거
                    db.collection("teams").document(teamId)
                        .update("memberIds", FieldValue.arrayRemove(userId)).await()

                    // 채팅방에서 유저 제거
                    db.collection("chats").document(teamId)
                        .update("participants", FieldValue.arrayRemove(userId)).await()

                    // 채팅방에서 해당 유저가 보낸 메시지 삭제
                    val messages = db.collection("chats").document(teamId)
                        .collection("messages")
                        .whereEqualTo("senderId", userId)
                        .get().await()
                    for (msg in messages.documents) {
                        msg.reference.delete().await()
                    }
                }
            }

            // 4. 유저의 teamIds에서 더미팀 제거
            for (teamId in dummyTeamIds) {
                db.collection("users").document(userId)
                    .update("teamIds", FieldValue.arrayRemove(teamId)).await()
            }

            onSuccess()
        } catch (e: Exception) {
            onFailure("초기화 실패: ${e.message}")
        }
    }

    // ═══════════════════════════════════════
    // 전체 더미 데이터 초기화
    // ═══════════════════════════════════════

    /**
     * 모든 시연 관련 데이터를 초기화한다:
     * 1. 모든 비더미 유저의 userPreferences 삭제
     * 2. 모든 likes 삭제
     * 3. 더미팀 채팅방 messages 비우기
     * 4. 더미팀 participants를 원래 더미 멤버로 복원
     * 5. 비더미 유저의 teamIds에서 더미팀 제거
     */
    suspend fun resetAllDemoData(
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            // 1. 시연자가 만든 likes 전부 삭제 (더미 유저가 아닌 유저가 보낸 것)
            val allLikes = db.collection("likes").get().await()
            for (doc in allLikes.documents) {
                val fromUserId = doc.getString("fromUserId") ?: ""
                // 더미 유저가 아닌 실제 유저가 보낸 좋아요만 삭제
                val userDoc = db.collection("users").document(fromUserId).get().await()
                val isDummy = userDoc.getBoolean("isDummy") ?: false
                if (!isDummy) {
                    doc.reference.delete().await()
                }
            }

            // 2. 비더미 유저의 userPreferences 삭제
            val allUsers = db.collection("users").get().await()
            for (userDoc in allUsers.documents) {
                val isDummy = userDoc.getBoolean("isDummy") ?: false
                val isAdmin = userDoc.getBoolean("isAdmin") ?: false
                if (!isDummy && !isAdmin) {
                    val userId = userDoc.id
                    // userPreferences 삭제
                    db.collection("userPreferences").document(userId).delete().await()
                }
            }

            // 3. 더미팀 채팅방 초기화
            val dummyTeams = db.collection("teams")
                .whereEqualTo("isDummy", true)
                .get().await()

            for (teamDoc in dummyTeams.documents) {
                val teamId = teamDoc.id
                val originalMemberIds = (teamDoc.get("memberIds") as? List<String>)
                    ?.filter { memberId ->
                        // 더미 유저만 남기기
                        val mDoc = db.collection("users").document(memberId).get().await()
                        mDoc.getBoolean("isDummy") ?: false
                    } ?: emptyList()

                // 채팅방 messages 전부 삭제
                val messages = db.collection("chats").document(teamId)
                    .collection("messages").get().await()
                for (msg in messages.documents) {
                    msg.reference.delete().await()
                }

                // participants를 원래 더미 멤버로 복원
                db.collection("chats").document(teamId)
                    .update("participants", originalMemberIds).await()

                // 팀 memberIds도 원래 더미 멤버로 복원
                db.collection("teams").document(teamId)
                    .update("memberIds", originalMemberIds).await()
            }

            // 4. 비더미 유저의 teamIds에서 더미팀 ID 제거
            val dummyTeamIds = dummyTeams.documents.map { it.id }
            for (userDoc in allUsers.documents) {
                val isDummy = userDoc.getBoolean("isDummy") ?: false
                if (!isDummy) {
                    val userId = userDoc.id
                    for (teamId in dummyTeamIds) {
                        db.collection("users").document(userId)
                            .update("teamIds", FieldValue.arrayRemove(teamId)).await()
                    }
                }
            }

            onSuccess()
        } catch (e: Exception) {
            onFailure("전체 초기화 실패: ${e.message}")
        }
    }
}