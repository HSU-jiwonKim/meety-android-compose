package com.bugzero.meety

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.firestore.FirebaseFirestore

class CallActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ACCEPT = "com.bugzero.meety.ACTION_ACCEPT_CALL"
        const val ACTION_DECLINE = "com.bugzero.meety.ACTION_DECLINE_CALL"
        const val EXTRA_CHAT_ID   = "chatId"
        const val EXTRA_CALL_TYPE = "callType"
        const val NOTIFICATION_ID = 9001
    }

    override fun onReceive(context: Context, intent: Intent) {
        val chatId   = intent.getStringExtra(EXTRA_CHAT_ID)   ?: return
        val callType = intent.getStringExtra(EXTRA_CALL_TYPE) ?: "voice"

        // 알림 닫기
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID)

        when (intent.action) {
            ACTION_ACCEPT -> {
                // 앱 열고 전화 수락 화면으로 이동
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("chatId",         chatId)
                    putExtra("callType",       callType)
                    putExtra("isIncomingCall", true)
                }
                context.startActivity(launchIntent)
            }
            ACTION_DECLINE -> {
                // Firestore에 통화 종료 기록 (앱 안 열고 바로 처리)
                FirebaseFirestore.getInstance()
                    .collection("calls")
                    .document(chatId)
                    .update("status", "ended")
            }
        }
    }
}
