package com.bugzero.meety.ui.call

import android.app.Application
import android.util.Log
import android.view.SurfaceView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bugzero.meety.data.repository.AgoraCallRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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
import kotlinx.coroutines.tasks.await

// ─── 통화 UI 상태 ────────────────────────────────────────────────────────────
sealed class CallUiState {
    object Idle : CallUiState()
    data class Calling(val callType: String, val channelName: String) : CallUiState()
    data class InCall(val callType: String, val channelName: String) : CallUiState()
    object Ended : CallUiState()
}

/** 채팅방 내 "통화 중" 배너에 표시할 정보 */
data class ActiveCallInfo(
    val callType: String,       // "video" | "voice"
    val participantCount: Int   // 현재 통화 중인 인원 수
)

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

    // ─── 원격 사용자 UID 목록 (Agora에서 자동 부여) ─────────────────────────
    private val _remoteUids = MutableStateFlow<Set<Int>>(emptySet())
    val remoteUids: StateFlow<Set<Int>> = _remoteUids.asStateFlow()

    // ─── 채팅방에서 수신 통화 감지용 (callType, callerId) ───────────────────
    private val _incomingCallState = MutableStateFlow<Pair<String, String>?>(null)
    val incomingCallState: StateFlow<Pair<String, String>?> = _incomingCallState.asStateFlow()

    // ─── 채팅방 "통화 중" 배너용 ─────────────────────────────────────────────
    private val _activeCallInfo = MutableStateFlow<ActiveCallInfo?>(null)
    val activeCallInfo: StateFlow<ActiveCallInfo?> = _activeCallInfo.asStateFlow()

    private var activeBannerListener: ListenerRegistration? = null

    private var rtcEngine: RtcEngine? = null
    private var currentChatId = ""
    private var currentUserId = ""
    private var currentCallerId = ""
    private var currentCallType = "voice"
    private var isCaller = false
    private var callConnectedAt: Long = 0L   // 첫 원격 참여자 입장 시각 (통화 연결 시각)
    private var callEndedEmitted = false     // 로그 중복 기록 방지 플래그
    private var callStatusListener: ListenerRegistration? = null

    // ─── Agora 이벤트 핸들러 ──────────────────────────────────────────────────
    private val rtcEventHandler = object : IRtcEngineEventHandler() {

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            Log.d("AgoraRTC", "채널 입장 성공: $channel, uid=$uid")
        }

        /** 새 참여자 입장 → 목록에 추가 + InCall 상태 전이 */
        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.d("AgoraRTC", "상대방 입장: uid=$uid")
            _remoteUids.value = _remoteUids.value + uid
            if (callConnectedAt == 0L) callConnectedAt = System.currentTimeMillis()
            val current = _callUiState.value
            if (current is CallUiState.Calling) {
                _callUiState.value = CallUiState.InCall(current.callType, current.channelName)
            }
        }

        /** 한 명이 이탈 → 목록에서 제거. 모두 빠지면 종료 */
        override fun onUserOffline(uid: Int, reason: Int) {
            Log.d("AgoraRTC", "상대방 이탈: uid=$uid, reason=$reason")
            _remoteUids.value = _remoteUids.value - uid
            // 남은 원격 참여자가 없고 내가 InCall 이면 통화 종료
            if (_remoteUids.value.isEmpty() && _callUiState.value is CallUiState.InCall) {
                viewModelScope.launch { endCall() }
            }
        }

        override fun onError(err: Int) {
            Log.e("AgoraRTC", "에러 발생: code=$err")
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
            rtcEngine?.addHandler(rtcEventHandler)
        }.onFailure { it.printStackTrace() }
    }

    // ─── 발신 (그룹 포함) ─────────────────────────────────────────────────────
    fun startCall(chatId: String, callType: String, callerId: String) {
        currentChatId = chatId
        currentUserId = callerId
        currentCallerId = callerId
        currentCallType = callType
        isCaller = true
        callEndedEmitted = false
        callConnectedAt = 0L
        viewModelScope.launch {
            val channelName = callRepository.startCall(chatId, callType, callerId)
            _callUiState.value = CallUiState.Calling(callType, channelName)
            joinChannel(channelName, callType)
            startCallStatusListener(chatId)
        }
    }

    // ─── 수신 수락 ────────────────────────────────────────────────────────────
    fun acceptCall(chatId: String, callType: String, userId: String = "") {
        currentChatId = chatId
        currentCallType = callType
        currentUserId = userId
        isCaller = false
        callEndedEmitted = false
        viewModelScope.launch {
            // 발신자 정보를 Firestore에서 미리 조회 (callLog 작성시 필요)
            runCatching {
                val doc = FirebaseFirestore.getInstance()
                    .collection("calls").document(chatId).get().await()
                currentCallerId = doc.getString("callerId") ?: ""
            }
            val channelName = callRepository.acceptCall(chatId, userId)
            _callUiState.value = CallUiState.Calling(callType, channelName)
            joinChannel(channelName, callType)
            _incomingCallState.value = null
            startCallStatusListener(chatId)
        }
    }

    // ─── 수신 거절 ────────────────────────────────────────────────────────────
    fun declineCall(chatId: String, userId: String = "") {
        viewModelScope.launch {
            _incomingCallState.value = null
            // 1:1이면 전체 종료, 그룹이면 내 다이얼로그만 닫고 나머지는 계속 벨 울림
            runCatching {
                val chatDoc = FirebaseFirestore.getInstance()
                    .collection("chats").document(chatId).get().await()
                @Suppress("UNCHECKED_CAST")
                val participants = (chatDoc.get("participants") as? List<String>) ?: emptyList()
                val isOneOnOne = participants.size <= 2
                if (isOneOnOne) {
                    callRepository.endCall(chatId, userId, forceEndForAll = true)
                }
            }
        }
    }

    // ─── 통화 종료 ────────────────────────────────────────────────────────────
    fun endCall() {
        callStatusListener?.remove()
        callStatusListener = null
        val chatIdSnapshot = currentChatId
        val userIdSnapshot = currentUserId
        val hadRemote = _remoteUids.value.isNotEmpty() || callConnectedAt > 0L
        val wasCaller = isCaller
        val callTypeSnapshot = currentCallType

        viewModelScope.launch {
            if (chatIdSnapshot.isNotEmpty()) {
                val forceEnd = wasCaller && !hadRemote
                callRepository.endCall(chatIdSnapshot, userIdSnapshot, forceEndForAll = forceEnd)
                tryWriteCallLog(chatIdSnapshot, callTypeSnapshot, hadRemote, wasCaller && !hadRemote)
            }
            leaveChannel()
            _callUiState.value = CallUiState.Ended
        }
    }

    /** 원자적 claim 성공 시에만 call_log 메시지를 기록 */
    private suspend fun tryWriteCallLog(
        chatId: String,
        callType: String,
        hadRemote: Boolean,
        canceledByCaller: Boolean
    ) {
        if (callEndedEmitted) return
        callEndedEmitted = true
        val durationSec = if (callConnectedAt > 0L)
            ((System.currentTimeMillis() - callConnectedAt) / 1000L).toInt()
        else 0
        val didClaim = callRepository.tryClaimCallLog(chatId)
        if (didClaim) {
            writeCallLogMessage(
                chatId = chatId,
                callType = callType,
                durationSec = durationSec,
                connected = hadRemote,
                canceledByCaller = canceledByCaller
            )
        }
    }

    /** 카카오톡 스타일 통화 로그 시스템 메시지 저장 */
    private suspend fun writeCallLogMessage(
        chatId: String,
        callType: String,
        durationSec: Int,
        connected: Boolean,
        canceledByCaller: Boolean
    ) {
        runCatching {
            val db = FirebaseFirestore.getInstance()
            val now = Timestamp.now()
            val label = when {
                !connected && canceledByCaller -> "call_canceled"
                !connected -> "call_missed"
                else -> "call_completed"
            }
            val contentText = buildCallLogText(callType, durationSec, label)
            val messageData = mapOf(
                "senderId" to "system",
                "senderName" to "system",
                "content" to contentText,
                "type" to "call_log",
                "callType" to callType,
                "callStatus" to label,
                "callDurationSec" to durationSec,
                "callerId" to currentCallerId,
                "createdAt" to now
            )
            db.collection("chats").document(chatId)
                .collection("messages")
                .add(messageData).await()
            db.collection("chats").document(chatId)
                .update(mapOf("lastMessage" to contentText, "lastMessageAt" to now))
                .await()
        }.onFailure { Log.e("CallVM", "call log 저장 실패: ${it.message}") }
    }

    private fun buildCallLogText(callType: String, durationSec: Int, status: String): String {
        val kindLabel = if (callType == "video") "영상통화" else "음성통화"
        return when (status) {
            "call_missed" -> "부재중 $kindLabel"
            "call_canceled" -> "취소된 $kindLabel"
            else -> {
                val m = durationSec / 60
                val s = durationSec % 60
                val durText = if (m > 0) "${m}분 ${s}초" else "${s}초"
                "$kindLabel · 통화시간 $durText"
            }
        }
    }

    // ─── 수신 통화 감지 시작/중단 (ChatRoomScreen에서 사용) ──────────────────
    fun listenForIncomingCall(chatId: String, currentUserId: String) {
        callRepository.listenForIncomingCall(
            chatId = chatId,
            currentUserId = currentUserId,
            onIncomingCall = { callType, callerId ->
                if (callerId != currentUserId) {
                    _incomingCallState.value = callType to callerId
                }
            },
            onCallEnded = {
                _incomingCallState.value = null
                _activeCallInfo.value = null
            }
        )
        // "통화 중" 배너용 별도 리스너 시작
        listenForActiveCallBanner(chatId, currentUserId)
    }

    /**
     * calls/{chatId} 문서를 감시해서 "active" 상태이고 내가 아직 참여하지 않은 경우
     * _activeCallInfo 를 업데이트한다.
     */
    private fun listenForActiveCallBanner(chatId: String, currentUserId: String) {
        activeBannerListener?.remove()
        activeBannerListener = FirebaseFirestore.getInstance()
            .collection("calls").document(chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    _activeCallInfo.value = null
                    return@addSnapshotListener
                }
                val status   = snapshot.getString("status")   ?: ""
                val callType = snapshot.getString("callType") ?: "voice"
                @Suppress("UNCHECKED_CAST")
                val joined   = (snapshot.get("joinedUsers") as? List<String>) ?: emptyList()

                // "active" 또는 "calling" 상태이고, 내가 아직 채널에 없을 때만 배너 표시
                val isOngoing = status == "active" || status == "calling"
                if (isOngoing && !joined.contains(currentUserId)) {
                    _activeCallInfo.value = ActiveCallInfo(callType, joined.size)
                } else {
                    _activeCallInfo.value = null
                }
            }
    }

    fun stopListeningForCalls(chatId: String) {
        callRepository.stopListeningForCalls(chatId)
        activeBannerListener?.remove()
        activeBannerListener = null
        _activeCallInfo.value = null
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
        _remoteUids.value   = emptySet()
        _isMuted.value      = false
        _isCameraOff.value  = false
        _isSpeakerOn.value  = true
        callConnectedAt     = 0L
        callEndedEmitted    = false
        isCaller            = false
        currentChatId       = ""
        currentUserId       = ""
        currentCallerId     = ""
    }

    // ─── Firestore 상태 감시 (모든 사용자가 떠남 → 이쪽도 종료) ────────────
    private fun startCallStatusListener(chatId: String) {
        callStatusListener?.remove()
        var isFirst = true
        callStatusListener = FirebaseFirestore.getInstance()
            .collection("calls").document(chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("CallVM", "callStatus 리스너 에러: ${error.message}")
                    return@addSnapshotListener
                }
                val status = snapshot?.getString("status") ?: return@addSnapshotListener
                if (isFirst) { isFirst = false; return@addSnapshotListener }

                if (status == "ended" && _callUiState.value !is CallUiState.Ended) {
                    Log.d("CallVM", "통화 전체 종료 감지 → 이쪽도 종료")
                    callStatusListener?.remove()
                    callStatusListener = null
                    val hadRemote = _remoteUids.value.isNotEmpty() || callConnectedAt > 0L
                    val wasCaller = isCaller
                    viewModelScope.launch {
                        // 발신자가 아직 수락 전인 상태에서 상대방 거절 → canceled 로 기록
                        tryWriteCallLog(chatId, currentCallType, hadRemote, wasCaller && !hadRemote)
                        leaveChannel()
                        _callUiState.value = CallUiState.Ended
                    }
                }
            }
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
        engine.joinChannel(null, channelName, 0, options)
    }

    private fun leaveChannel() {
        rtcEngine?.leaveChannel()
        rtcEngine?.stopPreview()
        _remoteUids.value = emptySet()
    }

    override fun onCleared() {
        super.onCleared()
        callStatusListener?.remove()
        callStatusListener = null
        activeBannerListener?.remove()
        activeBannerListener = null
        callRepository.stopListeningForCalls(currentChatId)
        rtcEngine?.removeHandler(rtcEventHandler)
        if (_callUiState.value is CallUiState.Calling || _callUiState.value is CallUiState.InCall) {
            rtcEngine?.leaveChannel()
            rtcEngine?.stopPreview()
        }
        rtcEngine = null
    }
}
