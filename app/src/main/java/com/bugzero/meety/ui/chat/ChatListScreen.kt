package com.bugzero.meety.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onChatClick: (chatId: String, roomName: String) -> Unit = { _, _ -> },
    viewModel: ChatViewModel = viewModel()
) {
    val chatList by viewModel.chatList.collectAsState()
    val requestList by viewModel.requestList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val friendList by viewModel.friendList.collectAsState()
    val isLoadingFriends by viewModel.isLoadingFriends.collectAsState()

    var showNewChatSheet by remember { mutableStateOf(false) }
    var showFriendSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        // 상단 타이틀 바
        TopAppBar(
            title = {
                Text(
                    text = "채팅",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            },
            actions = {
                IconButton(onClick = { showNewChatSheet = true }) {
                    Box {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "새 채팅",
                            tint = Color(0xFF374151),
                            modifier = Modifier.size(26.dp)
                        )
                        // 우측 하단에 + 뱃지
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color(0xFF374151), CircleShape)
                                .align(Alignment.BottomEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                fontSize = 9.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 9.sp
                            )
                        }
                    }
                }

            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )
        HorizontalDivider(color = Color(0xFFF3F4F6))

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFFA78BFA)
                    )
                }
                errorMessage != null -> {
                    ChatErrorView(
                        message = errorMessage!!,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                chatList.isEmpty() -> {
                    ChatEmptyView(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (requestList.isNotEmpty()) {
                            item {
                                Text(
                                    text = "요청",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                            items(requestList) { request ->
                                RequestListItem(
                                    request = request,
                                    onAccept = { viewModel.acceptRequest(request.likeId) },
                                    onReject = { viewModel.rejectRequest(request.likeId) }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = Color(0xFFF3F4F6)
                                )
                            }
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                        }

                        items(items = chatList, key = { it.id }) { chat ->
                            ChatListItem(
                                chat = chat,
                                timeText = viewModel.formatTime(chat.lastMessageAt),
                                onClick = { onChatClick(chat.id, chat.teamName) }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = Color(0xFFF3F4F6)
                            )
                        }
                    }
                }
            }
        }
    }

    // ── 새 채팅 선택 바텀시트 ──────────────────────────────────────
    if (showNewChatSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNewChatSheet = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp)
            ) {
                Text(
                    text = "새로운 채팅",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // 채팅 유형 선택 3개
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    NewChatTypeItem(
                        emoji = "💬",
                        label = "일반채팅",
                        onClick = {
                            showNewChatSheet = false
                            viewModel.loadFriendList()
                            showFriendSheet = true
                        }
                    )
                    NewChatTypeItem(
                        emoji = "🚩",
                        label = "팀채팅",
                        onClick = { showNewChatSheet = false }  // TODO
                    )
                    NewChatTypeItem(
                        emoji = "🔒",
                        label = "비밀채팅",
                        onClick = { showNewChatSheet = false }  // TODO
                    )
                }
            }
        }
    }

    // ── 친구 목록 바텀시트 ─────────────────────────────────────────
    if (showFriendSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFriendSheet = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 40.dp)
            ) {
                Text(
                    text = "대화상대 선택",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (isLoadingFriends) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFA78BFA))
                    }
                } else if (friendList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🫙", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("아직 대화 가능한 친구가 없어요", fontSize = 14.sp, color = Color.Gray)
                        }
                    }
                } else {
                    friendList.forEach { friend ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.createOrGetDirectChat(
                                        friend = friend,
                                        onSuccess = { chatId, roomName ->
                                            showFriendSheet = false
                                            onChatClick(chatId, roomName)
                                        },
                                        onFailure = { message ->
                                            android.util.Log.e("ChatListScreen", message)
                                        }
                                    )
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFFA78BFA), Color(0xFFF472B6))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👤", fontSize = 22.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = friend.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1F2937)
                                )
                                if (friend.department.isNotEmpty()) {
                                    Text(
                                        text = "${friend.department} · ${friend.mbti}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF3F4F6))
                    }
                }
            }
        }
    }
}

// ── 새 채팅 유형 아이템 ────────────────────────────────────────────
@Composable
private fun NewChatTypeItem(
    emoji: String,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color(0xFFF3F4F6)),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 26.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFF374151),
            fontWeight = FontWeight.Medium
        )
    }
}
/**
 * 채팅방 목록 단일 아이템 컴포저블
 */
@Composable
private fun ChatListItem(
    chat: ChatPreview,
    timeText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(Color(0xFFA78BFA), Color(0xFFF472B6)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = chat.emoji, fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = chat.teamName.ifEmpty { "알 수 없는 팀" }, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = chat.lastMessage.ifEmpty { "아직 메시지가 없습니다" }, fontSize = 13.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = timeText, fontSize = 11.sp, color = Color.LightGray)
            AnimatedVisibility(visible = chat.unreadCount > 0, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .background(color = Color(0xFFA78BFA), shape = RoundedCornerShape(10.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(), fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ChatEmptyView(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "💬", fontSize = 56.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "아직 채팅방이 없어요", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "팀을 매칭하고 대화를 시작해보세요!", fontSize = 14.sp, color = Color(0xFF9CA3AF))
    }
}

@Composable
private fun ChatErrorView(message: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "⚠️", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = message, fontSize = 14.sp, color = Color(0xFF9CA3AF))
    }
}

@Composable
private fun RequestListItem(
    request: com.bugzero.meety.ui.team.ReceivedLikeItem,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFE5E7EB)), contentAlignment = Alignment.Center) {
                Text(text = "👤")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = request.fromUserName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = "${request.fromUserDepartment} · ${request.fromUserMbti}", fontSize = 12.sp, color = Color.Gray)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onReject) { Text("거절") }
            Button(onClick = onAccept) { Text("수락") }
        }
    }
}
