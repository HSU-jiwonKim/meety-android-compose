package com.bugzero.meety.ui.chat

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.bugzero.meety.ui.feed.FeedConstants
import com.bugzero.meety.ui.theme.Brand2
import com.bugzero.meety.ui.theme.Ink
import com.bugzero.meety.ui.theme.Line
import com.bugzero.meety.ui.feed.MeetingDetailScreen
import com.bugzero.meety.ui.feed.TeamActionStatus
import com.bugzero.meety.ui.notification.NotificationViewModel
import com.bugzero.meety.ui.team.FriendProfileScreen
import com.bugzero.meety.ui.team.ProfilePreviewUiState
import com.bugzero.meety.ui.team.ReceivedLikeItem
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ChatListScreen(
    onChatClick: (chatId: String, roomName: String) -> Unit = { _, _ -> },
    onNotificationClick: () -> Unit = {},
    onLikeClick: () -> Unit = {},
    viewModel: ChatViewModel = viewModel(),
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val unreadCount by notificationViewModel.unreadCount.collectAsState()
    val currentUser = FirebaseAuth.getInstance().currentUser

    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            viewModel.refreshForAuthState()
        } else {
            viewModel.clearError()
        }
    }

    val chatList by viewModel.chatList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val friendList by viewModel.friendList.collectAsState()
    val isLoadingFriends by viewModel.isLoadingFriends.collectAsState()
    val pendingInvitations by viewModel.pendingInvitations.collectAsState()
    val selectedInvitationTeam by viewModel.selectedInvitationTeam.collectAsState()
    val selectedInvitation by viewModel.selectedInvitation.collectAsState()
    val receivedLikes by viewModel.requestList.collectAsState()
    val selectedUserProfile by viewModel.selectedUserProfile.collectAsState()
    val isLoadingProfile by viewModel.isLoadingProfile.collectAsState()

    // 받은 좋아요 리스트 실시간 구독
    LaunchedEffect(Unit) {
        viewModel.loadRequestList()
    }

    // 좋아요 발신자 프로필 다이얼로그
    var showLikeUserProfile by remember { mutableStateOf(false) }

    var showFriendScreen by remember { mutableStateOf(false) }

    // 다중 선택 모드
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedChatIds by remember { mutableStateOf(setOf<String>()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // 선택 모드에서 뒤로가기 → 선택 해제
    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedChatIds = emptySet()
    }

    // 삭제 확인 다이얼로그
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("채팅방 나가기", fontWeight = FontWeight.ExtraBold) },
            text = { Text("선택한 ${selectedChatIds.size}개의 채팅방에서 나가시겠어요?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    val toLeave = selectedChatIds.toList()
                    isSelectionMode = false
                    selectedChatIds = emptySet()
                    viewModel.leaveMultipleChatRooms(toLeave)
                }) {
                    Text("나가기", color = Color(0xFFFF5C8A), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("취소", color = Color(0xFF6B7280))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // 필터 탭: 전체 / 개인 / 단체(여러명 초대 그룹방) / 팀(팀 매칭방)
    val filters = listOf("전체", "개인", "단체", "팀")
    var selectedFilter by remember { mutableStateOf(filters[0]) }

    val filteredChatList = chatList.filter { chat ->
        when (selectedFilter) {
            "팀"   -> chat.type == "team"
            "단체" -> chat.type == "group"
            "개인" -> chat.type == "direct"
            else   -> true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F4F9))
        ) {
            // 항상 상단 바 표시
            ChatTopBar(
                onLikeClick = onLikeClick,
                onNotificationClick = onNotificationClick,
                onNewChatClick = {
                    viewModel.loadFriendList()
                    showFriendScreen = true
                },
                hasUnreadNotification = unreadCount > 0
            )

            // 항상 필터 탭 표시
            ChatFilterTabRow(
                filters = filters,
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )

            // 컨텐츠 영역
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color(0xFFA78BFA))
                                Spacer(Modifier.height(12.dp))
                                Text("채팅 목록을 불러오는 중...", color = Color(0xFF6B7280), fontSize = 14.sp)
                            }
                        }
                    }
                    errorMessage != null -> {
                        ChatErrorView(message = errorMessage!!, modifier = Modifier.align(Alignment.Center))
                    }
                    filteredChatList.isEmpty() && pendingInvitations.isEmpty() -> {
                        ChatEmptyView(modifier = Modifier.align(Alignment.Center))
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            // 받은 좋아요 카드 (전체 필터일 때만)
                            if (selectedFilter == "전체" && receivedLikes.isNotEmpty()) {
                                item {
                                    ReceivedLikesSection(
                                        likes         = receivedLikes,
                                        onAccept      = { like -> viewModel.acceptRequest(like.likeId) },
                                        onReject      = { like -> viewModel.rejectRequest(like.likeId) },
                                        onProfileClick = { like ->
                                            showLikeUserProfile = true
                                            viewModel.loadUserProfile(like.fromUserId)
                                        }
                                    )
                                }
                            }

                            // 대기 중인 초대 (전체 필터일 때만)
                            if (selectedFilter == "전체" && pendingInvitations.isNotEmpty()) {
                                item {
                                    PendingInvitationsSection(
                                        invitations = pendingInvitations,
                                        onAccept    = { inv -> viewModel.acceptInvitation(inv.id, inv.teamId, inv.chatId) {} },
                                        onReject    = { inv -> viewModel.rejectInvitation(inv.id) },
                                        onTapDetail = { inv -> viewModel.selectInvitation(inv) }
                                    )
                                }
                            }

                            // 채팅 목록 - 흰색 카드 컨테이너로 감싸기 (목업 .chat-list)
                            if (filteredChatList.isNotEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 20.dp)
                                            .padding(top = 6.dp)
                                            .fillMaxWidth()
                                            .shadow(2.dp, RoundedCornerShape(20.dp))
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color.White)
                                            .border(1.dp, Color(0xFFF1EFF5), RoundedCornerShape(20.dp))
                                    ) {
                                        Column {
                                            filteredChatList.forEachIndexed { index, chat ->
                                                ChatListItem(
                                                    chat = chat,
                                                    timeText = viewModel.formatChatListTime(chat.lastMessageAt),
                                                    isSelectionMode = isSelectionMode,
                                                    isSelected = selectedChatIds.contains(chat.id),
                                                    onClick = {
                                                        if (isSelectionMode) {
                                                            // 팀 채팅방은 선택 불가 (나가기 금지)
                                                            if (chat.type != "team") {
                                                                selectedChatIds = if (selectedChatIds.contains(chat.id))
                                                                    selectedChatIds - chat.id
                                                                else
                                                                    selectedChatIds + chat.id
                                                                if (selectedChatIds.isEmpty()) isSelectionMode = false
                                                            }
                                                        } else {
                                                            onChatClick(chat.id, chat.teamName)
                                                        }
                                                    },
                                                    onLongClick = {
                                                        // 팀 채팅방은 꾹 눌러도 선택 모드 진입 불가
                                                        if (chat.type != "team") {
                                                            isSelectionMode = true
                                                            selectedChatIds = selectedChatIds + chat.id
                                                        }
                                                    }
                                                )
                                                if (index < filteredChatList.lastIndex) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(1.dp)
                                                            .background(Color(0xFFF1EFF5))
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 새 채팅 화면 (애니메이션)
        AnimatedVisibility(
            visible = showFriendScreen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            FriendSelectionFullScreen(
                friends = friendList,
                isLoading = isLoadingFriends,
                onDismiss = { showFriendScreen = false },
                onCreateChat = { selectedFriends: List<UserProfileData>, customName: String ->
                    if (selectedFriends.size == 1) {
                        viewModel.createOrGetDirectChat(
                            friend = selectedFriends.first(),
                            onSuccess = { cid: String, name: String ->
                                showFriendScreen = false
                                onChatClick(cid, name)
                            },
                            onFailure = { msg -> Log.e("ChatListScreen", msg) }
                        )
                    } else {
                        viewModel.createChatWithFriends(
                            selectedFriends = selectedFriends,
                            customRoomName = customName,
                            onSuccess = { cid: String, name: String ->
                                showFriendScreen = false
                                onChatClick(cid, name)
                            },
                            onFailure = { msg -> Log.e("ChatListScreen", msg) }
                        )
                    }
                }
            )
        }

        // ── 다중 선택 모드 하단 액션바 ──
        if (isSelectionMode && !showFriendScreen && selectedInvitation == null) {
            SelectionActionBar(
                selectedCount = selectedChatIds.size,
                onCancel = { isSelectionMode = false; selectedChatIds = emptySet() },
                onDelete = { if (selectedChatIds.isNotEmpty()) showDeleteConfirm = true },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // ── 좋아요 발신자 프로필 — 인스타그램 스타일 전체화면 슬라이드 오버레이 ──
        AnimatedVisibility(
            visible = showLikeUserProfile,
            enter   = slideInVertically(initialOffsetY = { it }),
            exit    = slideOutVertically(targetOffsetY = { it })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                when {
                    isLoadingProfile || selectedUserProfile == null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color(0xFF8B5CF6))
                                Spacer(Modifier.height(12.dp))
                                Text("프로필을 불러오는 중...", fontSize = 14.sp, color = Color(0xFF6B7280))
                            }
                        }
                    }
                    else -> {
                        val profile = selectedUserProfile!!
                        FriendProfileScreen(
                            profile = ProfilePreviewUiState(
                                id             = profile.userId,
                                name           = profile.name,
                                profileImageUrl = profile.profileImageUrl,
                                profileImages  = if (profile.profileImageUrl.isNotBlank())
                                    listOf(profile.profileImageUrl) else emptyList(),
                                department     = profile.department,
                                age            = profile.age,
                                mbti           = profile.mbti,
                                bio            = profile.bio,
                                location       = profile.location,
                                height         = profile.height,
                                interests      = profile.interests,
                                foodLikes      = profile.foodLikes,
                                foodDislikes   = profile.foodDislikes,
                                schedule       = emptyMap()
                            ),
                            onBack = {
                                showLikeUserProfile = false
                                viewModel.clearUserProfile()
                            }
                        )
                    }
                }
            }
        }

        // ── 초대 상세보기 오버레이 (MeetingDetailScreen, INVITED 상태) ──
        AnimatedVisibility(
            visible = selectedInvitation != null,
            enter   = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit    = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            val inv = selectedInvitation
            if (inv != null) {
                if (selectedInvitationTeam == null) {
                    // 팀 정보 로딩 중 — 전체 화면 스피너로 대기
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFFA78BFA))
                            Spacer(Modifier.height(12.dp))
                            Text("팀 정보를 불러오는 중...", fontSize = 14.sp, color = Color(0xFF6B7280))
                        }
                    }
                } else {
                    MeetingDetailScreen(
                        team             = selectedInvitationTeam,
                        status           = TeamActionStatus.INVITED,
                        onBackClick      = { viewModel.clearSelectedInvitation() },
                        onAcceptInvite   = {
                            viewModel.acceptInvitation(inv.id, inv.teamId, inv.chatId) {
                                viewModel.clearSelectedInvitation()
                                onChatClick(inv.chatId, inv.teamName)
                            }
                        },
                        onRejectInvite   = {
                            viewModel.rejectInvitation(inv.id)
                            viewModel.clearSelectedInvitation()
                        }
                    )
                }
            }
        }
    }
}

