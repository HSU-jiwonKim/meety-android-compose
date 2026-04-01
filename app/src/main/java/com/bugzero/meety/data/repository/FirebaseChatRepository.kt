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
        val listenerA = db.collection("chats")
            .whereEqualTo("teamAId", userId)
            .addSnapshotListener { snapA, errA ->
                if (errA != null) { close(errA); return@addSnapshotListener }

                db.collection("chats")
                    .whereEqualTo("teamBId", userId)
                    .get()
                    .addOnSuccessListener { snapB ->
                        val merged = ((snapA?.documents ?: emptyList()) + snapB.documents)
                            .distinctBy { it.id }
                            .sortedByDescending { (it.get("lastMessageAt") as? Timestamp)?.seconds ?: 0L }

                        if (merged.isEmpty()) { trySend(emptyList()); return@addOnSuccessListener }

                        val previews = mutableListOf<ChatPreview>()
                        var completed = 0

                        for (doc in merged) {
                            val teamAId = doc.getString("teamAId") ?: ""
                            val teamBId = doc.getString("teamBId") ?: ""
                            val opponentId = if (teamAId == userId) teamBId else teamAId

                            db.collection("users").document(opponentId).get()
                                .addOnSuccessListener { userDoc ->
                                    previews.add(ChatPreview(
                                        id = doc.id,
                                        teamAId = teamAId,
                                        teamBId = teamBId,
                                        lastMessage = doc.getString("lastMessage") ?: "",
                                        lastMessageAt = try { doc.getTimestamp("lastMessageAt") } catch(e: Exception) { null },
                                        createdAt = try { doc.getTimestamp("createdAt") } catch(e: Exception) { null },
                                        teamName = userDoc.getString("name") ?: "알 수 없음",
                                        unreadCount = (doc.getLong("unreadCount") ?: 0L).toInt(),
                                        emoji = "💬"
                                    ))
                                    completed++
                                    if (completed == merged.size) trySend(previews.sortedByDescending { it.lastMessageAt?.seconds ?: 0L })
                                }
                                .addOnFailureListener {
                                    previews.add(ChatPreview(id = doc.id, teamAId = teamAId, teamBId = teamBId, lastMessage = doc.getString("lastMessage") ?: "", lastMessageAt = try { doc.getTimestamp("lastMessageAt") } catch(e: Exception) { null }, teamName = "알 수 없음", emoji = "💬"))
                                    completed++
                                    if (completed == merged.size) trySend(previews.sortedByDescending { it.lastMessageAt?.seconds ?: 0L })
                                }
                        }
                    }
            }
        awaitClose { listenerA.remove() }
    }

    override fun observeMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid ?: ""

        // 발신자 프로필 캐시 (매번 Firestore 조회 방지)
        val profileCache = mutableMapOf<String, Pair<String, String>>() // userId -> (name, profileImage)

        val listener = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }

                val docs = snap?.documents ?: emptyList()
                if (docs.isEmpty()) { trySend(emptyList()); return@addSnapshotListener }

                val messages = mutableListOf<ChatMessage>()
                var completed = 0

                for (doc in docs) {
                    val senderId = doc.getString("senderId") ?: ""
                    val content = doc.getString("content") ?: ""
                    val type = doc.getString("type") ?: "text"
                    val createdAt = try { doc.getTimestamp("createdAt") } catch(e: Exception) { null }
                    val isMe = senderId == currentUserId

                    if (isMe || profileCache.containsKey(senderId)) {
                        // 내 메시지이거나 캐시에 있으면 바로 추가
                        val cached = profileCache[senderId]
                        messages.add(ChatMessage(
                            id = doc.id,
                            senderId = senderId,
                            senderName = cached?.first ?: "",
                            senderProfileImage = cached?.second ?: "",
                            content = content,
                            type = type,
                            createdAt = createdAt,
                            isMe = isMe
                        ))
                        completed++
                        if (completed == docs.size) trySend(messages.toList())
                    } else {
                        // 상대방 프로필 조회
                        db.collection("users").document(senderId).get()
                            .addOnSuccessListener { userDoc ->
                                val name = userDoc.getString("name") ?: ""
                                val profileImages = userDoc.get("profileImages") as? List<*>
                                val profileImage = profileImages?.firstOrNull()?.toString() ?: ""
                                profileCache[senderId] = Pair(name, profileImage)
                                messages.add(ChatMessage(
                                    id = doc.id,
                                    senderId = senderId,
                                    senderName = name,
                                    senderProfileImage = profileImage,
                                    content = content,
                                    type = type,
                                    createdAt = createdAt,
                                    isMe = false
                                ))
                                completed++
                                if (completed == docs.size) trySend(messages.toList())
                            }
                            .addOnFailureListener {
                                messages.add(ChatMessage(id = doc.id, senderId = senderId, content = content, type = type, createdAt = createdAt, isMe = false))
                                completed++
                                if (completed == docs.size) trySend(messages.toList())
                            }
                    }
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun sendMessage(chatId: String, senderId: String, content: String, type: String) {
        val now = Timestamp.now()
        val senderName = try {
            val userDoc = db.collection("users").document(senderId).get().await()
            userDoc.getString("name") ?: "알 수 없음"
        } catch (e: Exception) { "알 수 없음" }

        val messageData = mapOf(
            "senderId" to senderId,
            "senderName" to senderName,
            "content" to content,
            "type" to type,
            "createdAt" to now
        )
        db.collection("chats").document(chatId).collection("messages").add(messageData).await()
        db.collection("chats").document(chatId).update(mapOf("lastMessage" to content, "lastMessageAt" to now)).await()
    }
}