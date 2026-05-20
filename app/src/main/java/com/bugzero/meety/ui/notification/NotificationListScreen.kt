package com.bugzero.meety.ui.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bugzero.meety.data.model.InAppNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 상단 알림 버튼을 눌렀을 때 보이는 알림 목록 화면.
 *
 * 알림 종류별 동작:
 *  - 메시지   → 해당 채팅방으로 이동
 *  - 음성/영상 통화 → 통화 화면으로 이동 (isIncoming=true → 수락/거절 가능)
 *  - 좋아요   → 카드 안에 [수락][거절] 버튼이 나옴
 *
 * 읽음 처리:
 *  - 메시지·통화 카드를 탭하면 즉시 그 한 건 삭제
 *  - 좋아요는 수락/거절 후 그 한 건 삭제
 *  - 화면을 빠져나가면 남은 알림 전부 일괄 삭제
 *  - "모두 삭제" 버튼으로 즉시 비우기도 가능
 */
@Composable
fun NotificationListScreen(
    onBackClick: () -> Unit,
    onMessageClick: (chatId: String) -> Unit = {},
    onCallClick: (chatId: String, callType: String) -> Unit = { _, _ -> },
    viewModel: NotificationViewModel = viewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 화면을 떠날 때 남은 알림 전부 삭제 (= 사용자가 알림을 "읽었다"고 간주)
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearAll()
        }
    }

    // 수락/거절 결과 메시지를 스낵바로 표시
    LaunchedEffect(actionMessage) {
        val msg = actionMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeActionMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F1F8))) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 상단바
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(Color.White)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = Color(0xFF4B4B4B)
                    )
                }
                Text(
                    text = "알림",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937),
                    modifier = Modifier.padding(start = 4.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                if (notifications.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearAll() }) {
                        Text(
                            text = "모두 삭제",
                            color = Color(0xFFEC4899),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (notifications.isEmpty()) {
                EmptyNotifications(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = notifications, key = { it.id }) { notif ->
                        NotificationItem(
                            notification = notif,
                            onItemClick = {
                                when (notif.type) {
                                    InAppNotification.TYPE_MESSAGE -> {
                                        // 채팅방 이동 → 그 한 건만 삭제
                                        viewModel.deleteOne(notif.id)
                                        if (notif.relatedId.isNotBlank()) {
                                            onMessageClick(notif.relatedId)
                                        }
                                    }
                                    InAppNotification.TYPE_CALL,
                                    InAppNotification.TYPE_VIDEO_CALL -> {
                                        // 통화 화면으로 이동 (isIncoming=true → 수락/거절 UI)
                                        viewModel.deleteOne(notif.id)
                                        if (notif.relatedId.isNotBlank()) {
                                            val callType =
                                                if (notif.type == InAppNotification.TYPE_VIDEO_CALL) "video"
                                                else "voice"
                                            onCallClick(notif.relatedId, callType)
                                        }
                                    }
                                    InAppNotification.TYPE_LIKE -> {
                                        // 좋아요는 카드 안의 [수락]/[거절] 버튼을 사용 → 카드 탭은 단순 삭제 X
                                    }
                                }
                            },
                            onAcceptLike = {
                                viewModel.acceptLike(notif.relatedId, notif.id)
                            },
                            onRejectLike = {
                                viewModel.rejectLike(notif.relatedId, notif.id)
                            }
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun EmptyNotifications(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.NotificationsNone,
                contentDescription = null,
                tint = Color(0xFFB44FD3),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "새로운 알림이 없어요",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF374151)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "전화, 메시지, 좋아요가 오면 여기에서 확인할 수 있어요",
                fontSize = 13.sp,
                color = Color(0xFF6B7280)
            )
        }
    }
}

@Composable
private fun NotificationItem(
    notification: InAppNotification,
    onItemClick: () -> Unit,
    onAcceptLike: () -> Unit,
    onRejectLike: () -> Unit
) {
    val style: Triple<ImageVector, Color, Color> = when (notification.type) {
        InAppNotification.TYPE_CALL ->
            Triple(Icons.Default.Call, Color(0xFF22C55E), Color(0xFFE8F7EE))
        InAppNotification.TYPE_VIDEO_CALL ->
            Triple(Icons.Default.Videocam, Color(0xFF3B82F6), Color(0xFFE6EFFE))
        InAppNotification.TYPE_MESSAGE ->
            Triple(Icons.Default.ChatBubble, Color(0xFF7C3AED), Color(0xFFEDE9FE))
        InAppNotification.TYPE_LIKE ->
            Triple(Icons.Default.Favorite, Color(0xFFEC4899), Color(0xFFFCE7F3))
        else ->
            Triple(Icons.Default.NotificationsNone, Color(0xFF6B7280), Color(0xFFF3F4F6))
    }
    val icon: ImageVector = style.first
    val tint: Color = style.second
    val bg: Color = style.third

    val isLike = notification.type == InAppNotification.TYPE_LIKE

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            // 좋아요는 버튼이 따로 있으니 카드 자체 탭 비활성화
            .then(
                if (!isLike) Modifier.clickable { onItemClick() }
                else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(bg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title.ifBlank { "알림" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (notification.body.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = notification.body,
                        fontSize = 13.sp,
                        color = Color(0xFF4B5563),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTimestamp(notification.timestamp),
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF)
                )
            }
        }

        if (isLike) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAcceptLike,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA78BFA))
                ) {
                    Text("수락", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = onRejectLike,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("거절", fontSize = 13.sp, color = Color(0xFF4B5563))
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000L -> "방금 전"
        diff < 60 * 60_000L -> "${diff / 60_000L}분 전"
        diff < 24 * 60 * 60_000L -> "${diff / (60 * 60_000L)}시간 전"
        else -> SimpleDateFormat("MM월 dd일", Locale.KOREA).format(Date(timestamp))
    }
}
