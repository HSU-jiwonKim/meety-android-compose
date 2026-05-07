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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bugzero.meety.data.repository.PlaceResult
import com.bugzero.meety.ui.call.CallViewModel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    chatId: String = "",
    roomName: String = "채팅방",
    onBackClick: () -> Unit = {},
    onVideoCallClick: () -> Unit = {},
    onVoiceCallClick: () -> Unit = {},
    onScheduleSyncClick: (List<String>) -> Unit = {},
    onAcceptCall: (chatId: String, callType: String) -> Unit = { _, _ -> },
    onJoinCall: (chatId: String, callType: String) -> Unit = { _, _ -> },
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

    // 팀원 자동 매칭
    val matchCandidates by viewModel.matchCandidates.collectAsState()
    val isLoadingCandidates by viewModel.isLoadingCandidates.collectAsState()
    val currentTeamId by viewModel.currentTeamId.collectAsState()

    // 시트 제어 상태
    var showManagementSheet by remember { mutableStateOf(false) }
    var showMemberSelectionSheet by remember { mutableStateOf(false) }
    var showInviteSheet by remember { mutableStateOf(false) } // ✨ 초대 시트 상태 추가
    var showAutoMatchingSheet by remember { mutableStateOf(false) } // 팀원 자동 매칭 시트
    var selectionMode by remember { mutableStateOf("kick") }

    // 수신 통화 감지
    val incomingCall by callViewModel.incomingCallState.collectAsState()
    val activeCallInfo by callViewModel.activeCallInfo.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // 장소 추천
    val placeRecommendations by viewModel.placeRecommendations.collectAsState()
    val isLoadingPlaces by viewModel.isLoadingPlaces.collectAsState()
    val placeError by viewModel.placeError.collectAsState()
    val transitAverages by viewModel.transitAverages.collectAsState()
    val transitBreakdowns by viewModel.transitBreakdowns.collectAsState()
    val refreshNotice by viewModel.refreshNotice.collectAsState()
    val savedPlaces by viewModel.savedPlaces.collectAsState()
    val savedPlaceKeys by viewModel.savedPlaceKeys.collectAsState()
    val showConditionSheet by viewModel.showConditionSheet.collectAsState()
    val searchRegion by viewModel.searchRegion.collectAsState()
    val regionTransitAvg by viewModel.regionTransitAvg.collectAsState()
    val regionTransitBreakdown by viewModel.regionTransitBreakdown.collectAsState()
    val recommendedRegionName by viewModel.recommendedRegionName.collectAsState()
    var showPlaceDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val snackScope = rememberCoroutineScope()

    // 채팅방 진입 시 수신 통화 리스너 시작
    LaunchedEffect(chatId) {
        callViewModel.listenForIncomingCall(chatId, currentUserId ?: "")
    }
    DisposableEffect(chatId) {
        onDispose { callViewModel.stopListeningForCalls(chatId) }
    }

    val chatType = if (chatId.startsWith("direct")) "direct" else if (chatId.contains("group")) "group" else "team"
    val isTeamChat = chatType == "team"
    val isDirectChat = chatType == "direct"

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

    // 수신 통화 다이얼로그 — 채팅방 안에서는 별도 팝업을 띄우지 않는다.
    // 전화는 시스템 상단 알림(MyFirebaseMessagingService.forIncomingCall)에서만 표시되도록 함.
    // (이전 동작 복구가 필요하면 아래 블록의 주석을 해제할 것)
    // incomingCall?.let { (callType, _) ->
    //     IncomingCallDialog(
    //         callType = callType,
    //         onAccept  = {
    //             callViewModel.clearIncomingCall()
    //             onAcceptCall(chatId, callType)
    //         },
    //         onDecline = { callViewModel.declineCall(chatId, currentUserId ?: "") }
    //     )
    // }

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
                    // ✨ 초대 버튼 누르면 친구 목록 새로고침하고 초대 시트 열기
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
                onScheduleSyncClick = {
                    coroutineScope.launch { drawerState.close() } // 1. 서랍 닫기
                    val memberIds = participants.map { it.userId } // 2. 현재 방에 있는 사람들의 UID만 뽑아내기
                    onScheduleSyncClick(memberIds) // 3. 그 UID 목록을 들고 화면 이동!
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
            snackbarHost = { SnackbarHost(snackbarHostState) },
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

                // ── 통화 중 배너 (참여 가능한 경우에만 표시) ──────────────────
                activeCallInfo?.let { info ->
                    ActiveCallBanner(
                        callType = info.callType,
                        participantCount = info.participantCount,
                        onJoinClick = { onJoinCall(chatId, info.callType) }
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        reverseLayout = true,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 52.dp, bottom = 12.dp)
                    ) {
                        val reversedMessages = messages.reversed()
                        itemsIndexed(items = reversedMessages, key = { _, it -> it.id }) { index, message ->
                            MessageItem(
                                message = message,
                                timeText = viewModel.formatTime(message.createdAt),
                                onProfileClick = {
                                    val participant = participants.find { it.userId == message.senderId }
                                    if (participant != null) {
                                        selectedParticipant = participant
                                        viewModel.loadUserProfile(message.senderId)
                                    }
                                }
                            )
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

                    // ── 장소 추천 버튼 (상단 가운데 — 피그마 디자인) ──────────
                    PlaceRecommendButton(
                        isLoading = isLoadingPlaces,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp),
                        onClick = {
                            showPlaceDialog = true
                            if (placeRecommendations.isEmpty() && !isLoadingPlaces) {
                                viewModel.recommendMeetingPlaces(chatId)
                            }
                        }
                    )
                }
                MessageInputBar(text = inputText, isSending = isSending, onTextChange = { inputText = it }, onSendClick = { if (inputText.isNotBlank()) { viewModel.sendMessage(chatId, inputText); inputText = "" } })
            }
        }
    }

    // ── 장소 추천 풀스크린 ────────────────────────────────────────────────
    if (showPlaceDialog) {
        Dialog(
            onDismissRequest = { showPlaceDialog = false; viewModel.clearPlaceRecommendations() },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,   // 가로 여백 제거
                dismissOnClickOutside = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.97f)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                PlaceRecommendationScreen(
                    isLoading = isLoadingPlaces,
                    places = placeRecommendations,
                    error = placeError,
                    areaName = recommendedRegionName,
                    participantCount = participants.size,
                    transitAverages = transitAverages,
                    transitBreakdowns = transitBreakdowns,
                    onRefresh = { viewModel.onRefreshPlaceRecommendations(chatId) },
                    onDismiss = { showPlaceDialog = false; viewModel.clearPlaceRecommendations() },
                    onFilterChanged = { filters ->
                        // "찜" 탭은 UI에서만 처리 — 검색 안 함
                        if (filters.contains("찜")) return@PlaceRecommendationScreen
                        if (filters.isNotEmpty()) {
                            viewModel.recommendMeetingPlaces(chatId, filters)
                        } else {
                            viewModel.recommendMeetingPlaces(chatId)
                        }
                    },
                    onSharePlace = { place ->
                        viewModel.sharePlaceToChat(chatId, place)
                        showPlaceDialog = false
                        viewModel.clearPlaceRecommendations()
                        snackScope.launch {
                            snackbarHostState.showSnackbar("'${place.name}' 장소를 공유했어요")
                        }
                    },
                    notice = refreshNotice,
                    onDismissNotice = { viewModel.dismissRefreshNotice() },
                    savedPlaces = savedPlaces,
                    savedPlaceKeys = savedPlaceKeys,
                    onToggleSave = { viewModel.toggleSavePlace(it) },
                    initialRadiusMeters = viewModel.currentRadiusMeters,
                    showConditionSheet = showConditionSheet,
                    onOpenConditionSheet = { viewModel.openConditionSheet() },
                    onCloseConditionSheet = { viewModel.closeConditionSheet() },
                    onApplyConditions = { r, kw, inc -> viewModel.applyConditionSheet(chatId, r, kw, inc) },
                    // ── "다른 지역으로 검색" / "중간 지점으로 돌아가기" ───────────
                    searchRegion = searchRegion,
                    onSelectRegion = { region ->
                        viewModel.searchByRegion(chatId, region, keywords = null)
                    },
                    onReturnToMidpoint = {
                        viewModel.returnToMidpoint(chatId)
                    },
                    regionAvgTransitMin = regionTransitAvg,
                    regionTransitBreakdown = regionTransitBreakdown
                )
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
        // 이미 방에 있는 사람은 제외하고 친구 목록 보여주기
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

    // ── 팀원 자동 매칭 시트 ─────────────────────────────────────
    if (showAutoMatchingSheet) {
        val alreadyInChat = participants.map { it.userId }.toSet()
        val teamIdToUse = currentTeamId.ifBlank { chatId }
        AutoMatchingSheet(
            teamName        = currentRoomName.ifEmpty { roomName },
            candidates      = matchCandidates,
            isLoading       = isLoadingCandidates,
            alreadyInChat   = alreadyInChat,
            onSendInvites   = { toUserIds ->
                viewModel.sendTeamInvitations(
                    teamId     = teamIdToUse,
                    chatId     = chatId,
                    teamName   = currentRoomName.ifEmpty { roomName },
                    teamEmoji  = "👥",
                    toUserIds  = toUserIds,
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
    chatId: String,
    roomName: String,
    participants: List<ParticipantItem>,
    requestList: List<ReceivedLikeItem>,
    isLeader: Boolean,
    isDirectChat: Boolean,
    viewModel: ChatViewModel,
    onParticipantClick: (ParticipantItem) -> Unit,
    onInviteClick: () -> Unit,
    onAutoMatchingClick: () -> Unit = {},
    onAcceptRequest: (String) -> Unit,
    onRejectRequest: (String) -> Unit,
    onTransferClick: () -> Unit,
    onScheduleSyncClick: () -> Unit,
    onLeaveClick: () -> Unit
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
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFF3F4F6)).clickable { onTransferClick() }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("⚙️", fontSize = 18.sp); Spacer(Modifier.width(10.dp))
                    Text("팀 채팅 관리", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF4B5563))
                }
                Spacer(modifier = Modifier.height(8.dp))

                // ✨ 팀원 자동 매칭 (팀장 + 팀 채팅 전용)
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFEDE9FE)).clickable { onAutoMatchingClick() }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🤝", fontSize = 18.sp); Spacer(Modifier.width(10.dp))
                    Text("팀원 자동 매칭", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF7C3AED))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ✨ 공강 추천 버튼 (팀장 구역 밖으로 꺼내서 누구나, 어떤 채팅방에서든 보이게 함!)
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE0F2FE)).clickable { onScheduleSyncClick() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📅", fontSize = 18.sp); Spacer(Modifier.width(10.dp))
                Text(text = "공강 추천", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1))
            }
            Spacer(modifier = Modifier.height(8.dp))

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
private fun MessageItem(message: ChatMessage, timeText: String, onProfileClick: () -> Unit = {}) {
    // 통화 로그 메시지 — 카카오톡 스타일 카드
    if (message.type == "call_log") {
        CallLogMessage(message = message, timeText = timeText)
        return
    }
    // 장소 카드 메시지 — 네이버 지도 딥링크 포함
    if (message.type == "place_card") {
        PlaceCardMessage(message = message, timeText = timeText, onProfileClick = onProfileClick)
        return
    }
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
        verticalAlignment = Alignment.Top // ✨ 프로필을 위쪽으로 맞춤 (카카오톡 스타일)
    ) {
        // ✨ 상대방(isMe == false)일 때만 프로필 사진(아이콘) 표시
        if (!isMe) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5E7EB))
                    .clickable { onProfileClick() }, // ✨ 프로필 클릭으로 다이얼로그 열기
                contentAlignment = Alignment.Center
            ) {
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
                                topStart = if (isMe) 16.dp else 4.dp, // ✨ 상대방은 왼쪽 위가 뾰족하게!
                                topEnd = if (isMe) 4.dp else 16.dp,   // ✨ 나는 오른쪽 위가 뾰족하게!
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
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            // 정보 로딩 중일 때 빙글빙글 도는 UI
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF8B5CF6)) // 보라색
                }
            } else {
                // DB에서 가져온 데이터 꺼내기 (null일 경우 기본값 세팅)
                val name = userProfile?.name ?: participant.name
                val initial = name.take(1).ifEmpty { "👤" }
                val age = userProfile?.age?.toString() ?: "알 수 없음"
                val department = userProfile?.department ?: "알 수 없음"
                val mbti = userProfile?.mbti ?: "알 수 없음"

                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. 큰 보라색 프로필 동그라미
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8B5CF6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = initial, fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. 이름과 나이
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFF3F4F6), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = "${age}세", fontSize = 12.sp, color = Color.DarkGray)
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    // 3. 통화 / 화상통화 버튼 아이콘
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEDE9FE)) // 연한 보라색 배경
                                .clickable { /* TODO: 음성통화 연결 */ },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "음성통화", tint = Color(0xFF8B5CF6))
                        }
                        Spacer(modifier = Modifier.width(24.dp))
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEDE9FE))
                                .clickable { /* TODO: 화상통화 연결 */ },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = "화상통화", tint = Color(0xFF8B5CF6))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = Color(0xFFF3F4F6))
                    Spacer(modifier = Modifier.height(20.dp))

                    // 4. 학과 및 MBTI 정보
                    ProfileDetailRow("학과", department)
                    Spacer(modifier = Modifier.height(12.dp))
                    ProfileDetailRow("MBTI", mbti)
                    Spacer(modifier = Modifier.height(24.dp))

                    // 5. 닫기 버튼
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("닫기", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

// 정보(학과, MBTI)를 양쪽 정렬로 예쁘게 그려주는 미니 부품!
@Composable
private fun ProfileDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, color = Color.Gray)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
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

