package com.bugzero.meety.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * 채팅방 목록 화면
 * - Firestore 실시간 구독으로 목록 자동 갱신
 * - 빈 상태 / 로딩 / 에러 상태 처리
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onChatClick: (chatId: String, roomName: String) -> Unit = { _, _ -> },
    viewModel: ChatViewModel = viewModel()
) {
    val chatList by viewModel.chatList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

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
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )
        HorizontalDivider(color = Color(0xFFF3F4F6))

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                // 로딩 상태
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFFA78BFA)
                    )
                }

                // 에러 상태
                errorMessage != null -> {
                    ChatErrorView(
                        message = errorMessage!!,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // 빈 상태
                chatList.isEmpty() -> {
                    ChatEmptyView(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // 목록 표시
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            items = chatList,
                            key = { it.id } // 효율적인 recomposition을 위한 key 지정
                        ) { chat ->
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
        // 프로필 이모지 원형 배경
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFA78BFA), Color(0xFFF472B6))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = chat.emoji,
                fontSize = 22.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 팀 이름 + 마지막 메시지
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chat.teamName.ifEmpty { "알 수 없는 팀" },
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = chat.lastMessage.ifEmpty { "아직 메시지가 없습니다" },
                fontSize = 13.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 시간 + 안읽은 메시지 배지
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = timeText,
                fontSize = 11.sp,
                color = Color.LightGray
            )

            // 안읽은 메시지 배지 (애니메이션으로 나타남/사라짐)
            AnimatedVisibility(
                visible = chat.unreadCount > 0,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFFA78BFA),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        // 99개 초과 시 "99+" 표시
                        text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(),
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * 채팅방 없을 때 빈 상태 화면
 */
@Composable
private fun ChatEmptyView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "💬",
            fontSize = 56.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "아직 채팅방이 없어요",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF374151)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "팀을 매칭하고 대화를 시작해보세요!",
            fontSize = 14.sp,
            color = Color(0xFF9CA3AF)
        )
    }
}

/**
 * 에러 발생 시 안내 화면
 */
@Composable
private fun ChatErrorView(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "⚠️", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = Color(0xFF9CA3AF)
        )
    }
}