package com.bugzero.meety.data.repository

import com.bugzero.meety.data.model.InAppNotification
import com.bugzero.meety.ui.chat.ChatMessage
import com.bugzero.meety.ui.chat.ChatPreview
import com.bugzero.meety.ui.chat.MatchCandidate
import com.bugzero.meety.ui.chat.TeamInvitation
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirebaseChatRepository : ChatRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val notificationRepository = InAppNotificationRepository(db, auth)

    override fun observeChatList(userId: String): Flow<List<ChatPreview>> = callbackFlow {
        val listener = db.collection("chats")
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }

                val docs = snap?.documents ?: emptyList()

                if (docs.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                launch {
                    try {
                        val previews = docs.map { doc ->
                            async {
                                val type = doc.getString("type") ?: "team"
                                // ✨ 일단 DB에 저장된 방 이름을 그대로 가져옵니다.
                                val dbTeamName = doc.getString("teamName") ?: ""
                                var displayTeamName = dbTeamName
                                val participants = doc.get("participants") as? List<String> ?: emptyList()

                                // ✨ 핵심 수정: 1:1 채팅(direct)은 항상 상대방 이름으로 표시,
                                // 그룹 채팅은 저장된 이름이 기본값/비어있을 때만 동적 이름 생성
                                val isDirectChat = type == "direct"
                                val isDefaultGroupChat = type == "group" && (dbTeamName.isBlank() || dbTeamName == "알 수 없는 팀" || dbTeamName.contains(","))

                                if (isDirectChat || isDefaultGroupChat) {
                                    val otherUserIds = participants.filter { it != userId }

                                    if (otherUserIds.isNotEmpty()) {
                                        try {
                                            val otherNames = mutableListOf<String>()

                                            for (pid in otherUserIds) {
                                                val userDoc = db.collection("users").document(pid).get().await()
                                                val name = userDoc.getString("name")
                                                if (!name.isNullOrBlank()) {
                                                    otherNames.add(name)
                                                }
                                            }

                                            if (otherNames.isNotEmpty()) {
                                                displayTeamName = if (otherNames.size <= 3) {
                                                    otherNames.joinToString(", ") // 3명 이하면 전부 나열
                                                } else {
                                                    "${otherNames.take(3).joinToString(", ")} 외 ${otherNames.size - 3}명" // 4명 이상이면 줄임
                                                }
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("ChatBug", "동적 방 이름 생성 실패: ${e.message}")
                                        }
                                    }
                                }

                                // ✨ 1. 파이어베이스 DB에서 사진 주소(URL) 찾아오기 특공대!
                                val teamId = doc.getString("teamId") ?: ""

                                // 일단 채팅 문서에 'imageUrl'이나 'teamImageUrl'이 있는지 확인합니다.
                                var fetchedImageUrl = doc.getString("imageUrl") ?: doc.getString("teamImageUrl") ?: ""

                                // 만약 채팅 문서에 사진이 없고, 방금 만든 팀(teamId)이라면?
                                // -> 팀(teams) DB까지 직접 찾아가서 사진을 싹 긁어옵니다!
                                if (fetchedImageUrl.isBlank() && teamId.isNotEmpty()) {
                                    try {
                                        val teamDoc = db.collection("teams").document(teamId).get().await()
                                        fetchedImageUrl = teamDoc.getString("teamProfileImage") ?: teamDoc.getString("teamImageUrl") ?: teamDoc.getString("imageUrl") ?: ""
                                    } catch (e: Exception) {
                                        android.util.Log.e("ChatBug", "팀 사진 가져오기 실패: ${e.message}")
                                        // 팀 문서가 없거나 에러나면 조용히 패스~
                                    }
                                }

                                ChatPreview(
                                    id = doc.id,
                                    teamId = teamId,
                                    lastMessage = doc.getString("lastMessage") ?: "",
                                    lastMessageAt = try { doc.getTimestamp("lastMessageAt") } catch (e: Exception) { null },
                                    createdAt = try { doc.getTimestamp("createdAt") } catch (e: Exception) { null },
                                    teamName = displayTeamName, // ✨ 이제 "test" 같이 설정한 이름은 보호받고 그대로 들어갑니다!
                                    unreadCount = (doc.getLong("unreadCount") ?: 0L).toInt(),
                                    emoji = doc.getString("emoji") ?: "👥",
                                    type = type,
                                    participantCount = participants.size,
                                    imageUrl = fetchedImageUrl // ✨ 2. 드디어 바구니에 사진 주소 쏙 넣기 완료!
                                )
                            }
                        }.awaitAll()

                        // 시간순 예쁘게 정렬 후 화면으로 쏘기
                        val sortedPreviews = previews.sortedByDescending { it.lastMessageAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                        trySend(sortedPreviews)

                    } catch (e: Exception) {
                        android.util.Log.e("ChatBug", "채팅 목록 매핑 중 전체 에러 발생: ${e.message}")
                    }
                }
            }

        awaitClose { listener.remove() }
    }

    override fun observeMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid ?: ""
        val profileCache = mutableMapOf<String, Pair<String, String>>()

        // ✨ 사용자별 "합류 시점(memberJoinedAt)" 을 먼저 읽어온다.
        // - 값이 있으면: 그 시점 이후 메시지만 보여준다 (재입장한 멤버는 이전 기록이 가려짐)
        // - 값이 없으면(레거시 채팅방): 기존 동작 유지 (전체 메시지)
        var listenerRef: com.google.firebase.firestore.ListenerRegistration? = null
        val setupJob = launch {
            val joinedAt: Timestamp? = try {
                val chatDoc = db.collection("chats").document(chatId).get().await()
                val map = chatDoc.get("memberJoinedAt") as? Map<*, *>
                map?.get(currentUserId) as? Timestamp
            } catch (e: Exception) {
                null
            }

            var query: com.google.firebase.firestore.Query = db.collection("chats")
                .document(chatId)
                .collection("messages")
            if (joinedAt != null) {
                query = query.whereGreaterThanOrEqualTo("createdAt", joinedAt)
            }
            query = query.orderBy("createdAt", Query.Direction.ASCENDING)

            listenerRef = query.addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }

                val docs = snap?.documents ?: emptyList()

                if (docs.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val messages = mutableListOf<ChatMessage>()
                var completed = 0

                for (doc in docs) {
                    val senderId = doc.getString("senderId") ?: ""
                    val content = doc.getString("content") ?: ""
                    val type = doc.getString("type") ?: "text"
                    val createdAt = try { doc.getTimestamp("createdAt") } catch (e: Exception) { null }
                    val isMe = senderId == currentUserId
                    // 통화 로그 전용 필드
                    val callType = doc.getString("callType") ?: ""
                    val callStatus = doc.getString("callStatus") ?: ""
                    val callDurationSec = (doc.getLong("callDurationSec") ?: 0L).toInt()
                    val callerId = doc.getString("callerId") ?: ""
                    // 장소 공유 카드 전용 필드
                    val placeName = doc.getString("placeName") ?: ""
                    val placeCategory = doc.getString("placeCategory") ?: ""
                    val placeAddress = doc.getString("placeAddress") ?: ""
                    val placeImageUrl = doc.getString("placeImageUrl") ?: ""
                    val placeReviewCount = (doc.getLong("placeReviewCount") ?: 0L).toInt()
                    val placePlaceId = doc.getString("placePlaceId") ?: ""
                    val placeLat = doc.getDouble("placeLat") ?: 0.0
                    val placeLng = doc.getDouble("placeLng") ?: 0.0

                    if (senderId == "system") {
                        messages.add(
                            ChatMessage(
                                id = doc.id,
                                senderId = senderId,
                                senderName = "system",
                                senderProfileImage = "",
                                content = content,
                                type = type,
                                createdAt = createdAt,
                                isMe = false,
                                callType = callType,
                                callStatus = callStatus,
                                callDurationSec = callDurationSec,
                                callerId = callerId,
                                placeName = placeName,
                                placeCategory = placeCategory,
                                placeAddress = placeAddress,
                                placeImageUrl = placeImageUrl,
                                placeReviewCount = placeReviewCount,
                                placePlaceId = placePlaceId,
                                placeLat = placeLat,
                                placeLng = placeLng
                            )
                        )
                        completed++
                        if (completed == docs.size) {
                            trySend(messages.sortedBy { it.createdAt?.seconds ?: 0L })
                        }
                    } else if (isMe || profileCache.containsKey(senderId)) {
                        val cached = profileCache[senderId]
                        messages.add(
                            ChatMessage(
                                id = doc.id,
                                senderId = senderId,
                                senderName = cached?.first ?: "",
                                senderProfileImage = cached?.second ?: "",
                                content = content,
                                type = type,
                                createdAt = createdAt,
                                isMe = isMe,
                                callType = callType,
                                callStatus = callStatus,
                                callDurationSec = callDurationSec,
                                callerId = callerId,
                                placeName = placeName,
                                placeCategory = placeCategory,
                                placeAddress = placeAddress,
                                placeImageUrl = placeImageUrl,
                                placeReviewCount = placeReviewCount,
                                placePlaceId = placePlaceId,
                                placeLat = placeLat,
                                placeLng = placeLng
                            )
                        )
                        completed++
                        if (completed == docs.size) {
                            trySend(messages.sortedBy { it.createdAt?.seconds ?: 0L })
                        }
                    } else {
                        db.collection("users").document(senderId).get()
                            .addOnSuccessListener { userDoc ->
                                val name = userDoc.getString("name") ?: ""
                                val profileImages = userDoc.get("profileImages") as? List<*>
                                val profileImage = profileImages?.firstOrNull()?.toString() ?: ""

                                profileCache[senderId] = Pair(name, profileImage)

                                messages.add(
                                    ChatMessage(
                                        id = doc.id,
                                        senderId = senderId,
                                        senderName = name,
                                        senderProfileImage = profileImage,
                                        content = content,
                                        type = type,
                                        createdAt = createdAt,
                                        isMe = false,
                                        callType = callType,
                                        callStatus = callStatus,
                                        callDurationSec = callDurationSec,
                                        callerId = callerId,
                                        placeName = placeName,
                                        placeCategory = placeCategory,
                                        placeAddress = placeAddress,
                                        placeImageUrl = placeImageUrl,
                                        placeReviewCount = placeReviewCount,
                                        placePlaceId = placePlaceId,
                                        placeLat = placeLat,
                                        placeLng = placeLng
                                    )
                                )
                                completed++
                                if (completed == docs.size) {
                                    trySend(messages.sortedBy { it.createdAt?.seconds ?: 0L })
                                }
                            }
                            .addOnFailureListener {
                                messages.add(
                                    ChatMessage(
                                        id = doc.id,
                                        senderId = senderId,
                                        content = content,
                                        type = type,
                                        createdAt = createdAt,
                                        isMe = false,
                                        callType = callType,
                                        callStatus = callStatus,
                                        callDurationSec = callDurationSec,
                                        callerId = callerId,
                                        placeName = placeName,
                                        placeCategory = placeCategory,
                                        placeAddress = placeAddress,
                                        placeImageUrl = placeImageUrl,
                                        placeReviewCount = placeReviewCount,
                                        placePlaceId = placePlaceId,
                                        placeLat = placeLat,
                                        placeLng = placeLng
                                    )
                                )
                                completed++
                                if (completed == docs.size) {
                                    trySend(messages.sortedBy { it.createdAt?.seconds ?: 0L })
                                }
                            }
                    }
                }
            }
        }

        awaitClose {
            setupJob.cancel()
            listenerRef?.remove()
        }
    }

    override suspend fun sendMessage(chatId: String, senderId: String, content: String, type: String) {
        val now = Timestamp.now()

        val senderName = if (senderId == "system") {
            "system"
        } else {
            try {
                val userDoc = db.collection("users").document(senderId).get().await()
                userDoc.getString("name") ?: "알 수 없음"
            } catch (e: Exception) {
                "알 수 없음"
            }
        }

        val messageData = mapOf(
            "senderId" to senderId,
            "senderName" to senderName,
            "content" to content,
            "type" to type,
            "createdAt" to now
        )

        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .add(messageData)
            .await()

        // 채팅 목록 미리보기는 타입별로 가공된 텍스트를 보여줌
        val previewText = when (type) {
            "sticker" -> "(이모티콘)"
            else      -> content
        }

        db.collection("chats")
            .document(chatId)
            .update(
                mapOf(
                    "lastMessage" to previewText,
                    "lastMessageAt" to now
                )
            )
            .await()

        // 인앱 알림: 시스템 메시지가 아닌 경우 채팅방의 다른 참여자들에게 알림 전송
        if (senderId != "system") {
            try {
                val chatDoc = db.collection("chats").document(chatId).get().await()
                @Suppress("UNCHECKED_CAST")
                val participants = (chatDoc.get("participants") as? List<String>) ?: emptyList()
                val roomName = chatDoc.getString("teamName")?.takeIf { it.isNotBlank() } ?: senderName
                android.util.Log.d(
                    "ChatRepo",
                    "메시지 알림 발송: chat=$chatId sender=$senderId participants=$participants"
                )
                notificationRepository.addNotificationToMany(
                    toUserIds = participants,
                    type = InAppNotification.TYPE_MESSAGE,
                    title = "$roomName · $senderName",
                    body = previewText,
                    relatedId = chatId,
                    fromUserName = senderName
                )
            } catch (e: Exception) {
                android.util.Log.e("ChatRepo", "메시지 알림 발송 실패: ${e.message}", e)
            }
        }
    }
    override suspend fun sendPlaceCard(
        chatId: String,
        senderId: String,
        placeName: String,
        placeCategory: String,
        placeAddress: String,
        placeImageUrl: String,
        placeReviewCount: Int,
        placePlaceId: String,
        placeLat: Double,
        placeLng: Double
    ) {
        val now = Timestamp.now()

        val senderName = try {
            val userDoc = db.collection("users").document(senderId).get().await()
            userDoc.getString("name") ?: "알 수 없음"
        } catch (e: Exception) {
            "알 수 없음"
        }

        // 채팅방 하단 lastMessage 프리뷰용 텍스트
        val previewContent = "📍 $placeName"

        val messageData = mapOf(
            "senderId" to senderId,
            "senderName" to senderName,
            "content" to previewContent,
            "type" to "place_card",
            "createdAt" to now,
            "placeName" to placeName,
            "placeCategory" to placeCategory,
            "placeAddress" to placeAddress,
            "placeImageUrl" to placeImageUrl,
            "placeReviewCount" to placeReviewCount,
            "placePlaceId" to placePlaceId,
            "placeLat" to placeLat,
            "placeLng" to placeLng
        )

        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .add(messageData)
            .await()

        db.collection("chats")
            .document(chatId)
            .update(
                mapOf(
                    "lastMessage" to previewContent,
                    "lastMessageAt" to now
                )
            )
            .await()
    }

    override suspend fun transferLeadershipAndLeave(chatId: String, currentUserId: String, newLeaderId: String) {
        val chatRef = db.collection("chats").document(chatId)

        // runTransaction을 쓰면 여러 수정을 '하나의 묶음'으로 안전하게 처리합니다.
        db.runTransaction { transaction ->
            val snapshot = transaction.get(chatRef)
            val participants = snapshot.get("participants") as? MutableList<String> ?: mutableListOf()

            // 1. 참여자 명단에서 나(기존 팀장)를 제거
            participants.remove(currentUserId)

            // 2. 새로운 팀장 ID로 교체하고 참여자 명단 업데이트
            transaction.update(chatRef, mapOf(
                "leaderId" to newLeaderId,
                "participants" to participants,
                "teamName" to "" // 1:1이 아닌 팀 채팅이라면 필요에 따라 처리
            ))
        }.await()
    }

    // ── 팀원 자동 매칭 / 초대 ──────────────────────────────

    override fun observePendingInvitations(userId: String): Flow<List<TeamInvitation>> = callbackFlow {
        // 복합 인덱스 불필요: toUserId 단일 필드만 쿼리하고 status 필터는 클라이언트에서 처리
        val listener = db.collection("teamInvitations")
            .whereEqualTo("toUserId", userId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    android.util.Log.e("ChatRepo", "observePendingInvitations 에러: ${err.message}")
                    trySend(emptyList()) // 에러 시 빈 목록 전송 (flow 종료 X)
                    return@addSnapshotListener
                }
                val invitations = snap?.documents?.mapNotNull { doc ->
                    try {
                        val status = doc.getString("status") ?: "pending"
                        if (status != "pending") return@mapNotNull null  // 클라이언트 필터
                        TeamInvitation(
                            id         = doc.id,
                            teamId     = doc.getString("teamId") ?: "",
                            chatId     = doc.getString("chatId") ?: "",
                            teamName   = doc.getString("teamName") ?: "",
                            teamEmoji  = doc.getString("teamEmoji") ?: "👥",
                            fromUserId = doc.getString("fromUserId") ?: "",
                            toUserId   = doc.getString("toUserId") ?: "",
                            status     = status,
                            createdAt  = try { doc.getTimestamp("createdAt") } catch (e: Exception) { null }
                        )
                    } catch (e: Exception) { null }
                } ?: emptyList()
                trySend(invitations)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun sendTeamInvitation(
        teamId: String,
        chatId: String,
        teamName: String,
        teamEmoji: String,
        fromUserId: String,
        toUserId: String
    ) {
        // 이미 pending 초대가 존재하면 중복 발송 방지
        val existing = db.collection("teamInvitations")
            .whereEqualTo("teamId", teamId)
            .whereEqualTo("toUserId", toUserId)
            .whereEqualTo("status", "pending")
            .get()
            .await()
        if (!existing.isEmpty) {
            android.util.Log.d("ChatRepo", "이미 초대 중인 유저: $toUserId → 스킵")
            return
        }

        val data = mapOf(
            "teamId"     to teamId,
            "chatId"     to chatId,
            "teamName"   to teamName,
            "teamEmoji"  to teamEmoji,
            "fromUserId" to fromUserId,
            "toUserId"   to toUserId,
            "status"     to "pending",
            "createdAt"  to Timestamp.now()
        )
        db.collection("teamInvitations").add(data).await()
    }

    override suspend fun acceptInvitation(invitationId: String, userId: String, teamId: String, chatId: String) {
        val now = Timestamp.now()

        // 1. 초대 상태 업데이트
        db.collection("teamInvitations").document(invitationId)
            .update("status", "accepted")
            .await()

        // 2. 팀 채팅방 참여자에 추가
        //    ✨ memberJoinedAt.{userId} 도 함께 set → 재입장이면 자동으로 덮어쓰기 되어
        //       observeMessages 의 시간 필터가 이전 메시지를 가린다.
        db.collection("chats").document(chatId)
            .update(
                mapOf(
                    "participants" to FieldValue.arrayUnion(userId),
                    "memberJoinedAt.$userId" to now
                )
            )
            .await()

        // 3. 팀 memberIds에 추가
        try {
            db.collection("teams").document(teamId)
                .update("memberIds", FieldValue.arrayUnion(userId))
                .await()
        } catch (e: Exception) {
            android.util.Log.e("ChatRepo", "팀 memberIds 업데이트 실패: ${e.message}")
        }

        // 4. 사용자 teamIds에 추가
        try {
            db.collection("users").document(userId)
                .update("teamIds", FieldValue.arrayUnion(teamId))
                .await()
        } catch (e: Exception) {
            android.util.Log.e("ChatRepo", "users.teamIds 업데이트 실패: ${e.message}")
        }

        // 5. 시스템 메시지 전송
        val userDoc = db.collection("users").document(userId).get().await()
        val userName = userDoc.getString("name") ?: "새 멤버"
        val systemMsg = mapOf(
            "senderId"  to "system",
            "content"   to "${userName}님이 팀에 합류했습니다! 🎉",
            "type"      to "system",
            "createdAt" to now
        )
        db.collection("chats").document(chatId).collection("messages").add(systemMsg).await()
        db.collection("chats").document(chatId).update(
            mapOf("lastMessage" to "${userName}님이 팀에 합류했습니다! 🎉", "lastMessageAt" to now)
        ).await()
    }

    override suspend fun rejectInvitation(invitationId: String) {
        db.collection("teamInvitations").document(invitationId)
            .update("status", "rejected")
            .await()
    }

    /**
     * 팀원 자동 매칭 후보자 로드
     *
     * 알고리즘:
     * 1. 팀 문서에서 tags + mbtiTags 추출
     * 2. userPreferences 컬렉션 전체 조회 (최대 200명)
     * 3. 각 사용자의 tagScores + mbtiScores 중 팀 태그와 겹치는 점수 합산
     * 4. 점수 높은 순으로 정렬, 기존 팀원 제외, 상위 15명 추출
     * 5. users 컬렉션에서 프로필 정보 fetch
     */
    override suspend fun loadMatchCandidates(teamId: String): List<MatchCandidate> {
        return try {
            // 1. 팀 정보 가져오기
            val teamDoc = db.collection("teams").document(teamId).get().await()
            @Suppress("UNCHECKED_CAST")
            val teamTags     = (teamDoc.get("tags")     as? List<String>) ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val teamMbtiTags = (teamDoc.get("mbtiTags") as? List<String>) ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val existingMemberIds = ((teamDoc.get("memberIds") as? List<String>) ?: emptyList()).toSet()

            if (teamTags.isEmpty() && teamMbtiTags.isEmpty()) {
                android.util.Log.w("ChatRepo", "팀에 tags/mbtiTags가 없어 매칭 불가")
                return emptyList()
            }

            // 2. userPreferences 전체 조회 (최대 200명)
            val prefSnap = db.collection("userPreferences")
                .limit(200)
                .get()
                .await()

            // 3. 각 사용자 점수 계산 — 기존 팀원 제외
            data class ScoredUser(val userId: String, val score: Int)

            val scoredUsers = prefSnap.documents
                .filter { it.id !in existingMemberIds }
                .mapNotNull { doc ->
                    @Suppress("UNCHECKED_CAST")
                    val tagScores  = (doc.get("tagScores")  as? Map<String, Long>) ?: emptyMap()
                    @Suppress("UNCHECKED_CAST")
                    val mbtiScores = (doc.get("mbtiScores") as? Map<String, Long>) ?: emptyMap()

                    val tagScore  = teamTags.sumOf     { tag  -> (tagScores[tag]   ?: 0L).toInt() }
                    val mbtiScore = teamMbtiTags.sumOf { mbti -> (mbtiScores[mbti] ?: 0L).toInt() }
                    val total = tagScore + mbtiScore

                    if (total > 0) ScoredUser(doc.id, total) else null
                }
                .sortedByDescending { it.score }
                .take(15)

            if (scoredUsers.isEmpty()) return emptyList()

            // 4. users 컬렉션에서 프로필 정보 병렬 fetch
            kotlinx.coroutines.coroutineScope {
                scoredUsers.map { scored ->
                    async {
                        try {
                            val userDoc = db.collection("users").document(scored.userId).get().await()
                            if (!userDoc.exists()) return@async null
                            MatchCandidate(
                                userId          = scored.userId,
                                name            = userDoc.getString("name") ?: "이름 없음",
                                profileImageUrl = (userDoc.get("profileImages") as? List<*>)
                                                    ?.firstOrNull()?.toString() ?: "",
                                mbti            = userDoc.getString("mbti") ?: "",
                                department      = userDoc.getString("department") ?: "",
                                matchScore      = scored.score
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                }.awaitAll().filterNotNull()
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatRepo", "자동 매칭 후보자 로드 실패: ${e.message}")
            emptyList()
        }
    }
}