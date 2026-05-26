package com.bugzero.meety.ui.feed

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Feed 패키지 전역에서 사용하는 상수 모음
 *
 * 색상이나 수치를 한 곳에서 관리하면,
 * 나중에 디자인이 바뀌어도 여기만 수정하면 된다.
 */
object FeedConstants {

    // ── 브랜드 그라데이션 (보라 → 마젠타 → 핑크) ──
    val GradientPurplePink = Brush.linearGradient(
        0f to Color(0xFF7B5CFF),
        0.45f to Color(0xFFA24BFF),
        1f to Color(0xFFFF5C8A)
    )
    val GradientStart = Color(0xFF7B5CFF)
    val GradientEnd = Color(0xFFFF5C8A)

    // ── 카드 배경 색상 팔레트 (목업 g1/g2/g3 + 파랑) ──
    val CardColorPalette = listOf(
        listOf(Color(0xFF9D7BFF), Color(0xFF5B3FE0)), // 보라
        listOf(Color(0xFF7DE0CF), Color(0xFF26A69A)), // 민트
        listOf(Color(0xFFFF9DC0), Color(0xFFEC407A)), // 핑크
        listOf(Color(0xFF90CAF9), Color(0xFF1E88E5))  // 파랑
    )

    // ── 공통 색상 ──
    val BackgroundGray = Color(0xFFF4F4F8)
    val IconGray = Color(0xFF56535F)
    val AccentPink = Color(0xFFFF5C8A)
    val PassRed = Color(0xFFFF4D7D)
    val ErrorRed = Color(0xFFEF4444)
    val LightPurpleBg = Color(0xFFF2EEFF)
    val PurpleBorder = Color(0xFFE0D7FF)
    val AiCardBg = Color(0xFFF2EEFF)

    // ── 스와이프 관련 수치 ──
    /** 이 값(px) 이상 드래그하면 Like/Pass로 처리 */
    const val SWIPE_THRESHOLD = 400f
    /** 카드 회전 정도를 조절하는 값 (클수록 덜 회전) */
    const val ROTATION_DIVISOR = 60f

    // ── 선호도 가중치 ──
    // FeedViewModel · FeedRepository 양쪽이 여기서만 참조해
    // 수치를 바꿔도 두 레이어가 항상 일치한다.
    const val TAG_LIKE_WEIGHT  = 1
    const val TAG_PASS_WEIGHT  = -1
    /** MBTI는 태그보다 2배 가중 */
    const val MBTI_LIKE_WEIGHT = 2
    const val MBTI_PASS_WEIGHT = -2

    // ── 자동 새로고침 ──
    /** 추천 피드를 백그라운드에서 자동 갱신하는 주기 (밀리초) — 기본 5분 */
    const val AUTO_REFRESH_INTERVAL_MS = 5 * 60 * 1_000L
}