// ─── 장소 카드 메시지 (채팅방에서 렌더) ──────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceCardMessage(
    message: ChatMessage,
    timeText: String,
    onProfileClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val isMe = message.isMe

    fun openPlaceDetail() {
        val nmapUri = if (message.placePlaceId.isNotBlank()) {
            android.net.Uri.parse("nmap://place?id=${message.placePlaceId}&appname=com.bugzero.meety")
        } else {
            android.net.Uri.parse("nmap://search?query=${android.net.Uri.encode(message.placeName)}&appname=com.bugzero.meety")
        }
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, nmapUri)
        intent.addCategory(android.content.Intent.CATEGORY_BROWSABLE)
        val fallbackUri = if (message.placePlaceId.isNotBlank()) {
            android.net.Uri.parse("https://m.place.naver.com/place/${message.placePlaceId}/home")
        } else {
            android.net.Uri.parse("https://map.naver.com/v5/search/${android.net.Uri.encode(message.placeName)}")
        }
        val fallback = android.content.Intent(android.content.Intent.ACTION_VIEW, fallbackUri)
        try { context.startActivity(intent) } catch (_: Exception) {
            try { context.startActivity(fallback) } catch (_: Exception) {}
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top // ✨ 프로필을 위쪽으로 맞춤 (텍스트 메시지와 동일)
    ) {
        // ✨ 상대방(isMe == false)일 때만 프로필 사진(아이콘) 표시 — 텍스트 메시지와 동일
        if (!isMe) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5E7EB))
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                if (message.senderProfileImage.isNotBlank()) {
                    coil.compose.AsyncImage(
                        model = message.senderProfileImage,
                        contentDescription = message.senderName,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Text(
                        text = message.senderName.take(1).ifEmpty { "👤" },
                        fontSize = 16.sp,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        // 이름 + 카드 + 시간을 담는 영역 (텍스트 메시지와 동일한 구조)
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

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
            ) {
                if (isMe) {
                    Text(
                        text = timeText,
                        fontSize = 10.sp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(end = 4.dp, bottom = 2.dp)
                    )
                }
                Surface(
                    onClick = { openPlaceDetail() },
                    shape = RoundedCornerShape(
                        topStart = if (isMe) 16.dp else 4.dp,
                        topEnd = if (isMe) 4.dp else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    ),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEAE8F4)),
                    modifier = Modifier.widthIn(max = 280.dp)
                ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // 상단 뱃지 + 네이버 지도
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📍", fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "공유된 장소",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF6C5CE7)
                    )
                }
                Spacer(Modifier.height(8.dp))

                // 이미지 + 정보
                Row(verticalAlignment = Alignment.Top) {
                    if (message.placeImageUrl.isNotBlank()) {
                        coil.compose.AsyncImage(
                            model = message.placeImageUrl,
                            contentDescription = message.placeName,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF3F1FA)),
                            contentAlignment = Alignment.Center
                        ) { Text("🗺️", fontSize = 24.sp) }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        if (message.placeCategory.isNotBlank()) {
                            Text(
                                text = message.placeCategory,
                                fontSize = 11.sp,
                                color = Color(0xFF9CA3AF)
                            )
                        }
                        Text(
                            text = message.placeName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A2E),
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        if (message.placeAddress.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = message.placeAddress,
                                fontSize = 11.sp,
                                color = Color(0xFF6B7280),
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        if (message.placeReviewCount > 0) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "방문자 리뷰 ${message.placeReviewCount}개",
                                fontSize = 11.sp,
                                color = Color(0xFF6C5CE7),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                // "네이버 지도에서 상세보기" 힌트
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3F1FA), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF6C5CE7),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "네이버 지도에서 상세보기",
                        fontSize = 11.sp,
                        color = Color(0xFF6C5CE7),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
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

