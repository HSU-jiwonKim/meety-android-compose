package com.bugzero.meety.ui.feed

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bugzero.meety.ui.feed.components.*
import com.bugzero.meety.ui.team.Team
import com.bugzero.meety.ui.theme.*

/**
 * 피드 메인 화면
 *
 * 역할: 상태에 따라 적절한 컴포넌트를 "조립"하는 것.
 * 실제 UI 조각들은 components/ 폴더에 분리되어 있다.
 */
@Composable
fun FeedScreen(
    viewModel: FeedViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentTeam = uiState.teams.getOrNull(uiState.currentIndex)
    val nextTeam = uiState.teams.getOrNull(uiState.currentIndex + 1)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FeedConstants.BackgroundGray)
        ) {
            // ── 상단 앱바 ──
            FeedTopBar()

            // ── 탭 전환 ──
            FeedTabBar(
                currentMode = uiState.viewMode,
                onModeChange = { viewModel.setViewMode(it) }
            )

            // ── 본문 영역 ──
            when {
                uiState.isLoading -> LoadingContent()
                uiState.errorMessage != null -> ErrorContent(
                    message = uiState.errorMessage ?: "",
                    onRetry = { viewModel.fetchRemoteTeams() }
                )
                else -> {
                    // 탭 전환 시 슬라이드 + 페이드 애니메이션
                    AnimatedContent(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        targetState = uiState.viewMode,
                        transitionSpec = {
                            if (targetState == FeedViewMode.LIST) {
                                (slideInHorizontally(tween(280)) { it } + fadeIn(tween(280))) togetherWith
                                    (slideOutHorizontally(tween(280)) { -it } + fadeOut(tween(200)))
                            } else {
                                (slideInHorizontally(tween(280)) { -it } + fadeIn(tween(280))) togetherWith
                                    (slideOutHorizontally(tween(280)) { it } + fadeOut(tween(200)))
                            }
                        },
                        label = "tab_transition"
                    ) { mode ->
                        if (mode == FeedViewMode.RECOMMEND) {
                            RecommendContent(
                                currentTeam = currentTeam,
                                nextTeam = nextTeam,
                                onLike = { viewModel.onCardSwiped(true) },
                                onPass = { viewModel.onCardSwiped(false) },
                                onInfo = { currentTeam?.let { viewModel.selectTeam(it.teamId) } },
                                onUndo = { viewModel.undoSwipe() },
                                onReset = { viewModel.resetFeed() }
                            )
                        } else {
                            ListContent(
                                teams = uiState.teams,
                                onTeamClick = { viewModel.selectTeam(it) }
                            )
                        }
                    }
                }
            }
        }

        // ── 상세 화면 오버레이 ──
        AnimatedVisibility(
            visible = uiState.selectedTeam != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            MeetingDetailScreen(
                team = uiState.selectedTeam,
                userPreferences = uiState.userPreferences,
                onLikeClick = { viewModel.onCardSwiped(true) },
                onPassClick = { viewModel.onCardSwiped(false) },
                onBackClick = { viewModel.clearSelectedTeam() }
            )
        }
    }
}

// ── 로딩 상태 ──
@Composable
private fun ColumnScope.LoadingContent() {
    Box(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Purple)
            Spacer(Modifier.height(12.dp))
            Text("팀 목록을 불러오는 중...", color = Gray500, fontSize = 14.sp)
        }
    }
}

// ── 에러 상태 ──
@Composable
private fun ColumnScope.ErrorContent(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = FeedConstants.ErrorRed,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(message, color = Gray700, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Purple)
            ) {
                Text("다시 시도")
            }
        }
    }
}

// ── 추천 카드 모드 ──
@Composable
private fun RecommendContent(
    currentTeam: Team?,
    nextTeam: Team?,
    onLike: () -> Unit,
    onPass: () -> Unit,
    onInfo: () -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            if (currentTeam != null) {
                // ── 스택 효과: 다음 카드가 뒤에 살짝 보임 ──
                nextTeam?.let { next ->
                    val nextColorIndex =
                        (next.teamId.hashCode() and Int.MAX_VALUE) % FeedConstants.CardColorPalette.size
                    val nextColors = FeedConstants.CardColorPalette[nextColorIndex]
                        .map { it.copy(alpha = 0.55f) }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .shadow(3.dp, RoundedCornerShape(24.dp))
                            .clip(RoundedCornerShape(24.dp))
                            .background(Brush.verticalGradient(nextColors))
                    )
                }

                // ── 현재 카드 ──
                SwipeCard(
                    team = currentTeam,
                    onLike = onLike,
                    onPass = onPass,
                    onInfo = onInfo
                )
            } else {
                // 모든 카드를 다 본 경우
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("모든 팀을 확인했습니다!", color = Gray500)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onReset,
                        colors = ButtonDefaults.buttonColors(containerColor = Purple)
                    ) {
                        Text("처음부터 다시보기")
                    }
                }
            }
        }

        if (currentTeam != null) {
            SwipeActionButtons(
                onUndo = onUndo,
                onPass = onPass,
                onLike = onLike
            )
        }
    }
}

// ── 전체 목록 모드 ──
@Composable
private fun ListContent(
    teams: List<Team>,
    onTeamClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(teams) { team ->
            TeamListItem(team, onTeamClick = onTeamClick)
        }
    }
}
