package com.bugzero.meety.data.repository

import com.bugzero.meety.ui.chat.ChatMessage
import com.bugzero.meety.ui.chat.ChatPreview
import com.bugzero.meety.ui.chat.MatchCandidate
import com.bugzero.meety.ui.chat.TeamInvitation
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.concurrent.ConcurrentHashMap

import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirebaseChatRepository : ChatRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // 유저 이름/프로필 캐시 — observeMessages, observeChatList, sendMessage 공통 사용
    private val userProfileCache = ConcurrentHashMap<String, Pair<String, String>>() // userId → (name, profileImageUrl)

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
                                val dbTeamName = doc.getString("teamName") ?: ""
                                var displayTeamName = dbTeamName
                                val participants = doc.get("participants") as? List<String> ?: emptyList()

                                val isDirectChat = type == "direct"
                                val isDefaultGroupChat = type == "group" && (dbTeamName.isBlank() || dbTeamName == "알 수 없는 팀" || dbTeamName.contains(","))

                                if (isDirectChat || isDefaultGroupChat) {
                                    val otherUserIds = participants.filter { it != userId }

                                    if (otherUserIds.isNotEmpty()) {
                                        try {
                                            // 병렬 fetch + 캐시 활용
                                            val otherNames = coroutineScope {
                                                otherUserIds.map { pid ->
                                                    async {
                                                        userProfileCache.getOrPut(pid) {
                                                            val userDoc = db.collection("users").document(pid).get().await()
                                                            val name = userDoc.getString("name") ?: ""
                                                            val img = (userDoc.get("profileImages") as? List<*>)?.firstOrNull()?.toString() ?: ""
                                                            Pair(name, img)
                                                        }.first
                                                    }
                                                }.awaitAll().filter { it.isNotBlank() }
                                            }

                                            if (otherNames.isNotEmpty()) {
                                                displayTeamName = if (otherNames.size <= 3) {
                                                    otherNames.joinToString(", ")
                                                } else {
                                                    "${otherNames.take(3).joinToString(", ")} 외 ${otherNames.size - 3}명"
                                                }
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("ChatBug", "동적 방 이름 생성 실패: ${e.message}")
                                        }
                                    }
                                }

                                ChatPreview(
                                    id = doc.id,
                                    teamId = doc.getString("teamId") ?: "",
                                    lastMessage = doc.getString("lastMessage") ?: "",
                                    lastMessageAt = try { doc.getTimestamp("lastMessageAt") } catch (e: Exception) { null },
                                    createdAt = try { doc.getTimestamp("createdAt") } catch (e: Exception) { null },
                                    teamName = displayTeamName, // ✨ 이제 "test" 같이 설정한 이름은 보호받고 그대로 들어갑니다!
                                    unreadCount = (doc.getLong("unreadCount") ?: 0L).toInt(),
                                    emoji = doc.getString("emoji") ?: "👥",
                                    type = type,
                                    participantCount = participants.size
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
        var processingJob: Job? = null  // 스냅샷마다 이전 작업 취소 후 재시작

        val listener = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }

                val docs = snap?.documents ?: emptyList()
                if (docs.isEmpty()) { trySend(emptyList()); return@addSnapshotListener }

                // 이전 처리 중인 코루틴 취소 → 최신 스냅샷만 처리
                processingJob?.cancel()
                processingJob = launch {
                    try {
                        val messages = coroutineScope {
                            docs.map { doc ->
                                async {
                                    val senderId   = doc.getString("senderId") ?: ""
                                    val content    = doc.getString("content") ?: ""
                                    val type       = doc.getString("type") ?: "text"
                                    val createdAt  = try { doc.getTimestamp("createdAt") } catch (e: Exception) { null }
                                    val isMe       = senderId == currentUserId

                                    if (senderId == "system") {
                                        return@async ChatMessage(
                                            id = doc.id, senderId = "system", senderName = "system",
                                            content = content, type = type, createdAt = createdAt, isMe = false
                                        )
                                    }

                                    // 클래스 레벨 캐시 공유 — 중복 Firestore 읽기 방지
                                    val (name, profileImage) = userProfileCache.getOrPut(senderId) {
                                        try {
                                            val userDoc = db.collection("users").document(senderId).get().await()
                                            val n   = userDoc.getString("name") ?: ""
                                            val img = (userDoc.get("profileImages") as? List<*>)?.firstOrNull()?.toString() ?: ""
                                            Pair(n, img)
                                        } catch (e: Exception) { Pair("", "") }
                                    }

                                    ChatMessage(
                                        id = doc.id, senderId = senderId, senderName = name,
                                        senderProfileImage = profileImage, content = content,
                                        type = type, createdAt = createdAt, isMe = isMe
                                    )
                                }
                            }.awaitAll()
                        }
                        trySend(messages.sortedBy { it.createdAt?.seconds ?: 0L })
                    } catch (e: Exception) {
                        android.util.Log.e("ChatRepo", "메시지 처리 에러: ${e.message}")
                    }
                }
            }

        awaitClose { listener.remove() }
    }

    override suspend fun sendMessage(chatId: String, senderId: String, content: String, type: String) {
        val now = Timestamp.now()

        val senderName = if (senderId == "system") {
            "system"
        } else {
            // 캐시 우선 조회 — 없을 때만 Firestore fetch 후 캐시 저장
            userProfileCache.getOrPut(senderId) {
                try {
                    val userDoc = db.collection("users").document(senderId).get().await()
                    val name = userDoc.getString("name") ?: "알 수 없음"
                    val img  = (userDoc.get("profileImages") as? List<*>)?.firstOrNull()?.toString() ?: ""
                    Pair(name, img)
                } catch (e: Exception) { Pair("알 수 없음", "") }
            }.first
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

        db.collection("chats")
            .document(chatId)
            .update(
                mapOf(
                    "lastMessage" to content,
                    "lastMessageAt" to now
                )
            )
            .await()
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
        db.collection("chats").document(chatId)
            .update("participants", FieldValue.arrayUnion(userId))
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
            val teamMbtiTags = (teamDoc.get("mbtiTags") as? List<String>) ?: emptyList()
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

            // 4. users 컬렉션에서 프로필 정보 병렬 fetch (순차 → coroutineScope + async)
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