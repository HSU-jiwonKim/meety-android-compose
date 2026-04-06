package com.bugzero.meety.ui.feed.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
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
import kotlin.math.sqrt

/**
 * 틴더 스타일의 스와이프 카드
 *
 * [개선] detectDragGestures → awaitEachGesture 커스텀 제스처 처리기
 *   - 수평 드래그: 스와이프 (Like/Pass)
 *   - 수직 드래그: 스크롤로 전달 (카드 내 LazyColumn과 충돌 없음)
 *   - 탭: 상세보기 (드래그와 별도로 처리)
 *   - 임계값 진입 시 햅틱 피드백
 */
@Composable
fun SwipeCard(
    team: Team,
    onLike: () -> Unit,
    onPass: () -> Unit,
    onInfo: () -> Unit
) {
    val offsetXAnim    = remember(team.teamId) { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val haptic         = LocalHapticFeedback.current
    val density        = LocalDensity.current

    // 8dp를 px로 변환 — 이 이상 움직여야 드래그로 판단
    val touchSlopPx = with(density) { 8.dp.toPx() }

    val rotation     = offsetXAnim.value / FeedConstants.ROTATION_DIVISOR
    val overlayAlpha = (abs(offsetXAnim.value) / (FeedConstants.SWIPE_THRESHOLD * 0.6f)).coerceIn(0f, 1f)
    val isLiking     = offsetXAnim.value > 30f
    val isPassing    = offsetXAnim.value < -30f

    // 임계값을 넘었는지 추적해 햅틱을 한 번만 발생시킨다
    var hapticFired by remember(team.teamId) { mutableStateOf(false) }

    val colorIndex = (team.teamId.hashCode() and Int.MAX_VALUE) % FeedConstants.CardColorPalette.size
    val bgColors   = FeedConstants.CardColorPalette[colorIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(offsetXAnim.value.roundToInt(), 0) }
            .rotate(rotation)
            .shadow(12.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .pointerInput(team.teamId) {
                // awaitEachGesture: 각 포인터 다운마다 독립적인 제스처 세션
                awaitPointerEventScope {
                    while (true) {
                        // 1) 첫 손가락 내려올 때까지 대기
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var totalDx = 0f
                        var totalDy = 0f
                        var isDragging = false
                        var isVertical = false
                        hapticFired = false

                        // 2) 손가락이 올라올 때까지 이벤트 루프
                        loop@ while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: break@loop

                            when (event.type) {
                                PointerEventType.Move -> {
                                    val dx = change.position.x - change.previousPosition.x
                                    val dy = change.position.y - change.previousPosition.y
                                    totalDx += dx
                                    totalDy += dy

                                    if (!isDragging) {
                                        val dist = sqrt(totalDx * totalDx + totalDy * totalDy)
                                        if (dist > touchSlopPx) {
                                            isDragging = true
                                            isVertical = abs(totalDy) > abs(totalDx)
                                        }
                                    }

                                    if (isDragging && !isVertical) {
                                        change.consume()

                                        // 임계값 통과 시 햅틱 (한 번만)
                                        val nextVal = offsetXAnim.value + dx
                                        val crossedThreshold =
                                            (nextVal > FeedConstants.SWIPE_THRESHOLD && offsetXAnim.value <= FeedConstants.SWIPE_THRESHOLD) ||
                                            (nextVal < -FeedConstants.SWIPE_THRESHOLD && offsetXAnim.value >= -FeedConstants.SWIPE_THRESHOLD)

                                        if (crossedThreshold && !hapticFired) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            hapticFired = true
                                        }

                                        coroutineScope.launch {
                                            offsetXAnim.snapTo(offsetXAnim.value + dx)
                                        }
                                    }
                                }

                                PointerEventType.Release -> {
                                    if (isDragging && !isVertical) {
                                        // 드래그 종료 — 임계값에 따라 날아가거나 원위치
                                        coroutineScope.launch {
                                            when {
                                                offsetXAnim.value > FeedConstants.SWIPE_THRESHOLD -> {
                                                    offsetXAnim.animateTo(
                                                        3000f,
                                                        animationSpec = tween(300, easing = FastOutLinearInEasing)
                                                    )
                                                    onLike()
                                                }
                                                offsetXAnim.value < -FeedConstants.SWIPE_THRESHOLD -> {
                                                    offsetXAnim.animateTo(
                                                        -3000f,
                                                        animationSpec = tween(300, easing = FastOutLinearInEasing)
                                                    )
                                                    onPass()
                                                }
                                                else -> {
                                                    offsetXAnim.animateTo(
                                                        0f,
                                                        animationSpec = spring(
                                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                                            stiffness    = Spring.StiffnessMedium
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    } else if (!isDragging) {
                                        // 드래그가 아니었으면 탭 → 상세보기
                                        onInfo()
                                    }
                                    break@loop
                                }

                                else -> break@loop
                            }
                        }
                    }
                }
            }
    ) {
        // ── 배경: 이미지 있으면 이미지, 없으면 그라데이션 ──
        if (team.teamProfileImage.isNotBlank()) {
            coil.compose.AsyncImage(
                model              = team.teamProfileImage,
                contentDescription = "팀 대표 사진",
                modifier           = Modifier.fillMaxSize(),
                contentScale       = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(bgColors)))
        }

        // ── 하단 어둡게 ──
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.4f to Color.Transparent, 1.0f to Color.Black.copy(alpha = 0.7f))
            )
        )

        // ── LIKE 오버레이 ──
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
                Text("LIKE", color = Color(0xFF4CAF50), fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, letterSpacing = 2.sp)
            }
        }

        // ── PASS 오버레이 ──
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
                Text("PASS", color = FeedConstants.PassRed, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, letterSpacing = 2.sp)
            }
        }

        // ── "탭하여 자세히 보기" 힌트 ──
        if (!isLiking && !isPassing) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = 16.dp)
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("👆", fontSize = 13.sp)
                    Spacer(Modifier.width(4.dp))
                    Text("탭하여 자세히 보기", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
                Text(team.teamName, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(Purple.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("${team.memberIds.size}명", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
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
