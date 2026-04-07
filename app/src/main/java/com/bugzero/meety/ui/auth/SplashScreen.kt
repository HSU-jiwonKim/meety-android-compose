package com.bugzero.meety.ui.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Meety 스플래시 화면
 *
 * 피그마 디자인 재현:
 *   - 보라→핑크 그라디언트 배경
 *   - 떠다니는 하트 파티클 20개 (아래→위, 페이드인/아웃)
 *   - 중앙 로고: 반투명 박스 안 하트 아이콘 (pulse 애니메이션)
 *   - "Meety" 타이틀 (아래에서 올라오며 페이드인)
 *   - "새로운 만남의 시작" 서브타이틀 (페이드인)
 *   - 3.2초 후 onSplashFinished 콜백
 */
@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {

    // 3.2초 타이머
    LaunchedEffect(Unit) {
        delay(3200L)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF9333EA), // purple-600
                        Color(0xFF7E22CE), // purple-700
                        Color(0xFFDB2777)  // pink-600
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // ── 떠다니는 하트 파티클 ──
        repeat(20) { index ->
            FloatingHeart(index = index)
        }

        // ── 중앙 로고 영역 ──
        CenterLogo()
    }
}

// =====================
// 중앙 로고 + 텍스트
// =====================
@Composable
private fun CenterLogo() {
    // 로고 전체 등장: opacity 0→1, scale 0.8→1
    val logoAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 500),
        label = "logoAlpha"
    )
    val logoScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 500),
        label = "logoScale"
    )

    // 하트 아이콘 pulse: 1 → 1.1 → 1 (1.5초 반복)
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // "Meety" 텍스트: 0.3초 딜레이 후 페이드인 + 위로 슬라이드
    var titleVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(300); titleVisible = true }
    val titleAlpha by animateFloatAsState(
        targetValue = if (titleVisible) 1f else 0f,
        animationSpec = tween(400),
        label = "titleAlpha"
    )
    val titleOffsetY by animateFloatAsState(
        targetValue = if (titleVisible) 0f else 20f,
        animationSpec = tween(400),
        label = "titleOffsetY"
    )

    // 서브타이틀: 0.5초 딜레이 후 페이드인
    var subtitleVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(500); subtitleVisible = true }
    val subtitleAlpha by animateFloatAsState(
        targetValue = if (subtitleVisible) 1f else 0f,
        animationSpec = tween(400),
        label = "subtitleAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .alpha(logoAlpha)
            .scale(logoScale)
    ) {
        // 하트 아이콘 박스
        Box(
            modifier = Modifier
                .size(96.dp)
                .scale(pulseScale)
                .background(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Meety",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // "Meety" 타이틀
        Text(
            text = "Meety",
            fontSize = 52.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .alpha(titleAlpha)
                .offset(y = titleOffsetY.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // "새로운 만남의 시작"
        Text(
            text = "새로운 만남의 시작",
            fontSize = 20.sp,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.alpha(subtitleAlpha)
        )
    }
}

// =====================
// 떠다니는 하트 파티클
// =====================
@Composable
private fun FloatingHeart(index: Int) {
    // 각 하트마다 고정된 랜덤 값 (recomposition 시 변하지 않도록 remember)
    val config = remember(index) {
        FloatingHeartConfig(
            xPercent = Random.nextFloat(),                   // 0~1 (화면 가로 위치)
            delayMs = (Random.nextFloat() * 2000).toInt(),   // 0~2초 딜레이
            durationMs = 3000 + (Random.nextFloat() * 2000).toInt(), // 3~5초
            size = (20 + Random.nextFloat() * 20).dp         // 20~40dp
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "heart_$index")

    // Y 위치: 화면 아래(1f) → 위(-0.2f)
    val yProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = config.durationMs,
                delayMillis = config.delayMs,
                easing = EaseOut
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "y_$index"
    )

    // 투명도: 0 → 0.6 → 0
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = config.durationMs
                delayMillis = config.delayMs
                0f at 0
                0.6f at (config.durationMs * 0.3f).toInt()
                0f at config.durationMs
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha_$index"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        val screenWidth = maxWidth

        // yProgress 0→1 을 화면 아래(+100dp)→위(-screenHeight*0.2) 로 매핑
        val startY = screenHeight + 100.dp
        val endY = -(screenHeight * 0.2f)
        val currentY = startY + (endY - startY) * yProgress

        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.4f),
            modifier = Modifier
                .size(config.size)
                .offset(
                    x = screenWidth * config.xPercent,
                    y = currentY
                )
                .alpha(alpha)
        )
    }
}

private data class FloatingHeartConfig(
    val xPercent: Float,
    val delayMs: Int,
    val durationMs: Int,
    val size: Dp
)