package com.bugzero.meety

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

/**
 * Android 10+(API 29) 이상에서 BroadcastReceiver로부터 Activity를 실행하기 위한
 * 짧은 수명의 Foreground Service.
 *
 * 흐름:
 *   CallActionReceiver (수락 버튼)
 *     → startForegroundService(CallLaunchService)
 *       → startActivity(MainActivity)  // foreground service이므로 백그라운드 제한 우회
 *       → stopSelf()
 */
class CallLaunchService : Service() {

    companion object {
        private const val CHANNEL_ID = "meety_call_launch"
        private const val SERVICE_NOTIFICATION_ID = 9002

        fun createLaunchIntent(
            context: Context,
            chatId: String,
            callType: String
        ): Intent = Intent(context, CallLaunchService::class.java).apply {
            putExtra("chatId", chatId)
            putExtra("callType", callType)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Foreground 알림 채널 생성 (서비스 시작 즉시 필요)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "통화 연결",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "통화 연결 중 표시되는 알림"
            }
        )

        // Foreground Service 시작 (phoneCall 타입)
        val serviceNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("통화 연결 중...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        ServiceCompat.startForeground(
            this,
            SERVICE_NOTIFICATION_ID,
            serviceNotification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            } else {
                0
            }
        )

        // Intent에서 통화 정보 추출
        val chatId = intent?.getStringExtra("chatId") ?: ""
        val callType = intent?.getStringExtra("callType") ?: "voice"

        // MainActivity 실행 → 통화 화면으로 직행
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("chatId", chatId)
            putExtra("callType", callType)
            putExtra("isIncomingCall", true)
        }
        startActivity(launchIntent)

        // 서비스 알림 제거 및 서비스 종료
        nm.cancel(SERVICE_NOTIFICATION_ID)
        stopSelf()

        return START_NOT_STICKY
    }
}
