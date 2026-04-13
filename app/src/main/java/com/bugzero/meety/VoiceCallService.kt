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
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.bugzero.meety.data.repository.AgoraCallRepository
import com.bugzero.meety.ui.call.CallViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 백그라운드 음성 통화 Foreground Service.
 *
 * 알림에서 음성 통화 수락 시:
 *   - 앱이 백그라운드 → 이 서비스에서 Agora 오디오만 처리, 앱 안 열림
 *   - 앱이 포그라운드 → 서비스 시작 + 진행 알림에서 "앱 열기" 탭 → 통화 화면
 *
 * 진행 중 알림에서 "앱 열기" 탭 → 통화 화면 열림
 * "통화 종료" 버튼 → 통화 끊고 서비스 종료
 */
class VoiceCallService : Service() {

    companion object {
        const val ACTION_START          = "com.bugzero.meety.VOICE_START"
        const val ACTION_END            = "com.bugzero.meety.VOICE_END"
        const val ACTION_TOGGLE_MUTE    = "com.bugzero.meety.VOICE_TOGGLE_MUTE"
        const val ACTION_TOGGLE_SPEAKER = "com.bugzero.meety.VOICE_TOGGLE_SPEAKER"

        private const val CHANNEL_ID      = "meety_ongoing_call"
        private const val NOTIFICATION_ID = 9003
        private const val TAG             = "VoiceCallService"

        // ── 외부(CallScreen 등)에서 관찰할 수 있는 상태 ─────────────────────────
        private val _isRunning    = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _callState    = MutableStateFlow<VoiceCallState>(VoiceCallState.Idle)
        val callState: StateFlow<VoiceCallState> = _callState.asStateFlow()

        private val _isMuted      = MutableStateFlow(false)
        val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

        private val _isSpeakerOn  = MutableStateFlow(true)
        val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

        var currentChatId: String = ""
            private set

        var rtcEngine: RtcEngine? = null
            private set

        /** 중복 종료 방지 플래그 */
        @Volatile
        private var isEnding = false

        fun createStartIntent(context: Context, chatId: String, callType: String): Intent =
            Intent(context, VoiceCallService::class.java).apply {
                action = ACTION_START
                putExtra("chatId", chatId)
                putExtra("callType", callType)
            }

        fun createEndIntent(context: Context): Intent =
            Intent(context, VoiceCallService::class.java).apply { action = ACTION_END }
    }

    sealed class VoiceCallState {
        object Idle    : VoiceCallState()
        data class Calling(val channelName: String) : VoiceCallState()
        data class InCall(val channelName: String)  : VoiceCallState()
        object Ended   : VoiceCallState()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val callRepository = AgoraCallRepository()
    private var firestoreListener: ListenerRegistration? = null

    // ─── Agora 이벤트 ────────────────────────────────────────────────────────
    private val rtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            Log.d(TAG, "채널 입장 성공: $channel, uid=$uid")
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.d(TAG, "상대방 입장: uid=$uid")
            val cur = _callState.value
            if (cur is VoiceCallState.Calling) {
                _callState.value = VoiceCallState.InCall(cur.channelName)
                updateOngoingNotification("통화 중")
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            Log.d(TAG, "상대방 이탈: uid=$uid, reason=$reason")
            if (_callState.value is VoiceCallState.InCall) {
                endCallAndStop()
            }
        }

        override fun onError(err: Int) {
            Log.e(TAG, "Agora 에러: code=$err")
        }
    }

    // ─── Service Lifecycle ───────────────────────────────────────────────────
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START          -> handleStart(intent)
            ACTION_END            -> endCallAndStop()
            ACTION_TOGGLE_MUTE    -> toggleMute()
            ACTION_TOGGLE_SPEAKER -> toggleSpeaker()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    // ─── 통화 시작 ───────────────────────────────────────────────────────────
    private fun handleStart(intent: Intent) {
        val chatId   = intent.getStringExtra("chatId") ?: return
        val callType = intent.getStringExtra("callType") ?: "voice"

        // ★ FCM의 Firestore 리스너 정리
        MyFirebaseMessagingService.removeCallStatusListener()

        // ★ 앱이 포그라운드에 있으면 → 서비스 대신 Activity에서 통화 화면 열기
        if (MainActivity.isInForeground) {
            Log.d(TAG, "앱 포그라운드 → Activity로 통화 화면 전달")
            dismissIncomingCallNotification()

            val activityIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("chatId",         chatId)
                putExtra("callType",       callType)
                putExtra("isIncomingCall", true)
            }
            startActivity(activityIntent)

            // 서비스는 사용하지 않으므로 즉시 종료
            stopSelf()
            return
        }

        // ── 앱이 백그라운드 → 서비스에서 백그라운드 음성 통화 처리 ──────────────
        currentChatId = chatId
        isEnding = false

