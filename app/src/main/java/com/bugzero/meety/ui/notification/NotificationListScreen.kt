package com.bugzero.meety.ui.notification

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bugzero.meety.data.model.InAppNotification
import java.text.SimpleDateFormat
import java.util.*

// ── 브랜드 컬러 (목업 CSS 변수와 동일) ───────────────────────────────────────
private val Brand1     = Color(0xFF7B5CFF)
private val Brand2     = Color(0xFFFF5C8A)
private val LikeRed    = Color(0xFFFF4D7D)
private val GreenOk    = Color(0xFF19C37D)
private val BlueAccent = Color(0xFF1E88E5)
private val Ink        = Color(0xFF17161D)
private val Ink2       = Color(0xFF56535F)
private val Ink3       = Color(0xFF9B98A6)
private val BgScreen   = Color(0xFFF4F4F8)
private val LineTwo    = Color(0xFFF1EFF5)
private val VioletSoft = Color(0xFFF2EEFF)
private val PinkSoft   = Color(0xFFFFECF3)
private val MintSoft   = Color(0xFFE5F8F3)
private val BlueSoft   = Color(0xFFE9F1FF)

private val gradBrush = Brush.linearGradient(listOf(Brand1, Color(0xFFA24BFF), Brand2))

// ── 탭 필터 ──────────────────────────────────────────────────────────────────
private enum class NotifFilter(val label: String) {
    ALL("전체"), LIKE("좋아요"), CHAT("채팅"), CALL("통화")
}

// ── 아바타 스타일 헬퍼 ────────────────────────────────────────────────────────
private data class AvatarStyle(
    val brush: Brush,
    val badgeColor: Color,
    val badgeIcon: ImageVector
)

private fun avatarStyle(type: String) = when (type) {
    InAppNotification.TYPE_LIKE -> AvatarStyle(
        Brush.linearGradient(listOf(Color(0xFFFF9DC0), Color(0xFFEC407A))),
        LikeRed, Icons.Default.Favorite
    )
    InAppNotification.TYPE_MESSAGE -> AvatarStyle(
        Brush.linearGradient(listOf(Color(0xFF7B5CFF), Color(0xFFA24BFF))),
        Brand1, Icons.Default.ChatBubble
    )
    InAppNotification.TYPE_CALL -> AvatarStyle(
        Brush.linearGradient(listOf(Color(0xFF7DE0CF), Color(0xFF26A69A))),
        GreenOk, Icons.Default.Call
    )
    InAppNotification.TYPE_VIDEO_CALL -> AvatarStyle(
        Brush.linearGradient(listOf(Color(0xFF4FC3F7), Color(0xFF1976D2))),
        BlueAccent, Icons.Default.Videocam
    )
    else -> AvatarStyle(
        Brush.linearGradient(listOf(Color(0xFF9B98A6), Color(0xFF6B7280))),
        Ink3, Icons.Default.Notifications
    )
}

// tag: Triple(텍스트, 글자색, 배경색) — 채팅은 태그 없음
private fun typeTag(type: String): Triple<String, Color, Color>? = when (type) {
    InAppNotification.TYPE_LIKE       -> Triple("좋아요", Color(0xFFE0457A), PinkSoft)
    InAppNotification.TYPE_CALL       -> Triple("전화",   Color(0xFF6D49E0), VioletSoft)
    InAppNotification.TYPE_VIDEO_CALL -> Triple("영상",   BlueAccent,        BlueSoft)
    else -> null
}

private fun defaultBody(type: String) = when (type) {
    InAppNotification.TYPE_LIKE       -> "회원님의 프로필에 좋아요를 눌렀어요"
    InAppNotification.TYPE_MESSAGE    -> "새 메시지가 도착했어요"
    InAppNotification.TYPE_CALL       -> "음성 통화가 왔었어요"
    InAppNotification.TYPE_VIDEO_CALL -> "영상 통화가 왔었어요"
    else -> ""
}

