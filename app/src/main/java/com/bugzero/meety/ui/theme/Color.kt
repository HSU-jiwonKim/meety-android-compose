package com.bugzero.meety.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Meety 2.0 디자인 토큰
 *
 * meety-redesign-mockup.html 의 :root CSS 변수를 그대로 옮겨온 색상 시스템.
 * 시그니처 보라(#7B5CFF) → 핑크(#FF5C8A) 그라데이션을 중심으로 한다.
 */

// ── Brand ──
val Brand1 = Color(0xFF7B5CFF)   // violet
val BrandMid = Color(0xFFA24BFF) // violet-magenta (그라데이션 중간)
val Brand2 = Color(0xFFFF5C8A)   // pink

// ── Ink (텍스트) ──
val Ink = Color(0xFF17161D)
val Ink2 = Color(0xFF56535F)
val Ink3 = Color(0xFF9B98A6)
val Ink4 = Color(0xFFC4C2CD)

// ── Surface ──
val MeetyBg = Color(0xFFF4F4F8)
val MeetySurface = Color(0xFFFFFFFF)
val MeetySurface2 = Color(0xFFFAFAFD)
val Line = Color(0xFFECEAF1)
val Line2 = Color(0xFFF1EFF5)

// ── Soft tints ──
val VioletSoft = Color(0xFFF2EEFF)
val PinkSoft = Color(0xFFFFECF3)
val MintSoft = Color(0xFFE5F8F3)
val BlueSoft = Color(0xFFE9F1FF)
val GradSoftStart = Color(0xFFEFE9FF)
val GradSoftEnd = Color(0xFFFFE8F1)

// ── Status ──
val LikePink = Color(0xFFFF4D7D)
val PassGray = Color(0xFF8E8B98)
val OkGreen = Color(0xFF19C37D)

// ── 텍스트/칩 그라데이션 보조 색상 ──
val VioletText = Color(0xFF6D49E0)
val PinkText = Color(0xFFE0457A)

// ── 시그니처 그라데이션 (135deg violet → magenta → pink) ──
val MeetyGradient = Brush.linearGradient(
    0f to Brand1,
    0.45f to BrandMid,
    1f to Brand2
)

/** 부드러운 배경용 그라데이션 (연보라 → 연핑크) */
val MeetyGradientSoft = Brush.linearGradient(
    listOf(GradSoftStart, GradSoftEnd)
)

/** 세로 방향 시그니처 그라데이션 (카드 배경 등) */
val MeetyGradientVertical = Brush.verticalGradient(
    listOf(Brand1, Brand2)
)

// ── 카드 배경 팔레트 (멤버 아바타 / 카드 더미용) ──
val CardGradientViolet = listOf(Color(0xFF9D7BFF), Color(0xFF5B3FE0))
val CardGradientMint = listOf(Color(0xFF7DE0CF), Color(0xFF26A69A))
val CardGradientPink = listOf(Color(0xFFFF9DC0), Color(0xFFEC407A))
val CardGradientBlue = listOf(Color(0xFF90CAF9), Color(0xFF1E88E5))

// ── 기존 머티리얼 기본 토큰 (호환 유지) ──
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
