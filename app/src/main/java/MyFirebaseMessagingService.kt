package com.bugzero.meety

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import com.bugzero.meety.CallActionReceiver.Companion.ACTION_ACCEPT
import com.bugzero.meety.CallActionReceiver.Companion.ACTION_DECLINE
import com.bugzero.meety.CallActionReceiver.Companion.EXTRA_CALL_TYPE
import com.bugzero.meety.CallActionReceiver.Companion.EXTRA_CHAT_ID
import com.bugzero.meety.CallActionReceiver.Companion.NOTIFICATION_ID
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

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

    // ─── 채팅 알림 : 탭하면 해당 채팅방으로 바로 이동 ───────────────────────
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

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        nm.notify(chatId.hashCode(), notification)
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

        // 수락 버튼 → 직접 MainActivity 열기
        // ⚠️ BroadcastReceiver를 거치면 Android 10+ 백그라운드 Activity 시작 제한에 막힘
        //    → PendingIntent.getActivity()로 직접 열어야 앱이 뜸
        val acceptIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
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
            .setOngoing(true)  // 스와이프로 못 지우게 (통화 알림 유지)

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
    }
}
