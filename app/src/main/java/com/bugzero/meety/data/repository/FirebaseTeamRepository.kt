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

                // ── 리더의 밸런스 답변 → 팀 balanceProfile 초기값 ──
                // 팀 생성 시점에는 리더 1명뿐이므로 리더 답변이 곧 팀 평균
                val leaderAnswers = extractBalanceAnswers(userDoc)
                val initialBalanceProfile: Map<String, Float> =
                    leaderAnswers.mapValues { it.value.toFloat() }

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
                    createdAt = System.currentTimeMillis(),
                    balanceProfile = initialBalanceProfile   // 리더 답변으로 초기화
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

                db.collection("users").document(fromUserId).get()
                    .addOnSuccessListener { userDoc ->
                        val mbti = userDoc.getString("mbti").orEmpty()
                        val profileImage = userDoc.getString("profileImage").orEmpty()
                        val userName = userDoc.getString("name").orEmpty().ifBlank { "새 팀원" }
                        // 새 팀원의 밸런스 답변 (balanceProfile 재계산에 사용)
                        val newMemberAnswers = extractBalanceAnswers(userDoc)

                        // 🔥 1. 팀에 추가
                        db.collection("teams").document(toTeamId)
                            .update(
                                mapOf(
                                    "memberIds" to FieldValue.arrayUnion(fromUserId),
                                    "mbtiTags" to FieldValue.arrayUnion(mbti),
                                    "profileImages" to FieldValue.arrayUnion(profileImage)
                                )
                            )
                            .addOnSuccessListener {

                                // 🔥 1-b. balanceProfile 재계산 (팀원 명단이 바뀌었으므로 비동기로 갱신)
                                // 실패해도 핵심 플로우(팀 합류)에 영향 없음
                                recomputeTeamBalanceProfile(toTeamId, fromUserId, newMemberAnswers) {}

                                // 🔥 2. 유저 teamIds 배열에 추가
                                db.collection("users").document(fromUserId)
                                    .update("teamIds", FieldValue.arrayUnion(toTeamId))
                                    .addOnSuccessListener {

                                        // 🔥 3. 좋아요 상태 변경
                                        db.collection("likes").document(likeId)
                                            .update(
                                                mapOf(
                                                    "status" to "accepted",
                                                    "respondedAt" to System.currentTimeMillis()
                                                )
                                            )
                                            .addOnSuccessListener {

                                                // 🔥 4. 채팅방 참가자 추가
                                                db.collection("chats").document(toTeamId)
                                                    .update("participants", FieldValue.arrayUnion(fromUserId))
                                                    .addOnSuccessListener {

                                                        // 🔥 5. 시스템 메시지 (userName 포함)
                                                        val systemMessage = mapOf(
                                                            "senderId" to "system",
                                                            "senderName" to "system",
                                                            "content" to "${userName}님이 입장했습니다.",
                                                            "type" to "system",
                                                            "createdAt" to com.google.firebase.Timestamp.now()
                                                        )

                                                        db.collection("chats")
                                                            .document(toTeamId)
                                                            .collection("messages")
                                                            .add(systemMessage)
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
            .addOnFailureListener { onFailure(it.message ?: "좋아요 조회 실패") }
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

        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val mbti = userDoc.getString("mbti") ?: ""
                val profileImage = userDoc.getString("profileImage") ?: ""

                db.collection("teams").document(teamId)
                    .update(
                        mapOf(
                            "memberIds" to FieldValue.arrayRemove(userId),
                            "mbtiTags" to FieldValue.arrayRemove(mbti),
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

    fun observeReceivedLikes(
        onUpdate: (List<ReceivedLikeItem>) -> Unit,
        onFailure: (String) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration? {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) { onFailure("로그인된 사용자가 없습니다."); return null }

        var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { userDoc ->
                @Suppress("UNCHECKED_CAST")
                val myTeamIds = (userDoc.get("teamIds") as? List<String>) ?: emptyList()
                if (myTeamIds.isEmpty()) { onUpdate(emptyList()); return@addOnSuccessListener }

                // whereIn 10개 제한 대응 chunked 처리
                val allResults = mutableListOf<ReceivedLikeItem>()
                val chunks = myTeamIds.chunked(10)
                var chunkCompleted = 0

                for (chunk in chunks) {
                    listenerRegistration = db.collection("likes")
                        .whereIn("toTeamId", chunk)
                        .whereEqualTo("status", "pending")
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) { onFailure(error.message ?: "실시간 구독 실패"); return@addSnapshotListener }
                            if (snapshot == null) { onUpdate(emptyList()); return@addSnapshotListener }

                            val docs = snapshot.documents
                            if (docs.isEmpty()) {
                                chunkCompleted++
                                if (chunkCompleted == chunks.size) onUpdate(allResults.sortedByDescending { it.createdAt })
                                return@addSnapshotListener
                            }

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
                                        if (chunkCompleted == chunks.size) onUpdate(allResults.sortedByDescending { it.createdAt })
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
                                            if (chunkCompleted == chunks.size) onUpdate(allResults.sortedByDescending { it.createdAt })
                                        }
                                    }
                                    .addOnFailureListener {
                                        results.add(ReceivedLikeItem(likeId = likeId, fromUserId = fromUserId, fromUserName = "이름 없음", fromTeamId = fromTeamId, createdAt = createdAt))
                                        completedCount++
                                        if (completedCount == docs.size) {
                                            allResults.addAll(results)
                                            chunkCompleted++
                                            if (chunkCompleted == chunks.size) onUpdate(allResults.sortedByDescending { it.createdAt })
                                        }
                                    }
                            }
                        }
                }
            }
            .addOnFailureListener { onFailure(it.message ?: "사용자 정보 조회 실패") }

        return listenerRegistration
    }

    // =====================================================================
    // 밸런스 프로필 헬퍼
    // =====================================================================

    /**
     * Firestore users 문서에서 밸런스게임 답변을 추출한다.
     *
     * 우선순위:
     *   1) users/{uid}.balanceProfile.answers — 중첩 맵 (회원가입 밸런스 게임 저장 포맷)
     *   2) users/{uid}.balanceAnswers         — 플랫 맵 (레거시)
     *
     * 값 타입은 Long(Firestore 기본) 또는 Int 모두 처리한다.
     */
    private fun extractBalanceAnswers(
        userDoc: com.google.firebase.firestore.DocumentSnapshot
    ): Map<String, Int> {
        // 1) 중첩
        val nested = (userDoc.get("balanceProfile") as? Map<*, *>)
            ?.let { (it["answers"] as? Map<*, *>) }
            ?.entries?.mapNotNull { (k, v) ->
                val key = k as? String ?: return@mapNotNull null
                val intVal: Int = when (v) {
                    is Long -> v.toInt()
                    is Int  -> v
                    else    -> return@mapNotNull null
                }
                key to intVal
            }?.toMap()
        if (!nested.isNullOrEmpty()) return nested

        // 2) 플랫
        return (userDoc.get("balanceAnswers") as? Map<*, *>)
            ?.entries?.mapNotNull { (k, v) ->
                val key = k as? String ?: return@mapNotNull null
                val intVal: Int = when (v) {
                    is Long -> v.toInt()
                    is Int  -> v
                    else    -> return@mapNotNull null
                }
                key to intVal
            }?.toMap() ?: emptyMap()
    }

    /**
     * 여러 팀원의 밸런스 답변 목록 → 축별 평균 맵 (Map<String, Float>).
     *
     * 각 축(axis)별로 모든 멤버의 값을 평균내어 팀 성향 벡터를 만든다.
     * 값 범위: -1.0 (optionA 전원) ~ +1.0 (optionB 전원).
     */
    private fun computeTeamProfile(allAnswers: List<Map<String, Int>>): Map<String, Float> {
        if (allAnswers.isEmpty()) return emptyMap()
        val accumulator = mutableMapOf<String, MutableList<Float>>()
        for (answers in allAnswers) {
            for ((axis, value) in answers) {
                accumulator.getOrPut(axis) { mutableListOf() }.add(value.toFloat())
            }
        }
        return accumulator.mapValues { (_, values) -> values.average().toFloat() }
    }

    /**
     * 팀 전체 멤버의 balanceAnswers를 읽어 팀 balanceProfile을 재계산한 뒤 Firestore에 저장한다.
     *
     * - acceptReceivedLike: 새 팀원 합류 직후 호출 (newMemberId/newMemberAnswers는 이미 갖고 있음)
     * - leaveTeam 등 멤버 변경 시점에도 호출 가능
     * - Firestore 쓰기 실패는 무시 — 핵심 플로우(팀 합류)에 영향 없음
     *
     * @param toTeamId         업데이트할 팀 ID
     * @param newMemberId      이미 추가된 새 팀원 ID (중복 조회 방지)
     * @param newMemberAnswers 새 팀원의 밸런스 답변 (이미 로드됨)
     * @param onDone           완료 콜백 (성공·실패 무관)
     */
    private fun recomputeTeamBalanceProfile(
        toTeamId: String,
        newMemberId: String,
        newMemberAnswers: Map<String, Int>,
        onDone: () -> Unit
    ) {
        db.collection("teams").document(toTeamId).get()
            .addOnSuccessListener { teamDoc ->
                @Suppress("UNCHECKED_CAST")
                val memberIds = (teamDoc.get("memberIds") as? List<String>) ?: listOf(newMemberId)
                val otherMembers = memberIds.filter { it != newMemberId }

                val allAnswers = mutableListOf<Map<String, Int>>()
                if (newMemberAnswers.isNotEmpty()) allAnswers.add(newMemberAnswers)

                if (otherMembers.isEmpty()) {
                    // 팀원이 새 멤버뿐 — 바로 저장
                    val profile = computeTeamProfile(allAnswers)
                    db.collection("teams").document(toTeamId)
                        .update("balanceProfile", profile)
                        .addOnSuccessListener { onDone() }
                        .addOnFailureListener { onDone() }
                    return@addOnSuccessListener
                }

                // 나머지 멤버 answers 병렬 조회
                var fetched = 0
                for (memberId in otherMembers) {
                    db.collection("users").document(memberId).get()
                        .addOnSuccessListener { memberDoc ->
                            val answers = extractBalanceAnswers(memberDoc)
                            if (answers.isNotEmpty()) allAnswers.add(answers)
                            fetched++
                            if (fetched == otherMembers.size) {
                                val profile = computeTeamProfile(allAnswers)
                                db.collection("teams").document(toTeamId)
                                    .update("balanceProfile", profile)
                                    .addOnSuccessListener { onDone() }
                                    .addOnFailureListener { onDone() }
                            }
                        }
                        .addOnFailureListener {
                            fetched++
                            if (fetched == otherMembers.size) {
                                val profile = computeTeamProfile(allAnswers)
                                db.collection("teams").document(toTeamId)
                                    .update("balanceProfile", profile)
                                    .addOnSuccessListener { onDone() }
                                    .addOnFailureListener { onDone() }
                            }
                        }
                }
            }
            .addOnFailureListener { onDone() }
    }
}