package com.bugzero.meety.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.window.Dialog
import com.bugzero.meety.ui.call.CallViewModel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.bugzero.meety.ui.team.ReceivedLikeItem
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    chatId: String = "",
    roomName: String = "채팅방",
    onBackClick: () -> Unit = {},
    onVideoCallClick: () -> Unit = {},
    onVoiceCallClick: () -> Unit = {},
    onAcceptCall: (chatId: String, callType: String) -> Unit = { _, _ -> },
    viewModel: ChatViewModel = viewModel(),
    callViewModel: CallViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val currentRoomName by viewModel.roomName.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val participants by viewModel.participants.collectAsState()
    val selectedUserProfile by viewModel.selectedUserProfile.collectAsState()
    val isLoadingProfile by viewModel.isLoadingProfile.collectAsState()
    val requestList by viewModel.requestList.collectAsState()

    // ✨ 친구 목록 상태 가져오기
    val friendList by viewModel.friendList.collectAsState()
    val matchCandidates by viewModel.matchCandidates.collectAsState()
    val isLoadingCandidates by viewModel.isLoadingCandidates.collectAsState()
    val currentTeamId by viewModel.currentTeamId.collectAsState()
    val currentChatType by viewModel.currentChatType.collectAsState()

    // 시트 제어 상태
    var showManagementSheet by remember { mutableStateOf(false) }
    var showMemberSelectionSheet by remember { mutableStateOf(false) }
    var showInviteSheet by remember { mutableStateOf(false) } // ✨ 초대 시트 상태 추가
    var showAutoMatchingSheet by remember { mutableStateOf(false) } // 팀원 자동 매칭 시트
    var selectionMode by remember { mutableStateOf("kick") }

    // 수신 통화 감지
    val incomingCall by callViewModel.incomingCallState.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // 채팅방 진입 시 수신 통화 리스너 시작
    LaunchedEffect(chatId) {
        callViewModel.listenForIncomingCall(chatId, currentUserId ?: "")
    }
    DisposableEffect(chatId) {
        onDispose { callViewModel.stopListeningForCalls(chatId) }
    }

    // Firestore에서 읽어온 실제 type 우선, 로드 전에는 chatId 패턴으로 fallback
    val resolvedChatType = currentChatType.ifBlank {
        when {
            chatId.startsWith("direct") -> "direct"
            chatId.contains("group")    -> "group"
            else                        -> "team"
        }
    }
    val isTeamChat   = resolvedChatType == "team"
    val isDirectChat = resolvedChatType == "direct"

    val isLeader = isTeamChat && participants.firstOrNull { it.isLeader }?.userId == currentUserId

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var selectedParticipant by remember { mutableStateOf<ParticipantItem?>(null) }
    var showLeaveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(chatId) { viewModel.enterChatRoom(chatId, roomName) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) { listState.animateScrollToItem(0) }
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            containerColor = Color.White,
            title = { Text("채팅방 나가기", fontWeight = FontWeight.Bold) },
            text = { Text("정말 나가시겠습니까?") },
            confirmButton = { TextButton(onClick = { viewModel.leaveChatRoom(chatId, onSuccess = { showLeaveDialog = false; onBackClick() }) }) { Text("나가기", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { showLeaveDialog = false }) { Text("취소") } }
        )
    }

    // 수신 통화 다이얼로그
    incomingCall?.let { (callType, _) ->
        IncomingCallDialog(
            callType = callType,
            onAccept  = {
                callViewModel.clearIncomingCall()
                onAcceptCall(chatId, callType)
            },
            onDecline = { callViewModel.declineCall(chatId) }
        )
    }

    selectedParticipant?.let { participant ->
        ParticipantProfileDialog(participant, selectedUserProfile, isLoadingProfile, isDirectChat, onDismiss = { selectedParticipant = null; viewModel.clearUserProfile() })
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatRoomDrawer(
                chatId = chatId,
                roomName = currentRoomName.ifEmpty { roomName },
                participants = participants,
                requestList = requestList,
                isLeader = isLeader,
                isDirectChat = isDirectChat,
                viewModel = viewModel,
                onParticipantClick = { participant ->
                    selectedParticipant = participant
                    viewModel.loadUserProfile(participant.userId)
                    coroutineScope.launch { drawerState.close() }
                },
                onInviteClick = {
                    viewModel.loadFriendList()
                    coroutineScope.launch { drawerState.close() }
                    showInviteSheet = true
                },
                onAutoMatchingClick = {
                    val teamIdToUse = currentTeamId.ifBlank { chatId }
                    viewModel.loadMatchCandidates(teamIdToUse)
                    coroutineScope.launch { drawerState.close() }
                    showAutoMatchingSheet = true
                },
                onAcceptRequest = { viewModel.acceptRequest(it) },
                onRejectRequest = { viewModel.rejectRequest(it) },
                onTransferClick = {
                    coroutineScope.launch { drawerState.close() }
                    showManagementSheet = true
                },
                onLeaveClick = {
                    coroutineScope.launch { drawerState.close() }
                    showLeaveDialog = true
                }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentRoomName.ifEmpty { roomName },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                if (participants.size > 2) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${participants.size}",
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기") } },
                    actions = {
                        IconButton(onClick = onVoiceCallClick) {
                            Icon(Icons.Default.Call, contentDescription = "음성통화", tint = Color(0xFF7C3AED))
                        }
                        IconButton(onClick = onVideoCallClick) {
                            Icon(Icons.Default.Videocam, contentDescription = "화상통화", tint = Color(0xFF7C3AED))
                        }
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "더보기")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            containerColor = Color(0xFFF9FAFB)
        ) { innerPadding ->
            Column(modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        reverseLayout = true,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp)
                    ) {
                        val reversedMessages = messages.reversed()
                        itemsIndexed(items = reversedMessages, key = { _, it -> it.id }) { index, message ->
                            MessageItem(message, viewModel.formatTime(message.createdAt))
                            val showDateSeparator = if (index == reversedMessages.lastIndex) {
                                true
                            } else {
                                val currentMsgDate = formatKakaoDate(message.createdAt)
                                val olderMsgDate = formatKakaoDate(reversedMessages[index + 1].createdAt)
                                currentMsgDate != olderMsgDate
                            }
                            if (showDateSeparator) {
                                DateDivider(formatKakaoDate(message.createdAt))
                            }
                        }
                    }
                    val showScrollToBottom by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }
                    ScrollToBottomButton(isVisible = showScrollToBottom, onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } }, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 8.dp))
                }
                val keyboardController = LocalSoftwareKeyboardController.current
                MessageInputBar(text = inputText, isSending = isSending, onTextChange = { inputText = it }, onSendClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(chatId, inputText)
                        inputText = ""
                        keyboardController?.hide()
                    }
                })
            }
        }
    }

    // 관리 메인 시트
    if (showManagementSheet) {
        TeamManagementSheet(
            onKickClick = { selectionMode = "kick"; showManagementSheet = false; showMemberSelectionSheet = true },
            onTransferClick = { selectionMode = "transfer"; showManagementSheet = false; showMemberSelectionSheet = true },
            onDismiss = { showManagementSheet = false }
        )
    }

    // 멤버 선택 및 실행 시트 (내보내기/양도)
    if (showMemberSelectionSheet) {
        val otherMembers = participants.filter { it.userId != currentUserId }
        MemberSelectionSheet(
            title = if (selectionMode == "kick") "멤버 내보내기" else "팀장 양도",
            members = otherMembers,
            buttonText = if (selectionMode == "kick") "내보내기" else "양도하기",
            onConfirm = { member ->
                if (selectionMode == "kick") {
                    viewModel.kickMember(chatId, member.userId) { showMemberSelectionSheet = false }
                } else {
                    viewModel.transferLeaderOnly(chatId, member.userId) { showMemberSelectionSheet = false }
                }
            },
            onDismiss = { showMemberSelectionSheet = false }
        )
    }

    // ✨ 대화상대 초대 시트
    if (showInviteSheet) {
        val availableFriends = friendList.filter { friend ->
            participants.none { it.userId == friend.userId }
        }
        InviteFriendSheet(
            friends = availableFriends,
            onInvite = { selectedFriends ->
                viewModel.inviteFriendsToChat(chatId, selectedFriends) {
                    showInviteSheet = false
                }
            },
            onDismiss = { showInviteSheet = false }
        )
    }

    // 팀원 자동 매칭 시트
    if (showAutoMatchingSheet) {
        val teamName = currentRoomName.ifEmpty { roomName }
        val teamIdToUse = currentTeamId.ifBlank { chatId }
        AutoMatchingSheet(
            teamName        = teamName,
            candidates      = matchCandidates,
            isLoading       = isLoadingCandidates,
            alreadyInChat   = participants.map { it.userId }.toSet(),
            onSendInvites   = { selectedIds ->
                viewModel.sendTeamInvitations(
                    teamId     = teamIdToUse,
                    chatId     = chatId,
                    teamName   = teamName,
                    teamEmoji  = "👥",
                    toUserIds  = selectedIds,
                    onSuccess  = { showAutoMatchingSheet = false }
                )
            },
            onDismiss = { showAutoMatchingSheet = false }
        )
    }
}