// 목업 .chat-row 구현
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatListItem(
    chat: ChatPreview,
    timeText: String,
    onClick: () -> Unit,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onLongClick: () -> Unit = {}
) {
    val isTeam   = chat.type == "team"
    val isGroup  = chat.type == "group"
    val isDirect = chat.type == "direct"

    // 개인/단체채팅 아바타 그라데이션 색상 (teamName 해시)
    val avatarColors = listOf(
        Color(0xFF7B5CFF), Color(0xFFFF5C8A), Color(0xFF26A69A),
        Color(0xFF1E88E5), Color(0xFFF5A623), Color(0xFFE91E8C)
    )
    val avatarColor = avatarColors[(chat.teamName.hashCode() and Int.MAX_VALUE) % avatarColors.size]

    // 타입 태그: 팀(핑크) / 단체(민트) / 개인(보라)
    val typeLabel    = when {
        isTeam  -> "팀"
        isGroup -> "단체"
        else    -> "개인"
    }
    val typeBgColor  = when {
        isTeam  -> Color(0xFFFFECF3)   // pink-soft
        isGroup -> Color(0xFFE5F8F3)   // mint-soft
        else    -> Color(0xFFF2EEFF)   // violet-soft
    }
    val typeTextColor = when {
        isTeam  -> Color(0xFFE0457A)
        isGroup -> Color(0xFF0F9173)
        else    -> Color(0xFF6D49E0)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) Color(0xFFF2EEFF) else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        // 선택 모드 체크박스
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color(0xFF7B5CFF) else Color.Transparent)
                    .border(2.dp, if (isSelected) Color(0xFF7B5CFF) else Color(0xFFD1D5DB), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Text("✓", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        // 아바타 (목업 .chat-row .pic)
        Box(modifier = Modifier.size(54.dp)) {
            if (chat.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = chat.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(18.dp))
                )
            } else if (isTeam) {
                // 팀: 연보라 배경 + 큰 이모지
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFF2EEFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chat.emoji.ifBlank { "👥" },
                        fontSize = 24.sp
                    )
                }
            } else if (isGroup) {
                // 단체: 참여자 프로필 그리드 (최대 4명, 본인 제외)
                if (chat.participantImages.isNotEmpty()) {
                    GroupAvatarGrid(
                        images = chat.participantImages,
                        modifier = Modifier.size(54.dp)
                    )
                } else {
                    // 이미지 로드 전 폴백: 민트 배경 + 이모지
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFFE5F8F3)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chat.emoji.ifBlank { "💬" },
                            fontSize = 24.sp
                        )
                    }
                }
            } else {
                // 개인: 그라데이션 배경 + 이니셜
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(avatarColor, avatarColor.copy(alpha = 0.75f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chat.teamName.firstOrNull()?.toString() ?: "?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            // 온라인 점 (목업 .pic .on) — 개인채팅에만
            if (isDirect) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color(0xFFF4F4F8)) // border 역할 (bg색)
                        .padding(2.5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF19C37D))
                )
            }
        }

        // 텍스트 영역 (목업 .c)
        Column(modifier = Modifier.weight(1f)) {
            // 상단 행: 이름+태그 / 시간 (목업 .top)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = chat.teamName,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = Color(0xFF17161D),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (chat.participantCount >= 3) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${chat.participantCount}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF9B98A6)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(typeBgColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = typeLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = typeTextColor
                        )
                    }
                    // NEW 배지: 처음 들어간 팀 채팅방에만 표시, 입장하면 사라짐
                    if (chat.isNew) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF7B5CFF), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "NEW",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = timeText,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF9B98A6)
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // 하단 행: 마지막 메시지 / 안읽음 배지 (목업 .msg + .unread)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (chat.isNew) "방금 매칭됐어요! 인사를 건네보세요" else chat.lastMessage,
                    fontSize = 13.sp,
                    color = if (chat.isNew) Color(0xFF7B5CFF) else Color(0xFF56535F),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    fontWeight = if (chat.isNew) FontWeight.Medium else FontWeight.Normal
                )
                if (chat.unreadCount > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                            .background(Color(0xFFFF5C8A), RoundedCornerShape(999.dp))
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (chat.unreadCount > 99) "99+" else "${chat.unreadCount}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatTopBar(
    onLikeClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onNewChatClick: () -> Unit,
    hasUnreadNotification: Boolean = false
) {
    // 목업: .appbar { padding:6px 20px 14px }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 목업: h2 { font-size:23px; font-weight:800; letter-spacing:-.02em }
        Text(
            "채팅",
            fontSize = 23.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.46).sp,
            color = Color(0xFF17161D)
        )

        // 오른쪽: 새 채팅방(+) + 알림 벨
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 새 채팅방 만들기 버튼
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shadow(2.dp, RoundedCornerShape(13.dp))
                    .background(Color.White, RoundedCornerShape(13.dp))
                    .border(1.dp, Line, RoundedCornerShape(13.dp))
                    .clickable(onClick = onNewChatClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "새 채팅방",
                    tint = Ink,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 알림 버튼
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shadow(2.dp, RoundedCornerShape(13.dp))
                    .background(Color.White, RoundedCornerShape(13.dp))
                    .border(1.dp, Line, RoundedCornerShape(13.dp))
                    .clickable(onClick = onNotificationClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Notifications,
                    contentDescription = "알림",
                    tint = Ink,
                    modifier = Modifier.size(20.dp)
                )
                if (hasUnreadNotification) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-9).dp, y = 8.dp)
                            .size(8.dp)
                            .background(Brand2, CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                    )
                }
            }
        }
    }
}