// ── 날짜 그룹화 ───────────────────────────────────────────────────────────────
private fun groupByDate(list: List<InAppNotification>): List<Pair<String, List<InAppNotification>>> {
    fun dayStart(daysAgo: Int) = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_YEAR, -daysAgo)
    }.timeInMillis

    val todayMs     = dayStart(0)
    val yesterdayMs = dayStart(1)
    val weekMs      = dayStart(7)

    val buckets = linkedMapOf(
        "오늘"   to mutableListOf<InAppNotification>(),
        "어제"   to mutableListOf(),
        "이번 주" to mutableListOf(),
        "이전"   to mutableListOf()
    )
    list.forEach { n ->
        when {
            n.timestamp >= todayMs     -> buckets["오늘"]
            n.timestamp >= yesterdayMs -> buckets["어제"]
            n.timestamp >= weekMs      -> buckets["이번 주"]
            else                       -> buckets["이전"]
        }?.add(n)
    }
    return buckets.entries.filter { it.value.isNotEmpty() }.map { it.key to it.value.toList() }
}

// ── 타임스탬프 포맷 ───────────────────────────────────────────────────────────
private fun formatTs(ts: Long): String {
    if (ts <= 0L) return ""
    val diff = System.currentTimeMillis() - ts
    return when {
        diff < 60_000L             -> "방금 전"
        diff < 3_600_000L          -> "${diff / 60_000L}분 전"
        diff < 86_400_000L         -> "${diff / 3_600_000L}시간 전"
        diff < 2 * 86_400_000L     -> "어제 " + SimpleDateFormat("a h:mm", Locale.KOREA).format(Date(ts))
        else                       -> SimpleDateFormat("M월 d일", Locale.KOREA).format(Date(ts))
    }
}

