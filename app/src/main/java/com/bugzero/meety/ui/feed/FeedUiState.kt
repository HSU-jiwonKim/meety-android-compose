package com.bugzero.meety.ui.feed

import com.bugzero.meety.ui.team.Team

/**
 * 피드 화면의 보기 모드
 *
 * 문자열 대신 enum을 쓰면 오타로 인한 버그를 컴파일 단계에서 잡을 수 있다.
 */
enum class FeedViewMode {
    RECOMMEND, // AI 추천 카드 스와이프
    LIST       // 전체 팀 목록
}

/**
 * 피드 화면의 전체 UI 상태
 *
 * ViewModel이 이 상태를 업데이트하면 → Screen이 자동으로 다시 그려진다.
 */
data class FeedUiState(
    val viewMode: FeedViewMode = FeedViewMode.RECOMMEND,
    val currentIndex: Int = 0,
    val teams: List<Team> = emptyList(),
    val history: List<Int> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val userPreferences: Map<String, Int> = emptyMap(),
    val selectedTeam: Team? = null
)
