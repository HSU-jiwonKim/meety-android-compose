package com.bugzero.meety.ui.call

import android.Manifest
import android.content.pm.PackageManager
import android.view.SurfaceView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

/**
 * 화상/음성 통화 화면 (1:1 및 다자간 지원).
 *
 * @param chatId       채팅방 ID (Agora 채널 이름과 Firebase 시그널링에 사용)
 * @param callType     "video" 또는 "voice"
 * @param isIncoming   true = 수신 측, false = 발신 측
 * @param currentUserId 현재 로그인 사용자 UID
 * @param onCallEnded  통화 종료 후 이전 화면으로 복귀 콜백
 */
@Composable
fun CallScreen(
    chatId: String,
    callType: String,
    isIncoming: Boolean,
    currentUserId: String,
    onCallEnded: () -> Unit,
    callViewModel: CallViewModel = viewModel()
) {
    val context = LocalContext.current

    val callState  by callViewModel.callUiState.collectAsState()
    val isMuted    by callViewModel.isMuted.collectAsState()
    val isCameraOff by callViewModel.isCameraOff.collectAsState()
    val isSpeakerOn by callViewModel.isSpeakerOn.collectAsState()
    val remoteUids by callViewModel.remoteUids.collectAsState()

    val requiredPermissions = if (callType == "video") {
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    } else {
        arrayOf(Manifest.permission.RECORD_AUDIO)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            callViewModel.initEngine()
            if (isIncoming) callViewModel.acceptCall(chatId, callType, currentUserId)
            else callViewModel.startCall(chatId, callType, currentUserId)
        }
    }

    LaunchedEffect(callState) {
        if (callState is CallUiState.Ended) {
            delay(800)
            callViewModel.resetState()
            onCallEnded()
        }
    }

    LaunchedEffect(Unit) {
        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            callViewModel.initEngine()
            if (isIncoming) callViewModel.acceptCall(chatId, callType, currentUserId)
            else callViewModel.startCall(chatId, callType, currentUserId)
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (callType == "video") {
            VideoCallContent(
                callState   = callState,
                remoteUids  = remoteUids,
                isMuted     = isMuted,
                isCameraOff = isCameraOff,
                onMuteClick       = { callViewModel.toggleMute() },
                onCameraClick     = { callViewModel.toggleCamera() },
                onSwitchCamera    = { callViewModel.switchCamera() },
                onEndCall         = { callViewModel.endCall() },
                onSetupLocalVideo = { callViewModel.setupLocalVideo(it) },
                onSetupRemoteVideo = { surface, uid -> callViewModel.setupRemoteVideo(surface, uid) }
            )
        } else {
            VoiceCallContent(
                callState   = callState,
                remoteUids  = remoteUids,
                isMuted     = isMuted,
                isSpeakerOn = isSpeakerOn,
                onMuteClick   = { callViewModel.toggleMute() },
                onSpeakerClick = { callViewModel.toggleSpeaker() },
                onEndCall      = { callViewModel.endCall() }
            )
        }
    }
}

// ─── 화상통화 UI (다자간 그리드) ─────────────────────────────────────────────

@Composable
private fun VideoCallContent(
    callState: CallUiState,
    remoteUids: Set<Int>,
    isMuted: Boolean,
    isCameraOff: Boolean,
    onMuteClick: () -> Unit,
    onCameraClick: () -> Unit,
    onSwitchCamera: () -> Unit,
    onEndCall: () -> Unit,
    onSetupLocalVideo: (SurfaceView) -> Unit,
    onSetupRemoteVideo: (SurfaceView, Int) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0B14))) {

        val uids = remoteUids.toList()
        when {
            uids.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(48.dp))
                        Text(
                            text = if (callState is CallUiState.Calling) "연결 중..." else "연결 중...",
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    }
                }
            }
            uids.size == 1 -> {
                // 1:1 화상통화: 상대방 전체화면
                AndroidView(
                    factory = { ctx -> SurfaceView(ctx).also { onSetupRemoteVideo(it, uids.first()) } },
                    update  = { surface -> onSetupRemoteVideo(surface, uids.first()) },
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                // 다자간: 나 포함 그리드 (KakaoTalk/Webex 스타일 — 내 화면도 같은 크기 타일)
                val LOCAL_UID = -1
                val allUids = listOf(LOCAL_UID) + uids
                val cols = if (allUids.size <= 4) 2 else 3
                LazyVerticalGrid(
                    columns = GridCells.Fixed(cols),
                    modifier = Modifier.fillMaxSize().padding(top = 48.dp, bottom = 140.dp, start = 6.dp, end = 6.dp),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(items = allUids, key = { it }) { uid ->
                        if (uid == LOCAL_UID) {
                            LocalVideoTile(
                                onSetupLocalVideo = onSetupLocalVideo,
                                isCameraOff = isCameraOff
                            )
                        } else {
                            RemoteVideoTile(uid = uid, onSetupRemoteVideo = onSetupRemoteVideo)
                        }
                    }
                }
            }
        }

        // 내 카메라 미리보기: 1:1 통화일 때만 PiP, 다자간은 그리드 타일에 포함됨
        if (!isCameraOff && uids.size == 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 60.dp, end = 16.dp)
                    .size(width = 108.dp, height = 152.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2D2D2D))
            ) {
                AndroidView(
                    factory = { ctx -> SurfaceView(ctx).also { onSetupLocalVideo(it) } },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 상단 참여자 수 & 타이머
        Box(
            modifier = Modifier.align(Alignment.TopStart).padding(top = 56.dp, start = 16.dp)
                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Column {
                Text(
                    text = if (uids.isEmpty()) "참여자 1명" else "참여자 ${uids.size + 1}명",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                if (callState is CallUiState.InCall) {
                    Spacer(Modifier.height(2.dp))
                    CallDurationTimer(fontSize = 12.sp)
                }
            }
        }

        // 하단 컨트롤 버튼
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 52.dp, start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CallControlButton(
                icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                label = if (isMuted) "음소거 해제" else "음소거",
                tintColor = Color.White,
                backgroundColor = if (isMuted) Color(0xFF555555) else Color(0xFF2D2D2D),
                onClick = onMuteClick
            )
            CallControlButton(
                icon = if (isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                label = if (isCameraOff) "카메라 켜기" else "카메라 끄기",
                tintColor = Color.White,
                backgroundColor = if (isCameraOff) Color(0xFF555555) else Color(0xFF2D2D2D),
                onClick = onCameraClick
            )
            CallControlButton(
                icon = Icons.Default.FlipCameraAndroid,
                label = "카메라 전환",
                tintColor = Color.White,
                backgroundColor = Color(0xFF2D2D2D),
                onClick = onSwitchCamera
            )
            EndCallButton(onClick = onEndCall)
        }
    }
}

@Composable
private fun LocalVideoTile(
    onSetupLocalVideo: (SurfaceView) -> Unit,
    isCameraOff: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1F1F28)),
        contentAlignment = Alignment.Center
    ) {
        if (isCameraOff) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(40.dp)
            )
        } else {
            AndroidView(
                factory = { ctx -> SurfaceView(ctx).also { onSetupLocalVideo(it) } },
                modifier = Modifier.fillMaxSize()
            )
        }
        // "나" 라벨 (좌하단)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text("나", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun RemoteVideoTile(uid: Int, onSetupRemoteVideo: (SurfaceView, Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1F1F28))
    ) {
        AndroidView(
            factory = { ctx -> SurfaceView(ctx).also { onSetupRemoteVideo(it, uid) } },
            update  = { surface -> onSetupRemoteVideo(surface, uid) },
            modifier = Modifier.fillMaxSize()
        )
    }
}