// ... 아래의 서랍, ParticipantRow, ReceivedLikeSheet, MessageInputBar, MessageItem, Dialog, ScrollToBottomButton 등은 기존과 동일

@Composable
private fun ChatRoomDrawer(
    chatId: String, roomName: String, participants: List<ParticipantItem>,
    requestList: List<ReceivedLikeItem>, isLeader: Boolean, isDirectChat: Boolean,
    viewModel: ChatViewModel, onParticipantClick: (ParticipantItem) -> Unit,
    onInviteClick: () -> Unit, onAutoMatchingClick: () -> Unit = {},
    onAcceptRequest: (String) -> Unit,
    onRejectRequest: (String) -> Unit, onTransferClick: () -> Unit, onLeaveClick: () -> Unit
) {
    var showRequestSheet by remember { mutableStateOf(false) }

    ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.8f), drawerContainerColor = Color.White) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(text = roomName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "대화상대 ${participants.size}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            Spacer(modifier = Modifier.height(8.dp))

            if (isLeader) {
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFF5F3FF)).clickable { showRequestSheet = true }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("💌", fontSize = 18.sp); Spacer(Modifier.width(10.dp))
                    Text("받은 관심", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF7C3AED))
                    if (requestList.isNotEmpty()) {
                        Spacer(Modifier.weight(1f))
                        Box(Modifier.background(Color(0xFFA78BFA), CircleShape).padding(horizontal = 8.dp, vertical = 2.dp)) {
                            Text(text = "${requestList.size}", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // ── 팀원 자동 매칭 버튼 ──
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFEDE9FE)).clickable { onAutoMatchingClick() }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🤝", fontSize = 18.sp); Spacer(Modifier.width(10.dp))
                    Text("팀원 자동 매칭", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF7C3AED))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFF3F4F6)).clickable { onTransferClick() }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("⚙️", fontSize = 18.sp); Spacer(Modifier.width(10.dp))
                    Text("팀 채팅 관리", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF4B5563))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(modifier = Modifier.fillMaxWidth().clickable { onInviteClick() }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFF3F4F6)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = "초대", tint = Color(0xFF6B7280), modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp)); Text("초대하기", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6366F1))
            }

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                participants.forEach { participant ->
                    ParticipantRow(chatId = chatId, participant = participant, isMe = participant.userId == FirebaseAuth.getInstance().currentUser?.uid, viewModel = viewModel, onClick = { onParticipantClick(participant) })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onLeaveClick, modifier = Modifier.fillMaxWidth()) { Text("채팅방 나가기", color = Color.Red, fontSize = 15.sp) }
        }
    }
    if (showRequestSheet) { ReceivedLikeSheet(requestList, onAcceptRequest, onRejectRequest, onDismiss = { showRequestSheet = false }) }
}

