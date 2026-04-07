package com.bugzero.meety.ui.feed

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.bugzero.meety.ui.feed.TeamActionStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentTeam = uiState.teams.getOrNull(uiState.currentIndex)
    val nextTeam    = uiState.teams.getOrNull(uiState.currentIndex + 1)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FeedConstants.BackgroundGray)
        ) {
            FeedTopBar()

            FeedTabBar(
                currentMode = uiState.viewMode,
                onModeChange = { viewModel.setViewMode(it) }
            )

            when {
                uiState.isLoading -> LoadingContent()
                uiState.errorMessage != null -> ErrorContent(
                    message = uiState.errorMessage ?: "",
                    onRetry = { viewModel.fetchRemoteTeams() }
                )
                else -> {
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
                                currentTeam    = currentTeam,
                                nextTeam       = nextTeam,
                                isLoadingMore  = uiState.isLoadingMore,
                                onLike         = { viewModel.onCardSwiped(true) },
                                onPass         = { viewModel.onCardSwiped(false) },
                                onInfo         = { currentTeam?.let { viewModel.selectTeam(it.teamId) } },
                                onUndo         = { viewModel.undoSwipe() },
                                onReset        = { viewModel.fetchRemoteTeams() }
                            )
                        } else {
                            // 목록 모드: pull-to-refresh (allTeams = 전체 보기)
                            PullToRefreshBox(
                                isRefreshing = uiState.isLoadingAllTeams,
                                onRefresh    = { viewModel.fetchAllTeams(loadMore = false) },
                                modifier     = Modifier.fillMaxSize()
                            ) {
                                if (uiState.allTeams.isEmpty() && !uiState.isLoadingAllTeams) {
                                    Box(
                                        modifier         = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Default.SearchOff,
                                                contentDescription = null,
                                                tint     = Gray500,
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(Modifier.height(12.dp))
                                            Text("활성 팀이 없어요", color = Gray500, fontSize = 14.sp)
                                        }
                                    }
                                } else {
                                    ListContent(
                                        teams         = uiState.allTeams,
                                        likedTeamIds  = uiState.likedTeamIds,
                                        passedTeamIds = uiState.passedTeamIds,
                                        myTeamId      = uiState.myTeamId,
                                        onTeamClick   = { viewModel.selectTeam(it) },
                                        onLoadMore    = { viewModel.fetchAllTeams(loadMore = true) },
                                        hasMore       = uiState.allTeamsHasMore
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 상세 화면 오버레이 ──
        AnimatedVisibility(
            visible = uiState.selectedTeam != null,
            enter   = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit    = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            // selectedTeam의 액션 상태 계산 (MY_TEAM > LIKED > PASSED > NONE)
            val selectedStatus = uiState.selectedTeam?.let { team ->
                when {
                    uiState.myTeamId.isNotEmpty() && team.teamId == uiState.myTeamId ->
                        TeamActionStatus.MY_TEAM
                    uiState.likedTeamIds.contains(team.teamId) ->
                        TeamActionStatus.LIKED
                    uiState.passedTeamIds.contains(team.teamId) ->
                        TeamActionStatus.PASSED
                    else -> TeamActionStatus.NONE
                }
            } ?: TeamActionStatus.NONE

            MeetingDetailScreen(
                team                 = uiState.selectedTeam,
                userPreferences      = uiState.userPreferences,
                status               = selectedStatus,
                memberProfiles       = uiState.memberProfiles,
                isMembersLoading     = uiState.isMembersLoading,
                onLikeClick          = { viewModel.onSelectedTeamLike() },
                onPassClick          = { viewModel.onSelectedTeamPass() },
                onCancelLike         = { viewModel.onCancelLikeFromDetail() },
                onSendLikeFromPassed = { viewModel.onSendLikeFromPassed() },
                onBackClick          = { viewModel.clearSelectedTeam() }
            )
        }
    }
}

// ── 로딩 상태 ──
@Composable
private fun ColumnScope.LoadingContent() {
    Box(
        modifier         = Modifier.weight(1f).fillMaxWidth(),
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
        modifier         = Modifier.weight(1f).fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint     = FeedConstants.ErrorRed,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(message, color = Gray700, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors  = ButtonDefaults.buttonColors(containerColor = Purple)
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
    isLoadingMore: Boolean,
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
                // 다음 카드 스택 효과
                nextTeam?.let { next ->
                    val nextColorIndex = (next.teamId.hashCode() and Int.MAX_VALUE) % FeedConstants.CardColorPalette.size
                    val nextColors = FeedConstants.CardColorPalette[nextColorIndex].map { it.copy(alpha = 0.55f) }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .shadow(3.dp, RoundedCornerShape(24.dp))
                            .clip(RoundedCornerShape(24.dp))
                            .background(Brush.verticalGradient(nextColors))
                    )
                }

                SwipeCard(
                    team   = currentTeam,
                    onLike = onLike,
                    onPass = onPass,
                    onInfo = onInfo
                )

                // 추가 페이지 로딩 중 표시
                if (isLoadingMore) {
                    CircularProgressIndicator(
                        modifier    = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
                        color       = Purple.copy(alpha = 0.6f),
                        strokeWidth = 2.dp
                    )
                }
            } else {
                Column(
                    modifier            = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("모든 팀을 확인했습니다!", color = Gray500)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onReset,
                        colors  = ButtonDefaults.buttonColors(containerColor = Purple)
                    ) {
                        Text("다시 불러오기")
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
    likedTeamIds: Set<String>,
    passedTeamIds: Set<String>,
    myTeamId: String,
    onTeamClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    hasMore: Boolean
) {
    val listState = rememberLazyListState()

    // 마지막 아이템 근처에 도달하면 추가 로딩
    val shouldLoadMore = remember(listState) {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 3
        }
    }
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && hasMore) onLoadMore()
    }

    LazyColumn(
        state          = listState,
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(teams) { team ->
            // 우선순위: MY_TEAM > LIKED > PASSED > NONE
            val status = when {
                myTeamId.isNotEmpty() && team.teamId == myTeamId -> TeamActionStatus.MY_TEAM
                likedTeamIds.contains(team.teamId)               -> TeamActionStatus.LIKED
                passedTeamIds.contains(team.teamId)              -> TeamActionStatus.PASSED
                else                                             -> TeamActionStatus.NONE
            }
            TeamListItem(team = team, onTeamClick = onTeamClick, status = status)
        }
    }
}
