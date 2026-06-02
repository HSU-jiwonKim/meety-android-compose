package com.bugzero.meety.data.repository

import android.util.Log
import com.bugzero.meety.data.model.InAppNotification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * 인앱 알림(상단 알림 버튼 목록) 전용 Repository.
 *
 * 컬렉션: notifications/{id}
 *   - toUserId: 알림 수신자 UID
 *   - type: "call" / "video_call" / "message" / "like"
 *   - title, body: 표시 문구
 *   - relatedId: chatId 또는 teamId
 *   - fromUserId, fromUserName: 발신자 정보
 *   - timestamp: 생성 시각(ms)
 *
 * "읽음 = 삭제" 모델이라 별도의 read flag는 두지 않는다.
 */
class InAppNotificationRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    companion object {
        private const val TAG = "InAppNotifRepo"
        private const val COLLECTION = "notifications"
    }

    /** 내가 받은 알림을 실시간으로 관찰 (최신순) */
    fun observeMyNotifications(): Flow<List<InAppNotification>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val listener = db.collection(COLLECTION)
            .whereEqualTo("toUserId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observe 실패: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(InAppNotification::class.java)?.copy(id = doc.id)
                }.sortedByDescending { it.timestamp }
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    /**
     * 알림을 생성한다.
     * 본인에게 보낼 알림이거나 toUserId가 비었으면 무시한다.
     */
    suspend fun addNotification(
        toUserId: String,
        type: String,
        title: String,
        body: String,
        relatedId: String,
        teamId: String = "",
        fromUserId: String = auth.currentUser?.uid ?: "",
        fromUserName: String = ""
    ): Result<Unit> {
        return try {
            if (toUserId.isBlank()) {
                Log.w(TAG, "addNotification skip: toUserId 비어있음 (type=$type)")
                return Result.success(Unit)
            }
            if (toUserId == auth.currentUser?.uid) {
                Log.d(TAG, "addNotification skip: 본인에게 발송 (type=$type)")
                return Result.success(Unit)
            }

            val id = UUID.randomUUID().toString()
            val notif = InAppNotification(
                id = id,
                toUserId = toUserId,
                type = type,
                title = title,
                body = body,
                relatedId = relatedId,
                teamId = teamId,
                fromUserId = fromUserId,
                fromUserName = fromUserName,
                timestamp = System.currentTimeMillis()
            )
            db.collection(COLLECTION).document(id).set(notif).await()
            Log.d(TAG, "addNotification OK: to=$toUserId type=$type id=$id")
            Result.success(Unit)
        } catch (e: Exception) {
            // Firestore 보안 규칙에서 막혔다면 PERMISSION_DENIED 가 찍힘
            Log.e(TAG, "addNotification 실패 (to=$toUserId type=$type): ${e.javaClass.simpleName} ${e.message}", e)
            Result.failure(e)
        }
    }

    /** 여러 사용자에게 한번에 알림 발송 (toUserIds에서 본인은 자동 제외) */
    suspend fun addNotificationToMany(
        toUserIds: List<String>,
        type: String,
        title: String,
        body: String,
        relatedId: String,
        teamId: String = "",
        fromUserName: String = ""
    ) {
        val myUid = auth.currentUser?.uid
        val recipients = toUserIds
            .filter { it.isNotBlank() && it != myUid }
            .distinct()

        Log.d(TAG, "addNotificationToMany type=$type recipients=${recipients.size} (입력=${toUserIds.size}) title=$title")

        recipients.forEach { uid ->
            addNotification(
                toUserId = uid,
                type = type,
                title = title,
                body = body,
                relatedId = relatedId,
                teamId = teamId,
                fromUserId = myUid ?: "",
                fromUserName = fromUserName
            )
        }
    }

    /** 알림 하나 삭제 (= 그 알림 한 건만 읽음 처리) */
    suspend fun deleteOne(notificationId: String): Result<Unit> {
        return try {
            if (notificationId.isBlank()) return Result.success(Unit)
            db.collection(COLLECTION).document(notificationId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteOne 실패: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * relatedId(= likeId 등)가 일치하는 내 알림을 모두 삭제한다.
     * 채팅 목록 화면 등에서 notificationId 없이 likeId만 알 때 사용.
     */
    suspend fun deleteByRelatedId(relatedId: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.success(Unit)
            if (relatedId.isBlank()) return Result.success(Unit)
            val snap = db.collection(COLLECTION)
                .whereEqualTo("toUserId", uid)
                .whereEqualTo("relatedId", relatedId)
                .get().await()
            val batch = db.batch()
            snap.documents.forEach { batch.delete(it.reference) }
            if (!snap.isEmpty) batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteByRelatedId 실패: ${e.message}")
            Result.failure(e)
        }
    }

    /** 내 알림 전부 삭제 (= 알림 목록을 모두 읽음 처리) */
    suspend fun deleteAllMine(): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.success(Unit)
            val snap = db.collection(COLLECTION)
                .whereEqualTo("toUserId", uid)
                .get().await()
            if (snap.isEmpty) return Result.success(Unit)

            // 500개 단위로 batch 분할 (Firestore 제한)
            snap.documents.chunked(450).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteAllMine 실패: ${e.message}")
            Result.failure(e)
        }
    }
}