@Composable
private fun ParticipantRow(chatId: String, participant: ParticipantItem, isMe: Boolean, viewModel: ChatViewModel, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 4.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFF3F4F6)), contentAlignment = Alignment.Center) {
                val profileAlpha = if (participant.isFriend) 1f else 0.5f
                Text(text = participant.emoji, fontSize = 20.sp, modifier = Modifier.graphicsLayer(alpha = profileAlpha))
            }
            if (!participant.isFriend && !participant.isLeader) {
                Text(text = "?", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(text = if (isMe) "${participant.name} (나)" else participant.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), color = if (participant.isFriend) Color(0xFF1F2937) else Color.Gray)
        if (!participant.isFriend && !isMe) {
            IconButton(onClick = { viewModel.addFriend(chatId, participant) }, modifier = Modifier.size(32.dp).background(Color(0xFFF3F4F6), CircleShape)) {
                Icon(Icons.Default.PersonAdd, contentDescription = "추가", tint = Color(0xFF4B5563), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceivedLikeSheet(requestList: List<ReceivedLikeItem>, onAccept: (String) -> Unit, onReject: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text("💌 받은 관심", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            if (requestList.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { Text("아직 받은 관심이 없어요", color = Color.Gray) }
            } else {
                requestList.forEach { item ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.fromUserName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Button(onClick = { onAccept(item.likeId) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA78BFA))) { Text("수락") }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { onReject(item.likeId) }) { Text("거절") }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageInputBar(text: String, isSending: Boolean, onTextChange: (String) -> Unit, onSendClick: () -> Unit) {
    Surface(color = Color.White, modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(value = text, onValueChange = onTextChange, modifier = Modifier.weight(1f), placeholder = { Text("메시지를 입력하세요") }, colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFFF3F4F6), unfocusedContainerColor = Color(0xFFF3F4F6), focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), shape = RoundedCornerShape(24.dp))
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onSendClick, enabled = text.isNotBlank() && !isSending) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "전송") }
        }
    }
}

