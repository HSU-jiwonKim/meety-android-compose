package com.bugzero.meety.ui.feed.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.bugzero.meety.ui.feed.FeedConstants
import com.bugzero.meety.ui.team.Team
import com.bugzero.meety.ui.theme.Purple
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 틴더 스타일의 스와이프 카드
 *
 * - 좌우로 드래그하면 Like/Pass 처리 (임계값 초과 시 카드가 화면 밖으로 날아감)
 * - 임계값 미달 시 spring 애니메이션으로 원위치
 * - 드래그 방향에 따라 LIKE / PASS 오버레이 표시
 * - 탭하면 상세보기
 */
@Composable
fun SwipeCard(
    team: Team,
    onLike: () -> Unit,
    onPass: () -> Unit,
    onInfo: () -> Unit
) {
    // team이 바뀔 때마다 애니메이션 초기화 (새 카드 → 0에서 시작)
    val offsetXAnim = remember(team.teamId) { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    val rotation = offsetXAnim.value / FeedConstants.ROTATION_DIVISOR

    // 드래그 비율에 따라 오버레이 투명도 계산 (0% ~ 100%)
    val overlayAlpha = (abs(offsetXAnim.value) / (FeedConstants.SWIPE_THRESHOLD * 0.6f)).coerceIn(0f, 1f)
    val isLiking = offsetXAnim.value > 30f
    val isPassing = offsetXAnim.value < -30f

    val colorIndex = (team.teamId.hashCode() and Int.MAX_VALUE) % FeedConstants.CardColorPalette.size
    val bgColors = FeedConstants.CardColorPalette[colorIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(offsetXAnim.value.roundToInt(), 0) }
            .rotate(rotation)
            .shadow(12.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .pointerInput(team.teamId) {
                detectDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            when {
                                // 임계값 초과 → 카드 날아가기
                                offsetXAnim.value > FeedConstants.SWIPE_THRESHOLD -> {
                                    offsetXAnim.animateTo(
                                        targetValue = 3000f,
                                        animationSpec = tween(300, easing = FastOutLinearInEasing)
                                    )
                                    onLike()
                                }
                                offsetXAnim.value < -FeedConstants.SWIPE_THRESHOLD -> {
                                    offsetXAnim.animateTo(
                                        targetValue = -3000f,
                                        animationSpec = tween(300, easing = FastOutLinearInEasing)
                                    )
                                    onPass()
                                }
                                // 임계값 미달 → 통통 튀며 원위치
                                else -> {
                                    offsetXAnim.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            offsetXAnim.snapTo(offsetXAnim.value + dragAmount.x)
                        }
                    }
                )
            }
            .clickable { onInfo() }
    ) {
        // ── 배경 그라데이션 ──
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(bgColors)))

        // ── 하단 어둡게 ──
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.4f to Color.Transparent, 1.0f to Color.Black.copy(alpha = 0.7f))
            )
        )

        // ── LIKE 오버레이 (오른쪽으로 드래그) ──
        if (isLiking) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 24.dp, top = 28.dp)
                    .alpha(overlayAlpha)
                    .border(2.5.dp, Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                    .background(Color(0xFF4CAF50).copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "LIKE",
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    letterSpacing = 2.sp
                )
            }
        }

        // ── PASS 오버레이 (왼쪽으로 드래그) ──
        if (isPassing) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 24.dp, top = 28.dp)
                    .alpha(overlayAlpha)
                    .border(2.5.dp, FeedConstants.PassRed, RoundedCornerShape(8.dp))
                    .background(FeedConstants.PassRed.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "PASS",
                    color = FeedConstants.PassRed,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    letterSpacing = 2.sp
                )
            }
        }

        // ── "탭하여 자세히 보기" 힌트 (스와이프 중이 아닐 때만) ──
        if (!isLiking && !isPassing) {
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
                    Text(
                        "탭하여 자세히 보기",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // ── 팀 정보 (하단) ──
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    team.teamName,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(Purple.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "${team.memberIds.size}명",
                        fontSize = 13.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(team.description, fontSize = 16.sp, color = Color.LightGray, maxLines = 2)
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