// ─── 통화 로그 메시지 (카카오톡 스타일) ────────────────────────────────────
@Composable
private fun CallLogMessage(message: ChatMessage, timeText: String) {
    val isVideo = message.callType == "video"
    val isMissedOrCanceled = message.callStatus == "call_missed" || message.callStatus == "call_canceled"
    val bgColor = if (isMissedOrCanceled) Color(0xFFFDECEC) else Color(0xFFF3F0FF)
    val accent = if (isMissedOrCanceled) Color(0xFFEF4444) else Color(0xFF7C3AED)

    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(bgColor)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = callLogTitle(message),
                    color = accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = callLogSubtitle(message),
                    color = Color(0xFF6B7280),
                    fontSize = 11.sp
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(text = timeText, fontSize = 10.sp, color = Color(0xFF9CA3AF))
        }
    }
}

private fun callLogTitle(message: ChatMessage): String {
    val kind = if (message.callType == "video") "영상통화" else "음성통화"
    return when (message.callStatus) {
        "call_missed" -> "부재중 $kind"
        "call_canceled" -> "취소된 $kind"
        else -> kind
    }
}

private fun callLogSubtitle(message: ChatMessage): String {
    return when (message.callStatus) {
        "call_missed" -> "응답 없음"
        "call_canceled" -> "상대방이 수락하기 전에 취소됨"
        else -> {
            val sec = message.callDurationSec
            val m = sec / 60
            val s = sec % 60
            if (m > 0) "통화시간 ${m}분 ${s}초" else "통화시간 ${s}초"
        }
    }
}

