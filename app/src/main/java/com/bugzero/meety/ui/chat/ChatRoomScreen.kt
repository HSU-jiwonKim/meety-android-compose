package com.bugzero.meety.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.bugzero.meety.ui.team.ReceivedLikeItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    chatId: String = "",
    roomName: String = "채팅방",
    onBackClick: () -> Unit = {},
    viewModel: ChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val currentRoomName by viewModel.roomName.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val participants by viewModel.participants.collectAsState()
    val selectedUserProfile by viewModel.selectedUserProfile.collectAsState()
    val isLoadingProfile by viewModel.isLoadingProfile.collectAsState()
    val requestList by viewModel.requestList.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var selectedParticipant by remember { mutableStateOf<ParticipantItem?>(null) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showDisbandDialog by remember { mutableStateOf(false) }
    var selectedNewLeader by remember { mutableStateOf<ParticipantItem?>(null) }

    LaunchedEffect(chatId) {
        viewModel.enterChatRoom(chatId, roomName)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    // ── 일반 멤버 나가기 다이얼로그 ──────────────────────────────────
    // ── 일반 멤버 나가기 다이얼로그 ──────────────────────────────────
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("채팅방 나가기") },
            text = { Text("채팅방을 나가면 대화 내용이 삭제돼요. 나가시겠어요?") },
            confirmButton = {
                TextButton(onClick = {
                    // ✅ 수정된 부분: 뷰모델에 성공/실패 콜백을 넘겨줍니다.
                    viewModel.leaveChatRoom(
                        chatId = chatId,
                        onSuccess = {
                            showLeaveDialog = false
                            onBackClick() // 데이터가 다 지워진 후 안전하게 뒤로 가기!
                        },
                        onFailure = { errorMessage ->
                            showLeaveDialog = false
                            // 여기서 실패 처리를 할 수 있어요 (예: 에러 로그 확인)
                        }
                    )
                }) {
                    Text("나가기", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) { Text("취소") }
            }
        )
    }

    // ── 팀장 양도 다이얼로그 ──────────────────────────────────────────
    if (showTransferDialog) {
        val otherParticipants = participants.filter { !it.isLeader }
        AlertDialog(
            onDismissRequest = { showTransferDialog = false },
            title = { Text("팀장 양도", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("나가기 전에 팀장을 양도할 멤버를 선택하세요.", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    otherParticipants.forEach { participant ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedNewLeader = participant }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedNewLeader?.userId == participant.userId,
                                onClick = { selectedNewLeader = participant },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFA78BFA))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = participant.name,
                                fontSize = 15.sp,
                                color = Color(0xFF1F2937)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedNewLeader?.let { newLeader ->
                            viewModel.transferLeaderAndLeave(
                                chatId = chatId,
                                newLeaderUserId = newLeader.userId,
                                onSuccess = {
                                    showTransferDialog = false
                                    onBackClick()
                                }
                            )
                        }
                    },
                    enabled = selectedNewLeader != null
                ) {
                    Text(
                        "양도하고 나가기",
                        color = if (selectedNewLeader != null) Color.Red else Color.Gray
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showTransferDialog = false }) { Text("취소") }
            }
        )
    }

    // ── 팀 해체 다이얼로그 (팀장 혼자 남았을 때) ─────────────────────
    // ── 팀장 양도 다이얼로그 ──────────────────────────────────────────
    if (showTransferDialog) {
        val otherParticipants = participants.filter { !it.isLeader }
        AlertDialog(
            onDismissRequest = { showTransferDialog = false },
            containerColor = Color.White,  // ✅ 배경 흰색
            title = {
                Text(
                    "팀장 양도",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)  // ✅ 제목 색상
                )
            },
            text = {
                Column {
                    Text(
                        "나가기 전에 팀장을 양도할 멤버를 선택하세요.",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280)  // ✅ 설명 색상
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    otherParticipants.forEach { participant ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedNewLeader = participant }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedNewLeader?.userId == participant.userId,
                                onClick = { selectedNewLeader = participant },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFA78BFA))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = participant.name,
                                fontSize = 15.sp,
                                color = Color(0xFF1F2937)  // ✅ 이름 색상
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedNewLeader?.let { newLeader ->
                            viewModel.transferLeaderAndLeave(
                                chatId = chatId,
                                newLeaderUserId = newLeader.userId,
                                onSuccess = {
                                    showTransferDialog = false
                                    onBackClick()
                                }
                            )
                        }
                    },
                    enabled = selectedNewLeader != null
                ) {
                    Text(
                        "양도하고 나가기",
                        color = if (selectedNewLeader != null) Color.Red else Color.Gray
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showTransferDialog = false }) {
                    Text("취소", color = Color(0xFF6B7280))
                }
            }
        )
    }

    // 프로필 다이얼로그
    selectedParticipant?.let { participant ->
        ParticipantProfileDialog(
            participant = participant,
            userProfile = selectedUserProfile,
            isLoading = isLoadingProfile,
            onDismiss = {
                selectedParticipant = null
                viewModel.clearUserProfile()
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatRoomDrawer(
                roomName = currentRoomName.ifEmpty { roomName },
                participants = participants,
                requestList = requestList,
                onParticipantClick = { participant ->
                    selectedParticipant = participant
                    viewModel.loadUserProfile(participant.userId)
                    coroutineScope.launch { drawerState.close() }
                },
                onInviteClick = {
                    coroutineScope.launch { drawerState.close() }
                },
                onAcceptRequest = { likeId -> viewModel.acceptRequest(likeId) },
                onRejectRequest = { likeId -> viewModel.rejectRequest(likeId) },
                onLeaveClick = {
                    val isLeader = viewModel.isCurrentUserLeader
                    val otherMembers = participants.filter { !it.isLeader }
                    when {
                        isLeader && otherMembers.isNotEmpty() -> {
                            coroutineScope.launch { drawerState.close() }
                            showTransferDialog = true
                        }
                        isLeader && otherMembers.isEmpty() -> {
                            coroutineScope.launch { drawerState.close() }
                            showDisbandDialog = true
                        }
                        else -> {
                            coroutineScope.launch { drawerState.close() }
                            showLeaveDialog = true
                        }
                    }
                }
            )
        },
        gesturesEnabled = true
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.align(Alignment.Center),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentRoomName.ifEmpty { roomName },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${participants.size}",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "뒤로가기"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            coroutineScope.launch { drawerState.open() }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "더보기"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            bottomBar = {
                MessageInputBar(
                    text = inputText,
                    isSending = isSending,
                    onTextChange = { inputText = it },
                    onSendClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(chatId, inputText)
                            inputText = ""
                        }
                    }
                )
            },
            containerColor = Color(0xFFF9FAFB)
        ) { innerPadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(items = messages, key = { it.id }) { message ->
                    MessageItem(
                        message = message,
                        timeText = viewModel.formatTime(message.createdAt)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Drawer
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatRoomDrawer(
    roomName: String,
    participants: List<ParticipantItem>,
    requestList: List<ReceivedLikeItem>,
    onParticipantClick: (ParticipantItem) -> Unit,
    onInviteClick: () -> Unit,
    onAcceptRequest: (String) -> Unit,
    onRejectRequest: (String) -> Unit,
    onLeaveClick: () -> Unit
) {
    var showRequestSheet by remember { mutableStateOf(false) }

    ModalDrawerSheet(
        modifier = Modifier.fillMaxWidth(0.8f),
        drawerContainerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = roomName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "대화상대 ${participants.size}",
                fontSize = 13.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(16.dp))

            // 받은 관심 탭
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F3FF))
                    .clickable { showRequestSheet = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("💌", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "받은 관심",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF7C3AED)
                )
                if (requestList.isNotEmpty()) {
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFA78BFA), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${requestList.size}",
                            fontSize = 11.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(16.dp))

            // 초대하기
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onInviteClick() }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3F4F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", fontSize = 22.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "초대하기",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6366F1)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 대화상대 목록
            participants.forEach { participant ->
                ParticipantRow(
                    participant = participant,
                    onClick = { onParticipantClick(participant) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.weight(1f))
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onLeaveClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("채팅방 나가기", color = Color.Red, fontSize = 15.sp)
            }
        }
    }

    if (showRequestSheet) {
        ReceivedLikeSheet(
            requestList = requestList,
            onAccept = onAcceptRequest,
            onReject = onRejectRequest,
            onDismiss = { showRequestSheet = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 받은 관심 바텀시트
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceivedLikeSheet(
    requestList: List<ReceivedLikeItem>,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "💌 받은 관심",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "나에게 관심을 보낸 사람들이에요",
                fontSize = 13.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (requestList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🫙", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("아직 받은 관심이 없어요", fontSize = 14.sp, color = Color.Gray)
                    }
                }
            } else {
                requestList.forEach { item ->
                    ReceivedLikeCard(
                        item = item,
                        onAccept = { onAccept(item.likeId) },
                        onReject = { onReject(item.likeId) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ReceivedLikeCard(
    item: ReceivedLikeItem,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFFA78BFA), Color(0xFFF472B6)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("👤", fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.fromUserName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
                Text(
                    text = item.fromUserMbti,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onReject,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("거절", fontSize = 13.sp)
                }
                Button(
                    onClick = onAccept,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA78BFA)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("수락", fontSize = 13.sp, color = Color.White)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 대화상대 행
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ParticipantRow(
    participant: ParticipantItem,
    onClick: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFFEDE9FE)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = participant.emoji, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = participant.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1F2937)
                )
                if (participant.isLeader) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFA78BFA), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("팀장", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 프로필 다이얼로그
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ParticipantProfileDialog(
    participant: ParticipantItem,
    userProfile: UserProfileData?,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFA78BFA), Color(0xFFF472B6)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = participant.emoji, fontSize = 36.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = participant.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    if (participant.isLeader) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFA78BFA), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("팀장", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFF3F4F6))
                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color(0xFFA78BFA),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                } else if (userProfile != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (userProfile.age > 0) ProfileInfoRow("나이", "${userProfile.age}세")
                        if (userProfile.department.isNotEmpty()) ProfileInfoRow("학과", userProfile.department)
                        if (userProfile.height > 0) ProfileInfoRow("키", "${userProfile.height}cm")
                        if (userProfile.location.isNotEmpty()) ProfileInfoRow("거주지", userProfile.location)
                        if (userProfile.mbti.isNotEmpty()) ProfileInfoRow("MBTI", userProfile.mbti)
                        if (userProfile.bio.isNotEmpty()) ProfileInfoRow("소개", userProfile.bio)
                    }

                    if (userProfile.interests.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("관심사", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                userProfile.interests.forEach { interest ->
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFEDE9FE), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(text = interest, fontSize = 12.sp, color = Color(0xFF7C3AED))
                                    }
                                }
                            }
                        }
                    }

                    if (userProfile.foodLikes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("좋아하는 음식", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                userProfile.foodLikes.forEach { food ->
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFDCFCE7), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(text = food, fontSize = 12.sp, color = Color(0xFF16A34A))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA78BFA))
                ) {
                    Text("닫기", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = Color(0xFF9CA3AF), fontWeight = FontWeight.Medium)
        Text(text = value, fontSize = 13.sp, color = Color(0xFF1F2937), fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 모델
// ─────────────────────────────────────────────────────────────────────────────

data class ParticipantItem(
    val userId: String,
    val name: String,
    val emoji: String,
    val isLeader: Boolean = false,
    val profileImage: String = ""
)

// ─────────────────────────────────────────────────────────────────────────────
// 메시지 아이템
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MessageItem(
    message: ChatMessage,
    timeText: String
) {
    if (message.senderId == "system") {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(text = message.content, fontSize = 12.sp, color = Color(0xFF6B7280))
            }
        }
        return
    }

    val isMe = message.isMe

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (isMe) {
            Text(
                text = timeText,
                fontSize = 10.sp,
                color = Color.LightGray,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .align(Alignment.Bottom)
            )
            Box(
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(Color(0xFFA78BFA))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(text = message.content, color = Color.White, fontSize = 15.sp, lineHeight = 22.sp)
            }
        } else {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = message.senderName,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 260.dp)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                            .background(Color.White)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(text = message.content, color = Color(0xFF1F2937), fontSize = 15.sp, lineHeight = 22.sp)
                    }
                    Text(
                        text = timeText,
                        fontSize = 10.sp,
                        color = Color.LightGray,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .align(Alignment.Bottom)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 메시지 입력창
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MessageInputBar(
    text: String,
    isSending: Boolean,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Surface(shadowElevation = 8.dp, color = Color.White) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("메시지를 입력하세요", color = Color.LightGray, fontSize = 15.sp) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSendClick() }),
                maxLines = 4,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF3F4F6),
                    unfocusedContainerColor = Color(0xFFF3F4F6),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSendClick,
                enabled = text.isNotBlank() && !isSending,
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = if (text.isNotBlank()) Color(0xFFA78BFA) else Color(0xFFE5E7EB),
                        shape = RoundedCornerShape(50)
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "전송",
                    tint = if (text.isNotBlank()) Color.White else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}