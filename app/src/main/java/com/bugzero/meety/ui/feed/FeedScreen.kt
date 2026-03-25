package com.bugzero.meety.ui.feed

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bugzero.meety.ui.theme.*
import kotlin.math.roundToInt

data class MockTeam(
    val id: String,
    val name: String,
    val department: String,
    val size: String,
    val tags: List<String>
)

val mockTeams = listOf(
    MockTeam("1", "경영 귀요미들", "경영학과", "3:3", listOf("활발한", "술좋아함", "MBTI-E")),
    MockTeam("2", "코딩 전사들", "컴퓨터공학과", "2:2", listOf("차분한", "취미공유", "갓생")),
    MockTeam("3", "미대 여신들", "시각디자인과", "3:3", listOf("힙한", "전시회", "패션")),
    MockTeam("4", "운동 매니아", "체육학과", "4:4", listOf("에너지넘치는", "활동적", "친절함")),
)

@Composable
fun FeedScreen(
    viewModel: FeedViewModel = viewModel(),
    onNavigateToProfile: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentTeam = uiState.teams.getOrNull(uiState.currentIndex)
    val gradientBrush = Brush.linearGradient(listOf(Color(0xFFB44FD3), Color(0xFFEC4899)))

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9FAFB))
        ) {
            // ── 상단 앱바 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(gradientBrush, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Meety",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.drawWithCache {
                            val brush = Brush.linearGradient(
                                listOf(Color(0xFFB44FD3), Color(0xFFEC4899))
                            )
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
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Search, contentDescription = "검색", tint = Color(0xFF4B4B4B))
                    }
                    Box {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Notifications, contentDescription = "알림", tint = Color(0xFF4B4B4B))
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFFEC4899), CircleShape)
                                .align(Alignment.TopEnd)
                                .offset(x = (-10).dp, y = 10.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .shadow(3.dp, CircleShape)
                            .background(Color.White, CircleShape)
                            .clickable { onNavigateToProfile() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "프로필",
                            tint = Color(0xFF9B59B6),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ── 탭 버튼 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isRecommend = uiState.viewMode == "recommend"
                val isList = uiState.viewMode == "list"

                Box(
                    modifier = Modifier
                        .border(BorderStroke(1.5.dp, Color(0xFFD1B8E8)), RoundedCornerShape(25.dp))
                        .background(Color.White, RoundedCornerShape(25.dp))
                        .padding(4.dp)
                ) {
                    Row {
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .then(
                                    if (isRecommend)
                                        Modifier.background(gradientBrush, RoundedCornerShape(20.dp))
                                    else Modifier
                                )
                                .clickable { viewModel.setViewMode("recommend") }
                                .padding(horizontal = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isRecommend) Color.White else Color(0xFF9B59B6)
                                )
                                Text(
                                    "추천",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = if (isRecommend) Color.White else Color(0xFF9B59B6)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .then(
                                    if (isList)
                                        Modifier.background(gradientBrush, RoundedCornerShape(20.dp))
                                    else Modifier
                                )
                                .clickable { viewModel.setViewMode("list") }
                                .padding(horizontal = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    Icons.Default.FormatListBulleted,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isList) Color.White else Color(0xFF9B59B6)
                                )
                                Text(
                                    "전체 목록",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = if (isList) Color.White else Color(0xFF9B59B6)
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.viewMode == "recommend") {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    if (currentTeam != null) {
                        SwipeCard(
                            team = currentTeam,
                            onLike = { viewModel.onCardSwiped(true) },
                            onPass = { viewModel.onCardSwiped(false) },
                            onInfo = { viewModel.selectTeam(currentTeam.id) }
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("모든 팀을 확인했습니다!", color = Gray500)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.resetFeed() },
                                colors = ButtonDefaults.buttonColors(containerColor = Purple)
                            ) {
                                Text("처음부터 다시보기")
                            }
                        }
                    }
                }

                if (currentTeam != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 뒤로가기
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .shadow(4.dp, CircleShape)
                                .background(Color.White, CircleShape)
                                .clickable { viewModel.undoSwipe() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Undo, "뒤로가기", tint = Gray500, modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.width(20.dp))

                        // X 버튼 - 핑크 테두리
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .shadow(6.dp, CircleShape)
                                .background(Color.White, CircleShape)
                                .border(2.dp, Color(0xFFFF4B6E).copy(alpha = 0.4f), CircleShape)
                                .clickable { viewModel.onCardSwiped(false) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, "패스", tint = Color(0xFFFF4B6E), modifier = Modifier.size(32.dp))
                        }
                        Spacer(Modifier.width(20.dp))

                        // 하트 버튼 - background 먼저, border 나중에 (테두리가 위에 표시됨)
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .shadow(6.dp, CircleShape)
                                .background(gradientBrush, CircleShape)
                                .border(2.dp, Color(0xFFFF4B6E).copy(alpha = 0.4f), CircleShape)
                                .clickable { viewModel.onCardSwiped(true) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Favorite, "좋아요", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.teams) { team ->
                        TeamListItem(team, onTeamClick = { id -> viewModel.selectTeam(id) })
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.selectedTeam != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            MeetingDetailScreen(
                team = uiState.selectedTeam,
                onLikeClick = { viewModel.onCardSwiped(true) },
                onPassClick = { viewModel.onCardSwiped(false) },
                onBackClick = { viewModel.selectTeam("") }
            )
        }
    }
}

@Composable
fun SwipeCard(
    team: MockTeam,
    onLike: () -> Unit,
    onPass: () -> Unit,
    onInfo: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val rotation = (offsetX / 60f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(offsetX.roundToInt(), 0) }
            .rotate(rotation)
            .shadow(12.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .pointerInput(team.id) {
                detectDragGestures(
                    onDragEnd = {
                        when {
                            offsetX > 400 -> { onLike(); offsetX = 0f }
                            offsetX < -400 -> { onPass(); offsetX = 0f }
                            else -> { offsetX = 0f }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                    }
                )
            }
            .clickable { onInfo() }
    ) {
        val bgColors = when (team.id) {
            "1" -> listOf(Color(0xFFB39DDB), Color(0xFF7E57C2))
            "2" -> listOf(Color(0xFF80CBC4), Color(0xFF26A69A))
            "3" -> listOf(Color(0xFFF48FB1), Color(0xFFEC407A))
            else -> listOf(Color(0xFF90CAF9), Color(0xFF1E88E5))
        }
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(bgColors)))
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.4f to Color.Transparent, 1.0f to Color.Black.copy(alpha = 0.7f))
            )
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 16.dp)
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .clickable { onInfo() }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("👆", fontSize = 13.sp)
                Spacer(Modifier.width(4.dp))
                Text("탭하여 자세히 보기", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(team.name, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(Purple.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(team.size, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(team.department, fontSize = 16.sp, color = Color.LightGray)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                team.tags.take(3).forEach { tag ->
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("#$tag", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun TeamListItem(team: MockTeam, onTeamClick: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onTeamClick(team.id) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEDE9FE)),
                contentAlignment = Alignment.Center
            ) {
                Text("👥", fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(team.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Gray900)
                Text("${team.department} · ${team.size}", fontSize = 13.sp, color = Gray500)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Gray400)
        }
    }
}