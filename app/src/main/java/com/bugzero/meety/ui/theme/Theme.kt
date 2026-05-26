package com.bugzero.meety.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/**
 * 화면 전반에서 직접 참조하는 단축 색상.
 * Meety 2.0 토큰(Color.kt)에 맞춰 값만 재조정해, 개별 화면을 건드리지 않아도
 * 전체 톤이 새 팔레트로 따라오도록 한다.
 */
val Purple = Brand1            // #7B5CFF
val PurpleLight = Color(0xFFA78BFA)
val Pink = Brand2              // #FF5C8A
val Background = MeetyBg       // #F4F4F8
val Surface = MeetySurface     // #FFFFFF
val Gray100 = Color(0xFFF2F0F7)
val Gray200 = Line             // #ECEAF1
val Gray400 = Ink4             // #C4C2CD
val Gray500 = Ink3             // #9B98A6
val Gray700 = Ink2             // #56535F
val Gray900 = Ink              // #17161D

private val MeetyColorScheme = lightColorScheme(
    primary = Brand1,
    onPrimary = Color.White,
    primaryContainer = VioletSoft,
    onPrimaryContainer = VioletText,
    secondary = Brand2,
    onSecondary = Color.White,
    secondaryContainer = PinkSoft,
    onSecondaryContainer = PinkText,
    tertiary = OkGreen,
    background = MeetyBg,
    onBackground = Ink,
    surface = MeetySurface,
    onSurface = Ink,
    surfaceVariant = MeetySurface2,
    onSurfaceVariant = Ink2,
    outline = Line,
    outlineVariant = Line2,
    error = Color(0xFFEF4444),
)

@Composable
fun MeetyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MeetyColorScheme,
        typography = Typography
    ) {
        // 모든 Text 의 기본 폰트를 Pretendard 로 지정
        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = Pretendard),
            content = content
        )
    }
}