// ===== 필터 탭 (세그먼티드 컨트롤) =====
// 목업: .seg { background:#EAE8F0; border-radius:14px; padding:4px; margin:0 20px 4px; gap:3px }
// 목업: .seg button.on { background:white; color:#17161D; box-shadow:sh-sm }
// 목업: .seg button { font-size:13.5px; font-weight:700; color:#9B98A6 }
@Composable
private fun ChatFilterTabRow(
    filters: List<String>,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFEAE8F0))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        filters.forEach { filter ->
            val isSelected = selectedFilter == filter
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .then(
                        if (isSelected)
                            Modifier
                                .shadow(2.dp, RoundedCornerShape(11.dp))
                                .background(Color.White)
                        else
                            Modifier.background(Color.Transparent)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onFilterSelected(filter) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = filter,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color(0xFF17161D) else Color(0xFF9B98A6)
                )
            }
        }
    }
}

@Composable
private fun ChatEmptyView(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "💬", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("아직 채팅방이 없어요", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
        Spacer(modifier = Modifier.height(8.dp))
        Text("팀을 매칭하고 대화를 시작해보세요!", fontSize = 14.sp, color = Color(0xFF6B7280))
    }
}

@Composable
private fun ChatErrorView(message: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "⚠️", fontSize = 56.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, fontSize = 14.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FriendSelectionFullScreen(
    friends: List<UserProfileData>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onCreateChat: (List<UserProfileData>, String) -> Unit // ✨ String(커스텀 방 이름) 매개변수 추가!
) {
    var selectedFriends by remember { mutableStateOf(setOf<UserProfileData>()) }
    var showGroupSetup by remember { mutableStateOf(false) } // ✨ 그룹 설정 화면 상태

    BackHandler {
        if (showGroupSetup) showGroupSetup = false // 설정 화면이면 뒤로가기 시 친구선택으로
        else onDismiss()
    }

    if (showGroupSetup) {
        // ✨ [새로운 플로우] 단체톡 설정 화면
        GroupChatSetupScreen(
            selectedFriends = selectedFriends.toList(),
            onBack = { showGroupSetup = false },
            onConfirm = { customName ->
                onCreateChat(selectedFriends.toList(), customName)
            }
        )
    } else {
        // [기존 플로우] 친구 선택 화면
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("대화상대 선택", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "닫기") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            bottomBar = {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            if (selectedFriends.size == 1) {
                                // 1:1 채팅은 설정 화면 없이 바로 생성!
                                onCreateChat(selectedFriends.toList(), "")
                            } else {
                                // ✨ 2명 이상이면 설정 화면 띄우기!
                                showGroupSetup = true
                            }
                        },
                        enabled = selectedFriends.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA78BFA), disabledContainerColor = Color(0xFFD1D5DB))
                    ) {
                        Text(
                            text = if (selectedFriends.isEmpty()) "상대를 선택해주세요" else "${selectedFriends.size}명과 채팅하기",
                            color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            containerColor = Color.White
        ) { innerPadding ->
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFA78BFA)) }
                } else if (friends.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🫙", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("아직 대화 가능한 친구가 없어요", fontSize = 14.sp, color = Color(0xFF6B7280))
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(friends) { friend ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedFriends = if (selectedFriends.contains(friend)) selectedFriends - friend else selectedFriends + friend
                                    }
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = selectedFriends.contains(friend), onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFA78BFA)))
                                Spacer(modifier = Modifier.width(16.dp))
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEDE9FE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (friend.profileImageUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = friend.profileImageUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text("👤", fontSize = 20.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(text = friend.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
                                    if (friend.department.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = "${friend.department} · ${friend.mbti}", fontSize = 13.sp, color = Color(0xFF6B7280))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── 받은 좋아요 섹션 ─────────────────────────────────────────────────────────
// 카드 여러 개: 겹침(스택) UI. 오른쪽으로 스와이프하면 해당 알림 카드 숨김.
@Composable
private fun ReceivedLikesSection(
    likes: List<ReceivedLikeItem>,
    onAccept: (ReceivedLikeItem) -> Unit,
    onReject: (ReceivedLikeItem) -> Unit,
    onProfileClick: (ReceivedLikeItem) -> Unit = {}
) {
    // 오른쪽 스와이프로 영구 숨긴 ID — SharedPreferences에 저장해 앱 재시작 후에도 유지
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("dismissed_likes", android.content.Context.MODE_PRIVATE)
    }
    var dismissedIds by remember {
        mutableStateOf(
            prefs.getStringSet("ids", emptySet())?.toSet() ?: emptySet()
        )
    }
    val visibleLikes = likes.filter { !dismissedIds.contains(it.likeId) }

    if (visibleLikes.isEmpty()) return

    val displayCount = minOf(visibleLikes.size, 3)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 14.dp)
    ) {
        // ── 뒤에 겹쳐 보이는 카드들 (먼저 그릴수록 z-order 낮음) ──
        // 앞 카드가 착지한 직후 딜레이 없이 슬쩍 나타남
        if (displayCount >= 3) {
            AnimatedVisibility(
                visible = true,
                enter   = fadeIn(animationSpec = tween(220, delayMillis = 180)) +
                          slideInVertically(animationSpec = tween(300, delayMillis = 180)) { -40 }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .offset(y = 14.dp)
                        .shadow(4.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(FeedConstants.GradientPurplePink)
                        .alpha(0.45f)
                        .height(188.dp)
                )
            }
        }
        if (displayCount >= 2) {
            AnimatedVisibility(
                visible = true,
                enter   = fadeIn(animationSpec = tween(200, delayMillis = 100)) +
                          slideInVertically(animationSpec = tween(280, delayMillis = 100)) { -30 }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                        .offset(y = 7.dp)
                        .shadow(6.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(FeedConstants.GradientPurplePink)
                        .alpha(0.65f)
                        .height(188.dp)
                )
            }
        }

        // ── 앞 카드 (스와이프 가능) ──
        SwipeableLikeCard(
            like          = visibleLikes[0],
            remainingCount = visibleLikes.size,
            onAccept      = { onAccept(visibleLikes[0]) },
            onReject      = { onReject(visibleLikes[0]) },
            onProfileClick = { onProfileClick(visibleLikes[0]) },
            onDismiss     = {
                val newIds = dismissedIds + visibleLikes[0].likeId
                dismissedIds = newIds
                prefs.edit().putStringSet("ids", newIds).apply()
            }
        )
    }
}

