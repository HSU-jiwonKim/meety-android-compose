package com.bugzero.meety.ui.feed

import com.bugzero.meety.ui.team.Team

/**
 * 피드 화면의 보기 모드
 */
enum class FeedViewMode {
    RECOMMEND, // AI 추천 카드 스와이프
    LIST       // 전체 팀 목록
}

/**
 * 스와이프 히스토리 한 항목
 *
 * likeId를 미리 저장해두면 undo 시 Firebase 응답을 기다리지 않고 즉시 cancelLike 가능하다.
 */
data class HistoryEntry(
    val index: Int,
    val team: Team,
    val isLike: Boolean,
    val likeId: String? = null   // 좋아요일 때만 존재, 패스는 null
)

/**
 * 피드 화면의 전체 UI 상태
 */
data class FeedUiState(
    val viewMode: FeedViewMode = FeedViewMode.RECOMMEND,
    val currentIndex: Int = 0,
    val teams: List<Team> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,     // pull-to-refresh 스피너 (전체 로딩과 구분)
    val isLoadingMore: Boolean = false,    // 추가 페이지 로딩 중
    val hasMore: Boolean = true,           // 서버에 더 있는지
    val errorMessage: String? = null,
    val userPreferences: Map<String, Int> = emptyMap(),
    val selectedTeam: Team? = null
)
