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

    // 중복 처리 방지: 현재 처리 중인 likeId 추적
    private val processingLikes = mutableSetOf<String>()

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

                    // 이미 처리 중인 좋아요는 건너뜀 (중복 처리 방지)
                    if (processingLikes.contains(likeId)) continue

                    val fromUserId = likeDoc.getString("fromUserId") ?: continue
                    val toTeamId = likeDoc.getString("toTeamId") ?: continue
                    val toTeamName = likeDoc.getString("toTeamName") ?: ""

                    processingLikes.add(likeId)

                    // 수락 처리
                    autoAcceptLike(
                        likeId, fromUserId, toTeamId, toTeamName,
                        onAccepted = { teamName ->
                            // ✨ 2. 중복 완벽 방어: 성공했을 때는 processingLikes에서 지우지 않습니다!
                            // (앱이 켜져있는 동안 똑같은 요청이 파이어베이스 캐시 때문에 두 번 실행되는 것을 원천 차단)
                            onAccepted(teamName)
                        },
                        onFailure = { msg ->
                            processingLikes.remove(likeId) // 실패했을 때만 다시 시도할 수 있게 풀어줍니다.
                            onFailure(msg)
                        }
                    )
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
        // 먼저 문서가 아직 pending 상태인지 확인 (삭제/변경됐을 수 있음)
        db.collection("likes").document(likeId).get()
            .addOnSuccessListener { likeSnapshot ->
                // 문서가 없거나 이미 처리된 경우 스킵
                if (!likeSnapshot.exists() || likeSnapshot.getString("status") != "pending") {
                    return@addOnSuccessListener
                }

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
                                    .addOnFailureListener { onFailure("자동 수락 실패: ${it.message}") }
                            }
                            .addOnFailureListener { onFailure("자동 수락 실패: ${it.message}") }
                    }
                    .addOnFailureListener { e ->
                        // NOT_FOUND: 초기화 등으로 문서가 삭제된 경우 → 조용히 무시
                        if (e.message?.contains("NOT_FOUND") == true) return@addOnFailureListener
                        onFailure("자동 수락 실패: ${e.message}")
                    }
            }
            .addOnFailureListener { onFailure("자동 수락 실패: ${it.message}") }
    }

    fun stopAutoAcceptListener() {
        autoAcceptListener?.remove()
        autoAcceptListener = null
        processingLikes.clear()
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

            // 모든 더미팀 ID 수집 (memberIds 체크와 무관하게 teamIds에서 전부 제거해야 함)
            val allDummyTeamIds = dummyTeams.documents.map { it.id }

            for (teamDoc in dummyTeams.documents) {
                val teamId = teamDoc.id
                val memberIds = (teamDoc.get("memberIds") as? List<String>) ?: emptyList()

                // memberIds에 있는 경우에만 팀/채팅에서 실제로 제거
                if (memberIds.contains(userId)) {
                    // 팀에서 유저 제거
                    db.collection("teams").document(teamId)
                        .update("memberIds", FieldValue.arrayRemove(userId)).await()

                    // 채팅방에서 유저 제거 (문서가 없을 수도 있으므로 예외 무시)
                    try {
                        val chatDoc = db.collection("chats").document(teamId).get().await()
                        if (chatDoc.exists()) {
                            db.collection("chats").document(teamId)
                                .update("participants", FieldValue.arrayRemove(userId)).await()
                        }
                    } catch (_: Exception) { }

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

            // 4. 유저의 teamIds에서 모든 더미팀 제거
            // (이전에 전체 초기화로 memberIds가 이미 복원됐어도 teamIds는 남아있을 수 있음)
            for (teamId in allDummyTeamIds) {
                db.collection("users").document(userId)
                    .update("teamIds", FieldValue.arrayRemove(teamId)).await()
            }

            // 5. 리셋 신호 전송 → 해당 유저 앱에서 실시간 감지 후 피드 자동 리로드
            db.collection("resetSignals").document(userId)
                .set(mapOf("resetAt" to System.currentTimeMillis())).await()

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

                // participants를 원래 더미 멤버로 복원 (문서 없으면 생성, 있으면 덮어쓰기)
                val chatDocRef = db.collection("chats").document(teamId)
                val chatDoc = chatDocRef.get().await()
                if (chatDoc.exists()) {
                    chatDocRef.update("participants", originalMemberIds).await()
                } else {
                    chatDocRef.set(mapOf("participants" to originalMemberIds)).await()
                }

                // 팀 memberIds도 원래 더미 멤버로 복원
                db.collection("teams").document(teamId)
                    .update("memberIds", originalMemberIds).await()
            }

            // 4. 비더미 유저의 teamIds에서 더미팀 ID 제거 + 리셋 신호 전송
            val dummyTeamIds = dummyTeams.documents.map { it.id }
            val resetAt = System.currentTimeMillis()
            for (userDoc in allUsers.documents) {
                val isDummy = userDoc.getBoolean("isDummy") ?: false
                val isAdmin = userDoc.getBoolean("isAdmin") ?: false
                if (!isDummy && !isAdmin) {
                    val userId = userDoc.id
                    for (teamId in dummyTeamIds) {
                        db.collection("users").document(userId)
                            .update("teamIds", FieldValue.arrayRemove(teamId)).await()
                    }
                    // 각 유저에게 리셋 신호 전송 → 앱에서 실시간 감지 후 피드 자동 리로드
                    db.collection("resetSignals").document(userId)
                        .set(mapOf("resetAt" to resetAt)).await()
                }
            }

            onSuccess()
        } catch (e: Exception) {
            onFailure("전체 초기화 실패: ${e.message}")
        }
    }

    // ═══════════════════════════════════════
    // 사용자 팀 목록 조회 (더미팀 제외)
    // ═══════════════════════════════════════

    fun fetchNonDummyTeams(onResult: (List<com.bugzero.meety.ui.admin.TeamInfo>) -> Unit) {
        db.collection("teams")
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = snapshot?.documents
                    ?.filter { doc -> !(doc.getBoolean("isDummy") ?: false) }
                    ?.map { doc ->
                        com.bugzero.meety.ui.admin.TeamInfo(
                            teamId      = doc.getString("teamId") ?: doc.id,
                            teamName    = doc.getString("teamName") ?: "",
                            leaderId    = doc.getString("leaderId") ?: "",
                            memberCount = (doc.get("memberIds") as? List<*>)?.size ?: 0
                        )
                    } ?: emptyList()
                onResult(list)
            }
    }

    // ═══════════════════════════════════════
    // 사용자 팀 삭제 (더미팀은 호출 불가 — ViewModel에서 필터링)
    // ═══════════════════════════════════════

    suspend fun deleteTeam(
        teamId: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            // 1. 팀 문서에서 memberIds 가져오기
            val teamDoc = db.collection("teams").document(teamId).get().await()
            val memberIds = (teamDoc.get("memberIds") as? List<String>) ?: emptyList()

            // 2. 채팅방 메시지 전부 삭제
            val messages = db.collection("chats").document(teamId)
                .collection("messages").get().await()
            for (msg in messages.documents) {
                msg.reference.delete().await()
            }

            // 3. 채팅방 문서 삭제
            try {
                db.collection("chats").document(teamId).delete().await()
            } catch (_: Exception) { }

            // 4. 이 팀에 보낸 likes 삭제
            val likesToTeam = db.collection("likes")
                .whereEqualTo("toTeamId", teamId)
                .get().await()
            for (doc in likesToTeam.documents) {
                doc.reference.delete().await()
            }

            // 5. 각 멤버의 teamIds에서 제거
            for (memberId in memberIds) {
                db.collection("users").document(memberId)
                    .update("teamIds", FieldValue.arrayRemove(teamId)).await()
            }

            // 6. 팀 문서 삭제
            db.collection("teams").document(teamId).delete().await()

            onSuccess("✅ 팀을 삭제했습니다")
        } catch (e: Exception) {
            onFailure("팀 삭제 실패: ${e.message}")
        }
    }
}