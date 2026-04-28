package com.bugzero.meety.ui.chat

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bugzero.meety.ui.feed.FeedConstants
import com.bugzero.meety.ui.feed.MeetingDetailScreen
import com.bugzero.meety.ui.feed.TeamActionStatus
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ChatListScreen(
    onChatClick: (chatId: String, roomName: String) -> Unit = { _, _ -> },
    onNotificationClick: () -> Unit = {},
    onLikeClick: () -> Unit = {},
    viewModel: ChatViewModel = viewModel()
) {
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

    var showFriendScreen by remember { mutableStateOf(false) }

    // ✨ 1. 필터 목록과 상태 (여기서 필터링 처리)
    val filters = listOf("전체", "팀채팅", "개인채팅", "단체채팅")
    var selectedFilter by remember { mutableStateOf(filters[0]) }

    val filteredChatList = chatList.filter { chat ->
        when (selectedFilter) {
            "팀채팅" -> chat.type == "team"
            "개인채팅" -> chat.type == "direct"
            "단체채팅" -> chat.type == "group"
            else -> true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FeedConstants.BackgroundGray)
        ) {
            // 기존 예원님의 예쁜 상단바 유지!
            ChatTopBar(
                onLikeClick = onLikeClick,
                onNotificationClick = onNotificationClick,
                onNewChatClick = {
                    viewModel.loadFriendList()
                    showFriendScreen = true
                }
            )

            // ✨ 2. 카카오톡 스타일 상단 필터 UI 추가
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color(0xFF7C3AED) else Color(0xFFF3F4F6))
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) Color.White else Color(0xFF4B5563),
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

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
                    // 초대도 없고 채팅도 없을 때만 빈 화면
                    filteredChatList.isEmpty() && pendingInvitations.isEmpty() -> {
                        ChatEmptyView(modifier = Modifier.align(Alignment.Center))
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            // ── 대기 중인 초대 섹션 (전체 필터일 때만, 초대가 있을 때만) ──
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

                            items(items = filteredChatList, key = { it.id }) { chat ->
                                ChatListItem(
                                    chat = chat,
                                    timeText = viewModel.formatTime(chat.lastMessageAt),
                                    onClick = { onChatClick(chat.id, chat.teamName) }
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF3F4F6))
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

@Composable
private fun ChatListItem(chat: ChatPreview, timeText: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(FeedConstants.GradientPurplePink),
            contentAlignment = Alignment.Center
        ) {
            Text(text = chat.emoji, fontSize = 24.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = chat.teamName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // ✨ 수정된 부분: 방의 ID나 이름 따지지 말고, 그냥 '참여자가 3명 이상'이면 무조건 띄워줍니다!
                if (chat.participantCount > 2) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${chat.participantCount}",
                        fontSize = 14.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = chat.lastMessage,
                fontSize = 14.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(text = timeText, fontSize = 12.sp, color = Color(0xFF9CA3AF))
            if (chat.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                        .background(Color(0xFFA78BFA), CircleShape)
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "${chat.unreadCount}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ChatTopBar(
    onLikeClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onNewChatClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(FeedConstants.GradientPurplePink, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "Meety",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.drawWithCache {
                    val brush = FeedConstants.GradientPurplePink
                    onDrawWithContent {
                        drawContent()
                        drawRect(brush, blendMode = BlendMode.SrcAtop)
                    }
                }
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onNewChatClick) {
                Box(modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "새 채팅",
                        tint = FeedConstants.IconGray,
                        modifier = Modifier.size(24.dp).align(Alignment.TopStart)
                    )
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomEnd)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = FeedConstants.IconGray,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Box {
                IconButton(onClick = onNotificationClick) {
                    Icon(Icons.Default.Notifications, contentDescription = "알림", tint = FeedConstants.IconGray)
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(FeedConstants.AccentPink, CircleShape)
                        .align(Alignment.TopEnd)
                        .offset(x = (-10).dp, y = 10.dp)
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
                                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(FeedConstants.GradientPurplePink), contentAlignment = Alignment.Center) { Text("👤", fontSize = 20.sp) }
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

// ── 대기 중인 팀 초대 섹션 ──────────────────────────────────────────

@Composable
private fun PendingInvitationsSection(
    invitations: List<TeamInvitation>,
    onAccept: (TeamInvitation) -> Unit,
    onReject: (TeamInvitation) -> Unit,
    onTapDetail: (TeamInvitation) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // 섹션 헤더
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("💌 받은 초대", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .background(Color(0xFFA78BFA), CircleShape)
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text("${invitations.size}", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // 초대 카드 목록
        invitations.forEach { inv ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF5F3FF))
                    .clickable { onTapDetail(inv) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 팀 아이콘
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(FeedConstants.GradientPurplePink),
                    contentAlignment = Alignment.Center
                ) {
                    Text(inv.teamEmoji, fontSize = 20.sp)
                }

                Spacer(Modifier.width(12.dp))

                // 팀 이름 + 안내 문구
                Column(modifier = Modifier.weight(1f)) {
                    Text(inv.teamName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF1F2937))
                    Text("팀원 초대장이 도착했어요", fontSize = 12.sp, color = Color(0xFF7C3AED))
                }

                // 거절 / 수락 버튼
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick        = { onReject(inv) },
                        shape          = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier       = Modifier.height(36.dp)
                    ) {
                        Text("거절", fontSize = 13.sp, color = Color(0xFF6B7280))
                    }
                    Button(
                        onClick        = { onAccept(inv) },
                        shape          = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier       = Modifier.height(36.dp),
                        colors         = ButtonDefaults.buttonColors(containerColor = Color(0xFFA78BFA))
                    ) {
                        Text("수락", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        HorizontalDivider(
            modifier  = Modifier.padding(top = 4.dp),
            color     = Color(0xFFF3F4F6),
            thickness = 1.dp
        )
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
