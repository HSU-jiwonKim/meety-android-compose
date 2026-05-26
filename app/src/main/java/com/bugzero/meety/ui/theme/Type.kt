package com.bugzero.meety.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bugzero.meety.R

/**
 * Pretendard 폰트 패밀리
 *
 * res/font/ 에 번들된 정적 weight(Regular~ExtraBold)를 묶는다.
 * 목업이 사용하는 weight: 400/500/600/700/800
 */
val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),    // 400
    Font(R.font.pretendard_medium, FontWeight.Medium),     // 500
    Font(R.font.pretendard_semibold, FontWeight.SemiBold), // 600
    Font(R.font.pretendard_bold, FontWeight.Bold),         // 700
    Font(R.font.pretendard_extrabold, FontWeight.ExtraBold)// 800
)

/**
 * Pretendard 기반 타이포그래피.
 * 모든 머티리얼 텍스트 스타일에 Pretendard 를 적용하고,
 * 목업의 -0.02em 자간 느낌을 위해 큰 제목에 음수 letterSpacing 을 준다.
 */
val Typography = Typography(
    displayLarge = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.ExtraBold, fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-1).sp),
    displayMedium = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.ExtraBold, fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = (-0.5).sp),
    displaySmall = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = (-0.6).sp),
    headlineMedium = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.4).sp),
    titleMedium = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = (-0.2).sp),
    titleSmall = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.1.sp),
    bodyMedium = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodySmall = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 16.sp)
)
