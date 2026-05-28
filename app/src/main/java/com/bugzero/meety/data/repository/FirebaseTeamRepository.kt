package com.bugzero.meety.ui.team

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import com.google.firebase.firestore.Query

class FirebaseTeamRepository : TeamRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    override fun createTeam(
        teamName: String,
        description: String,
        tags: List<String>,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) { onFailure("로그인된 사용자가 없습니다."); return }

        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val mbti = userDoc.getString("mbti") ?: ""
                val profileImage = userDoc.getString("profileImage") ?: ""

                val teamRef = db.collection("teams").document()
                val teamId = teamRef.id

                val team = Team(
                    teamId = teamId,
                    leaderId = userId,
                    memberIds = listOf(userId),
                    mbtiTags = listOf(mbti),
                    tags = tags,
                    profileImages = listOf(profileImage),
                    teamProfileImage = "",
                    status = "active",
                    teamName = teamName,
                    description = description,
                    createdAt = System.currentTimeMillis()
                )

                // 🔥 1. 팀 생성
                teamRef.set(team)
                    .addOnSuccessListener {

                        // 🔥 2. 팀 채팅방 생성
                        val chatCreatedAt = com.google.firebase.Timestamp.now()
                        val chatData = mapOf(
                            "chatId" to teamId,
                            "teamId" to teamId,
                            "teamName" to teamName,
                            "leaderId" to userId,
                            "participants" to listOf(userId),
                            "type" to "team",
                            "createdAt" to chatCreatedAt,
                            "lastMessage" to "",
                            "lastMessageAt" to null,
                            "emoji" to "👥",
                            // ✨ 원년 멤버는 방 생성 시점부터 모든 메시지 가시. 재입장 멤버만
                            //    acceptInvitation 에서 더 늦은 시각으로 덮어써져 이전 기록이 가려짐.
                            "memberJoinedAt" to mapOf(userId to chatCreatedAt)
                        )

                        db.collection("chats").document(teamId)
                            .set(chatData)
                            .addOnSuccessListener {

                                // 🔥 3. 유저 teamIds 배열에 추가
                                db.collection("users").document(userId)
                                    .update("teamIds", FieldValue.arrayUnion(teamId))
                                    .addOnSuccessListener { onSuccess(teamId) }
                                    .addOnFailureListener { onFailure(it.message ?: "users.teamIds 업데이트 실패") }
                            }
                            .addOnFailureListener { onFailure(it.message ?: "팀 채팅방 생성 실패") }
                    }
                    .addOnFailureListener { onFailure(it.message ?: "teams 생성 실패") }
            }
            .addOnFailureListener { onFailure(it.message ?: "users 조회 실패") }
    }

    override fun loadReceivedLikes(onSuccess: (List<ReceivedLikeItem>) -> Unit, onFailure: (String) -> Unit) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) { onFailure("로그인된 사용자가 없습니다."); return }

        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { userDoc ->
                @Suppress("UNCHECKED_CAST")
                val myTeamIds = (userDoc.get("teamIds") as? List<String>) ?: emptyList()
                if (myTeamIds.isEmpty()) { onSuccess(emptyList()); return@addOnSuccessListener }

                // whereIn 10개 제한 대응 chunked 처리
                val allResults = mutableListOf<ReceivedLikeItem>()
                val chunks = myTeamIds.chunked(10)
                var chunkCompleted = 0

                for (chunk in chunks) {
                    db.collection("likes")
                        .whereIn("toTeamId", chunk)
                        .whereEqualTo("status", "pending")
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .get()
                        .addOnSuccessListener { likeSnapshot ->
                            if (likeSnapshot.isEmpty) {
                                chunkCompleted++
                                if (chunkCompleted == chunks.size) onSuccess(allResults.sortedByDescending { it.createdAt })
                                return@addOnSuccessListener
                            }

                            val docs = likeSnapshot.documents
                            val results = mutableListOf<ReceivedLikeItem>()
                            var completedCount = 0

                            for (likeDoc in docs) {
                                val likeId = likeDoc.getString("likeId").orEmpty().ifBlank { likeDoc.id }
                                val fromUserId = likeDoc.getString("fromUserId").orEmpty()
                                val fromTeamId = likeDoc.getString("fromTeamId").orEmpty()
                                val createdAt = likeDoc.getLong("createdAt") ?: 0L

                                if (fromUserId.isBlank()) {
                                    completedCount++
                                    if (completedCount == docs.size) {
                                        allResults.addAll(results)
                                        chunkCompleted++
                                        if (chunkCompleted == chunks.size) onSuccess(allResults.sortedByDescending { it.createdAt })
                                    }
                                    continue
                                }

                                db.collection("users").document(fromUserId).get()
                                    .addOnSuccessListener { senderDoc ->
                                        results.add(ReceivedLikeItem(
                                            likeId = likeId,
                                            fromUserId = fromUserId,
                                            fromUserName = senderDoc.getString("name").orEmpty().ifBlank { "이름 없음" },
                                            fromUserProfileImage = senderDoc.getString("profileImage").orEmpty(),
                                            fromUserMbti = senderDoc.getString("mbti").orEmpty(),
                                            fromUserDepartment = senderDoc.getString("department").orEmpty(),
                                            fromTeamId = fromTeamId,
                                            createdAt = createdAt
                                        ))
                                        completedCount++
                                        if (completedCount == docs.size) {
                                            allResults.addAll(results)
                                            chunkCompleted++
                                            if (chunkCompleted == chunks.size) onSuccess(allResults.sortedByDescending { it.createdAt })
                                        }
                                    }
                                    .addOnFailureListener {
                                        results.add(ReceivedLikeItem(likeId = likeId, fromUserId = fromUserId, fromUserName = "이름 없음", fromTeamId = fromTeamId, createdAt = createdAt))
                                        completedCount++
                                        if (completedCount == docs.size) {
                                            allResults.addAll(results)
                                            chunkCompleted++
                                            if (chunkCompleted == chunks.size) onSuccess(allResults.sortedByDescending { it.createdAt })
                                        }
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            chunkCompleted++
                            if (chunkCompleted == chunks.size) onSuccess(allResults.sortedByDescending { it.createdAt })
                        }
                }
            }
            .addOnFailureListener { e -> onFailure(e.message ?: "사용자 정보 조회 실패") }
    }

    override fun acceptReceivedLike(likeId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) { onFailure("로그인된 사용자가 없습니다."); return }

        db.collection("likes").document(likeId).get()
            .addOnSuccessListener { likeDoc ->

                // ✨ 1. 중복 완벽 방어: 이미 '수락'된 상태라면 중복해서 처리하지 않고 끝냅니다!
                if (likeDoc.getString("status") == "accepted") {
                    onSuccess()
                    return@addOnSuccessListener
                }

                val fromUserId = likeDoc.getString("fromUserId").orEmpty()
                val toTeamId = likeDoc.getString("toTeamId").orEmpty()

                if (fromUserId.isBlank() || toTeamId.isBlank()) {
                    onFailure("좋아요 정보가 올바르지 않습니다.")
                    return@addOnSuccessListener
                }

                // ✨ 2. 팀이 아직 존재하는지, 현재 사용자가 해당 팀의 팀장인지 검증
                db.collection("teams").document(toTeamId).get()
                    .addOnSuccessListener { teamDoc ->
                        if (!teamDoc.exists()) {
                            onFailure("해당 팀이 더 이상 존재하지 않습니다.")
                            return@addOnSuccessListener
                        }
                        val leaderId = teamDoc.getString("leaderId").orEmpty()
                        @Suppress("UNCHECKED_CAST")
                        val memberIds = (teamDoc.get("memberIds") as? List<String>) ?: emptyList()
                        if (leaderId != currentUserId && !memberIds.contains(currentUserId)) {
                            onFailure("팀에서 탈퇴한 상태로 수락할 수 없습니다.")
                            return@addOnSuccessListener
                        }
                        acceptReceivedLikeInternal(likeId, fromUserId, toTeamId, onSuccess, onFailure)
                    }
                    .addOnFailureListener { onFailure(it.message ?: "팀 정보 확인 실패") }
            }
            .addOnFailureListener { onFailure(it.message ?: "좋아요 정보 조회 실패") }
    }

    private fun acceptReceivedLikeInternal(
        likeId: String,
        fromUserId: String,
        toTeamId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("users").document(fromUserId).get()
            .addOnSuccessListener { userDoc ->
                val mbti         = userDoc.getString("mbti").orEmpty()
                val profileImage = userDoc.getString("profileImage").orEmpty()
                val userName     = userDoc.getString("name").orEmpty().ifBlank { "새 팀원" }

                // 🔥 1. 팀에 추가
                db.collection("teams").document(toTeamId)
                    .update(mapOf(
                        "memberIds"     to FieldValue.arrayUnion(fromUserId),
                        "mbtiTags"      to FieldValue.arrayUnion(mbti),
                        "profileImages" to FieldValue.arrayUnion(profileImage)
                    ))
                    .addOnSuccessListener {
                        // 🔥 2. 유저 teamIds 배열에 추가
                        db.collection("users").document(fromUserId)
                            .update("teamIds", FieldValue.arrayUnion(toTeamId))
                            .addOnSuccessListener {
                                // 🔥 3. 좋아요 상태 변경
                                db.collection("likes").document(likeId)
                                    .update(mapOf("status" to "accepted", "respondedAt" to System.currentTimeMillis()))
                                    .addOnSuccessListener {
                                        // 🔥 4. 채팅방 참가자 추가
                                        db.collection("chats").document(toTeamId)
                                            .update("participants", FieldValue.arrayUnion(fromUserId))
                                            .addOnSuccessListener {
                                                // 🔥 5. 시스템 메시지
                                                val systemMessage = mapOf(
                                                    "senderId"  to "system",
                                                    "senderName" to "system",
                                                    "content"   to "${userName}님이 입장했습니다.",
                                                    "type"      to "system",
                                                    "createdAt" to com.google.firebase.Timestamp.now()
                                                )
                                                db.collection("chats").document(toTeamId)
                                                    .collection("messages").add(systemMessage)
                                                    .addOnSuccessListener { onSuccess() }
                                                    .addOnFailureListener { onSuccess() } // 메시지 실패해도 입장은 성공
                                            }
                                            .addOnFailureListener { onFailure(it.message ?: "채팅방 참가자 추가 실패") }
                                    }
                                    .addOnFailureListener { onFailure(it.message ?: "좋아요 상태 업데이트 실패") }
                            }
                            .addOnFailureListener { onFailure(it.message ?: "사용자 teamIds 업데이트 실패") }
                    }
                    .addOnFailureListener { onFailure(it.message ?: "팀원 추가 실패") }
            }
            .addOnFailureListener { onFailure(it.message ?: "사용자 정보 조회 실패") }
    }

    override fun rejectReceivedLike(likeId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        db.collection("likes").document(likeId)
            .update(mapOf("status" to "rejected", "respondedAt" to System.currentTimeMillis()))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "거절 처리 실패") }
    }

    override fun loadMemberNames(memberIds: List<String>, onSuccess: (List<String>) -> Unit, onFailure: (String) -> Unit) {
        if (memberIds.isEmpty()) { onSuccess(emptyList()); return }

        db.collection("users")
            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), memberIds)
            .get()
            .addOnSuccessListener { snapshot ->
                val nameMap = mutableMapOf<String, String>()
                for (doc in snapshot.documents) {
                    nameMap[doc.id] = doc.getString("name").orEmpty().ifBlank { "이름 없음" }
                }
                onSuccess(memberIds.map { nameMap[it] ?: "이름 없음" })
            }
            .addOnFailureListener { e -> onFailure(e.message ?: "팀원 이름 조회 실패") }
    }

    override fun leaveTeam(teamId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) { onFailure("로그인된 사용자가 없습니다."); return }

        // 팀 문서를 먼저 조회해 팀장 혼자인지 확인
        db.collection("teams").document(teamId).get()
            .addOnSuccessListener { teamDoc ->
                @Suppress("UNCHECKED_CAST")
                val memberIds = (teamDoc.get("memberIds") as? List<String>) ?: emptyList()
                val leaderId  = teamDoc.getString("leaderId").orEmpty()
                val isLeaderAlone = leaderId == userId && memberIds.all { it == userId }

                if (isLeaderAlone) {
                    // 팀장이 유일한 멤버 → 팀 + 채팅방 + 관련 좋아요 삭제
                    deleteTeamCompletely(teamId, userId, onSuccess, onFailure)
                } else {
                    // 일반 탈퇴: memberIds에서 본인만 제거
                    db.collection("users").document(userId).get()
                        .addOnSuccessListener { userDoc ->
                            val mbti         = userDoc.getString("mbti") ?: ""
                            val profileImage = userDoc.getString("profileImage") ?: ""

                            db.collection("teams").document(teamId)
                                .update(
                                    mapOf(
                                        "memberIds"     to FieldValue.arrayRemove(userId),
                                        "mbtiTags"      to FieldValue.arrayRemove(mbti),
                                        "profileImages" to FieldValue.arrayRemove(profileImage)
                                    )
                                )
                                .addOnSuccessListener {
                                    db.collection("users").document(userId)
                                        .update("teamIds", FieldValue.arrayRemove(teamId))
                                        .addOnSuccessListener { onSuccess() }
                                        .addOnFailureListener { onFailure(it.message ?: "teamIds 제거 실패") }
                                }
                                .addOnFailureListener { onFailure(it.message ?: "팀 탈퇴 실패") }
                        }
                        .addOnFailureListener { onFailure(it.message ?: "사용자 조회 실패") }
                }
            }
            .addOnFailureListener { onFailure(it.message ?: "팀 정보 조회 실패") }
    }

    /**
     * 팀장이 혼자인 팀을 완전히 삭제한다.
     *  1) teams/{teamId} 삭제
     *  2) chats/{teamId} 삭제 (팀 생성 시 teamId == chatId 로 만들어짐)
     *  3) likes 컬렉션에서 이 팀 관련 pending 문서 삭제
     *  4) users/{userId} 의 teamIds 에서 제거
     */
    private fun deleteTeamCompletely(
        teamId: String,
        userId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val batch = db.batch()

        // 1) teams 문서 삭제
        batch.delete(db.collection("teams").document(teamId))

        // 2) 채팅방 삭제 (teamId == chatId)
        batch.delete(db.collection("chats").document(teamId))

        batch.commit()
            .addOnSuccessListener {
                // 3) users/{userId} 의 teamIds 에서 제거
                db.collection("users").document(userId)
                    .update("teamIds", FieldValue.arrayRemove(teamId))
                    .addOnSuccessListener {
                        // 4) 이 팀을 대상으로 한 pending likes 삭제 (소량이므로 순차 처리)
                        db.collection("likes")
                            .whereEqualTo("toTeamId", teamId)
                            .get()
                            .addOnSuccessListener { snap ->
                                if (snap.isEmpty) {
                                    onSuccess()
                                } else {
                                    val likeBatch = db.batch()
                                    snap.documents.forEach { likeBatch.delete(it.reference) }
                                    likeBatch.commit()
                                        .addOnSuccessListener { onSuccess() }
                                        .addOnFailureListener { onSuccess() } // likes 삭제 실패는 무시
                                }
                            }
                            .addOnFailureListener { onSuccess() } // likes 조회 실패도 무시
                    }
                    .addOnFailureListener { onFailure(it.message ?: "teamIds 제거 실패") }
            }
            .addOnFailureListener { onFailure(it.message ?: "팀 삭제 실패") }
    }

    override fun updateTeamProfileImage(teamId: String, imageUri: Uri, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        val fileName = "team_profile/${teamId}_${UUID.randomUUID()}.jpg"
        val imageRef = storage.reference.child(fileName)

        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl
                    .addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()
                        db.collection("teams").document(teamId)
                            .update("teamProfileImage", imageUrl)
                            .addOnSuccessListener { onSuccess(imageUrl) }
                            .addOnFailureListener { onFailure(it.message ?: "팀 이미지 저장 실패") }
                    }
                    .addOnFailureListener { onFailure(it.message ?: "URL 조회 실패") }
            }
            .addOnFailureListener { onFailure(it.message ?: "이미지 업로드 실패") }
    }

    /**
     * likes 컬렉션을 실시간 구독한다.
     * 등록된 모든 ListenerRegistration을 [onRegistrations] 콜백으로 전달하므로
     * 호출 측에서 정확히 remove() 할 수 있다.
     */
    fun observeReceivedLikes(
        onUpdate: (List<ReceivedLikeItem>) -> Unit,
        onFailure: (String) -> Unit,
        onRegistrations: (List<com.google.firebase.firestore.ListenerRegistration>) -> Unit = {}
    ) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) { onFailure("로그인된 사용자가 없습니다."); return }

        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { userDoc ->
                @Suppress("UNCHECKED_CAST")
                val myTeamIds = (userDoc.get("teamIds") as? List<String>) ?: emptyList()
                if (myTeamIds.isEmpty()) { onUpdate(emptyList()); onRegistrations(emptyList()); return@addOnSuccessListener }

                val chunks = myTeamIds.chunked(10)
                // 청크별 최신 결과를 보관 — 스냅샷이 올 때마다 해당 청크 결과를 덮어씀(실시간 누적 버그 수정)
                val chunkResultsMap = java.util.concurrent.ConcurrentHashMap<Int, List<ReceivedLikeItem>>()
                // 청크별 등록 목록 — 호출 측에서 일괄 remove() 하도록 반환
                val registrations = java.util.Collections.synchronizedList(
                    mutableListOf<com.google.firebase.firestore.ListenerRegistration>()
                )

                for ((chunkIdx, chunk) in chunks.withIndex()) {
                    val reg = db.collection("likes")
                        .whereIn("toTeamId", chunk)
                        .whereEqualTo("status", "pending")
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) { onFailure(error.message ?: "실시간 구독 실패"); return@addSnapshotListener }
                            val docs = snapshot?.documents ?: emptyList()

                            if (docs.isEmpty()) {
                                chunkResultsMap[chunkIdx] = emptyList()
                                onUpdate(chunkResultsMap.values.flatten().sortedByDescending { it.createdAt })
                                return@addSnapshotListener
                            }

                            val results = java.util.Collections.synchronizedList(mutableListOf<ReceivedLikeItem>())
                            var completedCount = 0

                            for (likeDoc in docs) {
                                val likeId      = likeDoc.getString("likeId").orEmpty().ifBlank { likeDoc.id }
                                val fromUserId  = likeDoc.getString("fromUserId").orEmpty()
                                val fromTeamId  = likeDoc.getString("fromTeamId").orEmpty()
                                val toTeamId    = likeDoc.getString("toTeamId").orEmpty()
                                val createdAt   = likeDoc.getLong("createdAt") ?: 0L

                                if (fromUserId.isBlank()) {
                                    synchronized(results) { completedCount++ }
                                    if (completedCount == docs.size) {
                                        chunkResultsMap[chunkIdx] = results.toList()
                                        onUpdate(chunkResultsMap.values.flatten().sortedByDescending { it.createdAt })
                                    }
                                    continue
                                }

                                // 발신자 정보 + 대상 팀 이름 병렬 조회
                                db.collection("users").document(fromUserId).get()
                                    .addOnSuccessListener { senderDoc ->
                                        @Suppress("UNCHECKED_CAST")
                                        val profileImage = (senderDoc.get("profileImages") as? List<*>)
                                            ?.firstOrNull()?.toString()
                                            ?: senderDoc.getString("profileImageUrl")
                                            ?: senderDoc.getString("mainProfileImageUrl")
                                            ?: senderDoc.getString("profileImage")
                                            ?: ""

                                        if (toTeamId.isNotBlank()) {
                                            db.collection("teams").document(toTeamId).get()
                                                .addOnSuccessListener { teamDoc ->
                                                    // Firestore에 "teamName" 필드로 저장됨 (Team 데이터 클래스 기준)
                                                    val teamName = teamDoc.getString("teamName")
                                                        ?.takeIf { it.isNotBlank() }
                                                        ?: teamDoc.getString("name").orEmpty()
                                                    results.add(ReceivedLikeItem(
                                                        likeId = likeId,
                                                        fromUserId = fromUserId,
                                                        fromUserName = senderDoc.getString("name").orEmpty().ifBlank { "이름 없음" },
                                                        fromUserProfileImage = profileImage,
                                                        fromUserMbti = senderDoc.getString("mbti").orEmpty(),
                                                        fromUserDepartment = senderDoc.getString("department").orEmpty(),
                                                        fromTeamId = fromTeamId,
                                                        toTeamId = toTeamId,
                                                        toTeamName = teamName,
                                                        createdAt = createdAt
                                                    ))
                                                    synchronized(results) { completedCount++ }
                                                    if (completedCount == docs.size) {
                                                        chunkResultsMap[chunkIdx] = results.toList()
                                                        onUpdate(chunkResultsMap.values.flatten().sortedByDescending { it.createdAt })
                                                    }
                                                }
                                                .addOnFailureListener {
                                                    results.add(ReceivedLikeItem(likeId = likeId, fromUserId = fromUserId,
                                                        fromUserName = senderDoc.getString("name").orEmpty().ifBlank { "이름 없음" },
                                                        fromUserProfileImage = profileImage,
                                                        fromUserMbti = senderDoc.getString("mbti").orEmpty(),
                                                        fromUserDepartment = senderDoc.getString("department").orEmpty(),
                                                        fromTeamId = fromTeamId, toTeamId = toTeamId, createdAt = createdAt))
                                                    synchronized(results) { completedCount++ }
                                                    if (completedCount == docs.size) {
                                                        chunkResultsMap[chunkIdx] = results.toList()
                                                        onUpdate(chunkResultsMap.values.flatten().sortedByDescending { it.createdAt })
                                                    }
                                                }
                                        } else {
                                            results.add(ReceivedLikeItem(likeId = likeId, fromUserId = fromUserId,
                                                fromUserName = senderDoc.getString("name").orEmpty().ifBlank { "이름 없음" },
                                                fromUserProfileImage = profileImage,
                                                fromUserMbti = senderDoc.getString("mbti").orEmpty(),
                                                fromUserDepartment = senderDoc.getString("department").orEmpty(),
                                                fromTeamId = fromTeamId, createdAt = createdAt))
                                            synchronized(results) { completedCount++ }
                                            if (completedCount == docs.size) {
                                                chunkResultsMap[chunkIdx] = results.toList()
                                                onUpdate(chunkResultsMap.values.flatten().sortedByDescending { it.createdAt })
                                            }
                                        }
                                    }
                                    .addOnFailureListener {
                                        results.add(ReceivedLikeItem(likeId = likeId, fromUserId = fromUserId,
                                            fromUserName = "이름 없음", fromTeamId = fromTeamId,
                                            toTeamId = toTeamId, createdAt = createdAt))
                                        synchronized(results) { completedCount++ }
                                        if (completedCount == docs.size) {
                                            chunkResultsMap[chunkIdx] = results.toList()
                                            onUpdate(chunkResultsMap.values.flatten().sortedByDescending { it.createdAt })
                                        }
                                    }
                            }
                        }
                    registrations.add(reg)
                    // 모든 청크 리스너가 등록되면 콜백으로 전달
                    if (registrations.size == chunks.size) {
                        onRegistrations(registrations.toList())
                    }
                }
            }
            .addOnFailureListener { onFailure(it.message ?: "사용자 정보 조회 실패") }
    }
}