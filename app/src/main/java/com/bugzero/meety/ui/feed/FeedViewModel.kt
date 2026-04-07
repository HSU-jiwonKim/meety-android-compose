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
                    it.copy(
                        userPreferences = pref.tagScores + pref.mbtiScores,
                        likedTeamIds    = pref.likedTeamIds.toSet(),
                        passedTeamIds   = pref.passedTeamIds.toSet()
                    )
                }
            }

            // 변경: fetchMyTeamId() → fetchMyTeamIds() (배열 지원)
            val myTeamIds = repository.fetchMyTeamIds()
            val myTeamId = myTeamIds.firstOrNull() ?: ""
            _uiState.update { it.copy(myTeamId = myTeamId) }

            fetchRemoteTeams()   // RECOMMEND 탭: 필터링된 추천 카드
            fetchAllTeams()      // LIST 탭: 전체 팀 목록
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
    // 전체보기 탭 팀 목록 (LIST)
    // =====================

    /**
     * LIST 탭 전용 — 좋아요·패스·내 팀을 포함한 전체 팀 목록을 로딩한다.
     * pull-to-refresh 시 loadMore=false로 호출해 목록을 초기화한다.
     */
    fun fetchAllTeams(loadMore: Boolean = false) {
        val state = _uiState.value
        if (state.isLoadingAllTeams) return
        if (loadMore && !state.allTeamsHasMore) return

        _uiState.update { it.copy(isLoadingAllTeams = true) }

        viewModelScope.launch {
            repository.fetchAllActiveTeams(loadMore = loadMore)
                .onSuccess { (teams, hasMore) ->
                    _uiState.update {
                        it.copy(
                            allTeams           = if (loadMore) it.allTeams + teams else teams,
                            isLoadingAllTeams  = false,
                            allTeamsHasMore    = hasMore
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingAllTeams = false) }
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
            val updatedLiked  = if (isLike) it.likedTeamIds  + currentTeam.teamId else it.likedTeamIds
            val updatedPassed = if (!isLike) it.passedTeamIds + currentTeam.teamId else it.passedTeamIds
            it.copy(
                currentIndex  = it.currentIndex + 1,
                history       = it.history + entry,
                likedTeamIds  = updatedLiked,
                passedTeamIds = updatedPassed
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
            val updatedLiked  = if (last.isLike)  it.likedTeamIds  - last.team.teamId else it.likedTeamIds
            val updatedPassed = if (!last.isLike) it.passedTeamIds - last.team.teamId else it.passedTeamIds
            it.copy(
                currentIndex  = last.index,
                history       = it.history.dropLast(1),
                likedTeamIds  = updatedLiked,
                passedTeamIds = updatedPassed
            )
        }
    }

    // =====================
    // 좋아요 / 패스 (상세 화면 — 목록에서 진입한 팀)
    // =====================

    /**
     * 상세화면에서 좋아요.
     *
     * - 추천 카드 모드에서 열린 경우 (selectedTeam == teams[currentIndex]):
     *   카드 스와이프와 동일하게 currentIndex 진행 + HistoryEntry 저장
     *   → 같은 카드에 중복 액션 방지
     * - 목록 모드에서 열린 경우:
     *   좋아요 저장 후 상세화면만 닫는다.
     */
    fun onSelectedTeamLike() {
        val state = _uiState.value
        val team = state.selectedTeam ?: return
        val likeId = UUID.randomUUID().toString()

        viewModelScope.launch {
            repository.saveLike(team, likeId)
        }
        updatePreferenceInMemory(team, isLike = true)

        val isCurrentCard = state.teams.getOrNull(state.currentIndex)?.teamId == team.teamId
        if (isCurrentCard) {
            val entry = HistoryEntry(
                index  = state.currentIndex,
                team   = team,
                isLike = true,
                likeId = likeId
            )
            _uiState.update {
                it.copy(
                    selectedTeam = null,
                    currentIndex = it.currentIndex + 1,
                    history      = it.history + entry,
                    likedTeamIds = it.likedTeamIds + team.teamId
                )
            }
            val remaining = state.teams.size - (state.currentIndex + 1)
            if (remaining <= 3 && state.hasMore) loadMoreTeams()
        } else {
            _uiState.update {
                it.copy(
                    selectedTeam = null,
                    likedTeamIds = it.likedTeamIds + team.teamId
                )
            }
        }
    }

    /**
     * 상세화면에서 패스.
     *
     * - 추천 카드 모드에서 열린 경우 (selectedTeam == teams[currentIndex]):
     *   카드 스와이프와 동일하게 currentIndex 진행 + HistoryEntry 저장
     *   → 같은 카드에 중복 액션 방지
     * - 목록 모드에서 열린 경우:
     *   패스 저장 후 상세화면만 닫는다.
     */
    fun onSelectedTeamPass() {
        val state = _uiState.value
        val team = state.selectedTeam ?: return

        viewModelScope.launch {
            repository.savePass(team)
        }
        updatePreferenceInMemory(team, isLike = false)

        val isCurrentCard = state.teams.getOrNull(state.currentIndex)?.teamId == team.teamId
        if (isCurrentCard) {
            val entry = HistoryEntry(
                index  = state.currentIndex,
                team   = team,
                isLike = false,
                likeId = null
            )
            _uiState.update {
                it.copy(
                    selectedTeam  = null,
                    currentIndex  = it.currentIndex + 1,
                    history       = it.history + entry,
                    passedTeamIds = it.passedTeamIds + team.teamId
                )
            }
            val remaining = state.teams.size - (state.currentIndex + 1)
            if (remaining <= 3 && state.hasMore) loadMoreTeams()
        } else {
            _uiState.update {
                it.copy(
                    selectedTeam  = null,
                    passedTeamIds = it.passedTeamIds + team.teamId
                )
            }
        }
    }

    // =====================
    // UI 조작
    // =====================

    fun setViewMode(mode: FeedViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun selectTeam(teamId: String) {
        // RECOMMEND 탭 teams 또는 LIST 탭 allTeams 모두에서 검색
        val team = _uiState.value.teams.find { it.teamId == teamId }
            ?: _uiState.value.allTeams.find { it.teamId == teamId }
        _uiState.update { it.copy(selectedTeam = team, memberProfiles = emptyList()) }
        team?.let { loadMemberProfiles(it) }
    }

    fun clearSelectedTeam() {
        _uiState.update { it.copy(selectedTeam = null, memberProfiles = emptyList()) }
    }

    /** 선택된 팀의 팀원 프로필을 비동기로 로딩한다. */
    private fun loadMemberProfiles(team: com.bugzero.meety.ui.team.Team) {
        if (team.memberIds.isEmpty()) return
        _uiState.update { it.copy(isMembersLoading = true) }
        viewModelScope.launch {
            repository.fetchMemberProfiles(team.memberIds)
                .onSuccess { profiles ->
                    _uiState.update { it.copy(memberProfiles = profiles, isMembersLoading = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(isMembersLoading = false) }
                }
        }
    }

    /**
     * 상세화면 — 좋아요 취소 (LIKED 상태에서 호출)
     *
     * Firebase에서 like 문서를 삭제하고 선호도를 역산한다.
     */
    fun onCancelLikeFromDetail() {
        val team = _uiState.value.selectedTeam ?: return
        viewModelScope.launch {
            repository.cancelLikeByTeamId(team.teamId)
            repository.reversePreferenceScores(team, wasLike = true)
        }
        reversePreferenceInMemory(team, wasLike = true)
        _uiState.update {
            it.copy(
                selectedTeam   = null,
                memberProfiles = emptyList(),
                likedTeamIds   = it.likedTeamIds - team.teamId
            )
        }
    }

    /**
     * 상세화면 — 패스했던 팀에 좋아요 전환 (PASSED 상태에서 호출)
     *
     * 패스 기록을 역산하고 신규 좋아요를 저장한다.
     */
    fun onSendLikeFromPassed() {
        val team = _uiState.value.selectedTeam ?: return
        val likeId = UUID.randomUUID().toString()
        viewModelScope.launch {
            repository.convertPassToLike(team, likeId)
        }
        // 인메모리 상태: pass 역산 → like 반영
        reversePreferenceInMemory(team, wasLike = false)
        updatePreferenceInMemory(team, isLike = true)
        _uiState.update {
            it.copy(
                selectedTeam   = null,
                memberProfiles = emptyList(),
                passedTeamIds  = it.passedTeamIds - team.teamId,
                likedTeamIds   = it.likedTeamIds  + team.teamId
            )
        }
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