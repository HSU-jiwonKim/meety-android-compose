package com.bugzero.meety.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugzero.meety.data.repository.FeedRepository
import com.bugzero.meety.ui.team.Team
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class FeedViewModel(
    private val repository: FeedRepository = FeedRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        loadPreferenceThenFetch()
    }

    // =====================
    // 초기화
    // =====================

    private fun loadPreferenceThenFetch() {
        viewModelScope.launch {
            val prefResult = repository.loadUserPreference()
            prefResult.onSuccess { pref ->
                _uiState.update {
                    it.copy(userPreferences = pref.tagScores + pref.mbtiScores)
                }
            }
            fetchRemoteTeams()
        }
    }

    // =====================
    // 팀 목록 불러오기
    // =====================

    /**
     * @param isRefresh true이면 pull-to-refresh — 전체 로딩 스피너 없이 새로고침
     */
    fun fetchRemoteTeams(isRefresh: Boolean = false) {
        if (isRefresh) {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
        } else {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        }

        viewModelScope.launch {
            repository.fetchActiveTeams(loadMore = false)
                .onSuccess { (teams, hasMore) ->
                    val sorted = sortByPreference(teams)
                    _uiState.update {
                        it.copy(
                            teams = sorted,
                            isLoading = false,
                            isRefreshing = false,
                            currentIndex = 0,
                            history = emptyList(),
                            hasMore = hasMore
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = error.message ?: "팀 목록을 불러오지 못했습니다."
                        )
                    }
                }
        }
    }

    /**
     * 남은 카드가 3장 이하일 때 자동 호출 — 다음 페이지를 미리 불러온다.
     */
    fun loadMoreTeams() {
        val state = _uiState.value
        if (!state.hasMore || state.isLoadingMore) return

        _uiState.update { it.copy(isLoadingMore = true) }

        viewModelScope.launch {
            repository.fetchActiveTeams(loadMore = true)
                .onSuccess { (newTeams, hasMore) ->
                    _uiState.update { state ->
                        // 아직 보지 않은 카드 뒤에 새 팀 추가
                        val seen   = state.teams.take(state.currentIndex)
                        val unseen = state.teams.drop(state.currentIndex)
                        val merged = seen + sortByPreference(unseen + newTeams)
                        state.copy(
                            teams = merged,
                            isLoadingMore = false,
                            hasMore = hasMore
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
        }
    }

    // =====================
    // 스와이프 (카드 모드)
    // =====================

    /**
     * 카드를 스와이프한다.
     *
     * likeId를 미리 생성해서 HistoryEntry에 저장 → undo 시 즉시 cancelLike 가능.
     * 남은 카드가 3장 이하가 되면 다음 페이지를 자동으로 불러온다.
     */
    fun onCardSwiped(isLike: Boolean) {
        val state = _uiState.value
        val currentTeam = state.teams.getOrNull(state.currentIndex) ?: return

        val likeId = if (isLike) UUID.randomUUID().toString() else null

        // Firebase 저장 (비동기)
        viewModelScope.launch {
            if (isLike && likeId != null) {
                repository.saveLike(currentTeam, likeId)
            } else {
                repository.savePass(currentTeam)
            }
        }

        updatePreferenceInMemory(currentTeam, isLike)

        val entry = HistoryEntry(
            index  = state.currentIndex,
            team   = currentTeam,
            isLike = isLike,
            likeId = likeId
        )

        _uiState.update {
            it.copy(
                currentIndex = it.currentIndex + 1,
                history      = it.history + entry
            )
        }

        // 남은 카드 3장 이하 → 다음 페이지 미리 로딩
        val remaining = state.teams.size - (state.currentIndex + 1)
        if (remaining <= 3 && state.hasMore) {
            loadMoreTeams()
        }
    }

    /**
     * undo — Firebase에서도 좋아요 취소 + 선호도 역산
     */
    fun undoSwipe() {
        val state = _uiState.value
        val last = state.history.lastOrNull() ?: return

        // Firebase 역산 (비동기)
        viewModelScope.launch {
            if (last.isLike && last.likeId != null) {
                repository.cancelLike(last.likeId)
            }
            repository.reversePreferenceScores(last.team, wasLike = last.isLike)
        }

        // 인메모리 역산
        reversePreferenceInMemory(last.team, last.isLike)

        _uiState.update {
            it.copy(
                currentIndex = last.index,
                history      = it.history.dropLast(1)
            )
        }
    }

    // =====================
    // 좋아요 / 패스 (상세 화면 — 목록에서 진입한 팀)
    // =====================

    /**
     * 상세화면에서 좋아요.
     * selectedTeam에 작용하며, 카드 스택의 currentIndex는 건드리지 않는다.
     */
    fun onSelectedTeamLike() {
        val team = _uiState.value.selectedTeam ?: return
        val likeId = UUID.randomUUID().toString()

        viewModelScope.launch {
            repository.saveLike(team, likeId)
        }
        updatePreferenceInMemory(team, isLike = true)
        _uiState.update { it.copy(selectedTeam = null) }
    }

    /**
     * 상세화면에서 패스.
     */
    fun onSelectedTeamPass() {
        val team = _uiState.value.selectedTeam ?: return

        viewModelScope.launch {
            repository.savePass(team)
        }
        updatePreferenceInMemory(team, isLike = false)
        _uiState.update { it.copy(selectedTeam = null) }
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

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // =====================
    // AI 취향 분석 (인메모리)
    // =====================

    private fun updatePreferenceInMemory(team: Team, isLike: Boolean) {
        val tagWeight  = if (isLike) TAG_LIKE_WEIGHT  else TAG_PASS_WEIGHT
        val mbtiWeight = if (isLike) MBTI_LIKE_WEIGHT else MBTI_PASS_WEIGHT

        val updated = _uiState.value.userPreferences.toMutableMap()
        team.tags.forEach     { tag  -> updated[tag]  = (updated[tag]  ?: 0) + tagWeight }
        team.mbtiTags.forEach { mbti -> updated[mbti] = (updated[mbti] ?: 0) + mbtiWeight }

        _uiState.update { it.copy(userPreferences = updated) }
        applyPreferenceSort()
    }

    private fun reversePreferenceInMemory(team: Team, wasLike: Boolean) {
        val tagWeight  = if (wasLike) TAG_PASS_WEIGHT  else TAG_LIKE_WEIGHT
        val mbtiWeight = if (wasLike) MBTI_PASS_WEIGHT else MBTI_LIKE_WEIGHT

        val updated = _uiState.value.userPreferences.toMutableMap()
        team.tags.forEach     { tag  -> updated[tag]  = (updated[tag]  ?: 0) + tagWeight }
        team.mbtiTags.forEach { mbti -> updated[mbti] = (updated[mbti] ?: 0) + mbtiWeight }

        _uiState.update { it.copy(userPreferences = updated) }
    }

    /**
     * 아직 보지 않은 카드만 정렬한다.
     * 이미 스와이프한 카드의 순서는 변하지 않는다.
     */
    private fun applyPreferenceSort() {
        val state = _uiState.value
        val prefs = state.userPreferences
        if (prefs.isEmpty()) return

        val seen   = state.teams.take(state.currentIndex)
        val unseen = sortByPreference(state.teams.drop(state.currentIndex))

        _uiState.update { it.copy(teams = seen + unseen) }
    }

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
