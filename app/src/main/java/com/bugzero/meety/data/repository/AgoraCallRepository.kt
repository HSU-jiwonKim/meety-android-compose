package com.bugzero.meety.data.repository

import android.util.Log
import com.bugzero.meety.data.model.InAppNotification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

/**
 * Firebase Firestore를 시그널링 서버로 활용한 통화 레포지토리. (그룹 통화 지원)
 *
 * Firestore 구조:
 *   calls/{chatId} = {
 *       callerId:    String,
 *       callType:    "video" | "voice",
 *       status:      "calling" | "active" | "ended",
 *       channelName: String,
 *       startedAt:   Long,
 *       joinedUsers: [String]   // 현재 채널에 참여 중인 사용자 UID
 *       callLogClaimedBy: String? // 통화 로그 작성 권한을 얻은 UID
 *   }
 */
class AgoraCallRepository : CallRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val listeners = mutableMapOf<String, ListenerRegistration>()
    private val notificationRepository = InAppNotificationRepository(db, auth)

    override suspend fun startCall(chatId: String, callType: String, callerId: String): String {
        return try {
            val channelName = "meety_${chatId.take(8)}_${System.currentTimeMillis()}"
            val callData = mapOf(
                "callerId"    to callerId,
                "callType"    to callType,
                "status"      to "calling",
                "channelName" to channelName,
                "startedAt"   to System.currentTimeMillis(),
                "joinedUsers" to listOf(callerId),
                "callLogClaimedBy" to ""
            )
            db.collection("calls").document(chatId).set(callData).await()

            // 인앱 알림: 채팅방 참여자(발신자 제외)에게 수신 전화 알림 추가
            try {
                val chatDoc = db.collection("chats").document(chatId).get().await()
                @Suppress("UNCHECKED_CAST")
                val participants = (chatDoc.get("participants") as? List<String>) ?: emptyList()
                val callerName = runCatching {
                    db.collection("users").document(callerId).get().await()
                        .getString("name") ?: ""
                }.getOrDefault("").ifBlank { "누군가" }

                val isVideo = callType == "video"
                val notifType = if (isVideo) InAppNotification.TYPE_VIDEO_CALL else InAppNotification.TYPE_CALL
                val callLabel = if (isVideo) "영상 통화" else "음성 통화"

                Log.d(
                    "AgoraCallRepo",
                    "통화 알림 발송: chat=$chatId caller=$callerId type=$callType participants=$participants"
                )
                notificationRepository.addNotificationToMany(
                    toUserIds = participants,
                    type = notifType,
                    title = "${callerName} 님의 $callLabel",
                    body = "${callLabel} 수신",
                    relatedId = chatId,
                    fromUserName = callerName
                )
            } catch (e: Exception) {
                Log.e("AgoraCallRepo", "통화 알림 발송 실패: ${e.message}", e)
            }

            channelName
        } catch (e: Exception) {
            Log.e("AgoraCallRepo", "startCall 실패: ${e.message}", e)
            ""
        }
    }

    override suspend fun endCall(chatId: String, userId: String, forceEndForAll: Boolean) {
        try {
            val docRef = db.collection("calls").document(chatId)
            if (forceEndForAll) {
                docRef.update(
                    mapOf(
                        "status" to "ended",
                        "joinedUsers" to FieldValue.arrayRemove(userId)
                    )
                ).await()
                return
            }

            // 내 UID를 joinedUsers에서 제거 후, 남은 사람이 없으면 status→ended
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (!snap.exists()) return@runTransaction null
                @Suppress("UNCHECKED_CAST")
                val current = (snap.get("joinedUsers") as? List<String>) ?: emptyList()
                val remaining = current.filter { it != userId }
                val newStatus = if (remaining.isEmpty()) "ended" else snap.getString("status")
                val updates = mutableMapOf<String, Any>(
                    "joinedUsers" to remaining
                )
                if (newStatus != null) updates["status"] = newStatus
                tx.update(docRef, updates)
                null
            }.await()
        } catch (e: Exception) {
            Log.e("AgoraCallRepo", "endCall 실패: ${e.message}", e)
        }
    }

    override suspend fun acceptCall(chatId: String, userId: String): String {
        return try {
            val docRef = db.collection("calls").document(chatId)
            // joinedUsers에 나를 추가하고 status를 "active"로 전이
            docRef.update(
                mapOf(
                    "status" to "active",
                    "joinedUsers" to FieldValue.arrayUnion(userId)
                )
            ).await()
            val doc = docRef.get().await()
            doc.getString("channelName") ?: ""
        } catch (e: Exception) {
            Log.e("AgoraCallRepo", "acceptCall 실패: ${e.message}", e)
            ""
        }
    }

    override fun listenForIncomingCall(
        chatId: String,
        currentUserId: String,
        onIncomingCall: (callType: String, callerId: String) -> Unit,
        onCallEnded: () -> Unit
    ) {
        listeners[chatId]?.remove()

        val listener = db.collection("calls").document(chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AgoraCallRepo", "listenForIncomingCall 오류: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) return@addSnapshotListener
                val status   = snapshot.getString("status")   ?: return@addSnapshotListener
                val callType = snapshot.getString("callType") ?: "voice"
                val callerId = snapshot.getString("callerId") ?: ""
                @Suppress("UNCHECKED_CAST")
                val joined   = (snapshot.get("joinedUsers") as? List<String>) ?: emptyList()

                when (status) {
                    "calling", "active" -> {
                        // 이미 채널에 참여 중이거나 내가 발신자면 다이얼로그 띄우지 않음
                        if (callerId != currentUserId && !joined.contains(currentUserId)) {
                            onIncomingCall(callType, callerId)
                        }
                    }
                    "ended" -> onCallEnded()
                }
            }
        listeners[chatId] = listener
    }

    override fun stopListeningForCalls(chatId: String) {
        listeners[chatId]?.remove()
        listeners.remove(chatId)
    }

    override suspend fun tryClaimCallLog(chatId: String): Boolean {
        return try {
            val docRef = db.collection("calls").document(chatId)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (!snap.exists()) return@runTransaction false
                val claimed = snap.getString("callLogClaimedBy") ?: ""
                if (claimed.isNotBlank()) return@runTransaction false
                tx.update(docRef, "callLogClaimedBy", "claimed")
                true
            }.await()
        } catch (e: Exception) {
            Log.e("AgoraCallRepo", "tryClaimCallLog 실패: ${e.message}", e)
            false
        }
    }
}