@Composable
private fun MessageItem(message: ChatMessage, timeText: String) {
    // 1. 시스템 메시지 (예: "OOO님이 나갔습니다")
    if (message.senderId == "system") {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.background(Color(0xFFE5E7EB), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text(text = message.content, fontSize = 12.sp, color = Color(0xFF6B7280))
            }
        }
        return
    }

    val isMe = message.isMe

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top // 프로필을 위쪽으로 맞춤
    ) {
        // ✨ 상대방(isMe == false)일 때만 프로필 사진(아이콘) 표시
        if (!isMe) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5E7EB)),
                contentAlignment = Alignment.Center
            ) {
                // TODO: 나중에 실제 profileImage URL이 있으면 AsyncImage로 교체하세요!
                // 지금은 이름의 첫 글자나 임시 이모지로 띄워줍니다.
                Text(
                    text = message.senderName.take(1).ifEmpty { "👤" },
                    fontSize = 16.sp,
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        // 말풍선과 이름, 시간을 담는 영역
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            // ✨ 상대방일 때만 이름 표시
            if (!isMe) {
                Text(
                    text = message.senderName.ifEmpty { "알 수 없음" },
                    fontSize = 13.sp,
                    color = Color(0xFF4B5563),
                    modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                )
            }

            // 말풍선 + 시간 가로 정렬
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
            ) {
                // 내 말풍선일 때 시간 (왼쪽)
                if (isMe) {
                    Text(
                        text = timeText,
                        fontSize = 10.sp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(end = 4.dp, bottom = 2.dp)
                    )
                }

                // 말풍선 박스
                Box(
                    modifier = Modifier
                        .widthIn(max = 240.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = if (isMe) 16.dp else 4.dp, // 상대방은 왼쪽 위가 뾰족하게!
                                topEnd = if (isMe) 4.dp else 16.dp,   // 나는 오른쪽 위가 뾰족하게!
                                bottomStart = 16.dp,
                                bottomEnd = 16.dp
                            )
                        )
                        .background(if (isMe) Color(0xFFA78BFA) else Color.White)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = message.content,
                        color = if (isMe) Color.White else Color(0xFF1F2937),
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                }

                // 상대방 말풍선일 때 시간 (오른쪽)
                if (!isMe) {
                    Text(
                        text = timeText,
                        fontSize = 10.sp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                    )
                }
            }
        }
    }
}
@Composable
private fun ParticipantProfileDialog(
    participant: ParticipantItem,
    userProfile: UserProfileData?,
    isLoading: Boolean,
    isDirectChat: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 아바타
                Box(
                    modifier = Modifier.size(72.dp).clip(CircleShape).background(Color(0xFFEDE9FE)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = participant.name.take(1).ifEmpty { "?" },
                        fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED)
                    )
                }
                Spacer(Modifier.height(12.dp))

                // 이름 + 팀장 뱃지
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(participant.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                    if (participant.isLeader) {
                        Box(
                            modifier = Modifier.background(Color(0xFFFEF3C7), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                        ) { Text("팀장", fontSize = 11.sp, color = Color(0xFFD97706), fontWeight = FontWeight.Bold) }
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (isLoading) {
                    CircularProgressIndicator(color = Color(0xFFA78BFA), modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(16.dp))
                } else if (userProfile != null) {
                    // 프로필 정보 행
                    @Composable
                    fun InfoRow(label: String, value: String) {
                        if (value.isBlank()) return
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, fontSize = 13.sp, color = Color(0xFF9CA3AF))
                            Text(value, fontSize = 13.sp, color = Color(0xFF1F2937), fontWeight = FontWeight.Medium)
                        }
                    }

                    InfoRow("학과", userProfile.department)
                    InfoRow("MBTI", userProfile.mbti)
                    InfoRow("나이", if (userProfile.age > 0) "${userProfile.age}세" else "")
                    InfoRow("키", if (userProfile.height > 0) "${userProfile.height}cm" else "")
                    InfoRow("지역", userProfile.location)

                    if (userProfile.bio.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth().background(Color(0xFFF9FAFB), RoundedCornerShape(10.dp)).padding(12.dp)
                        ) {
                            Text(userProfile.bio, fontSize = 13.sp, color = Color(0xFF6B7280), lineHeight = 20.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA78BFA))) {
                    Text("닫기", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ScrollToBottomButton(isVisible: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = isVisible, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut(), modifier = modifier) {
        IconButton(onClick = onClick, modifier = Modifier.size(40.dp).shadow(elevation = 4.dp, shape = CircleShape).background(Color.White, CircleShape)) {
            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "맨 아래로 이동", tint = Color(0xFF4B5563))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamManagementSheet(onKickClick: () -> Unit, onTransferClick: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp).padding(bottom = 32.dp)) {
            Text("팀 채팅 관리", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth().clickable { onKickClick() }.padding(vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🚫", fontSize = 20.sp); Spacer(Modifier.width(12.dp)); Text("멤버 내보내기", fontSize = 16.sp)
            }
            Row(modifier = Modifier.fillMaxWidth().clickable { onTransferClick() }.padding(vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("👑", fontSize = 20.sp); Spacer(Modifier.width(12.dp)); Text("팀장 양도", fontSize = 16.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberSelectionSheet(title: String, members: List<ParticipantItem>, buttonText: String, onConfirm: (ParticipantItem) -> Unit, onDismiss: () -> Unit) {
    var selectedMember by remember { mutableStateOf<ParticipantItem?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(items = members, key = { it.userId }) { member ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { selectedMember = member }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedMember?.userId == member.userId, onClick = { selectedMember = member })
                        Text(member.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(onClick = { selectedMember?.let { onConfirm(it) } }, enabled = selectedMember != null, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = if(title.contains("내보내기")) Color.Red else Color(0xFFA78BFA))) {
                Text(buttonText, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ✨ 새로운 대화상대 초대 시트 UI 부품
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InviteFriendSheet(
    friends: List<UserProfileData>,
    onInvite: (List<UserProfileData>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFriends by remember { mutableStateOf(setOf<UserProfileData>()) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
            Text("대화상대 초대", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            if (friends.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("초대할 수 있는 친구가 없습니다.", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(items = friends, key = { it.userId }) { friend ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedFriends = if (selectedFriends.contains(friend)) {
                                    selectedFriends - friend
                                } else {
                                    selectedFriends + friend
                                }
                            }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedFriends.contains(friend),
                                onCheckedChange = { checked ->
                                    selectedFriends = if (checked) selectedFriends + friend else selectedFriends - friend
                                }
                            )
                            Text(friend.name, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { onInvite(selectedFriends.toList()) },
                enabled = selectedFriends.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA78BFA))
            ) {
                Text(if (selectedFriends.isEmpty()) "선택해주세요" else "${selectedFriends.size}명 초대하기", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── 팀원 자동 매칭 시트 ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoMatchingSheet(
    teamName: String,
    candidates: List<MatchCandidate>,
    isLoading: Boolean,
    alreadyInChat: Set<String>,
    onSendInvites: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("🤝 팀원 자동 매칭", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "\"$teamName\" 팀 태그·MBTI 기반으로 취향이 맞는 유저를 찾았어요",
                fontSize = 13.sp, color = Color(0xFF6B7280)
            )
            Spacer(Modifier.height(16.dp))

            when {
                isLoading -> {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFFA78BFA))
                            Spacer(Modifier.height(12.dp))
                            Text("취향 데이터 분석 중...", fontSize = 13.sp, color = Color(0xFF6B7280))
                        }
                    }
                }
                candidates.isEmpty() -> {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", fontSize = 40.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("조건에 맞는 후보자가 없어요", fontSize = 14.sp, color = Color(0xFF6B7280))
                            Spacer(Modifier.height(4.dp))
                            Text("팀 태그를 더 추가하면 더 많은 후보자를 찾을 수 있어요", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                        }
                    }
                }
                else -> {
                    // 최고 점수 기준으로 매칭률 계산
                    val maxScore = candidates.maxOf { it.matchScore }.coerceAtLeast(1)

                    LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                        items(items = candidates, key = { it.userId }) { candidate ->
                            val alreadyMember = alreadyInChat.contains(candidate.userId)
                            val matchPct = (candidate.matchScore * 100 / maxScore).coerceIn(0, 100)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (!alreadyMember) Modifier.clickable {
                                            selectedIds = if (selectedIds.contains(candidate.userId))
                                                selectedIds - candidate.userId
                                            else selectedIds + candidate.userId
                                        } else Modifier
                                    )
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!alreadyMember) {
                                    Checkbox(
                                        checked = selectedIds.contains(candidate.userId),
                                        onCheckedChange = { checked ->
                                            selectedIds = if (checked) selectedIds + candidate.userId
                                            else selectedIds - candidate.userId
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFFA78BFA))
                                    )
                                } else {
                                    Spacer(Modifier.width(48.dp))
                                }

                                // 아바타
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEDE9FE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        candidate.name.take(1).ifEmpty { "?" },
                                        fontSize = 18.sp,
                                        color = Color(0xFF7C3AED),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        candidate.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1F2937)
                                    )
                                    val detail = listOfNotNull(
                                        candidate.department.takeIf { it.isNotBlank() },
                                        candidate.mbti.takeIf { it.isNotBlank() }
                                    ).joinToString(" · ")
                                    if (detail.isNotBlank()) {
                                        Text(detail, fontSize = 12.sp, color = Color(0xFF6B7280))
                                    }
                                    // 매칭률 바
                                    if (!alreadyMember) {
                                        Spacer(Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(Color(0xFFE9D5FF))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxHeight()
                                                        .fillMaxWidth(matchPct / 100f)
                                                        .background(Color(0xFFA78BFA))
                                                )
                                            }
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                "${matchPct}%",
                                                fontSize = 11.sp,
                                                color = Color(0xFF7C3AED),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                if (alreadyMember) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFD1FAE5), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("이미 멤버", fontSize = 11.sp, color = Color(0xFF059669))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!isLoading && candidates.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick  = { onSendInvites(selectedIds.toList()) },
                    enabled  = selectedIds.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFA78BFA), disabledContainerColor = Color(0xFFD1D5DB))
                ) {
                    Text(
                        text = if (selectedIds.isEmpty()) "초대할 후보자를 선택하세요" else "${selectedIds.size}명에게 초대 보내기",
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp
                    )
                }
            }
        }
    }
}

// ─── 수신 통화 다이얼로그 ────────────────────────────────────────────────────

@Composable
fun IncomingCallDialog(
    callType: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        containerColor = Color.White,
        title = {
            Text(
                text = if (callType == "video") "화상통화 수신" else "음성통화 수신",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Text(
                text = if (callType == "video") "화상통화가 걸려왔습니다." else "음성통화가 걸려왔습니다.",
                color = Color.Gray
            )
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
            ) {
                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("수락", color = Color.White)
            }
        },
        dismissButton = {
            Button(
                onClick = onDecline,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
            ) {
                Icon(Icons.Default.CallEnd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("거절", color = Color.White)
            }
        }
    )
}

fun formatKakaoDate(timestamp: com.google.firebase.Timestamp?): String {
    if (timestamp == null) return ""
    val sdf = SimpleDateFormat("yyyy년 M월 d일 EEEE", Locale.KOREA)
    return sdf.format(timestamp.toDate())
}

@Composable
fun DateDivider(dateText: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.background(Color(0xFF9CA3AF).copy(alpha = 0.4f), RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text(text = dateText, fontSize = 12.sp, color = Color.White)
        }
    }
}