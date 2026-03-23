package com.bugzero.meety.ui.feed

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bugzero.meety.ui.theme.*
import kotlin.math.roundToInt

// 1. 데이터 모델 정의
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
    // ViewModel 상태 관찰
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentTeam = uiState.teams.getOrNull(uiState.currentIndex)

    Box(modifier = Modifier.fillMaxSize()) {
        // 메인 피드 레이아웃
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9FAFB))
        ) {
            // 상단 탭 및 프로필 버튼
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 추천/리스트 전환 버튼
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("recommend" to "추천", "list" to "전체").forEach { (mode, label) ->
                        val isSelected = uiState.viewMode == mode
                        Button(
                            onClick = { viewModel.setViewMode(mode) },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Purple else Color.White,
                                contentColor = if (isSelected) Color.White else Gray500
                            ),
                            contentPadding = PaddingValues(0.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                // 프로필 편집 이동 버튼
                IconButton(
                    onClick = onNavigateToProfile,
                    modifier = Modifier.size(42.dp).background(Color.White, CircleShape).shadow(1.dp, CircleShape)
                ) {
                    Icon(Icons.Default.Person, contentDescription = "프로필", tint = Purple)
                }
            }

            // 뷰 모드에 따른 컨텐츠
            if (uiState.viewMode == "recommend") {
                // 추천 스와이프 모드
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    if (currentTeam != null) {
                        SwipeCard(
                            team = currentTeam,
                            onLike = { viewModel.onCardSwiped(true) },
                            onPass = { viewModel.onCardSwiped(false) },
                            onInfo = { viewModel.selectTeam(currentTeam.id) }
                        )
                    } else {
                        // 모든 카드를 다 봤을 때
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("모든 팀을 확인했습니다!", color = Gray500)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.resetFeed() }, colors = ButtonDefaults.buttonColors(containerColor = Purple)) {
                                Text("처음부터 다시보기")
                            }
                        }
                    }
                }

                // 하단 조작 버튼
                if (currentTeam != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.undoSwipe() },
                            modifier = Modifier.size(48.dp).background(Color.White, CircleShape).shadow(2.dp, CircleShape)
                        ) { Icon(Icons.Default.Undo, "되돌리기", tint = Gray500) }

                        Spacer(Modifier.width(24.dp))

                        IconButton(
                            onClick = { viewModel.onCardSwiped(false) },
                            modifier = Modifier.size(64.dp).background(Color.White, CircleShape).shadow(4.dp, CircleShape)
                        ) { Icon(Icons.Default.Close, "패스", tint = Color.Red, modifier = Modifier.size(32.dp)) }

                        Spacer(Modifier.width(24.dp))

                        IconButton(
                            onClick = { viewModel.onCardSwiped(true) },
                            modifier = Modifier.size(64.dp).background(Purple, CircleShape).shadow(4.dp, CircleShape)
                        ) { Icon(Icons.Default.Favorite, "좋아요", tint = Color.White, modifier = Modifier.size(32.dp)) }
                    }
                }
            } else {
                // 전체 목록 모드
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

        // 상세 화면 오버레이 (selectedTeam이 null이 아닐 때만 표시)
        AnimatedVisibility(
            visible = uiState.selectedTeam != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            MeetingDetailScreen(
                team = uiState.selectedTeam,
                onLikeClick = { viewModel.onCardSwiped(true) },
                onPassClick = { viewModel.onCardSwiped(false) },
                onBackClick = { viewModel.selectTeam("") } // id를 빈값으로 보내 null 처리
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

    // 카드 드래그 시 회전 각도 계산
    val rotation = (offsetX / 60f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(offsetX.roundToInt(), 0) }
            .rotate(rotation)
            .shadow(12.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .pointerInput(team.id) { // team.id가 바뀌면 상태 초기화
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
        // 배경 이미지 대신 색상 그라데이션 (임시)
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))
            )
        )

        // 카드 정보 영역
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(team.name, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier.background(Purple.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(team.size, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Text(team.department, fontSize = 18.sp, color = Color.LightGray)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                team.tags.take(3).forEach { tag ->
                    Text("#$tag", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
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
            // 팀 아이콘 (임시)
            Box(
                modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xFFEDE9FE)),
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