// ────────────────────────────────────────────────────────────────────────────
//  메인 화면
// ────────────────────────────────────────────────────────────────────────────
@Composable
fun NotificationListScreen(
    onBackClick: () -> Unit,
    onMessageClick: (chatId: String) -> Unit = {},
    onCallClick: (chatId: String, callType: String) -> Unit = { _, _ -> },
    viewModel: NotificationViewModel = viewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val actionMessage  by viewModel.actionMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var filter       by remember { mutableStateOf(NotifFilter.ALL) }
    var showSettings by remember { mutableStateOf(false) }

    // 알림 설정 토글 (로컬 UI 상태)
    var likeOn  by remember { mutableStateOf(true) }
    var chatOn  by remember { mutableStateOf(true) }
    var callOn  by remember { mutableStateOf(true) }
    var videoOn by remember { mutableStateOf(true) }

    // 화면 떠날 때 전체 읽음 처리(삭제)
    DisposableEffect(Unit) { onDispose { viewModel.clearAll() } }

    LaunchedEffect(actionMessage) {
        val msg = actionMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeActionMessage()
        }
    }

    val filtered = remember(notifications, filter) {
        when (filter) {
            NotifFilter.ALL  -> notifications
            NotifFilter.LIKE -> notifications.filter { it.type == InAppNotification.TYPE_LIKE }
            NotifFilter.CHAT -> notifications.filter { it.type == InAppNotification.TYPE_MESSAGE }
            NotifFilter.CALL -> notifications.filter {
                it.type == InAppNotification.TYPE_CALL || it.type == InAppNotification.TYPE_VIDEO_CALL
            }
        }
    }
    val groups = remember(filtered) { groupByDate(filtered) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgScreen)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── 앱바 ──────────────────────────────────────────────────────
            NotifAppBar(
                unreadCount     = notifications.size,
                showSettings    = showSettings,
                onBackClick     = onBackClick,
                onSettingsClick = {
                    showSettings = !showSettings
                    if (showSettings) filter = NotifFilter.ALL
                },
                onMarkAllRead   = { viewModel.clearAll() }
            )

            // ── 세그먼트 컨트롤 ───────────────────────────────────────────
            NotifSegControl(
                selected = filter,
                onSelect = { filter = it; showSettings = false }
            )

            when {
                showSettings -> {
                    // ── 설정 패널 ─────────────────────────────────────────
                    NotifSettingsPanel(
                        likeOn = likeOn,  onLike  = { likeOn  = it },
                        chatOn = chatOn,  onChat  = { chatOn  = it },
                        callOn = callOn,  onCall  = { callOn  = it },
                        videoOn = videoOn, onVideo = { videoOn = it }
                    )
                }
                filtered.isEmpty() -> NotifEmptyState(Modifier.fillMaxSize())
                else -> {
                    // ── 알림 목록 ─────────────────────────────────────────
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        groups.forEach { (label, items) ->
                            item(key = "lbl_$label") { NotifDateLabel(label) }
                            item(key = "grp_$label") {
                                NotifGroupCard {
                                    items.forEachIndexed { idx, notif ->
                                        if (idx > 0) {
                                            HorizontalDivider(
                                                color     = LineTwo,
                                                thickness = 1.dp,
                                                modifier  = Modifier.padding(start = 78.dp)
                                            )
                                        }
                                        NotifRow(
                                            notification = notif,
                                            onItemClick  = {
                                                when (notif.type) {
                                                    InAppNotification.TYPE_MESSAGE -> {
                                                        viewModel.deleteOne(notif.id)
                                                        if (notif.relatedId.isNotBlank()) onMessageClick(notif.relatedId)
                                                    }
                                                    InAppNotification.TYPE_CALL,
                                                    InAppNotification.TYPE_VIDEO_CALL -> {
                                                        viewModel.deleteOne(notif.id)
                                                        if (notif.relatedId.isNotBlank()) {
                                                            val t = if (notif.type == InAppNotification.TYPE_VIDEO_CALL) "video" else "voice"
                                                            onCallClick(notif.relatedId, t)
                                                        }
                                                    }
                                                    InAppNotification.TYPE_LIKE -> { /* 버튼에서 처리 */ }
                                                }
                                            },
                                            onAcceptLike = { viewModel.acceptLike(notif.relatedId, notif.id) },
                                            onRejectLike = { viewModel.rejectLike(notif.relatedId, notif.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ── 앱바 ─────────────────────────────────────────────────────────────────────
@Composable
private fun NotifAppBar(
    unreadCount: Int,
    showSettings: Boolean,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onMarkAllRead: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgScreen)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 뒤로가기 버튼
        IconBox(onClick = onBackClick) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로",
                tint = Ink,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                "알림",
                fontSize = 23.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Ink,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = if (unreadCount > 0) "새 알림 ${unreadCount}개" else "모두 읽음",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink3
            )
        }

        // 설정 버튼
        IconBox(
            onClick = onSettingsClick,
            bg = if (showSettings) VioletSoft else Color.White
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "설정",
                tint = if (showSettings) Brand1 else Ink,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(8.dp))

        // 모두 읽음 버튼
        IconBox(onClick = onMarkAllRead) {
            Icon(
                Icons.Default.DoneAll,
                contentDescription = "모두 읽음",
                tint = Ink,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun IconBox(
    onClick: () -> Unit,
    bg: Color = Color.White,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { content() }
    )
}

// ── 세그먼트 컨트롤 ───────────────────────────────────────────────────────────
@Composable
private fun NotifSegControl(selected: NotifFilter, onSelect: (NotifFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFEAE8F0))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        NotifFilter.entries.forEach { tab ->
            val isOn = tab == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (isOn) Color.White else Color.Transparent)
                    .clickable { onSelect(tab) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    tab.label,
                    fontSize   = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color      = if (isOn) Ink else Ink3
                )
            }
        }
    }
}

// ── 설정 패널 ─────────────────────────────────────────────────────────────────
@Composable
private fun NotifSettingsPanel(
    likeOn: Boolean,  onLike:  (Boolean) -> Unit,
    chatOn: Boolean,  onChat:  (Boolean) -> Unit,
    callOn: Boolean,  onCall:  (Boolean) -> Unit,
    videoOn: Boolean, onVideo: (Boolean) -> Unit
) {
    NotifDateLabel("알림 설정")
    NotifGroupCard {
        SettingRow("💜", "좋아요 알림",   "프로필 좋아요",    VioletSoft, likeOn,  onLike)
        HorizontalDivider(color = LineTwo, thickness = 1.dp)
        SettingRow("💬", "채팅 알림",     "새 메시지",        PinkSoft,   chatOn,  onChat)
        HorizontalDivider(color = LineTwo, thickness = 1.dp)
        SettingRow("📞", "전화 알림",     "음성 통화 수신",   MintSoft,   callOn,  onCall)
        HorizontalDivider(color = LineTwo, thickness = 1.dp)
        SettingRow("📹", "영상전화 알림", "영상 통화 수신",   BlueSoft,   videoOn, onVideo)
    }
}

@Composable
private fun SettingRow(
    emoji: String, label: String, sub: String,
    bgColor: Color, isOn: Boolean, onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isOn) }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 17.sp) }

        Spacer(Modifier.width(11.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(sub,   fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink3)
        }

        Switch(
            checked  = isOn,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor    = Color.White,
                checkedTrackColor    = Brand1,
                uncheckedThumbColor  = Color.White,
                uncheckedTrackColor  = Color(0xFFDAD8E2),
                uncheckedBorderColor = Color(0xFFDAD8E2)
            )
        )
    }
}

