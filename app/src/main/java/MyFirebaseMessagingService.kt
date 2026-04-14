package com.bugzero.meety

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import com.bugzero.meety.CallActionReceiver.Companion.ACTION_DECLINE
import com.bugzero.meety.CallActionReceiver.Companion.EXTRA_CHAT_ID
import com.bugzero.meety.CallActionReceiver.Companion.NOTIFICATION_ID
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        /** 현재 활성 중인 수신 전화 리스너 (상대방 끊기 감지용) */
        private var callStatusListener: ListenerRegistration? = null

        private const val CHAT_GROUP_KEY         = "meety_chat_group"
        private const val MISSED_CALL_GROUP_KEY  = "meety_missed_call_group"
        private const val CHAT_SUMMARY_ID        = 99900
        private const val MISSED_CALL_SUMMARY_ID = 99901
        private const val PREFS_NAME             = "meety_notification_prefs"

        /** 리스너 정리 (수락/거절 시 외부에서 호출) */
        fun removeCallStatusListener() {
            callStatusListener?.remove()
            callStatusListener = null
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        saveTokenToFirestore(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val type       = remoteMessage.data["type"]
        val chatId     = remoteMessage.data["chatId"]     ?: ""
        val callType   = remoteMessage.data["callType"]   ?: "voice"
        val callerName = remoteMessage.data["callerName"] ?: "Meety"
        val callLabel  = remoteMessage.data["callLabel"]  ?: if (callType == "video") "영상 통화" else "음성 통화"

        when (type) {
            "incoming_call" -> showCallNotification(callerName, callLabel, chatId, callType)
            "call_cancelled", "call_ended" -> {
                // 상대방이 전화를 끊음 → 수락/거절 알림 제거 + 부재중 알림 표시
                dismissCallNotification()
                showMissedCallNotification(callerName, callLabel, chatId, callType)
            }
            else -> {
                val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "새 메시지"
                val body  = remoteMessage.notification?.body  ?: remoteMessage.data["body"]  ?: ""
                showChatNotification(title, body, chatId)
            }
        }
    }

    private fun saveTokenToFirestore(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .update("fcmToken", token)
    }

    // ─── 채팅 알림 : 같은 사람 = 1개로 덮어씀, 2개부터 그룹 묶음 ────────────
    private fun showChatNotification(title: String, body: String, chatId: String) {
        val channelId = "meety_chat"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(channelId, "채팅 알림", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "새 메시지 알림" }
        )

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("chatId", chatId)
            putExtra("type", "chat")
        }
        val pendingIntent = PendingIntent.getActivity(
            this, chatId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 같은 chatId → 같은 notification ID → 최신 메시지로 덮어씀 (같은 사람 = 알림 1개)
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup(CHAT_GROUP_KEY)
            .build()

        nm.notify(chatId.hashCode(), notification)

        // ★ 요약 알림: 현재 활성 채팅 알림이 2개 이상이면 그룹 묶음 표시
        val activeChatCount = nm.activeNotifications.count { it.notification.group == CHAT_GROUP_KEY && it.id != CHAT_SUMMARY_ID }
        if (activeChatCount >= 2) {
            val summaryNotification = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Meety")
                .setContentText("새 메시지 ${activeChatCount}개")
                .setAutoCancel(true)
                .setGroup(CHAT_GROUP_KEY)
                .setGroupSummary(true)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            nm.notify(CHAT_SUMMARY_ID, summaryNotification)
        }
    }

    // ─── 수신 전화 알림 제거 (상대방 끊기 시) ──────────────────────────────────
    private fun dismissCallNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID)
        try { nm.cancel(null, NOTIFICATION_ID) } catch (_: Exception) {}
    }

    // ─── 부재중 전화 알림 (같은 사람 = 건수 표시, 2개부터 그룹 묶음) ────────
    private fun showMissedCallNotification(
        callerName: String,
        callLabel:  String,
        chatId:     String,
        callType:   String
    ) {
        val channelId = "meety_missed_call"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(channelId, "부재중 전화", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "부재중 전화 알림" }
        )

        // ★ 부재중 건수 추적 (SharedPreferences에 chatId별 카운트 저장)
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val countKey = "missed_count_$chatId"
        val missedCount = prefs.getInt(countKey, 0) + 1
        prefs.edit().putInt(countKey, missedCount).apply()

        // 알림 탭 → 채팅방으로 이동 + 부재중 카운트 초기화
        val chatIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("chatId", chatId)
            putExtra("type",   "chat")
            putExtra("clearMissedCount", true)
            putExtra("missedChatId",     chatId)
        }
        val chatPending = PendingIntent.getActivity(
            this, NOTIFICATION_ID + 10 + Math.abs(chatId.hashCode() % 500), chatIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "다시 전화" 액션 버튼 → 바로 통화 화면으로 이동
        val callBackIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("chatId",           chatId)
            putExtra("callType",         callType)
            putExtra("isCallBack",       true)
            putExtra("clearMissedCount", true)
            putExtra("missedChatId",     chatId)
        }
        val callBackPending = PendingIntent.getActivity(
            this, NOTIFICATION_ID + 510 + Math.abs(chatId.hashCode() % 500), callBackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ★ 같은 chatId → 같은 notification ID → 같은 사람의 부재중은 1개 (건수 업데이트)
        val missedCallNotificationId = NOTIFICATION_ID + 100 + Math.abs(chatId.hashCode() % 1000)

        val contentText = if (missedCount > 1) {
            "$callerName 님의 $callLabel (${missedCount}건)"
        } else {
            "$callerName 님의 $callLabel"
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("부재중 전화")
            .setContentText(contentText)
            .setSubText(if (missedCount > 1) "${missedCount}건" else null)
            .setNumber(missedCount)
            .setAutoCancel(true)
            .setContentIntent(chatPending)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, "\uD83D\uDCDE 다시 전화", callBackPending)
            .setGroup(MISSED_CALL_GROUP_KEY)
            .build()

        nm.notify(missedCallNotificationId, notification)

        // ★ 요약 알림: 활성 부재중 알림이 2개 이상(다른 사람들)이면 그룹 묶음 표시
        val activeMissedCount = nm.activeNotifications.count {
            it.notification.group == MISSED_CALL_GROUP_KEY && it.id != MISSED_CALL_SUMMARY_ID
        }
        if (activeMissedCount >= 2) {
            val summaryNotification = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("부재중 전화")
                .setContentText("부재중 전화 ${activeMissedCount}건")
                .setAutoCancel(true)
                .setGroup(MISSED_CALL_GROUP_KEY)
                .setGroupSummary(true)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            nm.notify(MISSED_CALL_SUMMARY_ID, summaryNotification)
        }
    }

    // ─── 전화 알림 ──────────────────────────────────────────────────────────────
    private fun showCallNotification(
        callerName: String,
        callLabel:  String,
        chatId:     String,
        callType:   String
    ) {
        val channelId = "meety_call"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(channelId, "전화 알림", NotificationManager.IMPORTANCE_HIGH)
                .apply {
                    description = "수신 전화 알림"
                    enableVibration(true)
                }
        )

        // 알림 탭 / fullScreenIntent → 앱 열고 통화 화면
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("chatId",         chatId)
            putExtra("callType",       callType)
            putExtra("isIncomingCall", true)
        }
        val fullScreenPending = PendingIntent.getActivity(
            this, NOTIFICATION_ID, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 수락 버튼 → 음성/영상 모두 앱 켜지고 통화 화면으로 이동
        val acceptIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("chatId",         chatId)
            putExtra("callType",       callType)
            putExtra("isIncomingCall", true)
        }
        val acceptPending = PendingIntent.getActivity(
            this, NOTIFICATION_ID + 1, acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 거절 버튼 → CallActionReceiver
        val declineIntent = Intent(this, CallActionReceiver::class.java).apply {
            action = ACTION_DECLINE
            putExtra(EXTRA_CHAT_ID, chatId)
        }
        val declinePending = PendingIntent.getBroadcast(
            this, NOTIFICATION_ID + 2, declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Android 14+에서 USE_FULL_SCREEN_INTENT 권한이 없으면 CallStyle 대신 일반 버튼 방식 사용
        val canUseFullScreen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            NotificationManagerCompat.from(this).canUseFullScreenIntent()
        } else {
            true // Android 13 이하는 항상 허용
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(fullScreenPending)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)

        if (canUseFullScreen) {
            // ── CallStyle: 초록 수락 / 빨간 거절 다이얼 버튼 ──────────────────
            val caller = Person.Builder()
                .setName("$callerName ($callLabel)")
                .setImportant(true)
                .build()

            builder
                .setFullScreenIntent(fullScreenPending, true)
                .setStyle(
                    NotificationCompat.CallStyle
                        .forIncomingCall(caller, declinePending, acceptPending)
                        .setAnswerButtonColorHint(0xFF4CAF50.toInt())   // 초록
                        .setDeclineButtonColorHint(0xFFF44336.toInt())  // 빨강
                )
        } else {
            // ── Fallback: 일반 알림 + 수락/거절 액션 버튼 ────────────────────
            builder
                .setContentTitle("$callerName ($callLabel)")
                .setContentText("수신 전화")
                .addAction(0, "✅ 수락", acceptPending)
                .addAction(0, "❌ 거절", declinePending)
        }

        nm.notify(NOTIFICATION_ID, builder.build())

        // ── Firestore 리스너: 상대방이 전화를 끊으면(status→ended) 알림 자동 제거 ──
        startCallStatusListener(callerName, callLabel, chatId, callType)
    }

    /**
     * calls/{chatId} 문서의 status 변화를 실시간 감시.
     * status가 "ended"로 바뀌면 수신 전화 알림을 제거하고 부재중 알림을 표시한다.
     * 수락/거절 시에는 removeCallStatusListener()로 정리.
     */
    private fun startCallStatusListener(
        callerName: String,
        callLabel:  String,
        chatId:     String,
        callType:   String
    ) {
        // 기존 리스너가 있으면 제거
        removeCallStatusListener()

        // 첫 스냅샷(이전 통화의 잔여 상태)을 무시하기 위한 플래그
        var isFirstSnapshot = true

        callStatusListener = FirebaseFirestore.getInstance()
            .collection("calls")
            .document(chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FCM_CallListener", "리스너 오류: ${error.message}")
                    return@addSnapshotListener
                }
                val status = snapshot?.getString("status") ?: return@addSnapshotListener

                // 첫 스냅샷: "calling" 상태가 맞는지만 확인하고 무시
                // (이전 통화의 "ended"가 남아있으면 바로 부재중 처리되는 것 방지)
                if (isFirstSnapshot) {
                    isFirstSnapshot = false
                    // 첫 스냅샷이 이미 "calling"이면 정상 → 다음 변경부터 감시
                    // 첫 스냅샷이 "ended"면 이전 통화 잔여 → 무시
                    if (status == "calling") {
                        Log.d("FCM_CallListener", "통화 감시 시작: chatId=$chatId")
                    }
                    return@addSnapshotListener
                }

                // 두 번째 스냅샷부터 상태 변화에 반응
                when (status) {
                    "ended" -> {
                        // 상대방이 끊음 → 수신 전화 알림 제거 + 부재중 알림
                        dismissCallNotification()
                        showMissedCallNotification(callerName, callLabel, chatId, callType)
                        removeCallStatusListener()
                    }
                    "accepted" -> {
                        // 본인이 수락함 → 리스너만 정리
                        removeCallStatusListener()
                    }
                }
            }
    }
}