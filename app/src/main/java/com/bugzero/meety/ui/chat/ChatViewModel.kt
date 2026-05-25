package com.bugzero.meety.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugzero.meety.data.repository.ChatRepository
import com.bugzero.meety.data.repository.FirebaseChatRepository
import com.bugzero.meety.data.repository.MeetingPlaceRepository
import com.bugzero.meety.data.repository.PlaceResult
import com.bugzero.meety.ui.team.FirebaseTeamRepository
import com.bugzero.meety.ui.team.ReceivedLikeItem
import com.bugzero.meety.ui.team.Team
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

// ── 데이터 모델 (여기 한 번만 정의!) ──────────────────────
data class UserProfileData(
    val userId: String = "",
    val name: String = "",
    val age: Int = 0,
    val department: String = "",
    val height: Int = 0,
    val location: String = "",
    val mbti: String = "",
    val bio: String = "",
    val interests: List<String> = emptyList(),
    val foodLikes: List<String> = emptyList(),
    val foodDislikes: List<String> = emptyList(),
    val profileImageUrl: String = ""
)

data class ParticipantItem(
    val userId: String,
    val name: String,
    val emoji: String,
    val isLeader: Boolean = false,
    val profileImage: String = "",
    val isFriend: Boolean = false
)

class ChatViewModel(
    private val repository: ChatRepository = FirebaseChatRepository(),
    private val teamRepository: FirebaseTeamRepository = FirebaseTeamRepository(),
    private val placeRepository: MeetingPlaceRepository = MeetingPlaceRepository()
) : ViewModel() {

    private var likesListener: com.google.firebase.firestore.ListenerRegistration? = null
    private val currentUserIdOrNull: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    private val _chatList = MutableStateFlow<List<ChatPreview>>(emptyList())
    val chatList: StateFlow<List<ChatPreview>> = _chatList.asStateFlow()

    private val _requestList = MutableStateFlow<List<ReceivedLikeItem>>(emptyList())
    val requestList: StateFlow<List<ReceivedLikeItem>> = _requestList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _roomName = MutableStateFlow("")
    val roomName: StateFlow<String> = _roomName.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _participants = MutableStateFlow<List<ParticipantItem>>(emptyList())
    val participants: StateFlow<List<ParticipantItem>> = _participants.asStateFlow()

    private val _selectedUserProfile = MutableStateFlow<UserProfileData?>(null)
    val selectedUserProfile: StateFlow<UserProfileData?> = _selectedUserProfile.asStateFlow()

    private val _isLoadingProfile = MutableStateFlow(false)
    val isLoadingProfile: StateFlow<Boolean> = _isLoadingProfile.asStateFlow()

    private val _friendList = MutableStateFlow<List<UserProfileData>>(emptyList())
    val friendList: StateFlow<List<UserProfileData>> = _friendList.asStateFlow()

    private val _isLoadingFriends = MutableStateFlow(false)
    val isLoadingFriends: StateFlow<Boolean> = _isLoadingFriends.asStateFlow()

    // ── 현재 채팅방 메타 (chat 문서에서 읽어온 실제 값) ──
    private val _currentTeamId = MutableStateFlow("")
    val currentTeamId: StateFlow<String> = _currentTeamId.asStateFlow()

    private val _currentChatType = MutableStateFlow("")   // "team" | "direct" | "group"
    val currentChatType: StateFlow<String> = _currentChatType.asStateFlow()

    // ── 팀원 자동 매칭 / 초대 ──
    private val _pendingInvitations = MutableStateFlow<List<TeamInvitation>>(emptyList())
    val pendingInvitations: StateFlow<List<TeamInvitation>> = _pendingInvitations.asStateFlow()

    private val _matchCandidates = MutableStateFlow<List<MatchCandidate>>(emptyList())
    val matchCandidates: StateFlow<List<MatchCandidate>> = _matchCandidates.asStateFlow()

    private val _isLoadingCandidates = MutableStateFlow(false)
    val isLoadingCandidates: StateFlow<Boolean> = _isLoadingCandidates.asStateFlow()

    private val _selectedInvitation = MutableStateFlow<TeamInvitation?>(null)
    val selectedInvitation: StateFlow<TeamInvitation?> = _selectedInvitation.asStateFlow()

    private val _selectedInvitationTeam = MutableStateFlow<Team?>(null)
    val selectedInvitationTeam: StateFlow<Team?> = _selectedInvitationTeam.asStateFlow()

    // ─── 장소 추천 ────────────────────────────────────────────────────────────
    private val _placeRecommendations = MutableStateFlow<List<PlaceResult>>(emptyList())
    val placeRecommendations: StateFlow<List<PlaceResult>> = _placeRecommendations.asStateFlow()

    private val _isLoadingPlaces = MutableStateFlow(false)
    val isLoadingPlaces: StateFlow<Boolean> = _isLoadingPlaces.asStateFlow()

    private val _placeError = MutableStateFlow<String?>(null)
    val placeError: StateFlow<String?> = _placeError.asStateFlow()

    // 대중교통 소요시간 (placeKey → 평균 분) — 하위 호환용으로 유지, UI에선 미사용
    private val _transitAverages = MutableStateFlow<Map<String, Int>>(emptyMap())
    val transitAverages: StateFlow<Map<String, Int>> = _transitAverages.asStateFlow()

    // 대중교통 소요시간 (placeKey → 사용자별 breakdown) — 하위 호환용으로 유지, UI에선 미사용
    private val _transitBreakdowns = MutableStateFlow<Map<String, List<TransitUserInfo>>>(emptyMap())
    val transitBreakdowns: StateFlow<Map<String, List<TransitUserInfo>>> = _transitBreakdowns.asStateFlow()

    // 지역(중간 지점) 기준 대중교통 평균 시간 — 가게별 계산 대신 지역 단위로 한 번만 계산
    private val _regionTransitAvg = MutableStateFlow<Int?>(null)
    val regionTransitAvg: StateFlow<Int?> = _regionTransitAvg.asStateFlow()

    // 지역 기준 참여자별 대중교통 breakdown
    private val _regionTransitBreakdown = MutableStateFlow<List<TransitUserInfo>>(emptyList())
    val regionTransitBreakdown: StateFlow<List<TransitUserInfo>> = _regionTransitBreakdown.asStateFlow()

    // 추천된 지역명 (예: "서울 강남구") — 헤더에 표시
    private val _recommendedRegionName = MutableStateFlow("")
    val recommendedRegionName: StateFlow<String> = _recommendedRegionName.asStateFlow()

    // ── 재추천 개선 (이미 본 장소 제외 / 반경 / 찜 / 조건 시트) ─────────────────
    /** "다시 추천" 눌렀을 때 누적되는 본 장소 키 (name|address). */
    private val shownPlaceKeys = mutableSetOf<String>()
    /** 현재 세션 검색 반경(m). isRefresh=false 로 시작하면 800m 로 리셋. */
    private var searchRadiusMeters: Int = 800
    /** 반경 확장 단계: 800 → 1500 → 3000 → 5000 */
    private val radiusLadder = listOf(800, 1500, 3000, 5000)
    /** 최근 "다시 추천" 탭 타임스탬프 — 30초 내 3회면 조건 시트 자동 노출. */
    private val recentRefreshTimes = mutableListOf<Long>()

    /** 결과 상단에 1회성으로 띄워주는 안내 문구 (예: "이미 본 3곳 제외 · 반경 1.5km로 넓힘"). */
    private val _refreshNotice = MutableStateFlow<String?>(null)
    val refreshNotice: StateFlow<String?> = _refreshNotice.asStateFlow()

    /** 이번 채팅방 세션에서 찜(하트)한 장소들 — 재추천해도 제외되지 않음. */
    private val _savedPlaces = MutableStateFlow<List<PlaceResult>>(emptyList())
    val savedPlaces: StateFlow<List<PlaceResult>> = _savedPlaces.asStateFlow()

    /** 찜한 장소의 key 집합 (UI 에서 하트 상태 빠른 조회용). */
    private val _savedPlaceKeys = MutableStateFlow<Set<String>>(emptySet())
    val savedPlaceKeys: StateFlow<Set<String>> = _savedPlaceKeys.asStateFlow()

    /** 조건 바꾸기 BottomSheet 노출 여부 — 4단계/5단계에서 공유. */
    private val _showConditionSheet = MutableStateFlow(false)
    val showConditionSheet: StateFlow<Boolean> = _showConditionSheet.asStateFlow()

    /**
     * 지역 직접 검색 모드 — null 이면 "중간 지점" 기준, 문자열이면 해당 지역명("서울 용산구").
     * UI 에서는 헤더/하단 버튼이 이 값에 따라 바뀐다.
     */
    private val _searchRegion = MutableStateFlow<String?>(null)
    val searchRegion: StateFlow<String?> = _searchRegion.asStateFlow()

    /** 마지막으로 사용한 키워드(필터). 지역 전환 시에도 유지하기 위해 보관. */
    private var lastKeywords: List<String>? = null

    /** 마지막으로 계산한 참여자 위치 목록. 지역 전환 시 대중교통 재계산에 재사용. */
    private var lastTransitParticipants: List<TransitParticipant> = emptyList()

    /** 현재 반경(UI 슬라이더 초기값 용). */
    val currentRadiusMeters: Int get() = searchRadiusMeters

    private fun placeKey(p: PlaceResult): String = "${p.name}|${p.address}"

    /** API 키 미설정 여부 — UI에서 "예정 기능" 안내용 */
    val isPlaceApiReady: Boolean get() = placeRepository.isApiKeyConfigured()

    init { refreshForAuthState() }

    fun refreshForAuthState() {
        val userId = currentUserIdOrNull
        if (userId.isNullOrBlank()) { clearForLoggedOutState(); return }
        likesListener?.remove(); likesListener = null
        _chatList.value = emptyList(); _requestList.value = emptyList(); _errorMessage.value = null
        loadChatList(); loadRequestList(); saveFcmToken(); loadSavedPlaces(); loadPendingInvitations()
    }

    private fun clearForLoggedOutState() {
        likesListener?.remove(); likesListener = null
        _chatList.value = emptyList(); _requestList.value = emptyList(); _messages.value = emptyList()
        _participants.value = emptyList(); _friendList.value = emptyList(); _selectedUserProfile.value = null
        _isLoading.value = false; _isSending.value = false; _isLoadingFriends.value = false; _isLoadingProfile.value = false
        _errorMessage.value = null; _roomName.value = ""
        _pendingInvitations.value = emptyList(); _matchCandidates.value = emptyList()
        _selectedInvitation.value = null; _selectedInvitationTeam.value = null
    }

    fun clearError() { _errorMessage.value = null }

    fun enterChatRoom(chatId: String, roomName: String) {
        val userId = currentUserIdOrNull ?: return
        _roomName.value = roomName
        _currentTeamId.value = ""
        _currentChatType.value = ""
        observeMessages(chatId)
        loadParticipants(chatId)
        loadSavedPlaces()   // 찜 목록 Firestore에서 복원
    }

    /** 앱 재시작 후에도 유지되도록 Firestore에서 찜 목록 로드 */
    private fun loadSavedPlaces() {
        val userId = currentUserIdOrNull ?: return
        viewModelScope.launch {
            try {
                val snapshot = FirebaseFirestore.getInstance()
                    .collection("users").document(userId)
                    .collection("savedMeetingPlaces").get().await()
                val places = snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        PlaceResult(
                            name        = doc.getString("name") ?: return@mapNotNull null,
                            address     = doc.getString("address") ?: "",
                            category    = doc.getString("category") ?: "",
                            phone       = doc.getString("phone") ?: "",
                            lat         = doc.getDouble("lat") ?: 0.0,
                            lng         = doc.getDouble("lng") ?: 0.0,
                            imageUrl    = doc.getString("imageUrl") ?: "",
                            imageUrls   = (doc.get("imageUrls") as? List<*>)
                                              ?.mapNotNull { it?.toString() } ?: emptyList(),
                            reviewCount = doc.getLong("reviewCount")?.toInt() ?: 0,
                            placeId     = doc.getString("placeId") ?: ""
                        )
                    }.getOrNull()
                }
                _savedPlaces.value = places
                _savedPlaceKeys.value = places.map { placeKey(it) }.toSet()
                Log.d("ChatVM", "찜 목록 로드 완료: ${places.size}곳")
            } catch (e: Exception) {
                Log.e("ChatVM", "찜 목록 로드 실패: ${e.message}")
            }
        }
    }

    /** Firestore 저장용 문서 ID — placeId 우선, 없으면 name+address 해시 */
    private fun savedPlaceDocId(place: PlaceResult): String =
        if (place.placeId.isNotBlank()) "id_${place.placeId}"
        else "${place.name}${place.address}"
            .filter { it.isLetterOrDigit() }
            .take(100)
            .ifBlank { place.name.hashCode().toString() }

    fun loadChatList() {
        val userId = currentUserIdOrNull ?: return
        viewModelScope.launch {
            _isLoading.value = true
            repository.observeChatList(userId).catch { _errorMessage.value = "목록 로드 실패"; _isLoading.value = false }
                .collect { _chatList.value = it; _isLoading.value = false }
        }
    }

    fun loadRequestList() {
        val userId = currentUserIdOrNull ?: return
        likesListener?.remove()
        likesListener = teamRepository.observeReceivedLikes(onUpdate = { _requestList.value = it }, onFailure = { _errorMessage.value = it })
    }

    fun acceptRequest(likeId: String) {
        teamRepository.acceptReceivedLike(likeId, onSuccess = { loadRequestList(); loadChatList() }, onFailure = { _errorMessage.value = it })
    }

    fun rejectRequest(likeId: String) {
        teamRepository.rejectReceivedLike(likeId, onSuccess = { loadRequestList() }, onFailure = { _errorMessage.value = it })
    }

    fun observeMessages(chatId: String) {
        viewModelScope.launch {
            repository.observeMessages(chatId).catch { _errorMessage.value = "메시지 로드 실패" }.collect { _messages.value = it }
        }
    }

    fun sendMessage(chatId: String, content: String) {
        val userId = currentUserIdOrNull ?: return
        if (content.isBlank()) return
        viewModelScope.launch {
            _isSending.value = true
            try { repository.sendMessage(chatId, userId, content.trim()) }
            finally { _isSending.value = false }
        }
    }

    /** 스티커(이모티콘) 전송 — content 에는 sticker id 가 들어가고 type="sticker" */
    fun sendSticker(chatId: String, stickerId: String) {
        val userId = currentUserIdOrNull ?: return
        if (stickerId.isBlank()) return
        viewModelScope.launch {
            _isSending.value = true
            try {
                repository.sendMessage(
                    chatId   = chatId,
                    senderId = userId,
                    content  = stickerId,
                    type     = "sticker"
                )
            } catch (e: Exception) {
                Log.e("ChatVM", "sendSticker 실패: ${e.message}")
                _errorMessage.value = "이모티콘 전송에 실패했어요"
            } finally {
                _isSending.value = false
            }
        }
    }

    /** 장소 추천 화면에서 선택한 업체를 채팅방에 카드 메시지로 공유 */
    fun sharePlaceToChat(chatId: String, place: PlaceResult) {
        val userId = currentUserIdOrNull ?: return
        if (chatId.isBlank() || place.name.isBlank()) return
        viewModelScope.launch {
            _isSending.value = true
            try {
                repository.sendPlaceCard(
                    chatId = chatId,
                    senderId = userId,
                    placeName = place.name,
                    placeCategory = place.category,
                    placeAddress = place.address,
                    placeImageUrl = place.imageUrl,
                    placeReviewCount = place.reviewCount,
                    placePlaceId = place.placeId,
                    placeLat = place.lat,
                    placeLng = place.lng
                )
            } catch (e: Exception) {
                Log.e("ChatVM", "sharePlaceToChat 실패: ${e.message}")
                _errorMessage.value = "장소 공유에 실패했어요"
            } finally {
                _isSending.value = false
            }
        }
    }

    fun formatTime(timestamp: com.google.firebase.Timestamp?): String {
        timestamp ?: return ""
        val date = timestamp.toDate()
        return SimpleDateFormat("HH:mm", Locale.KOREA).format(date)
    }

    fun formatChatListTime(timestamp: com.google.firebase.Timestamp?): String {
        if (timestamp == null) return ""

        val targetDate = timestamp.toDate()

        val todayCal = Calendar.getInstance()
        val targetCal = Calendar.getInstance().apply { time = targetDate }

        val todayYear = todayCal.get(Calendar.YEAR)
        val todayMonth = todayCal.get(Calendar.MONTH)
        val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)
        val targetYear = targetCal.get(Calendar.YEAR)

        // 오늘 자정(00:00:00) 구하기 (이걸 기준으로 어제/오늘을 나눕니다)
        val todayMidnight = Calendar.getInstance().apply {
            set(todayYear, todayMonth, todayDay, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val targetTime = targetCal.timeInMillis

        return when {
            // 1. 오늘 (타겟 시간이 오늘 자정 이후) -> 기존처럼 시간만 표시
            targetTime >= todayMidnight -> {
                SimpleDateFormat("HH:mm", Locale.KOREA).format(targetDate)
                // 💡 만약 카톡처럼 '오후 8:52' 스타일로 바꾸고 싶다면 "a h:mm" 으로 적어주시면 됩니다!
            }
            // 2. 어제 (타겟 시간이 어제 자정 ~ 오늘 자정 전)
            targetTime >= (todayMidnight - 24 * 60 * 60 * 1000) -> {
                "어제"
            }
            // 3. 그 이전 (올해 안) -> "5월 12일" 형식
            todayYear == targetYear -> {
                SimpleDateFormat("M월 d일", Locale.KOREA).format(targetDate)
            }
            // 4. 작년 등 해가 넘어간 경우 -> "2025.12.31" 형식
            else -> {
                SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(targetDate)
            }
        }
    }

    fun saveFcmToken() {
        val userId = currentUserIdOrNull ?: return
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            FirebaseFirestore.getInstance().collection("users").document(userId).update("fcmToken", token)
        }
    }

    // ✨ [수정] 참여자 로드 시 '나'와 '팀장' 정렬 로직 추가
    fun loadParticipants(chatId: String) {
        val myId = currentUserIdOrNull ?: return
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()

                // 1. 내 친구 목록 가져오기
                val friendsSnapshot = db.collection("users").document(myId).collection("friends").get().await()
                val friendIds = friendsSnapshot.documents.mapNotNull { it.getString("friendUserId") }.toSet()

                // 2. 채팅방 정보 가져와서 타입(Type) 확인하기
                val chatDoc = db.collection("chats").document(chatId).get().await()
                val chatType = chatDoc.getString("type") ?: "group" // 기본값은 group
                val isDirect = chatType == "direct" || chatId.startsWith("direct")
                val isTeam = chatType == "team" // ✨ 팀 채팅인지 체크!

                // 채팅 문서에서 실제 teamId, chatType 저장 (팀원 자동 매칭 시 사용)
                val resolvedTeamId = chatDoc.getString("teamId")?.takeIf { it.isNotBlank() } ?: chatId
                _currentTeamId.value = resolvedTeamId
                _currentChatType.value = chatType

                @Suppress("UNCHECKED_CAST")
                val participantIds = chatDoc.get("participants") as? List<String> ?: return@launch

                val rawItems = participantIds.mapIndexed { index, pUserId ->
                    val userDoc = db.collection("users").document(pUserId).get().await()

                    val shouldShowLeader = isTeam && index == 0

                    // ✨ [핵심 로직] 파이어베이스 필드명이 다를 수 있으므로 3가지를 다 찔러서 무조건 사진을 가져옵니다!
                    val fetchedImage = (userDoc.get("profileImages") as? List<*>)?.firstOrNull()?.toString()
                        ?: userDoc.getString("profileImageUrl")
                        ?: userDoc.getString("profileImage")
                        ?: ""

                    ParticipantItem(
                        userId = pUserId,
                        name = userDoc.getString("name") ?: "알 수 없음",
                        emoji = if (shouldShowLeader) "👑" else "👤",
                        isLeader = shouldShowLeader,
                        profileImage = fetchedImage, // 👈 완벽하게 가져온 이미지 변수를 쏙 넣어줍니다!
                        isFriend = friendIds.contains(pUserId) || pUserId == myId
                    )
                }

                // 3. 정렬 로직 (팀장 -> 나 -> 나머지)
                val myItem = rawItems.find { it.userId == myId }
                val leaderItem = rawItems.find { it.isLeader }
                val others = rawItems.filter { it.userId != myId && !it.isLeader }

                val sortedList = if (isDirect) {
                    listOfNotNull(myItem) + others
                } else if (isTeam) {
                    // 팀 채팅: 팀장 -> 나 -> 나머지 순서
                    if (myItem?.userId == leaderItem?.userId) listOfNotNull(myItem) + others
                    else listOfNotNull(leaderItem, myItem) + others
                } else {
                    // 일반 단체톡: 나 -> 나머지 순서 (팀장이 없으므로)
                    listOfNotNull(myItem) + others
                }

                _participants.value = sortedList
            } catch (e: Exception) {
                android.util.Log.e("ChatVM", "Participants Error: ${e.message}")
            }
        }
    }

    fun addFriend(chatId: String, targetUser: ParticipantItem) {
        val myId = currentUserIdOrNull ?: return
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val friendData = mapOf("friendUserId" to targetUser.userId, "name" to targetUser.name, "addedAt" to FieldValue.serverTimestamp())
                db.collection("users").document(myId).collection("friends").document(targetUser.userId).set(friendData).await()
                loadParticipants(chatId)
            } catch (e: Exception) { Log.e("ChatVM", "Add Friend Error: ${e.message}") }
        }
    }



    fun createOrGetDirectChat(friend: UserProfileData, onSuccess: (String, String) -> Unit, onFailure: (String) -> Unit) {
        val myId = currentUserIdOrNull ?: return
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()

                // ✨ [수정 포인트] 파이어베이스 인덱스 에러를 피하기 위해 조건 하나를 뺍니다!
                // 먼저 내가 참여한 방을 싹 다 가져옵니다.
                val snapshot = db.collection("chats")
                    .whereArrayContains("participants", myId)
                    .get()
                    .await()

                var existingChatId: String? = null
                for (doc in snapshot.documents) {
                    val type = doc.getString("type") ?: ""
                    val participants = doc.get("participants") as? List<String> ?: emptyList()

                    // ✨ 앱 안에서 걸러내기: 'direct' 타입이고, 딱 2명이며, 그 친구가 있는지 확인!
                    if (type == "direct" && participants.size == 2 && participants.contains(friend.userId)) {
                        existingChatId = doc.id
                        break
                    }
                }

                if (existingChatId != null) {
                    // 2-A. 기존에 온전한 1:1 방이 남아있다면 그 방으로 이동
                    withContext(Dispatchers.Main) { onSuccess(existingChatId, friend.name) }
                } else {
                    // 2-B. 완전 새로운 1:1 방 생성!
                    val newChatId = "direct_${UUID.randomUUID()}"
                    val now = com.google.firebase.Timestamp.now()
                    val ids = listOf(myId, friend.userId).sorted()

                    db.collection("chats").document(newChatId).set(
                        mapOf(
                            "type" to "direct",
                            "participants" to ids,
                            "teamName" to friend.name,
                            "emoji" to "💬",
                            "createdAt" to now,
                            "lastMessage" to "1:1 채팅방이 생성되었습니다.", // 첫 메시지도 센스있게!
                            "lastMessageAt" to now,
                            // ✨ 원년 멤버는 방 생성 시점부터 가시. 재입장 시 덮어써져 이전 기록이 가려짐.
                            "memberJoinedAt" to ids.associateWith { now }
                        )
                    ).await()

                    withContext(Dispatchers.Main) { onSuccess(newChatId, friend.name) }
                }
            } catch (e: Exception) {
                // ✨ 진짜 에러 원인이 뭔지 안드로이드 스튜디오 Logcat에 빨간 글씨로 찍어줍니다!
                android.util.Log.e("ChatViewModel", "1:1 채팅 에러: ${e.message}", e)
                withContext(Dispatchers.Main) { onFailure("생성 실패") }
            }
        }
    }

    // ✨ 그룹 채팅방 생성 (커스텀 방 이름 추가!)
    fun createChatWithFriends(
        selectedFriends: List<UserProfileData>,
        customRoomName: String = "",
        onSuccess: (String, String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val myId = currentUserIdOrNull ?: return
        viewModelScope.launch {
            try {
                val chatId = "group_${UUID.randomUUID()}"
                val ids = listOf(myId) + selectedFriends.map { it.userId }

                // ✨ 여기가 핵심입니다! (입력한 이름이 있으면 쓰고, 없으면 친구들 이름 나열)
                val defaultName = selectedFriends.joinToString(", ") { it.name }
                val finalRoomName = if (customRoomName.isNotBlank()) customRoomName else defaultName

                val now = com.google.firebase.Timestamp.now()

                // ✨ DB에 저장할 때 "teamName"에 finalRoomName이 잘 들어가야 합니다!
                FirebaseFirestore.getInstance().collection("chats").document(chatId).set(
                    mapOf(
                        "type" to "group",
                        "participants" to ids,
                        "teamName" to finalRoomName, // <--- 이 부분이 제대로 들어갔는지 확인!
                        "emoji" to "👥",
                        "createdAt" to now,
                        "lastMessage" to "채팅방이 생성되었습니다.",
                        "lastMessageAt" to now,
                        // ✨ 원년 멤버 전원에게 생성 시점 기록. 재입장 시 자동으로 덮어써짐.
                        "memberJoinedAt" to ids.associateWith { now }
                    )
                ).await()

                withContext(Dispatchers.Main) { onSuccess(chatId, finalRoomName) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onFailure("그룹 생성 실패") }
            }
        }
    }

    fun leaveChatRoom(chatId: String, onSuccess: () -> Unit = {}, onFailure: (String) -> Unit = {}) {
        val userId = currentUserIdOrNull ?: return
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val userDoc = db.collection("users").document(userId).get().await()
                val myName = userDoc.getString("name") ?: "알 수 없음"
                val now = com.google.firebase.Timestamp.now()

                val chatRef = db.collection("chats").document(chatId)

                // 1) 채팅방에서 나가기
                val systemMsg = mapOf("senderId" to "system", "content" to "${myName}님이 나갔습니다.", "type" to "system", "createdAt" to now)
                chatRef.collection("messages").add(systemMsg).await()
                chatRef.update(mapOf("lastMessage" to "${myName}님이 나갔습니다.", "lastMessageAt" to now, "participants" to FieldValue.arrayRemove(userId))).await()

                // ✨ 2) 팀(teams) 컬렉션에서도 나를 삭제 (필드명 memberIds 로 수정!)
                val chatDoc = chatRef.get().await()
                val teamId = chatDoc.getString("teamId") ?: chatId
                try {
                    db.collection("teams").document(teamId).update("memberIds", FieldValue.arrayRemove(userId)).await()
                } catch (e: Exception) { }

                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onFailure("실패") }
            }
        }
    }

    fun transferLeaderAndLeave(chatId: String, newLeaderUserId: String, onSuccess: () -> Unit) {
        val userId = currentUserIdOrNull ?: return
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val userDoc = db.collection("users").document(userId).get().await()
                val myName = userDoc.getString("name") ?: "알 수 없음"
                val now = com.google.firebase.Timestamp.now()

                val currentParticipants = _participants.value.map { it.userId }.toMutableList()
                currentParticipants.remove(newLeaderUserId); currentParticipants.remove(userId)
                val newParticipants = mutableListOf(newLeaderUserId) + currentParticipants
                db.collection("chats").document(chatId).update("participants", newParticipants).await()

                val leaveMsg = mapOf("senderId" to "system", "content" to "${myName}님이 나갔습니다.", "type" to "system", "createdAt" to now)
                val newLeaderName = _participants.value.find { it.userId == newLeaderUserId }?.name ?: "새 팀장"
                val transMsg = mapOf("senderId" to "system", "content" to "${newLeaderName}님이 팀장이 되었습니다.", "type" to "system", "createdAt" to com.google.firebase.Timestamp(now.seconds, now.nanoseconds + 1000))

                db.collection("chats").document(chatId).collection("messages").add(leaveMsg).await()
                db.collection("chats").document(chatId).collection("messages").add(transMsg).await()
                db.collection("chats").document(chatId).update(mapOf("lastMessage" to "${newLeaderName}님이 팀장이 되었습니다.", "lastMessageAt" to now)).await()

                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) { }
        }
    }

    // ✨ 1. 실수로 지워졌던 친구 목록 불러오기 함수 복구
    fun loadFriendList() {
        val userId = currentUserIdOrNull ?: return
        viewModelScope.launch {
            _isLoadingFriends.value = true
            try {
                val friendsSnapshot = FirebaseFirestore.getInstance().collection("users").document(userId).collection("friends").get().await()
                val friendIds = friendsSnapshot.documents.mapNotNull { it.getString("friendUserId") }
                val friends = mutableListOf<UserProfileData>()
                for (id in friendIds) {
                    val doc = FirebaseFirestore.getInstance().collection("users").document(id).get().await()
                    if (doc.exists()) {
                        friends.add(UserProfileData(userId = doc.id, name = doc.getString("name") ?: "알 수 없음", department = doc.getString("department") ?: "", mbti = doc.getString("mbti") ?: ""))
                    }
                }
                _friendList.value = friends.sortedBy { it.name }
            } finally { _isLoadingFriends.value = false }
        }
    }


    // ✨ 관심사와 음식을 꽉꽉 채워오고 프사까지 완벽하게 가져오는 함수
    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _isLoadingProfile.value = true
            try {
                val doc = FirebaseFirestore.getInstance().collection("users").document(userId).get().await()

                // ✨ [수정 포인트 1] 이 똑똑한 변수를...
                val photoUrl = doc.getString("profileImageUrl")
                    ?: doc.getString("profileImage")
                    ?: (doc.get("profileImages") as? List<*>)?.firstOrNull()?.toString()
                    ?: ""

                @Suppress("UNCHECKED_CAST")
                val fetchedInterests = (doc.get("interests") as? List<String>) ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val fetchedFoodLikes = (doc.get("foodLikes") as? List<String>) ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val fetchedFoodDislikes = (doc.get("foodDislikes") as? List<String>) ?: emptyList()

                _selectedUserProfile.value = UserProfileData(
                    userId = doc.id,
                    name = doc.getString("name") ?: "",
                    mbti = doc.getString("mbti") ?: "",
                    department = doc.getString("department") ?: "",
                    age = doc.getLong("age")?.toInt() ?: 0,
                    height = doc.getLong("height")?.toInt() ?: 0,
                    location = doc.getString("location") ?: "",
                    bio = doc.getString("bio") ?: "",

                    interests = fetchedInterests,
                    foodLikes = fetchedFoodLikes,
                    foodDislikes = fetchedFoodDislikes,

                    // ✨ [수정 포인트 2] 여기서 photoUrl 변수를 그대로 사용해야 합니다!
                    profileImageUrl = photoUrl
                )
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "프로필 로드 에러: ${e.message}")
            } finally {
                _isLoadingProfile.value = false
            }
        }
    }
    fun clearUserProfile() { _selectedUserProfile.value = null }

    /**
     * 채팅방 참여자들의 location 을 Firestore에서 읽어
     * 중간 지점을 계산한 뒤 네이버 Local Search로 장소를 추천한다.
     *
     * @param isRefresh "다시 추천받기" 경로에서 호출 시 true.
     *                  true면 [shownPlaceKeys] 를 유지한 채 새 결과를 찾고,
     *                  필요 시 반경을 [radiusLadder] 를 따라 자동 확장한다.
     * @param radiusOverride 조건 바꾸기 시트에서 사용자가 직접 지정한 반경(m).
     * @param includeShown true면 "이미 본 곳도 포함" — 제외 집합을 일시 무시.
     */
    fun recommendMeetingPlaces(
        chatId: String,
        keywords: List<String>? = null,
        isRefresh: Boolean = false,
        radiusOverride: Int? = null,
        includeShown: Boolean = false
    ) {
        lastKeywords = keywords
        // 지역 직접 검색 모드면 그쪽으로 분기
        val region = _searchRegion.value
        if (!region.isNullOrBlank()) {
            searchPopularPlacesInRegion(region, keywords)
            return
        }
        viewModelScope.launch {
            _isLoadingPlaces.value = true
            _placeError.value = null
            _refreshNotice.value = null
            // 신규 세션이면 누적 상태 초기화
            if (!isRefresh) {
                shownPlaceKeys.clear()
                searchRadiusMeters = radiusOverride ?: 800
            } else if (radiusOverride != null) {
                searchRadiusMeters = radiusOverride
            }
            _placeRecommendations.value = emptyList()
            try {
                val db = FirebaseFirestore.getInstance()

                // 1. 채팅방 참여자 UID 목록 조회
                val chatDoc = db.collection("chats").document(chatId).get().await()
                @Suppress("UNCHECKED_CAST")
                val participantIds = chatDoc.get("participants") as? List<String> ?: emptyList()
                Log.d("ChatVM", "━━━ 장소 추천 시작 ━━━")
                Log.d("ChatVM", "채팅방 ID: $chatId")
                Log.d("ChatVM", "참여자 수: ${participantIds.size}명, UIDs: $participantIds")

                // 2. 각 참여자의 상세 정보 수집 (이름/사진/위치)
                data class ParticipantInfo(
                    val userId: String,
                    val name: String,
                    val profileImage: String,
                    val locationStr: String,
                    val coord: com.bugzero.meety.data.repository.LatLng?
                )
                val participantInfos = participantIds.map { uid ->
                    val userDoc = db.collection("users").document(uid).get().await()
                    val loc = userDoc.getString("location")?.trim().orEmpty()
                    val name = userDoc.getString("name") ?: uid
                    val img = userDoc.getString("profileImageUrl").orEmpty()
                    Log.d("ChatVM", "  참여자 [$name] → location: '${loc.ifBlank { "(없음)" }}'")
                    ParticipantInfo(uid, name, img, loc, null)
                }
                val locations = participantInfos.map { it.locationStr }.filter { it.isNotBlank() }

                if (locations.isEmpty()) {
                    _placeError.value = "참여자 위치 정보가 없습니다.\nFirestore users 컬렉션에 location 필드를 확인해주세요."
                    return@launch
                }
                Log.d("ChatVM", "유효한 위치: ${locations.size}개 → $locations")

                // 3. 각 주소 → 좌표 변환 (네이버 지오코딩) — 참여자별로도 보관
                val participantWithCoord: List<ParticipantInfo> = participantInfos.map { p ->
                    val c = if (p.locationStr.isNotBlank()) placeRepository.geocodeAddress(p.locationStr) else null
                    p.copy(coord = c)
                }
                val coords = participantWithCoord.mapNotNull { it.coord }

                if (coords.isEmpty()) {
                    _placeError.value = "위치 정보를 변환할 수 없습니다.\n프로필의 사는 곳을 '서울 강남구' 형식으로 입력해주세요."
                    return@launch
                }
                Log.d("ChatVM", "좌표 변환 성공: ${coords.size}개")
                coords.forEachIndexed { i, c ->
                    Log.d("ChatVM", "  좌표[$i]: (${c.lat}, ${c.lng}) ← '${locations.getOrNull(i)}'")
                }

                // 4. 중간 지점 계산
                val centroid = placeRepository.calculateCentroid(coords)
                Log.d("ChatVM", "★ 중간 지점: (${centroid.lat}, ${centroid.lng})")

                // 4-1. 참여자별 대중교통 시간을 중간 지점 기준으로 딱 한 번 계산 (가게별 계산 제거)
                val transitParticipantsForRegion = participantWithCoord.mapNotNull { p ->
                    val c = p.coord ?: return@mapNotNull null
                    TransitParticipant(p.userId, p.name, p.profileImage, p.locationStr, c.lat, c.lng)
                }
                // 지역 전환 시 재계산할 수 있도록 저장
                lastTransitParticipants = transitParticipantsForRegion
                computeRegionTransit(centroid, transitParticipantsForRegion)

                // 5. 중간 지점이 속한 구(시/군) 를 역지오코딩으로 알아낸 뒤
                //    구 단위 인기 장소 TOP 100 (카카오맵 스타일) 조회.
                //    실패하면 반경 기반 폴백으로 내려간다.
                val searchKeywords = if (!keywords.isNullOrEmpty()) keywords else listOf("카페", "음식점", "맛집")
                val midpointRegion = placeRepository.resolveRegionName(centroid.lat, centroid.lng)
                if (!midpointRegion.isNullOrBlank()) {
                    _recommendedRegionName.value = midpointRegion
                    Log.d("ChatVM", "중간 지점 → 지역 '$midpointRegion' 기준 인기 TOP 100 조회")
                    val placesRaw = placeRepository.searchPlacesInRegion(
                        regionName = midpointRegion,
                        keywords = searchKeywords,
                        limit = 100
                    )
                    if (placesRaw.isNotEmpty()) {
                        _placeRecommendations.value = placesRaw
                        _transitAverages.value = emptyMap()
                        _transitBreakdowns.value = emptyMap()
                        _refreshNotice.value =
                            "📍 중간 지점 '${midpointRegion}' 의 인기 ${searchKeywords.joinToString("·")} TOP ${placesRaw.size}"

                        // 이미지·리뷰 보강 후 리뷰 수 내림차순 재정렬
                        val enriched = placeRepository.enrichPlacesWithDetails(placesRaw)
                        val ranked = enriched.withIndex()
                            .sortedWith(
                                compareByDescending<IndexedValue<PlaceResult>> { it.value.reviewCount }
                                    .thenBy { it.index }
                            )
                            .map { it.value }
                        _placeRecommendations.value = ranked
                        _refreshNotice.value =
                            "📍 중간 지점 '${midpointRegion}' 의 인기 ${searchKeywords.joinToString("·")} TOP ${ranked.size}"

                        Log.d("ChatVM", "━━━ 장소 추천 완료 (구 단위 TOP ${ranked.size}) ━━━")
                        return@launch
                    }
                    Log.w("ChatVM", "구 단위 검색 결과 0건 → 반경 기반 폴백으로 전환")
                }

                // ── 폴백: 역지오코딩 실패 혹은 해당 구에 결과가 없을 때, 기존 반경 검색 ──
                val initialRadius = searchRadiusMeters
                val exclude: Set<String> = if (includeShown) emptySet() else shownPlaceKeys.toSet()

                var places: List<PlaceResult> = emptyList()
                var usedRadius = initialRadius
                val ladder = radiusLadder.filter { it >= initialRadius }.ifEmpty { listOf(initialRadius) }
                for (r in ladder) {
                    places = placeRepository.searchNearbyPlaces(
                        lat = centroid.lat,
                        lng = centroid.lng,
                        keywords = searchKeywords,
                        radiusMeters = r,
                        excludeKeys = exclude
                    )
                    usedRadius = r
                    if (places.size >= 3) break
                }
                // 풀 고갈 — 제외 집합을 리셋하고 한 번 더
                var poolExhausted = false
                if (places.isEmpty() && exclude.isNotEmpty()) {
                    poolExhausted = true
                    shownPlaceKeys.clear()
                    places = placeRepository.searchNearbyPlaces(
                        lat = centroid.lat,
                        lng = centroid.lng,
                        keywords = searchKeywords,
                        radiusMeters = usedRadius,
                        excludeKeys = emptySet()
                    )
                }
                Log.d("ChatVM", "검색 결과: ${places.size}개 장소 (반경=${usedRadius}m, 제외=${exclude.size})")
                places.forEachIndexed { i, p ->
                    Log.d("ChatVM", "  장소[$i]: ${p.name} (${p.category}) - ${p.address}")
                }

                if (places.isEmpty()) {
                    _placeError.value = "주변 장소를 찾을 수 없습니다.\n반경을 넓혀 다시 시도해주세요."
                } else {
                    // 세션 상태 갱신
                    shownPlaceKeys.addAll(places.map(::placeKey))
                    searchRadiusMeters = usedRadius

                    // 안내 배너
                    val notice = buildRefreshNotice(
                        isRefresh = isRefresh,
                        excludedCount = exclude.size,
                        initialRadius = initialRadius,
                        usedRadius = usedRadius,
                        includeShown = includeShown,
                        poolExhausted = poolExhausted
                    )
                    _refreshNotice.value = notice

                    // 먼저 기본 결과를 보여주고
                    _placeRecommendations.value = places
                    _transitAverages.value = emptyMap()
                    _transitBreakdowns.value = emptyMap()

                    // 이미지/리뷰 데이터를 비동기로 보강
                    Log.d("ChatVM", "이미지·리뷰 보강 시작...")
                    val enriched = placeRepository.enrichPlacesWithDetails(places)
                    _placeRecommendations.value = enriched
                    enriched.forEachIndexed { i, p ->
                        Log.d("ChatVM", "  보강[$i]: ${p.name} → 이미지 ${p.imageUrls.size}장, 리뷰 ${p.reviewCount}개, 별점 ${p.rating}")
                    }

                    // 대중교통 계산은 지역(중간 지점) 기준으로 이미 위에서 한 번 수행했으므로 생략
                }
                Log.d("ChatVM", "━━━ 장소 추천 완료 ━━━")
            } catch (e: Exception) {
                Log.e("ChatVM", "장소 추천 오류: ${e.message}", e)
                _placeError.value = "오류가 발생했습니다: ${e.message}"
            } finally {
                _isLoadingPlaces.value = false
            }
        }
    }

    fun clearPlaceRecommendations() {
        _placeRecommendations.value = emptyList()
        _placeError.value = null
        _transitAverages.value = emptyMap()
        _transitBreakdowns.value = emptyMap()
        _regionTransitAvg.value = null
        _regionTransitBreakdown.value = emptyList()
        _recommendedRegionName.value = ""
        _refreshNotice.value = null
        // 세션 종료 — 다음 오픈 시 새 결과 원하도록 누적 상태 초기화.
        // 찜(savedPlaces) 은 채팅방 단위 선호이므로 유지.
        shownPlaceKeys.clear()
        searchRadiusMeters = 800
        recentRefreshTimes.clear()
        // 지역 모드도 초기화 — 다음 오픈 시 기본(중간 지점)으로 시작
        _searchRegion.value = null
        lastKeywords = null
        lastTransitParticipants = emptyList()
    }

    // ─── 지역 직접 검색 (다른 지역으로 검색 / 중간 지점 복귀) ─────────────────

    /**
     * "다른 지역으로 검색" — 사용자가 고른 지역을 기준으로 인기 장소를 가져온다.
     * 중간 지점 계산을 건너뛰고, 해당 지역 + 현재 필터(카테고리) 로 Naver Local Search.
     *
     * @param chatId     현재 채팅방 ID (시그니처 일관성 유지용, 지역 모드에서는 미사용)
     * @param regionName "서울 용산구" 같이 시/도 + 시/군/구 공백 조합
     * @param keywords   현재 적용 중인 필터(카테고리). null/empty 면 "카페" 기본.
     */
    fun searchByRegion(
        @Suppress("UNUSED_PARAMETER") chatId: String,
        regionName: String,
        keywords: List<String>? = null
    ) {
        val region = regionName.trim()
        if (region.isBlank()) return
        _searchRegion.value = region
        // keywords 를 명시적으로 null 로 넘기면 마지막 필터를 그대로 이어 쓴다
        val effective = keywords ?: lastKeywords
        lastKeywords = effective
        searchPopularPlacesInRegion(region, effective)
    }

    /**
     * "중간 지점으로 돌아가기" — 지역 모드를 해제하고 참여자 중간 지점 기준으로 재추천.
     */
    fun returnToMidpoint(chatId: String) {
        _searchRegion.value = null
        // 세션 초기화 후 중간 지점 기준 추천
        shownPlaceKeys.clear()
        searchRadiusMeters = 800
        recommendMeetingPlaces(chatId = chatId, keywords = lastKeywords, isRefresh = false)
    }

    /** 지역 기준 인기 장소 검색 실제 구현. recommendMeetingPlaces / searchByRegion 에서 공유. */
    private fun searchPopularPlacesInRegion(regionName: String, keywords: List<String>?) {
        viewModelScope.launch {
            _isLoadingPlaces.value = true
            _placeError.value = null
            _refreshNotice.value = null
            _placeRecommendations.value = emptyList()
            _transitAverages.value = emptyMap()
            _transitBreakdowns.value = emptyMap()
            _regionTransitAvg.value = null
            _regionTransitBreakdown.value = emptyList()
            _recommendedRegionName.value = regionName
            try {
                val searchKeywords = if (!keywords.isNullOrEmpty()) keywords else listOf("카페")
                val places = placeRepository.searchPlacesInRegion(
                    regionName = regionName,
                    keywords = searchKeywords,
                    limit = 100
                )
                if (places.isEmpty()) {
                    _placeError.value = "'${regionName}' 에서 장소를 찾지 못했어요.\n다른 지역이나 카테고리로 다시 시도해주세요."
                } else {
                    _placeRecommendations.value = places
                    _refreshNotice.value =
                        "📍 ${regionName} 의 인기 ${searchKeywords.joinToString("·")} TOP ${places.size}"
                    // 이미지·리뷰 수 보강 — 보강 후 리뷰 수 기준으로 재정렬해 "인기 순위" 에 가깝게
                    val enriched = placeRepository.enrichPlacesWithDetails(places)
                    // 리뷰가 있는 업체를 우선, 같으면 원 순서(정확도) 유지
                    val ranked = enriched.withIndex()
                        .sortedWith(
                            compareByDescending<IndexedValue<PlaceResult>> { it.value.reviewCount }
                                .thenBy { it.index }
                        )
                        .map { it.value }
                    _placeRecommendations.value = ranked
                    // 최종 순위가 확정된 뒤 배너도 실제 개수로 갱신
                    _refreshNotice.value =
                        "📍 ${regionName} 의 인기 ${searchKeywords.joinToString("·")} TOP ${ranked.size}"

                    // 선택한 지역의 중심 좌표를 구해 대중교통 시간 한 번 계산
                    if (lastTransitParticipants.isNotEmpty()) {
                        val regionCoord = withContext(Dispatchers.IO) {
                            placeRepository.geocodeAddress(regionName)
                        }
                        if (regionCoord != null) {
                            computeRegionTransit(regionCoord, lastTransitParticipants)
                            Log.d("ChatVM", "지역 '$regionName' 좌표: (${regionCoord.lat}, ${regionCoord.lng}) → 대중교통 계산 시작")
                        } else {
                            Log.w("ChatVM", "지역 '$regionName' 좌표 변환 실패 — 대중교통 표시 생략")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatVM", "지역 검색 오류: ${e.message}", e)
                _placeError.value = "지역 검색 중 오류: ${e.message}"
            } finally {
                _isLoadingPlaces.value = false
            }
        }
    }

    // ─── 재추천 / 조건 시트 / 찜 UI 래퍼 ──────────────────────────────────────

    /**
     * "다시 추천받기" 버튼 탭 진입점.
     * - 항상 isRefresh=true 로 위임해 기존 본 장소를 제외한 결과를 받는다.
     * - 30초 내 3회 연속 탭 감지 시 [showConditionSheet] 을 true 로 켜 조건 시트를 노출.
     */
    fun onRefreshPlaceRecommendations(chatId: String, keywords: List<String>? = null) {
        val now = System.currentTimeMillis()
        recentRefreshTimes.removeAll { now - it > 30_000 }
        recentRefreshTimes.add(now)
        if (recentRefreshTimes.size >= 3) {
            _showConditionSheet.value = true
            recentRefreshTimes.clear()
            // 시트를 띄우되, 일반 재추천도 함께 수행 — 기다림 지연 없음.
        }
        recommendMeetingPlaces(chatId = chatId, keywords = keywords, isRefresh = true)
    }

    fun openConditionSheet() { _showConditionSheet.value = true }
    fun closeConditionSheet() { _showConditionSheet.value = false }

    /**
     * 조건 바꾸기 시트에서 확정 버튼을 누르면 호출.
     * @param radius 사용자가 슬라이더로 고른 반경(m).
     * @param keywords 분위기/카테고리 태그 목록. 비어있으면 기본값 사용.
     * @param includeShown true면 "이미 본 곳 다시 포함" — 이번 한 번에 한해 제외 집합 무시.
     */
    fun applyConditionSheet(
        chatId: String,
        radius: Int,
        keywords: List<String>,
        includeShown: Boolean
    ) {
        _showConditionSheet.value = false
        if (includeShown) {
            // 제외 집합을 통째로 비움 — 다음 "다시 추천" 부터 다시 누적.
            shownPlaceKeys.clear()
        }
        recommendMeetingPlaces(
            chatId = chatId,
            keywords = keywords.ifEmpty { null },
            isRefresh = true,   // 반경/조건 바꿨다고 처음부터 다시 시작하지는 않음
            radiusOverride = radius,
            includeShown = includeShown
        )
    }

    /** 결과 상단 배너에 보여줄 "뭐가 바뀌었는지" 한 줄 문구. 변화 없으면 null. */
    private fun buildRefreshNotice(
        isRefresh: Boolean,
        excludedCount: Int,
        initialRadius: Int,
        usedRadius: Int,
        includeShown: Boolean,
        poolExhausted: Boolean
    ): String? {
        if (!isRefresh) return null
        val parts = mutableListOf<String>()
        when {
            includeShown -> parts += "이미 본 곳도 포함"
            excludedCount > 0 -> parts += "이미 본 ${excludedCount}곳 제외"
        }
        if (usedRadius > initialRadius) parts += "반경 ${formatRadius(usedRadius)}로 넓힘"
        if (poolExhausted) parts += "처음부터 다시"
        return if (parts.isEmpty()) null else parts.joinToString(" · ")
    }

    private fun formatRadius(m: Int): String =
        if (m >= 1000) "${"%.1f".format(m / 1000.0).trimEnd('0').trimEnd('.')}km" else "${m}m"

    fun dismissRefreshNotice() { _refreshNotice.value = null }

    /** 장소 찜/해제 토글. UI 즉시 반영 + Firestore 영구 저장. */
    fun toggleSavePlace(place: PlaceResult) {
        val userId = currentUserIdOrNull ?: return
        val key = placeKey(place)
        val current = _savedPlaces.value
        val already = current.any { placeKey(it) == key }
        val docRef = FirebaseFirestore.getInstance()
            .collection("users").document(userId)
            .collection("savedMeetingPlaces").document(savedPlaceDocId(place))

        if (already) {
            // 찜 해제 — UI 즉시 반영 후 Firestore 삭제
            _savedPlaces.value = current.filterNot { placeKey(it) == key }
            _savedPlaceKeys.value = _savedPlaceKeys.value - key
            viewModelScope.launch {
                try { docRef.delete().await() }
                catch (e: Exception) { Log.e("ChatVM", "찜 해제 저장 실패: ${e.message}") }
            }
        } else {
            // 찜 추가 — UI 즉시 반영 후 Firestore 저장
            _savedPlaces.value = current + place
            _savedPlaceKeys.value = _savedPlaceKeys.value + key
            viewModelScope.launch {
                try {
                    docRef.set(mapOf(
                        "name"        to place.name,
                        "address"     to place.address,
                        "category"    to place.category,
                        "phone"       to place.phone,
                        "lat"         to place.lat,
                        "lng"         to place.lng,
                        "imageUrl"    to place.imageUrl,
                        "imageUrls"   to place.imageUrls,
                        "reviewCount" to place.reviewCount,
                        "placeId"     to place.placeId,
                        "savedAt"     to FieldValue.serverTimestamp()
                    )).await()
                } catch (e: Exception) {
                    Log.e("ChatVM", "찜 저장 실패: ${e.message}")
                }
            }
        }
    }

    fun isPlaceSaved(place: PlaceResult): Boolean =
        _savedPlaceKeys.value.contains(placeKey(place))

    // ─── 대중교통 시간 계산 ───────────────────────────────────────────────────
    private data class TransitParticipant(
        val userId: String,
        val name: String,
        val profileImage: String,
        val location: String,
        val lat: Double,
        val lng: Double
    )

    /**
     * 각 장소에 대해 참여자별 대중교통 소요시간을 계산해 _transitAverages/_transitBreakdowns 에 반영.
     * 네트워크 호출이 많아질 수 있어 IO 디스패처에서 병렬 수행한다.
     */
    private fun computeTransitDurations(
        places: List<PlaceResult>,
        participants: List<TransitParticipant>
    ) {
        if (places.isEmpty() || participants.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val averages = mutableMapOf<String, Int>()
            val breakdowns = mutableMapOf<String, List<TransitUserInfo>>()

            for (place in places) {
                if (place.lat == 0.0 && place.lng == 0.0) continue
                val key = if (place.placeId.isNotBlank()) {
                    "id:${place.placeId}"
                } else {
                    "ll:${place.lat},${place.lng}:${place.name}"
                }

                val userRows = participants.map { p ->
                    val min = try {
                        placeRepository.fetchTransitDurationMinutes(
                            startLat = p.lat,
                            startLng = p.lng,
                            goalLat = place.lat,
                            goalLng = place.lng
                        )
                    } catch (e: Exception) {
                        Log.w("ChatVM", "transit ${p.name} → ${place.name} 실패: ${e.message}")
                        null
                    }
                    TransitUserInfo(
                        userId = p.userId,
                        userName = p.name,
                        profileImage = p.profileImage,
                        location = p.location,
                        minutes = min ?: -1
                    )
                }

                val validMins = userRows.map { it.minutes }.filter { it >= 0 }
                if (validMins.isNotEmpty()) {
                    averages[key] = validMins.average().toInt()
                }
                breakdowns[key] = userRows

                // 장소 하나 계산될 때마다 UI 업데이트 (점진적 표시)
                _transitAverages.value = averages.toMap()
                _transitBreakdowns.value = breakdowns.toMap()
            }
            Log.d("ChatVM", "대중교통 시간 계산 완료: ${averages.size}/${places.size} 장소")
        }
    }

    /**
     * 중간 지점(centroid) 기준으로 참여자별 대중교통 소요시간을 딱 한 번 계산.
     * 가게별로 계산하는 computeTransitDurations 대신 이 함수를 사용해 로딩 속도를 개선한다.
     */
    private fun computeRegionTransit(
        centroid: com.bugzero.meety.data.repository.LatLng,
        participants: List<TransitParticipant>
    ) {
        if (participants.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val userRows = participants.map { p ->
                val min = try {
                    placeRepository.fetchTransitDurationMinutes(
                        startLat = p.lat,
                        startLng = p.lng,
                        goalLat = centroid.lat,
                        goalLng = centroid.lng
                    )
                } catch (e: Exception) {
                    Log.w("ChatVM", "region transit ${p.name} 실패: ${e.message}")
                    null
                }
                TransitUserInfo(
                    userId = p.userId,
                    userName = p.name,
                    profileImage = p.profileImage,
                    location = p.location,
                    minutes = min ?: -1
                )
            }
            val validMins = userRows.map { it.minutes }.filter { it >= 0 }
            _regionTransitAvg.value = if (validMins.isNotEmpty()) validMins.average().toInt() else null
            _regionTransitBreakdown.value = userRows
            Log.d("ChatVM", "지역 대중교통 계산 완료: 평균 ${_regionTransitAvg.value}분")
        }
    }

    fun transferLeaderOnly(chatId: String, newLeaderId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val now = com.google.firebase.Timestamp.now()

                // 1. 참여자 목록 가져와서 순서 바꾸기 (새 팀장을 맨 앞으로)
                val chatDoc = db.collection("chats").document(chatId).get().await()
                val participants = chatDoc.get("participants") as? List<String> ?: return@launch
                val newParticipants = mutableListOf(newLeaderId) + participants.filter { it != newLeaderId }

                // 2. DB 업데이트
                db.collection("chats").document(chatId).update("participants", newParticipants).await()

                // 3. 시스템 메시지 추가
                val newLeaderName = _participants.value.find { it.userId == newLeaderId }?.name ?: "새 팀장"
                val systemMsg = mapOf(
                    "senderId" to "system",
                    "content" to "${newLeaderName}님이 팀장이 되었습니다.",
                    "type" to "system",
                    "createdAt" to now
                )
                db.collection("chats").document(chatId).collection("messages").add(systemMsg).await()
                db.collection("chats").document(chatId).update(mapOf("lastMessage" to "${newLeaderName}님이 팀장이 되었습니다.", "lastMessageAt" to now)).await()

                // 4. UI 갱신 및 성공 콜백
                loadParticipants(chatId)
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                _errorMessage.value = "양도 중 오류가 발생했습니다: ${e.message}"
            }
        }
    }

    fun kickMember(chatId: String, targetUserId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val userDoc = db.collection("users").document(targetUserId).get().await()
                val targetName = userDoc.getString("name") ?: "멤버"

                val chatRef = db.collection("chats").document(chatId)

                // 1) 채팅방(chats) 참여자 목록에서 삭제
                chatRef.update("participants", FieldValue.arrayRemove(targetUserId)).await()

                // ✨ 2) 팀(teams) 컬렉션에서도 해당 멤버 삭제 (필드명 memberIds 로 수정!)
                val chatDoc = chatRef.get().await()
                val teamId = chatDoc.getString("teamId") ?: chatId
                try {
                    db.collection("teams").document(teamId).update("memberIds", FieldValue.arrayRemove(targetUserId)).await()
                } catch (e: Exception) {
                    // 팀 문서가 없는 일반 단체톡일 경우 에러 무시하고 패스
                }

                // 3) 시스템 메시지 전송
                val systemMsg = mapOf(
                    "senderId" to "system",
                    "content" to "${targetName}님이 내보내졌습니다.",
                    "type" to "system",
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
                chatRef.collection("messages").add(systemMsg).await()

                loadParticipants(chatId) // UI 새로고침
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                _errorMessage.value = "내보내기 실패: ${e.message}"
            }
        }
    }

    fun inviteFriendsToChat(chatId: String, selectedFriends: List<UserProfileData>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val newParticipantIds = selectedFriends.map { it.userId }
                val chatDocRef = db.collection("chats").document(chatId)

                // 1. 현재 채팅방 정보 가져오기
                val chatDoc = chatDocRef.get().await()
                val currentType = chatDoc.getString("type") ?: "group"

                // 2. 업데이트할 내용 준비 (참여자 추가)
                val inviteNow = com.google.firebase.Timestamp.now()
                val updates = mutableMapOf<String, Any>(
                    "participants" to com.google.firebase.firestore.FieldValue.arrayUnion(*newParticipantIds.toTypedArray())
                )
                // ✨ 신규 초대 멤버는 이 시점부터 메시지를 볼 수 있도록 memberJoinedAt 도 함께 set.
                //    재초대(이전에 나갔다 다시 들어오는 경우)도 자동으로 새 시각으로 덮어쓰기 됨.
                for (newId in newParticipantIds) {
                    updates["memberJoinedAt.$newId"] = inviteNow
                }

                // ✨ 3. 만약 1:1(direct) 채팅방이었다면 단체톡(group)으로 타입 변경!
                if (currentType == "direct") {
                    updates["type"] = "group"
                    updates["emoji"] = "👥"
                }

                // DB 업데이트 실행
                chatDocRef.update(updates).await()

                // 4. 시스템 메시지 추가: "OOO님이 초대되었습니다."
                val names = selectedFriends.joinToString(", ") { it.name }
                val systemMsg = mapOf(
                    "senderId" to "system",
                    "content" to "${names}님이 초대되었습니다.",
                    "type" to "system",
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
                chatDocRef.collection("messages").add(systemMsg).await()

                // 5. UI 갱신
                loadParticipants(chatId)
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                _errorMessage.value = "초대 실패: ${e.message}"
            }
        }
    }

    // ── 팀원 자동 매칭 / 초대 메서드 ──────────────────────────────

    fun loadPendingInvitations() {
        val userId = currentUserIdOrNull ?: return
        viewModelScope.launch {
            repository.observePendingInvitations(userId)
                .catch { e ->
                    // 초대 로드 실패는 채팅 목록 전체 에러로 전파하지 않음 (조용히 처리)
                    android.util.Log.e("ChatVM", "초대 목록 로드 실패: ${e.message}")
                    _pendingInvitations.value = emptyList()
                }
                .collect { _pendingInvitations.value = it }
        }
    }

    fun loadMatchCandidates(teamId: String) {
        viewModelScope.launch {
            _isLoadingCandidates.value = true
            try {
                _matchCandidates.value = repository.loadMatchCandidates(teamId)
            } catch (e: Exception) {
                _errorMessage.value = "후보자 목록 로드 실패: ${e.message}"
            } finally {
                _isLoadingCandidates.value = false
            }
        }
    }

    fun sendTeamInvitations(
        teamId: String,
        chatId: String,
        teamName: String,
        teamEmoji: String,
        toUserIds: List<String>,
        onSuccess: () -> Unit
    ) {
        val fromUserId = currentUserIdOrNull ?: return
        viewModelScope.launch {
            try {
                toUserIds.forEach { toUserId ->
                    repository.sendTeamInvitation(
                        teamId     = teamId,
                        chatId     = chatId,
                        teamName   = teamName,
                        teamEmoji  = teamEmoji,
                        fromUserId = fromUserId,
                        toUserId   = toUserId
                    )
                }
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                _errorMessage.value = "초대 전송 실패: ${e.message}"
            }
        }
    }

    fun acceptInvitation(invitationId: String, teamId: String, chatId: String, onSuccess: () -> Unit) {
        val userId = currentUserIdOrNull ?: return
        viewModelScope.launch {
            try {
                repository.acceptInvitation(invitationId, userId, teamId, chatId)
                loadChatList()
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                _errorMessage.value = "수락 실패: ${e.message}"
            }
        }
    }

    fun rejectInvitation(invitationId: String) {
        viewModelScope.launch {
            try {
                repository.rejectInvitation(invitationId)
            } catch (e: Exception) {
                _errorMessage.value = "거절 실패: ${e.message}"
            }
        }
    }

    fun selectInvitation(invitation: TeamInvitation) {
        _selectedInvitation.value = invitation
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val doc = db.collection("teams").document(invitation.teamId).get().await()
                if (doc.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    _selectedInvitationTeam.value = Team(
                        teamId           = doc.id,
                        leaderId         = doc.getString("leaderId") ?: "",
                        memberIds        = (doc.get("memberIds") as? List<String>) ?: emptyList(),
                        mbtiTags         = (doc.get("mbtiTags") as? List<String>) ?: emptyList(),
                        tags             = (doc.get("tags") as? List<String>) ?: emptyList(),
                        teamProfileImage = doc.getString("teamProfileImage") ?: "",
                        teamName         = doc.getString("teamName") ?: invitation.teamName,
                        description      = doc.getString("description") ?: "",
                        status           = doc.getString("status") ?: "active"
                    )
                }
            } catch (e: Exception) {
                _errorMessage.value = "팀 정보 로드 실패"
            }
        }
    }

    fun clearSelectedInvitation() {
        _selectedInvitation.value = null
        _selectedInvitationTeam.value = null
    }


    fun uploadImageAndSendMessage(chatId: String, uri: android.net.Uri) {
        val userId = currentUserIdOrNull ?: return
        viewModelScope.launch {
            _isSending.value = true
            try {
                // 1. Storage에 사진 올리기
                val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
                val imageRef = storageRef.child("chat_images/${chatId}/${UUID.randomUUID()}.jpg")

                imageRef.putFile(uri).await()
                val downloadUrl = imageRef.downloadUrl.await().toString()

                // 2. Firestore에 메시지 저장
                val db = FirebaseFirestore.getInstance()
                val now = com.google.firebase.Timestamp.now()
                val userDoc = db.collection("users").document(userId).get().await()
                val myName = userDoc.getString("name") ?: "알 수 없음"
                val myProfile = userDoc.getString("profileImageUrl") ?: ""

                val messageData = mapOf(
                    "id" to UUID.randomUUID().toString(),
                    "chatId" to chatId,
                    "senderId" to userId,
                    "senderName" to myName,
                    "senderProfileImage" to myProfile,
                    "content" to "사진을 보냈습니다.",
                    "imageUrl" to downloadUrl, // ✨ 사진 주소 저장
                    "type" to "image",         // ✨ 타입: image
                    "createdAt" to now
                )

                db.collection("chats").document(chatId).collection("messages").add(messageData).await()
                db.collection("chats").document(chatId).update(
                    mapOf("lastMessage" to "사진을 보냈습니다.", "lastMessageAt" to now)
                ).await()

            } catch (e: Exception) {
                Log.e("ChatViewModel", "사진 전송 실패: ${e.message}")
            } finally {
                _isSending.value = false
            }
        }
    }
}