// ── 날짜 라벨 ─────────────────────────────────────────────────────────────────
@Composable
private fun NotifDateLabel(label: String) {
    Text(
        text     = label,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        color    = Ink3,
        letterSpacing = 0.2.sp
    )
}

// ── 그룹 카드 (흰색 라운드 카드) ─────────────────────────────────────────────
@Composable
private fun NotifGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier       = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape          = RoundedCornerShape(20.dp),
        color          = Color.White,
        shadowElevation = 2.dp,
        border         = BorderStroke(1.dp, LineTwo)
    ) {
        Column(content = content)
    }
}

// ── 알림 행 ───────────────────────────────────────────────────────────────────
@Composable
private fun NotifRow(
    notification: InAppNotification,
    onItemClick:  () -> Unit,
    onAcceptLike: () -> Unit,
    onRejectLike: () -> Unit
) {
    val isLike = notification.type == InAppNotification.TYPE_LIKE
    val style  = avatarStyle(notification.type)
    val tag    = typeTag(notification.type)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // 좌측 미읽음 그라데이션 바
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                .background(gradBrush)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .then(if (!isLike) Modifier.clickable(onClick = onItemClick) else Modifier)
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 아바타
                NotifAvatar(style = style, initial = notification.fromUserName.firstOrNull()?.uppercaseChar()?.toString() ?: "?")

                Spacer(Modifier.width(12.dp))

                // 텍스트 영역
                Column(modifier = Modifier.weight(1f)) {
                    // 이름 + 타입 태그
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text       = notification.fromUserName.ifBlank { notification.title },
                            fontSize   = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Ink,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis,
                            modifier   = Modifier.weight(1f, fill = false)
                        )
                        if (tag != null) {
                            NotifTag(text = tag.first, textColor = tag.second, bgColor = tag.third)
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text       = notification.body.ifBlank { defaultBody(notification.type) },
                        fontSize   = 13.sp,
                        color      = Ink2,
                        maxLines   = 2,
                        overflow   = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text       = formatTs(notification.timestamp),
                        fontSize   = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Ink3
                    )
                }

                Spacer(Modifier.width(8.dp))

                // 미읽음 핑크 점
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Brand2)
                )
            }

            // 좋아요: 수락 / 거절 버튼
            if (isLike) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick  = onAcceptLike,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = Brand1)
                    ) {
                        Text("수락", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick  = onRejectLike,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(10.dp),
                        border   = BorderStroke(1.dp, Color(0xFFDAD8E2))
                    ) {
                        Text("거절", fontSize = 13.sp, color = Ink2)
                    }
                }
            }
        }
    }
}

// ── 아바타 + 배지 ─────────────────────────────────────────────────────────────
@Composable
private fun NotifAvatar(style: AvatarStyle, initial: String) {
    Box(modifier = Modifier.size(54.dp)) {
        // 아바타 원형 박스
        Box(
            modifier = Modifier
                .size(50.dp)
                .align(Alignment.TopStart)
                .clip(RoundedCornerShape(16.dp))
                .background(style.brush),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = initial,
                color      = Color.White,
                fontSize   = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        // 타입 배지 (우측 하단)
        Box(
            modifier = Modifier
                .size(22.dp)
                .align(Alignment.BottomEnd)
                .clip(RoundedCornerShape(8.dp))
                .background(style.badgeColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector     = style.badgeIcon,
                contentDescription = null,
                tint            = Color.White,
                modifier        = Modifier.size(12.dp)
            )
        }
    }
}

// ── 타입 태그 칩 ──────────────────────────────────────────────────────────────
@Composable
private fun NotifTag(text: String, textColor: Color, bgColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text       = text,
            fontSize   = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = textColor
        )
    }
}

// ── 빈 상태 ───────────────────────────────────────────────────────────────────
@Composable
private fun NotifEmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🔔", fontSize = 48.sp)
            Text(
                "새로운 알림이 없어요",
                fontSize   = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Ink2
            )
            Text(
                "좋아요, 채팅, 전화, 영상전화가 오면\n여기서 확인할 수 있어요",
                fontSize   = 13.sp,
                color      = Ink3,
                textAlign  = TextAlign.Center,
                lineHeight = 19.sp
            )
        }
    }
}