        // ★ Foreground 알림 먼저 시작 (Android 12+ 5초 제한)
        createNotificationChannel()
        val notification = buildOngoingNotification("연결 중...")
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            else 0
        )

        // ★ Foreground 시작 후 수신 전화 알림 제거
        dismissIncomingCallNotification()

        _isRunning.value = true
        _callState.value = VoiceCallState.Idle
        _isMuted.value = false
        _isSpeakerOn.value = true

        // Agora 엔진 초기화
        initEngine()

        // Firestore: 수락 → 채널 가입
        scope.launch {
            val channelName = callRepository.acceptCall(chatId)
            if (channelName.isNotEmpty()) {
                _callState.value = VoiceCallState.Calling(channelName)
                joinChannel(channelName)
                startFirestoreListener(chatId)
            } else {
                Log.e(TAG, "채널 이름 조회 실패 → 서비스 종료")
                endCallAndStop()
            }
        }
    }

    // ─── 수신 전화 알림 제거 ─────────────────────────────────────────────────
    private fun dismissIncomingCallNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(CallActionReceiver.NOTIFICATION_ID)       // 9001
        try { nm.cancel(null, CallActionReceiver.NOTIFICATION_ID) } catch (_: Exception) {}

        // ★ CallStyle 알림은 일부 기기에서 즉시 제거 안 되는 경우가 있어 재시도
        scope.launch {
            kotlinx.coroutines.delay(300)
            nm.cancel(CallActionReceiver.NOTIFICATION_ID)
            try { nm.cancel(null, CallActionReceiver.NOTIFICATION_ID) } catch (_: Exception) {}
        }
    }

    // ─── Agora 엔진 ──────────────────────────────────────────────────────────
    private fun initEngine() {
        if (rtcEngine != null) return
        runCatching {
            val config = RtcEngineConfig().apply {
                mContext      = applicationContext
                mAppId        = CallViewModel.AGORA_APP_ID
                mEventHandler = rtcEventHandler
            }
            rtcEngine = RtcEngine.create(config)
        }.onFailure { Log.e(TAG, "Agora 엔진 초기화 실패", it) }
    }

    private fun joinChannel(channelName: String) {
        val engine = rtcEngine ?: return
        engine.disableVideo()
        engine.enableAudio()
        engine.setEnableSpeakerphone(true)

        val options = ChannelMediaOptions().apply {
            clientRoleType        = Constants.CLIENT_ROLE_BROADCASTER
            channelProfile        = Constants.CHANNEL_PROFILE_COMMUNICATION
            publishCameraTrack    = false
            publishMicrophoneTrack = true
        }
        engine.joinChannel(null, channelName, 0, options)
    }

    // ─── Firestore 리스너 (상대방 끊기 감지) ─────────────────────────────────
    private fun startFirestoreListener(chatId: String) {
        firestoreListener?.remove()
        var isFirst = true
        firestoreListener = FirebaseFirestore.getInstance()
            .collection("calls").document(chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val status = snapshot?.getString("status") ?: return@addSnapshotListener
                if (isFirst) { isFirst = false; return@addSnapshotListener }
                if (status == "ended") endCallAndStop()
            }
    }

    // ─── 컨트롤 ──────────────────────────────────────────────────────────────
    private fun toggleMute() {
        val muted = !_isMuted.value
        _isMuted.value = muted
        rtcEngine?.muteLocalAudioStream(muted)
    }

    private fun toggleSpeaker() {
        val on = !_isSpeakerOn.value
        _isSpeakerOn.value = on
        rtcEngine?.setEnableSpeakerphone(on)
    }

    // ─── 통화 종료 + 서비스 정지 ─────────────────────────────────────────────
    private fun endCallAndStop() {
        // ★ 중복 호출 방지 (Agora onUserOffline + Firestore 리스너 동시 트리거 가능)
        if (isEnding) return
        isEnding = true

        val chatId = currentChatId
        Log.d(TAG, "endCallAndStop: chatId=$chatId")

        // ★ Firestore 리스너 먼저 제거 (재호출 방지)
        firestoreListener?.remove()
        firestoreListener = null

        // ★ Agora 먼저 정리 (오디오 즉시 중단)
        rtcEngine?.leaveChannel()
        rtcEngine?.stopPreview()
        runCatching { RtcEngine.destroy() }
        rtcEngine = null

        // 상태 즉시 업데이트
        _callState.value = VoiceCallState.Ended
        _isRunning.value = false

        // ★ Firestore 업데이트를 비동기로 처리 (메인 스레드 블로킹 방지)
        if (chatId.isNotEmpty()) {
            scope.launch {
                try {
                    callRepository.endCall(chatId)
                } catch (e: Exception) {
                    Log.e(TAG, "endCall Firestore 업데이트 실패: ${e.message}")
                }
            }
        }

        currentChatId = ""
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun cleanup() {
        firestoreListener?.remove()
        firestoreListener = null
        rtcEngine?.leaveChannel()
        rtcEngine?.stopPreview()
        runCatching { RtcEngine.destroy() }
        rtcEngine = null
        _callState.value = VoiceCallState.Ended
        _isRunning.value = false
        currentChatId = ""
        isEnding = false
    }

    // ─── 알림 ────────────────────────────────────────────────────────────────
    private fun createNotificationChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "진행 중 통화", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "음성 통화 중 표시되는 알림" }
        )
    }

    private fun buildOngoingNotification(statusText: String): android.app.Notification {
        // "앱 열기" → 통화 화면으로 이동
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("chatId",         currentChatId)
            putExtra("callType",       "voice")
            putExtra("isIncomingCall", true)
            putExtra("fromService",    true)
        }
        val openPending = PendingIntent.getActivity(
            this, NOTIFICATION_ID, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "통화 종료" 버튼
        val endIntent = Intent(this, VoiceCallService::class.java).apply { action = ACTION_END }
        val endPending = PendingIntent.getService(
            this, NOTIFICATION_ID + 1, endIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Meety 음성 통화")
            .setContentText(statusText)
            .setOngoing(true)
            .setContentIntent(openPending)
            .addAction(0, "\uD83D\uDCDE 통화 종료", endPending)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .build()
    }

    private fun updateOngoingNotification(statusText: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildOngoingNotification(statusText))
    }
}
