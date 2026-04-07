package com.bugzero.meety.data.repository

import com.bugzero.meety.ui.chat.ChatMessage
import com.bugzero.meety.ui.chat.ChatPreview
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

                val previews = docs.map { doc ->
                    ChatPreview(
                        id = doc.id,
                        teamId = doc.getString("teamId") ?: "",
                        lastMessage = doc.getString("lastMessage") ?: "",
                        lastMessageAt = try { doc.getTimestamp("lastMessageAt") } catch (e: Exception) { null },
                        createdAt = try { doc.getTimestamp("createdAt") } catch (e: Exception) { null },
                        teamName = doc.getString("teamName") ?: "알 수 없는 팀",
                        unreadCount = (doc.getLong("unreadCount") ?: 0L).toInt(),
                        emoji = doc.getString("emoji") ?: "👥",
                        type = doc.getString("type") ?: "team"
                    )
                }.sortedByDescending { it.lastMessageAt?.seconds ?: it.createdAt?.seconds ?: 0L }

                trySend(previews)
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
}