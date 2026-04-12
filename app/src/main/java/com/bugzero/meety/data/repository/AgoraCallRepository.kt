package com.bugzero.meety.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

/**
 * Firebase Firestore를 시그널링 서버로 활용한 통화 레포지토리.
 *
 * Firestore 구조:
 *   calls/{chatId} = {
 *       callerId: String,
 *       callType: "video" | "voice",
 *       status: "calling" | "accepted" | "ended",
 *       channelName: String,
 *       startedAt: Long
 *   }
 */
class AgoraCallRepository : CallRepository {

    private val db = FirebaseFirestore.getInstance()
    private val listeners = mutableMapOf<String, ListenerRegistration>()

    override suspend fun startCall(chatId: String, callType: String, callerId: String): String {
        return try {
            val channelName = "meety_${chatId.take(8)}_${System.currentTimeMillis()}"
            val callData = mapOf(
                "callerId"    to callerId,
                "callType"    to callType,
                "status"      to "calling",
                "channelName" to channelName,
                "startedAt"   to System.currentTimeMillis()
            )
            db.collection("calls").document(chatId).set(callData).await()
            channelName
        } catch (e: Exception) {
            Log.e("AgoraCallRepo", "startCall 실패: ${e.message}", e)
            ""
        }
    }

    override suspend fun endCall(chatId: String) {
        try {
            db.collection("calls").document(chatId)
                .update("status", "ended")
                .await()
        } catch (e: Exception) {
            Log.e("AgoraCallRepo", "endCall 실패: ${e.message}", e)
        }
    }

    override suspend fun acceptCall(chatId: String): String {
        return try {
            db.collection("calls").document(chatId)
                .update("status", "accepted")
                .await()
            val doc = db.collection("calls").document(chatId).get().await()
            doc.getString("channelName") ?: ""
        } catch (e: Exception) {
            Log.e("AgoraCallRepo", "acceptCall 실패: ${e.message}", e)
            ""
        }
    }

    override fun listenForIncomingCall(
        chatId: String,
        onIncomingCall: (callType: String, callerId: String) -> Unit
    ) {
        listeners[chatId]?.remove()

        val listener = db.collection("calls").document(chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AgoraCallRepo", "listenForIncomingCall 오류: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                val status   = snapshot.getString("status")   ?: return@addSnapshotListener
                val callType = snapshot.getString("callType") ?: "voice"
                val callerId = snapshot.getString("callerId") ?: ""
                if (status == "calling") {
                    onIncomingCall(callType, callerId)
                }
            }
        listeners[chatId] = listener
    }

    override fun stopListeningForCalls(chatId: String) {
        listeners[chatId]?.remove()
        listeners.remove(chatId)
    }
}