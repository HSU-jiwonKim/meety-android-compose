package com.bugzero.meety.ui.feed.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugzero.meety.ui.feed.FeedConstants
import com.bugzero.meety.ui.team.Team
import com.bugzero.meety.ui.theme.Purple
import com.bugzero.meety.ui.theme.VioletText
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * 틴더 스타일의 스와이프 카드
 *
 * whyOpen / onWhyOpen 은 부모(RecommendContent)가 관리한다.
 * MatchReasonSheet가 액션 버튼 영역까지 덮으려면 SwipeCard 밖에서 렌더해야 하기 때문.
 */
@Composable
fun SwipeCard(
    team: Team,
    onLike: () -> Unit,
    onPass: () -> Unit,
    onInfo: () -> Unit,
    /** FeedUiState.cardFitScoreCache 에서 사전 계산된 종합 매칭 점수 */
    fitScore: Int = 70,
    // ? 버튼 상태는 부모가 관리
    whyOpen: Boolean = false,
    onWhyOpen: () -> Unit = {}
) {
    val offsetXAnim    = remember(team.teamId) { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val haptic         = LocalHapticFeedback.current
    val density        = LocalDensity.current
    val touchSlopPx    = with(density) { 8.dp.toPx() }

    val rotation     = offsetXAnim.value / FeedConstants.ROTATION_DIVISOR
    val overlayAlpha = (abs(offsetXAnim.value) / (FeedConstants.SWIPE_THRESHOLD * 0.6f)).coerceIn(0f, 1f)
    val isLiking     = offsetXAnim.value > 30f
    val isPassing    = offsetXAnim.value < -30f
    var hapticFired  by remember(team.teamId) { mutableStateOf(false) }

    val colorIndex = (team.teamId.hashCode() and Int.MAX_VALUE) % FeedConstants.CardColorPalette.size
    val bgColors   = FeedConstants.CardColorPalette[colorIndex]

    // ── 무한 애니메이션: ping 링 + 말풍선 floaty ──
    val infTrans = rememberInfiniteTransition(label = "qbtn_anim")
    val pingScale by infTrans.animateFloat(
        initialValue  = 1f, targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label         = "pingScale"
    )
    val pingAlpha by infTrans.animateFloat(
        initialValue  = 0.6f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label         = "pingAlpha"
    )
    val floatOffset by infTrans.animateFloat(
        initialValue  = 0f, targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "floatOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(offsetXAnim.value.roundToInt(), 0) }
            .rotate(rotation)
            .shadow(12.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .pointerInput(team.teamId) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var totalDx = 0f; var totalDy = 0f
                        var isDragging = false; var isVertical = false
                        hapticFired = false

                        loop@ while (true) {
                            val event  = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break@loop

                            when (event.type) {
                                PointerEventType.Move -> {
                                    val dx = change.position.x - change.previousPosition.x
                                    val dy = change.position.y - change.previousPosition.y
                                    totalDx += dx; totalDy += dy

                                    if (!isDragging) {
                                        val dist = sqrt(totalDx * totalDx + totalDy * totalDy)
                                        if (dist > touchSlopPx) {
                                            isDragging = true
                                            isVertical = abs(totalDy) > abs(totalDx)
                                        }
                                    }
                                    if (isDragging && !isVertical) {
                                        change.consume()
                                        val nextVal = offsetXAnim.value + dx
                                        val crossed =
                                            (nextVal >  FeedConstants.SWIPE_THRESHOLD && offsetXAnim.value <=  FeedConstants.SWIPE_THRESHOLD) ||
                                                    (nextVal < -FeedConstants.SWIPE_THRESHOLD && offsetXAnim.value >= -FeedConstants.SWIPE_THRESHOLD)
                                        if (crossed && !hapticFired) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            hapticFired = true
                                        }
                                        coroutineScope.launch { offsetXAnim.snapTo(offsetXAnim.value + dx) }
                                    }
                                }
                                PointerEventType.Release -> {
                                    if (isDragging && !isVertical) {
                                        coroutineScope.launch {
                                            when {
                                                offsetXAnim.value >  FeedConstants.SWIPE_THRESHOLD -> {
                                                    offsetXAnim.animateTo(3000f, tween(300, easing = FastOutLinearInEasing)); onLike()
                                                }
                                                offsetXAnim.value < -FeedConstants.SWIPE_THRESHOLD -> {
                                                    offsetXAnim.animateTo(-3000f, tween(300, easing = FastOutLinearInEasing)); onPass()
                                                }
                                                else -> offsetXAnim.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
                                            }
                                        }
                                    } else if (!isDragging && !down.isConsumed) {
                                        // 탭 → 상세보기 (? 버튼이 소비한 이벤트는 무시)
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
        // ── 배경 ──
        if (team.teamProfileImage.isNotBlank()) {
            coil.compose.AsyncImage(
                model              = team.teamProfileImage,
                contentDescription = "팀 대표 사진",
                modifier           = Modifier.fillMaxSize(),
                contentScale       = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(bgColors)))
        }

        // ── 하단 어둠 그라데이션 ──
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.4f to Color.Transparent, 1.0f to Color.Black.copy(alpha = 0.7f))
            )
        )

        // ── LIKE / PASS 오버레이 ──
        if (isLiking) {
            Box(
                Modifier
                    .align(Alignment.TopStart).padding(start = 24.dp, top = 28.dp)
                    .alpha(overlayAlpha)
                    .border(2.5.dp, Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                    .background(Color(0xFF4CAF50).copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) { Text("LIKE", color = Color(0xFF4CAF50), fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, letterSpacing = 2.sp) }
        }
        if (isPassing) {
            Box(
                Modifier
                    .align(Alignment.TopEnd).padding(end = 24.dp, top = 28.dp)
                    .alpha(overlayAlpha)
                    .border(2.5.dp, FeedConstants.PassRed, RoundedCornerShape(8.dp))
                    .background(FeedConstants.PassRed.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) { Text("PASS", color = FeedConstants.PassRed, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, letterSpacing = 2.sp) }
        }

        // ── 좌상단: "지금 활동중" 대신 매칭 점수 배지 ──
        if (!isLiking && !isPassing) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp, top = 14.dp)
                    .shadow(4.dp, RoundedCornerShape(999.dp))
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.95f))
                    .padding(horizontal = 11.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text  = "$fitScore",
                        style = TextStyle(
                            brush      = FeedConstants.GradientPurplePink,
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "매칭",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color(0xFF56535F)
                    )
                }
            }
        }

        // ── 팀 정보 (하단) ──
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    team.teamName, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.4).sp, color = Color.White
                )
                team.mbtiTags.firstOrNull()?.takeIf { it.isNotBlank() }?.let { mbti ->
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) { Text(mbti, fontSize = 12.sp, color = VioletText, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                "멤버 ${team.memberIds.size}명" +
                        (team.description.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                fontSize = 13.sp, fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.92f), maxLines = 1
            )
            if (team.tags.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    team.tags.take(3).forEach { tag ->
                        Box(
                            Modifier
                                .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 11.dp, vertical = 6.dp)
                        ) { Text(tag, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }

        // ── ? 버튼 + 말풍선 (스와이프/시트 열림 중엔 숨김) ──
        if (!isLiking && !isPassing && !whyOpen) {

            // ① ? 버튼: 46dp 컨테이너에 ping 링(뒤) + 버튼(앞)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 10.dp)
                    .size(46.dp),
                contentAlignment = Alignment.Center
            ) {
                // ping: 확장되며 사라지는 흰 테두리 원
                Box(
                    Modifier
                        .size(46.dp)
                        .scale(pingScale)
                        .alpha(pingAlpha)
                        .border(2.dp, Color.White, CircleShape)
                )
                // 실제 버튼
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.94f))
                        .pointerInput(team.teamId) {
                            awaitPointerEventScope {
                                while (true) {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    down.consume()
                                    var released = false
                                    loop@ while (true) {
                                        val ev = awaitPointerEvent()
                                        val ch = ev.changes.firstOrNull { it.id == down.id } ?: break@loop
                                        ch.consume()
                                        if (ev.type == PointerEventType.Release) { released = true; break@loop }
                                        if (ev.type != PointerEventType.Move) break@loop
                                    }
                                    if (released) onWhyOpen()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("?", color = Purple, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            // ② 말풍선: floaty 위아래 + 꼬리(회전 사각형)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 58.dp, end = 14.dp)
                    .offset(y = floatOffset.dp)
            ) {
                // 본체
                Box(
                    modifier = Modifier
                        .padding(top = 7.dp)
                        .shadow(8.dp, RoundedCornerShape(13.dp))
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color.White)
                        .padding(horizontal = 13.dp, vertical = 9.dp)
                ) {
                    Text(
                        "왜 이 팀이 매칭 되었나요?",
                        color = Color(0xFF17161D), fontSize = 12.5.sp, fontWeight = FontWeight.Bold
                    )
                }
                // 꼬리: 45° 회전 흰 사각형 → 말풍선 위에 z-order로 겹쳐 삼각형처럼 보임
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 13.dp)
                        .size(12.dp)
                        .rotate(45f)
                        .background(Color.White)
                )
            }
        }
    }
}
