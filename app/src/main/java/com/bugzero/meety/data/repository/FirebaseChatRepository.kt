package com.bugzero.meety.data.repository

import com.bugzero.meety.ui.chat.ChatMessage
import com.bugzero.meety.ui.chat.ChatPreview
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
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
        val profileCache = mutableMapOf<String, Pair<String, String>>()

        val listener = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
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

                val messages = mutableListOf<ChatMessage>()
                var completed = 0

                for (doc in docs) {
                    val senderId = doc.getString("senderId") ?: ""
                    val content = doc.getString("content") ?: ""
                    val type = doc.getString("type") ?: "text"
                    val createdAt = try { doc.getTimestamp("createdAt") } catch (e: Exception) { null }
                    val isMe = senderId == currentUserId

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
                                isMe = false
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
                                isMe = isMe
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
                                        isMe = false
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
                                        isMe = false
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

        awaitClose { listener.remove() }
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
}