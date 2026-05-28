package com.bugzero.meety.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugzero.meety.data.repository.FeedRepository
import com.bugzero.meety.ui.team.Team
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.delay
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

    // 팀 상세화면 실시간 구독 핸들
    private var teamDetailListener: ListenerRegistration? = null

    init {
        loadPreferenceThenFetch()
        observeResetSignal()
        observeMyTeamIds()
        observeLikedTeamIds()
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
    // 좋아요 상태 실시간 감지 (나가기·초기화 등으로 변경 시 즉시 피드 반영)
    // =====================

    private fun observeLikedTeamIds() {
        viewModelScope.launch {
            repository.observeLikedTeamIds().collect { liked ->
                _uiState.update { it.copy(likedTeamIds = liked) }
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

            currentTeam.tags.forEach { tag ->
                updatedPreferences[tag] = (updatedPreferences[tag] ?: 0) + tagWeight
            }

            currentTeam.mbtiTags.forEach { mbti ->
                updatedPreferences[mbti] = (updatedPreferences[mbti] ?: 0) + mbtiWeight
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
                userPreferences = updatedPreferences
            )
        }

        val latestState = _uiState.value
        val remaining = latestState.teams.size - latestState.currentIndex

        if (remaining <= 3 && latestState.hasMore) {
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
        // 팀 문서 실시간 감시 시작 → 멤버 변경 즉시 반영
        observeTeamDetail(teamId)
    }

    fun clearSelectedTeam() {
        teamDetailListener?.remove()
        teamDetailListener = null
        _uiState.update { it.copy(selectedTeam = null, memberProfiles = emptyList()) }
    }

    /** 팀 문서를 실시간으로 감시 — memberIds 변경 시 프로필 즉시 재로드 */
    private fun observeTeamDetail(teamId: String) {
        teamDetailListener?.remove()
        teamDetailListener = FirebaseFirestore.getInstance()
            .collection("teams")
            .document(teamId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                val updatedTeam = snapshot.toObject(Team::class.java) ?: return@addSnapshotListener
                val current = _uiState.value.selectedTeam ?: return@addSnapshotListener
                if (updatedTeam.teamId != current.teamId) return@addSnapshotListener

                val memberIdsChanged = updatedTeam.memberIds.toSet() != current.memberIds.toSet()
                _uiState.update { it.copy(selectedTeam = updatedTeam) }
                if (memberIdsChanged) {
                    loadMemberProfiles(updatedTeam)
                }
            }
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
            it.copy(
                teams          = newTeams,
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
        val tagWeight  = if (isLike) FeedConstants.TAG_LIKE_WEIGHT  else FeedConstants.TAG_PASS_WEIGHT
        val mbtiWeight = if (isLike) FeedConstants.MBTI_LIKE_WEIGHT else FeedConstants.MBTI_PASS_WEIGHT

        val updated = _uiState.value.userPreferences.toMutableMap()
        team.tags.forEach     { tag  -> updated[tag]  = (updated[tag]  ?: 0) + tagWeight }
        team.mbtiTags.forEach { mbti -> updated[mbti] = (updated[mbti] ?: 0) + mbtiWeight }

        _uiState.update { it.copy(userPreferences = updated) }
        // applyPreferenceSort()
    }

    private fun reversePreferenceInMemory(team: Team, wasLike: Boolean) {
        val tagWeight  = if (wasLike) FeedConstants.TAG_PASS_WEIGHT  else FeedConstants.TAG_LIKE_WEIGHT
        val mbtiWeight = if (wasLike) FeedConstants.MBTI_PASS_WEIGHT else FeedConstants.MBTI_LIKE_WEIGHT

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



    override fun onCleared() {
        teamDetailListener?.remove()
        super.onCleared()
    }
}