// ── 스와이프 가능한 좋아요 카드 ────────────────────────────────────────────────
@Composable
private fun SwipeableLikeCard(
    like: ReceivedLikeItem,
    remainingCount: Int,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onProfileClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    // like.likeId를 key로 — 카드가 바뀌면 모든 상태 초기화
    val offsetX = remember(like.likeId) { Animatable(0f) }
    val swipeThreshold = 200f

    // ── 입장 애니메이션: 위에서 날아와 꽂히는 효과 ─────────────────────────────
    val entryY        = remember(like.likeId) { Animatable(-900f) }   // 위에서 낙하
    val entryRotZ     = remember(like.likeId) { Animatable(14f) }     // 기울어진 채 날아옴
    val entryRotX     = remember(like.likeId) { Animatable(-22f) }    // 앞으로 넘어지며 꽂힘 (3D)
    val entryScale    = remember(like.likeId) { Animatable(0.78f) }   // 작게 시작 → 커지며 꽂힘
    val entryAlpha    = remember(like.likeId) { Animatable(0f) }

    LaunchedEffect(like.likeId) {
        // 알파: 빠르게 페이드인 (카드가 갑자기 등장하는 느낌)
        launch { entryAlpha.animateTo(1f, animationSpec = tween(100)) }

        // Y축 낙하: 스프링 반동으로 꽂히는 느낌
        launch {
            entryY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.58f,          // 중간 반동 — 꽂힌 뒤 살짝 튕김
                    stiffness    = Spring.StiffnessMedium
                )
            )
        }
        // 회전: 기울어진 채 날아와 반동으로 정면 정렬
        launch {
            entryRotZ.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.65f,
                    stiffness    = Spring.StiffnessMediumLow
                )
            )
        }
        // X축 회전: 위에서 앞으로 넘어지며 정착 (3D 입체감)
        launch {
            entryRotX.animateTo(
                targetValue = -1.5f,
                animationSpec = spring(
                    dampingRatio = 0.60f,
                    stiffness    = Spring.StiffnessMedium
                )
            )
        }
        // 스케일: 작게 시작 → 꽂히면서 약간 오버슈트 후 정착
        launch {
            entryScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.55f,
                    stiffness    = Spring.StiffnessMedium
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // 스와이프: offset으로 터치 영역도 함께 이동
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .graphicsLayer {
                // 입장 + 스와이프 효과 합산
                translationY   = entryY.value
                rotationZ      = entryRotZ.value + (offsetX.value / 40f).coerceIn(-8f, 8f)
                rotationX      = entryRotX.value
                scaleX         = entryScale.value
                scaleY         = entryScale.value
                alpha          = entryAlpha.value * (1f - offsetX.value / 800f).coerceIn(0f, 1f)
                cameraDistance = 10f * density   // 입체감 원근 거리
            }
            .pointerInput(like.likeId) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        scope.launch {
                            // 왼쪽 드래그 차단 (오른쪽만 허용)
                            offsetX.snapTo((offsetX.value + dragAmount).coerceAtLeast(0f))
                        }
                    },
                    onDragEnd = {
                        scope.launch {
                            if (offsetX.value > swipeThreshold) {
                                offsetX.animateTo(1400f, animationSpec = tween(220))
                                onDismiss()
                            } else {
                                offsetX.animateTo(0f, animationSpec = spring())
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch { offsetX.animateTo(0f, animationSpec = spring()) }
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation    = 22.dp,
                    shape        = RoundedCornerShape(20.dp),
                    ambientColor = Color(0xFF6B00FF).copy(alpha = 0.45f),
                    spotColor    = Color(0xFFFF2D87).copy(alpha = 0.50f)
                )
                .clip(RoundedCornerShape(20.dp))
                .background(FeedConstants.GradientPurplePink)
        ) {
            // 목업 .deco: 우측 상단 반투명 장식 원
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .offset(x = 70.dp, y = (-30).dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(15.dp)
            ) {
                // ── 헤더 행: 💜 + 팀이름 + 카드수 배지 + 프로필 ──
                Row(
                    modifier            = Modifier.fillMaxWidth(),
                    verticalAlignment   = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment   = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("💜", fontSize = 14.sp)
                        Text(
                            text = if (like.toTeamName.isNotBlank())
                                "${like.toTeamName}에 받은 좋아요"
                            else
                                "받은 좋아요",
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Color.White,
                            maxLines   = 1,
                            overflow   = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier   = Modifier.weight(1f, fill = false)
                        )
                        // 남은 카드 수 배지
                        if (remainingCount > 1) {
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text       = "$remainingCount",
                                    fontSize   = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color      = Color.White
                                )
                            }
                        }
                    }
                    // 프로필 사진 — 클릭하면 발신자 프로필 보기
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f))
                            .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                            .clickable { onProfileClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (like.fromUserProfileImage.isNotBlank()) {
                            AsyncImage(
                                model            = like.fromUserProfileImage,
                                contentDescription = null,
                                contentScale     = ContentScale.Crop,
                                modifier         = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else {
                            Text(
                                text       = like.fromUserName.firstOrNull()?.toString() ?: "?",
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = Color.White
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 이름
                Text(
                    text       = like.fromUserName.ifBlank { "알 수 없음" },
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 16.sp,
                    color      = Color.White
                )

                // 학과 · MBTI
                val subText = listOf(like.fromUserDepartment, like.fromUserMbti)
                    .filter { it.isNotBlank() }.joinToString(" · ")
                if (subText.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text       = subText,
                        fontSize   = 12.5.sp,
                        color      = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (like.toTeamName.isNotBlank())
                        "회원님의 ${like.toTeamName}에 합류하고 싶어해요"
                    else
                        "회원님 팀에 합류하고 싶어해요",
                    fontSize   = 12.5.sp,
                    color      = Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(13.dp))

                // ── 수락 / 거절 버튼 ──
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(13.dp))
                            .background(Color.White)
                            .clickable { onAccept() }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "수락",
                            fontSize   = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Color(0xFF7B5CFF)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(13.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { onReject() }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "거절",
                            fontSize   = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Color.White
                        )
                    }
                }
            }

            // ── 광택 오버레이: 대각선 흰빛 광택 (카드 입체감) ──
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            0.00f to Color.White.copy(alpha = 0.22f),
                            0.40f to Color.White.copy(alpha = 0.06f),
                            1.00f to Color.Transparent,
                            start = Offset.Zero,
                            end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
            )

            // ── 상단 엣지 하이라이트: 얇은 흰 빛 띠 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.55f),
                                Color.White.copy(alpha = 0.55f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // ── 하단 엣지 그림자: 카드 두께감 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0xFF3A0080).copy(alpha = 0.28f),
                                Color(0xFF3A0080).copy(alpha = 0.28f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

// ── 대기 중인 팀 초대 섹션 ──────────────────────────────────────────
// 목업: .invite-card { margin:8px 20px 6px; padding:15px; border-radius:20px;
//   background:grad; box-shadow:0 10px 26px rgba(123,92,255,.3); overflow:hidden }
// 목업: .deco { position:absolute; right:-30px; top:-30px; width:130px; height:130px;
//   border-radius:50%; background:rgba(255,255,255,.12) }
@Composable
private fun PendingInvitationsSection(
    invitations: List<TeamInvitation>,
    onAccept: (TeamInvitation) -> Unit,
    onReject: (TeamInvitation) -> Unit,
    onTapDetail: (TeamInvitation) -> Unit
) {
    // 섹션 헤더 없이, 초대 카드 목록만 (목업과 동일)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 6.dp)
    ) {
        invitations.forEach { inv ->
            // 목업 .invite-card: 그라데이션 카드 + .deco 장식 원
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = Color(0xFF7B5CFF).copy(alpha = 0.3f),
                        spotColor = Color(0xFF7B5CFF).copy(alpha = 0.3f)
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(FeedConstants.GradientPurplePink)
                    .clickable { onTapDetail(inv) }
            ) {
                // .deco: 장식용 반투명 원 (오른쪽 위)
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .offset(x = 70.dp, y = (-30).dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                )

                // 카드 내용
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(15.dp)
                ) {
                    // .h: 이모지 + "받은 초대" (목업 font-size:13px, font-weight:800)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(inv.teamEmoji.ifBlank { "💌" }, fontSize = 14.sp)
                        Text(
                            "받은 초대",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    // .nm: 팀 이름 (font-size:16px, font-weight:800)
                    Text(
                        inv.teamName,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Spacer(Modifier.height(2.dp))
                    // .sub: 설명 (font-size:12.5px, opacity:.9)
                    Text(
                        "팀원 초대장이 도착했어요",
                        fontSize = 12.5.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(13.dp))
                    // .btns: 수락/거절 (gap:9dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        // .accept: white bg, brand-1 text
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(13.dp))
                                .background(Color.White)
                                .clickable { onAccept(inv) }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "수락",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF7B5CFF)
                            )
                        }
                        // .reject: rgba(255,255,255,.2) bg, white text
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(13.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable { onReject(inv) }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "거절",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

// ✨ 카카오톡 스타일 '그룹채팅방 정보 설정' 화면 부품 (파일 맨 아래에 추가!)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupChatSetupScreen(
    selectedFriends: List<UserProfileData>,
    onBack: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val defaultName = selectedFriends.joinToString(", ") { it.name }
    var roomNameInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("그룹채팅방 정보 설정", fontSize = 18.sp, fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기") }
                },
                actions = {
                    TextButton(onClick = { onConfirm(roomNameInput) }) {
                        Text("확인", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 프로필 썸네일 (카톡 스타일 대체용 둥근 프로필)
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape).background(Color(0xFFF3F4F6)),
                contentAlignment = Alignment.Center
            ) {
                Text("👥", fontSize = 40.sp)
            }

            Spacer(modifier = Modifier.height(40.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text("채팅방 이름", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))

                // 이름 입력 텍스트 필드
                TextField(
                    value = roomNameInput,
                    onValueChange = { if (it.length <= 50) roomNameInput = it },
                    placeholder = { Text(defaultName, color = Color.LightGray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF3F4F6),
                        unfocusedContainerColor = Color(0xFFF3F4F6),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "채팅시작 전, 내가 설정한 그룹채팅방의 사진과 이름은 다른 모든 대화상대에게도 동일하게 보입니다.",
                    fontSize = 12.sp, color = Color.Gray, lineHeight = 18.sp
                )
            }
        }
    }
}

// ── 다중 선택 모드 하단 액션바 ─────────────────────────────────────────────
@Composable
private fun SelectionActionBar(
    selectedCount: Int,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFF1EFF5), RoundedCornerShape(20.dp))
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 취소 버튼
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF3F4F6))
                    .clickable(onClick = onCancel)
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text("취소", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
            }

            // 선택 개수
            Text(
                text = "${selectedCount}개 선택",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF17161D)
            )

            // 나가기(삭제) 버튼
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selectedCount > 0) Color(0xFFFF5C8A) else Color(0xFFD1D5DB))
                    .clickable(enabled = selectedCount > 0, onClick = onDelete)
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text("나가기", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ── 단체 채팅방 참여자 원형 프로필 그리드 (카카오톡 스타일) ─────────────────
// 1명: 큰 원 중앙 | 2명: 원 2개 나란히 | 3명: 왼쪽 1 + 오른쪽 2 세로 | 4명: 2×2
@Composable
private fun GroupAvatarGrid(images: List<String>, modifier: Modifier = Modifier) {
    val count = images.size.coerceIn(1, 4)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        when (count) {
            1 -> CircleProfileImage(images[0], 42)
            2 -> Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleProfileImage(images[0], 24)
                Spacer(modifier = Modifier.width(3.dp))
                CircleProfileImage(images[1], 24)
            }
            3 -> Box(modifier = Modifier.fillMaxSize()) {
                // 삼각형 배치: 위 2개 + 아래 중앙 1개 (카카오톡 스타일)
                // 뒤에서 앞 순서로 그려야 앞 아바타가 위에 보임
                OverlappedAvatar(images[2], 26, Modifier.offset(13.dp, 24.dp)) // 아래 중앙
                OverlappedAvatar(images[1], 26, Modifier.offset(24.dp, 1.dp))  // 위 오른쪽
                OverlappedAvatar(images[0], 26, Modifier.offset(1.dp,  1.dp))  // 위 왼쪽
            }
            else -> Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.Center) {
                    CircleProfileImage(images[0], 24)
                    Spacer(modifier = Modifier.width(3.dp))
                    CircleProfileImage(images[1], 24)
                }
                Spacer(modifier = Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.Center) {
                    CircleProfileImage(images[2], 24)
                    Spacer(modifier = Modifier.width(3.dp))
                    CircleProfileImage(images[3], 24)
                }
            }
        }
    }
}

/**
 * 3명 겹침 레이아웃 전용 아바타 — 흰색 테두리로 겹친 경계를 구분함.
 * modifier 로 Alignment(TopStart / Center / BottomEnd) 위치를 외부에서 주입.
 */
@Composable
private fun OverlappedAvatar(imageUrl: String, sizeDp: Int, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape((sizeDp * 0.28f).dp)
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .border(1.5.dp, Color.White, shape)
            .clip(shape)
            .background(Color(0xFFD1D5DB)),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(text = "👤", fontSize = (sizeDp * 0.35f).sp)
        }
    }
}

/** 그리드용 프로필 이미지 셀. sizeDp: 크기(dp 정수) */
@Composable
private fun CircleProfileImage(imageUrl: String, sizeDp: Int) {
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape((sizeDp * 0.28f).dp))
            .background(Color(0xFFD1D5DB)),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(text = "👤", fontSize = (sizeDp * 0.4f).sp)
        }
    }
}
