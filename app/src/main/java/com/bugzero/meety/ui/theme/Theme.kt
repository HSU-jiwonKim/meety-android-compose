package com.bugzero.meety.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Purple = Color(0xFF7C3AED)
val PurpleLight = Color(0xFFA78BFA)
val Pink = Color(0xFFF472B6)
val Background = Color(0xFFF9FAFB)
val Surface = Color(0xFFFFFFFF)
val Gray100 = Color(0xFFF3F4F6)
val Gray200 = Color(0xFFE5E7EB)
val Gray400 = Color(0xFF9CA3AF)
val Gray500 = Color(0xFF6B7280)
val Gray700 = Color(0xFF374151)
val Gray900 = Color(0xFF111827)

private val LightColorScheme = darkColorScheme(
    primary = Purple,
    secondary = Pink,
    background = Background,
    surface = Surface,
    onPrimary = Color.White,
    onBackground = Gray900,
    onSurface = Gray900,
)

@Composable
fun MeetyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