// ─── 음성통화 UI (다자간 참여자 그리드) ──────────────────────────────────────

@Composable
private fun VoiceCallContent(
    callState: CallUiState,
    remoteUids: Set<Int>,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    onMuteClick: () -> Unit,
    onSpeakerClick: () -> Unit,
    onEndCall: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF7C3AED), Color(0xFF3B0764))))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // 통화 상태 텍스트
            Text(
                text = when (callState) {
                    is CallUiState.Calling -> "연결 중..."
                    is CallUiState.InCall  -> if (remoteUids.size > 1) "그룹 통화 중 (${remoteUids.size + 1}명)" else "통화 중"
                    else                   -> ""
                },
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )

            // 통화 시간 타이머
            if (callState is CallUiState.InCall) {
                Spacer(modifier = Modifier.height(8.dp))
                CallDurationTimer()
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 참여자 아바타 그리드 (나 + 원격 참여자들)
            if (remoteUids.isEmpty()) {
                // 연결 대기 — 아바타 1개만
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                }
            } else {
                val all = listOf(0) + remoteUids.toList()  // 0 = 나
                val cols = if (all.size <= 4) 2 else 3
                LazyVerticalGrid(
                    columns = GridCells.Fixed(cols),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .heightIn(max = 380.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    items(items = all, key = { it }) { uid ->
                        ParticipantAvatar(uid = uid, isMe = uid == 0)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 하단 컨트롤
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 80.dp, start = 40.dp, end = 40.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CallControlButton(
                    icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    label = if (isMuted) "음소거 해제" else "음소거",
                    tintColor = Color.White,
                    backgroundColor = if (isMuted) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.2f),
                    onClick = onMuteClick
                )
                EndCallButton(size = 72, onClick = onEndCall)
                CallControlButton(
                    icon = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    label = if (isSpeakerOn) "스피커 끄기" else "스피커 켜기",
                    tintColor = Color.White,
                    backgroundColor = if (isSpeakerOn) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.2f),
                    onClick = onSpeakerClick
                )
            }
        }
    }
}

@Composable
private fun ParticipantAvatar(uid: Int, isMe: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
        Text(
            text = if (isMe) "나" else "참여자",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ─── 공통 컴포넌트 ────────────────────────────────────────────────────────────

@Composable
private fun CallControlButton(
    icon: ImageVector,
    label: String,
    tintColor: Color,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .background(backgroundColor, CircleShape)
        ) {
            Icon(icon, contentDescription = label, tint = tintColor, modifier = Modifier.size(26.dp))
        }
        Text(label, color = Color.White, fontSize = 11.sp)
    }
}

@Composable
private fun EndCallButton(size: Int = 64, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(size.dp)
                .background(Color(0xFFDC2626), CircleShape)
        ) {
            Icon(
                Icons.Default.CallEnd,
                contentDescription = "통화 종료",
                tint = Color.White,
                modifier = Modifier.size((size * 0.44f).dp)
            )
        }
        Text("종료", color = Color.White, fontSize = 11.sp)
    }
}

@Composable
private fun CallDurationTimer(fontSize: androidx.compose.ui.unit.TextUnit = 16.sp) {
    var totalSeconds by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            totalSeconds++
        }
    }
    Text(
        text = "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60),
        color = Color.White.copy(alpha = 0.8f),
        fontSize = fontSize
    )
}
