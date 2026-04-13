package com.bugzero.meety

import android.app.NotificationManager
import android.app.PendingIntent
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
        val chatId   = intent.getStringExtra(EXTRA_CHAT_ID)   ?: return
        val callType = intent.getStringExtra(EXTRA_CALL_TYPE) ?: "voice"

        // 알림 즉시 제거
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID)

        when (intent.action) {
            ACTION_ACCEPT -> {
                // BroadcastReceiver 안에서 startActivity()는 Android 10+ 에서 차단됨.
                // 해결책: MyFirebaseMessagingService에서 만든 fullScreenPending과
                // 동일한 파라미터(requestCode = NOTIFICATION_ID)로 PendingIntent를 재조회한 뒤
                // send()로 실행 → 시스템 컨텍스트에서 발동되므로 백그라운드 제한 없음.
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("chatId",         chatId)
                    putExtra("callType",       callType)
                    putExtra("isIncomingCall", true)
                }
                val pending = PendingIntent.getActivity(
                    context,
                    NOTIFICATION_ID,          // fullScreenPending과 동일한 requestCode
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                pending.send()
            }
            ACTION_DECLINE -> {
                FirebaseFirestore.getInstance()
                    .collection("calls")
                    .document(chatId)
                    .update("status", "ended")
            }
        }
    }
}