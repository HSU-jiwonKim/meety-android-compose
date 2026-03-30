package com.bugzero.meety.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugzero.meety.ui.team.Team
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FeedViewModel(
    private val repository: FeedRepository = FeedRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        // Firebase에서 선호도를 먼저 불러온 뒤 팀 목록을 로딩한다.
        // 선호도가 있으면 팀 목록이 올 때 즉시 정렬에 반영된다.
        loadPreferenceThenFetch()
    }

    // =====================
    // 초기화
    // =====================

    /**
     * Firebase에서 사용자 선호도를 불러온 뒤 팀 목록을 가져온다.
     * 앱을 껐다 켜도 이전에 쌓인 AI 취향 데이터가 유지된다.
     */
    private fun loadPreferenceThenFetch() {
        viewModelScope.launch {
            val prefResult = repository.loadUserPreference()
            prefResult.onSuccess { pref ->
                _uiState.update {
                    it.copy(userPreferences = pref.tagScores + pref.mbtiScores)
                }
            }
            // 선호도 로딩 성공/실패 무관하게 팀 목록은 항상 가져온다
            fetchRemoteTeams()
        }
    }

    // =====================
    // 팀 목록 불러오기
    // =====================

    fun fetchRemoteTeams() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            repository.fetchActiveTeams()
                .onSuccess { teams ->
                    val sorted = sortByPreference(teams)
                    _uiState.update {
                        it.copy(
                            teams = sorted,
                            isLoading = false,
                            currentIndex = 0,
                            history = emptyList()
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "팀 목록을 불러오지 못했습니다."
                        )
                    }
                }
        }
    }

    // =====================
    // 스와이프 (좋아요 / 패스)
    // =====================

    /**
     * 카드를 스와이프한다.
     *
     * 1) Firebase에 좋아요/패스 저장 (백그라운드)
     * 2) 인메모리 선호도 즉시 업데이트 → 다음 카드 순서에 반영
     * 3) 다음 카드로 이동
     *
     * Firebase 저장 실패 시 UI는 멈추지 않는다 (silent fail).
     * 저장 실패는 에러 로그로만 남긴다.
     */
    fun onCardSwiped(isLike: Boolean) {
        val state = _uiState.value
        val currentTeam = state.teams.getOrNull(state.currentIndex) ?: return

        // Firebase 저장 (비동기 — UI 블로킹 없음)
        viewModelScope.launch {
            if (isLike) {
                repository.saveLike(currentTeam)
            } else {
                repository.savePass(currentTeam)
            }
        }

        // 인메모리 선호도 업데이트 (즉시 반영)
        if (isLike) updatePreferenceInMemory(currentTeam, isLike = true)
        else updatePreferenceInMemory(currentTeam, isLike = false)

        // 다음 카드로 이동
        _uiState.update {
            it.copy(
                currentIndex = it.currentIndex + 1,
                history = it.history + it.currentIndex
            )
        }
    }

    fun undoSwipe() {
        _uiState.update { state ->
            if (state.history.isNotEmpty()) {
                state.copy(
                    currentIndex = state.history.last(),
                    history = state.history.dropLast(1)
                )
            } else state
        }
    }

    // =====================
    // UI 조작
    // =====================

    fun setViewMode(mode: FeedViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun selectTeam(teamId: String) {
        val team = _uiState.value.teams.find { it.teamId == teamId }
        _uiState.update { it.copy(selectedTeam = team) }
    }

    fun clearSelectedTeam() {
        _uiState.update { it.copy(selectedTeam = null) }
    }

    fun resetFeed() {
        _uiState.update { it.copy(currentIndex = 0, history = emptyList()) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // =====================
    // AI 취향 분석 (인메모리)
    // =====================

    /**
     * 스와이프 결과를 인메모리 선호도 맵에 즉시 반영한다.
     * Firebase 저장은 별도로 이루어지며, 여기선 UI 반응성만 담당한다.
     */
    private fun updatePreferenceInMemory(team: Team, isLike: Boolean) {
        val tagWeight  = if (isLike) TAG_LIKE_WEIGHT  else TAG_PASS_WEIGHT
        val mbtiWeight = if (isLike) MBTI_LIKE_WEIGHT else MBTI_PASS_WEIGHT

        val updated = _uiState.value.userPreferences.toMutableMap()

        team.tags.forEach { tag ->
            updated[tag] = (updated[tag] ?: 0) + tagWeight
        }
        team.mbtiTags.forEach { mbti ->
            updated[mbti] = (updated[mbti] ?: 0) + mbtiWeight
        }

        _uiState.update { it.copy(userPreferences = updated) }

        // 팀 목록 재정렬
        applyPreferenceSort()
    }

    private fun applyPreferenceSort() {
        val prefs = _uiState.value.userPreferences
        if (prefs.isEmpty()) return

        val sorted = sortByPreference(_uiState.value.teams)
        _uiState.update { it.copy(teams = sorted) }
    }

    /** 선호도 점수 기준으로 팀 목록을 내림차순 정렬한다. */
    private fun sortByPreference(teams: List<Team>): List<Team> {
        val prefs = _uiState.value.userPreferences
        if (prefs.isEmpty()) return teams

        return teams.sortedByDescending { team ->
            val tagScore  = team.tags.sumOf { prefs[it] ?: 0 }
            val mbtiScore = team.mbtiTags.sumOf { prefs[it] ?: 0 }
            tagScore + mbtiScore
        }
    }

    companion object {
        private const val TAG_LIKE_WEIGHT  = 1
        private const val TAG_PASS_WEIGHT  = -1
        private const val MBTI_LIKE_WEIGHT = 2
        private const val MBTI_PASS_WEIGHT = -2
    }
}
