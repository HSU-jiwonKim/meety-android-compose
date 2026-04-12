package com.bugzero.meety.ui.call

import android.app.Application
import android.view.SurfaceView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bugzero.meety.data.repository.AgoraCallRepository
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ─── 통화 UI 상태 ────────────────────────────────────────────────────────────
sealed class CallUiState {
    object Idle : CallUiState()
    data class Calling(val callType: String, val channelName: String) : CallUiState()
    data class InCall(val callType: String, val channelName: String) : CallUiState()
    object Ended : CallUiState()
}

class CallViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        // ⚠️ console.agora.io 에서 발급받은 App ID로 반드시 교체하세요
        const val AGORA_APP_ID = "f7ef982264fc4aba8c8dc497c1c6c4cf"
    }

    private val callRepository = AgoraCallRepository()

    // ─── 통화 상태 ────────────────────────────────────────────────────────────
    private val _callUiState = MutableStateFlow<CallUiState>(CallUiState.Idle)
    val callUiState: StateFlow<CallUiState> = _callUiState.asStateFlow()

    // ─── 컨트롤 상태 ──────────────────────────────────────────────────────────
    private val _isMuted      = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isCameraOff  = MutableStateFlow(false)
    val isCameraOff: StateFlow<Boolean> = _isCameraOff.asStateFlow()

    private val _isSpeakerOn  = MutableStateFlow(true)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    // ─── 원격 사용자 UID (Agora에서 자동 부여) ───────────────────────────────
    private val _remoteUid = MutableStateFlow(0)
    val remoteUid: StateFlow<Int> = _remoteUid.asStateFlow()

    // ─── 채팅방에서 수신 통화 감지용 (chatId → callType to callerId) ─────────
    private val _incomingCallState = MutableStateFlow<Pair<String, String>?>(null)
    val incomingCallState: StateFlow<Pair<String, String>?> = _incomingCallState.asStateFlow()

    private var rtcEngine: RtcEngine? = null
    private var currentChatId = ""

    // ─── Agora 이벤트 핸들러 ──────────────────────────────────────────────────
    private val rtcEventHandler = object : IRtcEngineEventHandler() {

        /** 상대방이 채널에 입장 → 통화 중 상태로 전환 */
        override fun onUserJoined(uid: Int, elapsed: Int) {
            _remoteUid.value = uid
            val current = _callUiState.value
            if (current is CallUiState.Calling) {
                _callUiState.value = CallUiState.InCall(current.callType, current.channelName)
            }
        }

        /** 상대방이 채널 이탈 → 통화 종료 */
        override fun onUserOffline(uid: Int, reason: Int) {
            _remoteUid.value = 0
            viewModelScope.launch { endCall() }
        }
    }

    // ─── 엔진 초기화 ──────────────────────────────────────────────────────────
    fun initEngine() {
        if (rtcEngine != null) return
        runCatching {
            val config = RtcEngineConfig().apply {
                mContext      = getApplication()
                mAppId        = AGORA_APP_ID
                mEventHandler = rtcEventHandler
            }
            rtcEngine = RtcEngine.create(config)
        }.onFailure { it.printStackTrace() }
    }

    // ─── 발신 ─────────────────────────────────────────────────────────────────
    fun startCall(chatId: String, callType: String, callerId: String) {
        currentChatId = chatId
        viewModelScope.launch {
            val channelName = callRepository.startCall(chatId, callType, callerId)
            _callUiState.value = CallUiState.Calling(callType, channelName)
            joinChannel(channelName, callType)
        }
    }

    // ─── 수신 수락 ────────────────────────────────────────────────────────────
    fun acceptCall(chatId: String, callType: String) {
        currentChatId = chatId
        viewModelScope.launch {
            val channelName = callRepository.acceptCall(chatId)
            _callUiState.value = CallUiState.Calling(callType, channelName)
            joinChannel(channelName, callType)
            _incomingCallState.value = null
        }
    }

    // ─── 수신 거절 ────────────────────────────────────────────────────────────
    fun declineCall(chatId: String) {
        viewModelScope.launch {
            callRepository.endCall(chatId)
            _incomingCallState.value = null
        }
    }

    // ─── 통화 종료 ────────────────────────────────────────────────────────────
    fun endCall() {
        viewModelScope.launch {
            if (currentChatId.isNotEmpty()) callRepository.endCall(currentChatId)
            leaveChannel()
            _callUiState.value = CallUiState.Ended
        }
    }

    // ─── 수신 통화 감지 시작/중단 (ChatRoomScreen에서 사용) ──────────────────
    fun listenForIncomingCall(chatId: String, currentUserId: String) {
        callRepository.listenForIncomingCall(chatId) { callType, callerId ->
            if (callerId != currentUserId) {
                _incomingCallState.value = callType to callerId
            }
        }
    }

    fun stopListeningForCalls(chatId: String) {
        callRepository.stopListeningForCalls(chatId)
    }

    fun clearIncomingCall() {
        _incomingCallState.value = null
    }

    // ─── 컨트롤 ───────────────────────────────────────────────────────────────
    fun toggleMute() {
        val muted = !_isMuted.value
        _isMuted.value = muted
        rtcEngine?.muteLocalAudioStream(muted)
    }

    fun toggleCamera() {
        val off = !_isCameraOff.value
        _isCameraOff.value = off
        rtcEngine?.muteLocalVideoStream(off)
    }

    fun toggleSpeaker() {
        val on = !_isSpeakerOn.value
        _isSpeakerOn.value = on
        rtcEngine?.setEnableSpeakerphone(on)
    }

    fun switchCamera() {
        rtcEngine?.switchCamera()
    }

    // ─── 비디오 Surface 연결 ──────────────────────────────────────────────────
    fun setupLocalVideo(surface: SurfaceView) {
        val canvas = VideoCanvas(surface, VideoCanvas.RENDER_MODE_HIDDEN, 0)
        rtcEngine?.setupLocalVideo(canvas)
    }

    fun setupRemoteVideo(surface: SurfaceView, uid: Int) {
        val canvas = VideoCanvas(surface, VideoCanvas.RENDER_MODE_HIDDEN, uid)
        rtcEngine?.setupRemoteVideo(canvas)
    }

    // ─── 상태 초기화 (통화 종료 후 재사용) ────────────────────────────────────
    fun resetState() {
        _callUiState.value  = CallUiState.Idle
        _remoteUid.value    = 0
        _isMuted.value      = false
        _isCameraOff.value  = false
        _isSpeakerOn.value  = true
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────
    private fun joinChannel(channelName: String, callType: String) {
        val engine = rtcEngine ?: return
        if (callType == "video") {
            engine.enableVideo()
            engine.startPreview()
        } else {
            engine.disableVideo()
        }
        engine.enableAudio()
        engine.setEnableSpeakerphone(true)

        val options = ChannelMediaOptions().apply {
            clientRoleType       = Constants.CLIENT_ROLE_BROADCASTER
            channelProfile       = Constants.CHANNEL_PROFILE_COMMUNICATION
            publishCameraTrack   = callType == "video"
            publishMicrophoneTrack = true
        }
        // token: null → Agora 콘솔에서 "테스트 모드(무인증)" 활성화 필요
        // 프로덕션에서는 서버에서 발급한 token 전달
        engine.joinChannel(null, channelName, 0, options)
    }

    private fun leaveChannel() {
        rtcEngine?.leaveChannel()
        rtcEngine?.stopPreview()
        _remoteUid.value = 0
    }

    override fun onCleared() {
        super.onCleared()
        callRepository.stopListeningForCalls(currentChatId)
        rtcEngine?.leaveChannel()
        rtcEngine?.stopPreview()
        RtcEngine.destroy()
        rtcEngine = null
    }
}
