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
        fetchRemoteTeams()
    }

    // =====================
    // Firebase에서 팀 목록 가져오기
    // =====================

    fun fetchRemoteTeams() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = repository.fetchActiveTeams()

            result.onSuccess { teams ->
                _uiState.update {
                    it.copy(
                        teams = teams,
                        isLoading = false,
                        currentIndex = 0,
                        history = emptyList()
                    )
                }
            }.onFailure { error ->
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
    // UI 조작
    // =====================

    fun setViewMode(mode: FeedViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun onCardSwiped(isLike: Boolean) {
        val state = _uiState.value
        val currentTeam = state.teams.getOrNull(state.currentIndex) ?: return

        if (isLike) {
            updateAiPreference(currentTeam)
        }

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
                val lastIdx = state.history.last()
                state.copy(
                    currentIndex = lastIdx,
                    history = state.history.dropLast(1)
                )
            } else state
        }
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
    // AI 취향 분석
    // =====================

    private fun updateAiPreference(team: Team) {
        val currentPrefs = _uiState.value.userPreferences.toMutableMap()

        team.mbtiTags.forEach { mbti ->
            currentPrefs[mbti] = (currentPrefs[mbti] ?: 0) + MBTI_SCORE_WEIGHT
        }
        team.tags.forEach { tag ->
            currentPrefs[tag] = (currentPrefs[tag] ?: 0) + TAG_SCORE_WEIGHT
        }

        _uiState.update { it.copy(userPreferences = currentPrefs) }
        applyAiRecommendation()
    }

    private fun applyAiRecommendation() {
        val prefs = _uiState.value.userPreferences
        if (prefs.isEmpty()) return

        val sortedTeams = _uiState.value.teams.sortedByDescending { team ->
            val mbtiScore = team.mbtiTags.sumOf { prefs[it] ?: 0 }
            val tagScore = team.tags.sumOf { prefs[it] ?: 0 }
            mbtiScore + tagScore
        }

        _uiState.update { it.copy(teams = sortedTeams) }
    }

    companion object {
        /** MBTI 일치 시 부여되는 선호도 점수 */
        private const val MBTI_SCORE_WEIGHT = 2
        /** 일반 태그 일치 시 부여되는 선호도 점수 */
        private const val TAG_SCORE_WEIGHT = 1
    }
}
