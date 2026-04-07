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
 * 전체보기 탭에서 각 팀의 액션 상태를 나타내는 enum
 *
 * 우선순위: MY_TEAM > LIKED > PASSED > NONE
 */
enum class TeamActionStatus {
    NONE,     // 아무 액션 없음 (추천 대상)
    LIKED,    // 좋아요를 보낸 팀
    PASSED,   // 패스한 팀
    MY_TEAM   // 내가 소속된 팀
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
    // ── 추천 카드 모드 (RECOMMEND 탭) ──
    val viewMode: FeedViewMode = FeedViewMode.RECOMMEND,
    val currentIndex: Int = 0,
    val teams: List<Team> = emptyList(),          // 필터링된 추천 목록
    val history: List<HistoryEntry> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,            // pull-to-refresh (RECOMMEND 탭)
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val errorMessage: String? = null,
    val userPreferences: Map<String, Int> = emptyMap(),
    val selectedTeam: Team? = null,

    // ── 전체보기 모드 (LIST 탭) ──
    val allTeams: List<Team> = emptyList(),        // 좋아요/패스/내 팀 포함 전체 목록
    val isLoadingAllTeams: Boolean = false,
    val allTeamsHasMore: Boolean = true,

    // ── 상태 배지 계산용 ──
    val likedTeamIds: Set<String> = emptySet(),    // 좋아요 보낸 팀 ID 집합
    val passedTeamIds: Set<String> = emptySet(),   // 패스한 팀 ID 집합
    val myTeamId: String = "",                     // 내가 소속된 팀 ID

    // ── 팀 상세화면 — 팀원 프로필 ──
    val memberProfiles: List<MemberProfile> = emptyList(),
    val isMembersLoading: Boolean = false
)
