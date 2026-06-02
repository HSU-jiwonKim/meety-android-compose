package com.bugzero.meety.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugzero.meety.data.repository.FeedRepository
import com.bugzero.meety.data.repository.MeetingPlaceRepository
import com.bugzero.meety.ui.team.Team
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class FeedViewModel(
    private val repository: FeedRepository = FeedRepository(),
    private val meetingRepo: MeetingPlaceRepository = MeetingPlaceRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUserProfile()
        loadPreferenceThenFetch()
        observeResetSignal()
        observeMyTeamIds()
        startPeriodicRefresh()
    }

    // =====================
    // 내 팀 목록 실시간 감지
    // =====================

    /**
     * users/{userId}의 teamIds를 실시간으로 감시한다.
     * 좋아요 승인으로 팀 합류 시 즉시 myTeamIds가 갱신되어
     * 전체 목록에서 "내 팀" 배지가 바로 반영된다.
     */
    private fun observeMyTeamIds() {
        viewModelScope.launch {
            repository.observeMyTeamIds().collect { teamIds ->
                _uiState.update { it.copy(myTeamIds = teamIds.toSet()) }
            }
        }
    }

    // =====================
    // 어드민 리셋 실시간 감지
    // =====================

    /**
     * resetSignals/{userId} 문서를 실시간으로 감시한다.
     * 어드민이 초기화 버튼을 누르면 해당 문서에 타임스탬프가 기록되고,
     * 이 함수가 감지해 피드 전체를 초기 상태로 리로드한다.
     */
    private fun observeResetSignal() {
        viewModelScope.launch {
            repository.observeResetSignal().collect {
                // 좋아요·패스·선호도·스와이프 히스토리 전부 초기화
                _uiState.update {
                    it.copy(
                        userPreferences = emptyMap(),
                        userTagScores   = emptyMap(),
                        likedTeamIds    = emptySet(),
                        passedTeamIds   = emptySet(),
                        currentIndex    = 0,
                        history         = emptyList()
                    )
                }
                // 내 팀 ID 갱신 (더미팀에서 제거됐을 수 있음)
                val myTeamIds = repository.fetchMyTeamIds()
                _uiState.update { it.copy(myTeamIds = myTeamIds.toSet()) }
                // 팀 목록 재로드
                fetchRemoteTeams()
                fetchAllTeams()
            }
        }
    }

    // =====================
    // 자동 새로고침 (백그라운드)
    // =====================

    /**
     * AUTO_REFRESH_INTERVAL_MS 주기로 새로 생성된 팀을 스와이프 큐 끝에 추가한다.
     *
     * - 이미 큐에 있거나 좋아요·패스·내 팀인 경우 제외
     * - 선호도 기준으로 정렬 후 아직 안 본 카드 뒤에 삽입
     */
    private fun startPeriodicRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(FeedConstants.AUTO_REFRESH_INTERVAL_MS)
                refreshNewTeams()
            }
        }
    }

    private fun refreshNewTeams() {
        viewModelScope.launch {
            val state = _uiState.value
            val excludeIds = state.likedTeamIds +
                    state.passedTeamIds +
                    state.myTeamIds +
                    state.teams.map { it.teamId }.toSet()

            repository.fetchNewTeams(excludeIds)
                .onSuccess { newTeams ->
                    if (newTeams.isEmpty()) return@onSuccess
                    _uiState.update { current ->
                        val sorted = sortByPreference(newTeams, _uiState.value.userPreferences)
                        val seen   = current.teams.take(current.currentIndex)
                        val unseen = current.teams.drop(current.currentIndex)
                        current.copy(teams = seen + unseen + sorted)
                    }
                }
        }
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
                        userTagScores   = pref.tagScores,
                        likedTeamIds    = pref.likedTeamIds.toSet(),
                        passedTeamIds   = pref.passedTeamIds.toSet()
                    )
                }
            }

            val myTeamIds = repository.fetchMyTeamIds()
            _uiState.update { it.copy(myTeamIds = myTeamIds.toSet()) }

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
                    val sorted = sortByPreference(teams, _uiState.value.userPreferences)
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
                    prefetchCardData(sorted, fromIndex = 0)
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
                        val merged = seen + sortByPreference(unseen + newTeams, _uiState.value.userPreferences )
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

        // Firebase 저장은 비동기로 처리
        viewModelScope.launch {
            if (isLike && likeId != null) {
                repository.saveLike(currentTeam, likeId)
            } else {
                repository.savePass(currentTeam)
            }
        }

        val entry = HistoryEntry(
            index = state.currentIndex,
            team = currentTeam,
            isLike = isLike,
            likeId = likeId
        )

        _uiState.update { current ->
            val tagWeight = if (isLike) {
                FeedConstants.TAG_LIKE_WEIGHT
            } else {
                FeedConstants.TAG_PASS_WEIGHT
            }

            val mbtiWeight = if (isLike) {
                FeedConstants.MBTI_LIKE_WEIGHT
            } else {
                FeedConstants.MBTI_PASS_WEIGHT
            }

            val updatedPreferences = current.userPreferences.toMutableMap()
            // userTagScores: 태그 점수만 별도 추적 (MBTI 제외) — Top N 매칭 근거 카드용
            val updatedTagScores = current.userTagScores.toMutableMap()

            currentTeam.tags.forEach { tag ->
                updatedPreferences[tag] = (updatedPreferences[tag] ?: 0) + tagWeight
                updatedTagScores[tag]   = (updatedTagScores[tag]   ?: 0) + tagWeight
            }

            currentTeam.mbtiTags.forEach { mbti ->
                updatedPreferences[mbti] = (updatedPreferences[mbti] ?: 0) + mbtiWeight
                // mbti는 userTagScores에 포함하지 않는다 (Top N 태그 전용)
            }

            val newIndex = current.currentIndex + 1

            val updatedLikedTeamIds = if (isLike) {
                current.likedTeamIds + currentTeam.teamId
            } else {
                current.likedTeamIds
            }

            val updatedPassedTeamIds = if (!isLike) {
                current.passedTeamIds + currentTeam.teamId
            } else {
                current.passedTeamIds
            }

            val seenTeams = current.teams.take(newIndex)
            val unseenTeams = current.teams.drop(newIndex)

            val sortedUnseenTeams = sortByPreference(
                teams = unseenTeams,
                prefs = updatedPreferences
            )

            current.copy(
                teams = seenTeams + sortedUnseenTeams,
                currentIndex = newIndex,
                history = current.history + entry,
                likedTeamIds = updatedLikedTeamIds,
                passedTeamIds = updatedPassedTeamIds,
                userPreferences = updatedPreferences,
                userTagScores   = updatedTagScores
            )
        }

        val latestState = _uiState.value
        val remaining = latestState.teams.size - latestState.currentIndex

        if (remaining <= 3 && latestState.hasMore) {
            loadMoreTeams()
        }

        // 다음 카드 데이터 사전 fetch (거리·프로필·점수)
        prefetchCardData(latestState.teams, latestState.currentIndex)
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
            val newState = _uiState.value
            val remaining = newState.teams.size - newState.currentIndex
            if (remaining <= 3 && newState.hasMore) loadMoreTeams()
            // onCardSwiped()와 동일하게 다음 카드 데이터 사전 fetch
            prefetchCardData(newState.teams, newState.currentIndex)
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
            val newState = _uiState.value
            val remaining = newState.teams.size - newState.currentIndex
            if (remaining <= 3 && newState.hasMore) loadMoreTeams()
            // onCardSwiped()와 동일하게 다음 카드 데이터 사전 fetch
            prefetchCardData(newState.teams, newState.currentIndex)
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
     * 취소한 팀은 현재 스와이프 위치에 다시 삽입되어 재확인 가능하다.
     */
    fun onCancelLikeFromDetail() {
        val team = _uiState.value.selectedTeam ?: return
        viewModelScope.launch {
            repository.cancelLikeByTeamId(team.teamId)
            repository.reversePreferenceScores(team, wasLike = true)
        }
        reversePreferenceInMemory(team, wasLike = true)
        _uiState.update {
            // 취소된 팀을 현재 인덱스 위치에 다시 삽입 → 다음에 바로 다시 볼 수 있음
            val newTeams = it.teams.toMutableList().apply { add(it.currentIndex, team) }
            // 히스토리에서 이 팀의 좋아요 항목 제거.
            // 제거하지 않으면 이후 undo가 이미 취소된 like를 다시 cancelLike + 역산 이중 호출한다.
            val cleanHistory = it.history.filterNot { e ->
                e.team.teamId == team.teamId && e.isLike
            }
            it.copy(
                teams          = newTeams,
                selectedTeam   = null,
                memberProfiles = emptyList(),
                likedTeamIds   = it.likedTeamIds - team.teamId,
                history        = cleanHistory
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
            // 히스토리에서 이 팀의 패스 항목 제거.
            // 남겨두면 undo 시 isLike=false 항목을 보고 reversePreferenceScores(wasLike=false)를
            // 호출 → 이미 전환된 좋아요 상태에서 패스 역산을 다시 적용(가중치 이중 추가)하고,
            // likedTeamIds에서도 제거되지 않아 좋아요가 남는다.
            // "패스→좋아요 전환"은 상세화면에서 다시 취소 가능하므로 undo 스택에서는 제거한다.
            val cleanHistory = it.history.filterNot { e ->
                e.team.teamId == team.teamId && !e.isLike
            }
            it.copy(
                selectedTeam   = null,
                memberProfiles = emptyList(),
                passedTeamIds  = it.passedTeamIds - team.teamId,
                likedTeamIds   = it.likedTeamIds  + team.teamId,
                history        = cleanHistory
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
        val tagWeight  = if (isLike) FeedConstants.TAG_LIKE_WEIGHT  else FeedConstants.TAG_PASS_WEIGHT
        val mbtiWeight = if (isLike) FeedConstants.MBTI_LIKE_WEIGHT else FeedConstants.MBTI_PASS_WEIGHT

        val updated     = _uiState.value.userPreferences.toMutableMap()
        val updatedTags = _uiState.value.userTagScores.toMutableMap()

        team.tags.forEach { tag ->
            updated[tag]     = (updated[tag]     ?: 0) + tagWeight
            updatedTags[tag] = (updatedTags[tag] ?: 0) + tagWeight
        }
        team.mbtiTags.forEach { mbti -> updated[mbti] = (updated[mbti] ?: 0) + mbtiWeight }

        _uiState.update { it.copy(userPreferences = updated, userTagScores = updatedTags) }
        // applyPreferenceSort()
    }

    private fun reversePreferenceInMemory(team: Team, wasLike: Boolean) {
        val tagWeight  = if (wasLike) FeedConstants.TAG_PASS_WEIGHT  else FeedConstants.TAG_LIKE_WEIGHT
        val mbtiWeight = if (wasLike) FeedConstants.MBTI_PASS_WEIGHT else FeedConstants.MBTI_LIKE_WEIGHT

        val updated     = _uiState.value.userPreferences.toMutableMap()
        val updatedTags = _uiState.value.userTagScores.toMutableMap()

        team.tags.forEach { tag ->
            updated[tag]     = (updated[tag]     ?: 0) + tagWeight
            updatedTags[tag] = (updatedTags[tag] ?: 0) + tagWeight
        }
        team.mbtiTags.forEach { mbti -> updated[mbti] = (updated[mbti] ?: 0) + mbtiWeight }

        _uiState.update { it.copy(userPreferences = updated, userTagScores = updatedTags) }
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
        val unseen = sortByPreference(state.teams.drop(state.currentIndex), _uiState.value.userPreferences)

        _uiState.update { it.copy(teams = seen + unseen) }
    }

    private fun sortByPreference(
        teams: List<Team>,
        prefs: Map<String, Int>
    ): List<Team> {
        if (prefs.isEmpty()) return teams

        return teams.sortedByDescending { team ->
            val tagScore = team.tags.sumOf { prefs[it] ?: 0 }
            val mbtiScore = team.mbtiTags.sumOf { prefs[it] ?: 0 }
            tagScore + mbtiScore
        }
    }

    // =====================
    // 유저 프로필 로드
    // =====================

    /**
     * 앱 시작 시 1회 호출 — 현재 로그인 유저의 프로필을 로드해 UiState에 저장한다.
     * 이후 prefetchCardData()에서 매칭 근거 계산의 기준점으로 사용된다.
     */
    private fun loadCurrentUserProfile() {
        viewModelScope.launch {
            repository.fetchCurrentUserProfile()
                .onSuccess { profile ->
                    _uiState.update { it.copy(currentUserProfile = profile) }
                    android.util.Log.d("FeedVM", "유저 프로필 로드 완료: ${profile.userId}")
                }
                .onFailure {
                    android.util.Log.w("FeedVM", "유저 프로필 로드 실패: ${it.message}")
                }
        }
    }

    // =====================
    // 카드 데이터 사전 fetch
    // =====================

    /**
     * 현재 인덱스 기준으로 앞 [FeedConstants.MATCH_PREFETCH_AHEAD]장의 카드에 대해
     * 팀원 프로필 + 거리 계산 + 종합 점수를 미리 계산해 캐시에 저장한다.
     *
     * - 이미 캐시된 팀은 건너뛴다.
     * - 유저 프로필이 아직 로드되지 않은 경우 거리/점수 계산을 생략한다.
     */
    private fun prefetchCardData(teams: List<Team>, fromIndex: Int) {
        val end = minOf(fromIndex + FeedConstants.MATCH_PREFETCH_AHEAD, teams.size)
        if (fromIndex >= end) return

        val teamsToFetch = teams.subList(fromIndex, end)

        viewModelScope.launch {
            teamsToFetch.forEach { team ->
                val cached = _uiState.value.cardMemberProfilesCache.containsKey(team.teamId)
                if (cached) return@forEach

                // 1. 팀원 프로필 fetch
                val profilesResult = repository.fetchMemberProfiles(team.memberIds)
                val profiles = profilesResult.getOrElse { emptyList() }

                _uiState.update {
                    it.copy(cardMemberProfilesCache = it.cardMemberProfilesCache + (team.teamId to profiles))
                }

                // 2. 거리 계산 (유저 위치가 있을 때만)
                val userProfile = _uiState.value.currentUserProfile
                val distanceResults: List<MemberDistanceResult> =
                    if (userProfile != null && userProfile.location.isNotBlank()) {
                        computeDistanceScores(userProfile.location, profiles)
                    } else {
                        emptyList()
                    }

                if (distanceResults.isNotEmpty()) {
                    _uiState.update {
                        it.copy(cardDistanceCache = it.cardDistanceCache + (team.teamId to distanceResults))
                    }
                }

                // 3. 종합 fit score 계산
                val state = _uiState.value
                val fitScore = computeFitScore(
                    team = team,
                    userProfile = state.currentUserProfile,
                    members = profiles,
                    distanceResults = distanceResults,
                    userTagScores = state.userTagScores,
                    actionCount = state.likedTeamIds.size + state.passedTeamIds.size
                )
                _uiState.update {
                    it.copy(cardFitScoreCache = it.cardFitScoreCache + (team.teamId to fitScore))
                }
            }
            // 배치 전체 점수 계산 완료 후 미열람 카드를 점수 내림차순 재정렬
            applyFitScoreSort()
        }
    }

    /**
     * cardFitScoreCache 점수 기준으로 아직 보지 않은 카드를 내림차순 정렬한다.
     *
     * - 점수가 캐시된 팀: 점수 높은 순
     * - 아직 점수 없는 팀: 뒤쪽에 원래 순서 유지
     * - 이미 스와이프한 카드(currentIndex 이전)는 건드리지 않는다.
     */
    private fun applyFitScoreSort() {
        val state = _uiState.value
        val cache = state.cardFitScoreCache
        if (cache.isEmpty()) return

        val seen   = state.teams.take(state.currentIndex)
        val unseen = state.teams.drop(state.currentIndex)

        val (scored, unscored) = unseen.partition { cache.containsKey(it.teamId) }
        val sortedUnseen = scored.sortedByDescending { cache[it.teamId] ?: 0 } + unscored

        _uiState.update { it.copy(teams = seen + sortedUnseen) }
    }

    // =====================
    // 거리 점수 계산
    // =====================

    /**
     * 유저 위치에서 각 팀원 위치까지의 대중교통 소요시간을 조회해 점수로 변환한다.
     *
     * 소요시간 → 점수 공식: (100 - minutes × 0.8).coerceIn(0, 100)
     *   0분  → 100점, 30분 → 76점, 60분 → 52점, 125분 → 0점
     */
    private suspend fun computeDistanceScores(
        userLocation: String,
        members: List<MemberProfile>
    ): List<MemberDistanceResult> {
        val userLatLng = meetingRepo.geocodeAddress(userLocation) ?: return emptyList()

        return members.mapNotNull { member ->
            if (member.location.isBlank()) return@mapNotNull null
            val memberLatLng = meetingRepo.geocodeAddress(member.location) ?: return@mapNotNull null

            val distanceKm = meetingRepo.haversineKm(
                userLatLng.lat, userLatLng.lng,
                memberLatLng.lat, memberLatLng.lng
            )
            val transitMinutes = meetingRepo.fetchTransitDurationMinutes(
                startLat = userLatLng.lat,
                startLng = userLatLng.lng,
                goalLat  = memberLatLng.lat,
                goalLng  = memberLatLng.lng
            ) ?: (distanceKm / 22.0 * 60).toInt().coerceAtLeast(5) // 실패 시 평균 22 km/h 휴리스틱

            val score = (100 - transitMinutes * 0.8).toInt().coerceIn(0, 100)

            MemberDistanceResult(
                memberId        = member.userId,
                memberName      = member.name,
                memberLocation  = member.location,
                transitMinutes  = transitMinutes,
                distanceKm      = distanceKm,
                score           = score
            )
        }
    }

    // =====================
    // 종합 매칭 점수 계산
    // =====================

    /**
     * 팀에 대한 종합 fit score (0–100)를 계산한다.
     *
     * 가중치 배분 (데이터 유무에 따라 동적 조정):
     *   - 태그 선호도  30%
     *   - 가치관 일치  30%  (team.balanceProfile 없으면 제외)
     *   - 동네 근접도  20%  (distanceResults 없으면 제외)
     *   - 팀원 공통점  20%
     *
     * 실제 가중치는 사용 가능한 컴포넌트 비율로 재정규화된다.
     */
    internal fun computeFitScore(
        team: Team,
        userProfile: CurrentUserProfile?,
        members: List<MemberProfile>,
        distanceResults: List<MemberDistanceResult>,
        userTagScores: Map<String, Int>,
        actionCount: Int
    ): Int {
        val components = mutableListOf<Pair<Int, Double>>() // (점수, 가중치)

        // 1. 태그 선호도
        val tagScore = computeTagScore(
            teamTags = team.tags + team.mbtiTags,
            userTagScores = userTagScores,
            actionCount = actionCount
        )
        components.add(tagScore to 0.30)

        // 2. 가치관 일치 (balanceProfile 있을 때만)
        if (userProfile != null) {
            val balanceScore = computeBalanceScore(userProfile.balanceAnswers, team.balanceProfile)
            if (balanceScore != null) components.add(balanceScore to 0.30)
        }

        // 3. 동네 근접도 (거리 데이터 있을 때만)
        if (distanceResults.isNotEmpty()) {
            val avgDistScore = distanceResults.map { it.score }.average().toInt()
            components.add(avgDistScore to 0.20)
        }

        // 4. 팀원 공통점
        if (userProfile != null) {
            val commonScore = computeCommonalityScore(userProfile, members)
            components.add(commonScore to 0.20)
        }

        if (components.isEmpty()) return 70

        val totalWeight = components.sumOf { it.second }
        val weightedSum = components.sumOf { (score, weight) -> score * weight }
        return (weightedSum / totalWeight).toInt().coerceIn(0, 100)
    }

    /**
     * 유저가 선호하는 태그와 팀 태그의 일치도 (0–100).
     *
     * 스와이프 횟수가 MATCH_UNLOCK_THRESHOLD 미만이면 기본값 70을 반환한다.
     * 이상이면 유저 상위 TOP_TAG_N 태그 중 팀이 몇 개 포함하는지로 계산한다.
     */
    private fun computeTagScore(
        teamTags: List<String>,
        userTagScores: Map<String, Int>,
        actionCount: Int
    ): Int {
        if (actionCount < FeedConstants.MATCH_UNLOCK_THRESHOLD) return 70
        val topTags = userTagScores.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
            .take(FeedConstants.MATCH_TOP_TAG_N)
            .map { it.key }
        if (topTags.isEmpty()) return 70
        val matches = topTags.count { tag -> teamTags.contains(tag) }
        return (50 + matches.toDouble() / topTags.size * 50).toInt()
    }

    /**
     * 유저 답변(axis → -1|+1)과 팀 balanceProfile(axis → -1.0~+1.0)의 가치관 일치도 (0–100).
     *
     * 각 axis별 점수: ((userVal × teamAvg + 1) / 2) × 100
     *   완전 일치(1 × 1 = 1) → 100점
     *   정반대(-1 × 1 = -1)  →   0점
     *   팀이 반반(teamAvg=0) →  50점 (절반 부여 정책)
     */
    private fun computeBalanceScore(
        userAnswers: Map<String, Int>,
        teamProfile: Map<String, Float>
    ): Int? {
        if (userAnswers.isEmpty() || teamProfile.isEmpty()) return null
        val axes = userAnswers.keys.intersect(teamProfile.keys)
        if (axes.isEmpty()) return null

        val axisScores = axes.map { axis ->
            val userVal = userAnswers[axis]?.toFloat() ?: return@map 50f
            val teamAvg = teamProfile[axis] ?: return@map 50f
            ((userVal * teamAvg + 1f) / 2f * 100f).coerceIn(0f, 100f)
        }
        return axisScores.average().toInt().coerceIn(0, 100)
    }

    /**
     * 유저 프로필과 팀원들의 공통 관심사/음식 기반 공통점 점수 (0–100).
     *
     * 각 팀원별로 Jaccard 유사도를 계산한 뒤 평균을 낸다.
     * 항목이 전혀 없으면 50(중립)을 기본값으로 사용한다.
     *
     * 확장 포인트(Direction B): 나이대, 학과, MBTI 등을 필드 추가 시
     *   이 함수에서 추가 컴포넌트로 계산하면 된다.
     */
    private fun computeCommonalityScore(
        userProfile: CurrentUserProfile,
        members: List<MemberProfile>
    ): Int {
        if (members.isEmpty()) return 50

        val memberScores = members.map { member ->
            val scores = mutableListOf<Int>()

            // 관심사 Jaccard
            val interestUnion = (userProfile.interests + member.interests).toSet().size
            if (interestUnion > 0) {
                val interestIntersect = userProfile.interests.toSet()
                    .intersect(member.interests.toSet()).size
                scores.add((interestIntersect.toDouble() / interestUnion * 100).toInt())
            }

            // 음식 취향 Jaccard
            val foodUnion = (userProfile.foodLikes + member.foodLikes).toSet().size
            if (foodUnion > 0) {
                val foodIntersect = userProfile.foodLikes.toSet()
                    .intersect(member.foodLikes.toSet()).size
                scores.add((foodIntersect.toDouble() / foodUnion * 100).toInt())
            }

            if (scores.isEmpty()) 50 else scores.average().toInt()
        }
        return memberScores.average().toInt().coerceIn(0, 100)
    }

    // =====================
    // 디버그 더미 데이터 주입
    // =====================

    /**
     * 디버그 빌드에서 DummyData 를 FeedUiState 에 직접 주입한다.
     *
     * 사용 예시 (FeedScreen.kt 또는 Activity):
     * ```kotlin
     * if (BuildConfig.DEBUG) {
     *     LaunchedEffect(Unit) { DummyData.injectTo(viewModel) }
     * }
     * ```
     *
     * @param teams           피드에 표시할 팀 목록
     * @param currentUser     로그인 유저 프로필
     * @param userTopTags     상위 태그 (매칭 근거 Top N 카드용)
     * @param actionCount     좋아요+패스 누적 횟수 (>=10 이면 해금)
     * @param memberCache     teamId → 팀원 프로필 캐시
     * @param distanceCache   teamId → 거리 결과 캐시
     * @param fitScoreCache   teamId → 종합 점수 캐시
     */
    fun injectDummyState(
        teams         : List<com.bugzero.meety.ui.team.Team>,
        currentUser   : CurrentUserProfile,
        userTopTags   : List<String>,
        actionCount   : Int,
        memberCache   : Map<String, List<MemberProfile>>,
        distanceCache : Map<String, List<MemberDistanceResult>>,
        fitScoreCache : Map<String, Int>
    ) {
        val tagScores: Map<String, Int> = userTopTags.mapIndexed { idx, tag ->
            tag to (actionCount - idx)          // 순위별 점수 역산
        }.toMap()

        val likedCount  = actionCount / 2
        val passedCount = actionCount - likedCount

        _uiState.update { old ->
            old.copy(
                teams                  = teams,
                allTeams               = teams,
                isLoading              = false,
                currentUserProfile     = currentUser,
                userTagScores          = tagScores,
                likedTeamIds           = (1..likedCount).map  { "dummy-liked-$it"  }.toSet(),
                passedTeamIds          = (1..passedCount).map { "dummy-passed-$it" }.toSet(),
                cardMemberProfilesCache = memberCache,
                cardDistanceCache      = distanceCache,
                cardFitScoreCache      = fitScoreCache
            )
        }
    }

}