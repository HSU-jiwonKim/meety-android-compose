package com.bugzero.meety

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.firestore.FirebaseFirestore

class CallActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ACCEPT  = "com.bugzero.meety.ACTION_ACCEPT_CALL"
        const val ACTION_DECLINE = "com.bugzero.meety.ACTION_DECLINE_CALL"
        const val EXTRA_CHAT_ID   = "chatId"
        const val EXTRA_CALL_TYPE = "callType"
        const val NOTIFICATION_ID = 9001
    }

    override fun onReceive(context: Context, intent: Intent) {
        val chatId = intent.getStringExtra(EXTRA_CHAT_ID) ?: return

        // 알림 즉시 제거
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID)
        try { nm.cancel(null, NOTIFICATION_ID) } catch (_: Exception) {}

        // Firestore 상태 감시 리스너 정리 (수락/거절 모두)
        MyFirebaseMessagingService.removeCallStatusListener()

        when (intent.action) {
            ACTION_ACCEPT -> { /* 알림 제거 완료 — Activity는 시스템이 직접 실행 */ }

            ACTION_DECLINE -> {
                FirebaseFirestore.getInstance()
                    .collection("calls")
                    .document(chatId)
                    .update("status", "ended")
            }
        }
    }
}