// ─── 장소 추천 버튼 (상단 좌측 그라디언트 필 — 스크린샷 디자인) ──────────────
@Composable
private fun PlaceRecommendButton(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF4285F4), Color(0xFF7C3AED))
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(gradient)
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (isLoading) "분석 중..." else "장소 추천",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ─── 장소 추천 다이얼로그 (스크린샷 디자인) ────────────────────────────────
@Composable
private fun PlaceRecommendationDialog(
    isLoading: Boolean,
    isApiReady: Boolean,
    places: List<PlaceResult>,
    error: String?,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // 헤더
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF4285F4), Color(0xFF7C3AED)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("장소 추천", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                }

                Spacer(Modifier.height(18.dp))

                when {
                    // ── API 키 미설정: 예정 기능 안내 (스크린샷 디자인과 동일) ──
                    !isApiReady -> {
                        Text(
                            text = "예정 기능:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                        Spacer(Modifier.height(10.dp))
                        val features = listOf(
                            "참여자 중간 지점 계산",
                            "근처 카페/식당 추천",
                            "AI 기반 맞춤 장소 추천",
                            "음식 취향 기반 필터링"
                        )
                        features.forEach { feature ->
                            Row(
                                modifier = Modifier.padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF7C3AED))
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(feature, fontSize = 14.sp, color = Color(0xFF374151))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "※ 네이버 API 키를 설정하면 실제 추천이 활성화됩니다.",
                            fontSize = 11.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }

                    // ── 로딩 중 ───────────────────────────────────────────────
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color(0xFF7C3AED), modifier = Modifier.size(36.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("참여자 위치 분석 중...", fontSize = 13.sp, color = Color(0xFF6B7280))
                                Text("중간 지점을 계산하고 있어요", fontSize = 11.sp, color = Color(0xFF9CA3AF))
                            }
                        }
                    }

                    // ── 에러 ─────────────────────────────────────────────────
                    error != null && places.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("😅", fontSize = 32.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(error, fontSize = 13.sp, color = Color(0xFF6B7280), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                Spacer(Modifier.height(12.dp))
                                TextButton(onClick = onRefresh) {
                                    Text("다시 시도", color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // ── Top 5 결과 ────────────────────────────────────────────
                    places.isNotEmpty() -> {
                        Text(
                            text = "📍 만나기 좋은 장소 TOP ${places.size}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7C3AED)
                        )
                        Spacer(Modifier.height(10.dp))
                        places.forEachIndexed { idx, place ->
                            PlaceResultCard(rank = idx + 1, place = place)
                            if (idx < places.lastIndex) Spacer(Modifier.height(8.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                        TextButton(
                            onClick = onRefresh,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("다시 검색", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 확인 버튼 (스크린샷 디자인과 동일)
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4285F4)
                    )
                ) {
                    Text("확인", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PlaceResultCard(rank: Int, place: PlaceResult) {
    val rankColors = listOf(
        Color(0xFFFFD700), // 1위: 금
        Color(0xFFC0C0C0), // 2위: 은
        Color(0xFFCD7F32), // 3위: 동
        Color(0xFF9CA3AF), // 4위
        Color(0xFF9CA3AF)  // 5위
    )
    val rankColor = rankColors.getOrNull(rank - 1) ?: Color(0xFF9CA3AF)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF9FAFB))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 순위 뱃지
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(rankColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$rank",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = place.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937),
                maxLines = 1
            )
            Text(
                text = place.category,
                fontSize = 11.sp,
                color = Color(0xFF7C3AED)
            )
            if (place.address.isNotBlank()) {
                Text(
                    text = place.address,
                    fontSize = 11.sp,
                    color = Color(0xFF6B7280),
                    maxLines = 1
                )
            }
        }
        if (place.phone.isNotBlank()) {
            Icon(
                Icons.Default.Phone,
                contentDescription = "전화",
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ─── 통화 중 배너 (채팅방 상단 고정) ─────────────────────────────────────────
@Composable
private fun ActiveCallBanner(
    callType: String,
    participantCount: Int,
    onJoinClick: () -> Unit
) {
    val isVideo = callType == "video"
    val accentColor = Color(0xFF7C3AED)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F3FF))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 아이콘
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(accentColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        // 텍스트
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isVideo) "영상통화 진행 중" else "음성통화 진행 중",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                text = "${participantCount}명 참여 중 · 탭하여 참여하세요",
                fontSize = 11.sp,
                color = Color(0xFF6B7280)
            )
        }

        // 참여 버튼
        Button(
            onClick = onJoinClick,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text("참여하기", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }

    HorizontalDivider(color = Color(0xFFE9E5FF), thickness = 1.dp)
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