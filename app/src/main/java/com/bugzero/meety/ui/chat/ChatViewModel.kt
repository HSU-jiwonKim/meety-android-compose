package com.bugzero.meety.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugzero.meety.data.repository.ChatRepository
import com.bugzero.meety.data.repository.FirebaseChatRepository
import com.bugzero.meety.ui.team.FirebaseTeamRepository
import com.bugzero.meety.ui.team.ReceivedLikeItem
import com.bugzero.meety.ui.team.Team
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
    private val teamRepository: FirebaseTeamRepository = FirebaseTeamRepository()
) : ViewModel() {

    private var likesListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var messagesJob: kotlinx.coroutines.Job? = null  // 채팅방 전환 시 이전 구독 취소용
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

    init { refreshForAuthState() }

    fun refreshForAuthState() {
        val userId = currentUserIdOrNull
        if (userId.isNullOrBlank()) { clearForLoggedOutState(); return }
        likesListener?.remove(); likesListener = null
        _chatList.value = emptyList(); _requestList.value = emptyList(); _errorMessage.value = null
        loadChatList(); loadRequestList(); saveFcmToken(); loadPendingInvitations()
    }

    private fun clearForLoggedOutState() {
        likesListener?.remove(); likesListener = null
        _chatList.value = emptyList(); _requestList.value = emptyList(); _messages.value = emptyList()
        _participants.value = emptyList(); _friendList.value = emptyList(); _selectedUserProfile.value = null
        _isLoading.value = false; _isSending.value = false; _isLoadingFriends.value = false; _isLoadingProfile.value = false
        _errorMessage.value = null; _roomName.value = ""; _currentTeamId.value = ""; _currentChatType.value = ""
        _pendingInvitations.value = emptyList(); _matchCandidates.value = emptyList()
        _selectedInvitation.value = null; _selectedInvitationTeam.value = null
    }

    fun clearError() { _errorMessage.value = null }

    fun enterChatRoom(chatId: String, roomName: String) {
        _roomName.value = roomName
        _messages.value = emptyList()      // 이전 채팅방 메시지 즉시 초기화
        _participants.value = emptyList()  // 이전 채팅방 참여자 즉시 초기화
        _currentTeamId.value = ""
        _currentChatType.value = ""
        observeMessages(chatId)
        loadParticipants(chatId)
    }

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
        // loadChatList() 불필요 — observeChatList 실시간 리스너가 자동 갱신
        teamRepository.acceptReceivedLike(likeId, onSuccess = { loadRequestList() }, onFailure = { _errorMessage.value = it })
    }

    fun rejectRequest(likeId: String) {
        teamRepository.rejectReceivedLike(likeId, onSuccess = { loadRequestList() }, onFailure = { _errorMessage.value = it })
    }

    fun observeMessages(chatId: String) {
        messagesJob?.cancel()  // 이전 채팅방 구독 취소
        messagesJob = viewModelScope.launch {
            repository.observeMessages(chatId)
                .catch { _errorMessage.value = "메시지 로드 실패" }
                .collect { _messages.value = it }
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

    fun formatTime(timestamp: com.google.firebase.Timestamp?): String {
        timestamp ?: return ""
        val date = timestamp.toDate()
        return SimpleDateFormat("HH:mm", Locale.KOREA).format(date)
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

                // 채팅 문서에서 실제 teamId, chatType 저장
                val resolvedTeamId = chatDoc.getString("teamId")?.takeIf { it.isNotBlank() } ?: chatId
                _currentTeamId.value = resolvedTeamId
                _currentChatType.value = chatType

                @Suppress("UNCHECKED_CAST")
                val participantIds = chatDoc.get("participants") as? List<String> ?: return@launch

                // 병렬 fetch — 순차 N번 → async/awaitAll
                val rawItems = kotlinx.coroutines.coroutineScope {
                    participantIds.mapIndexed { index, pUserId ->
                        async {
                            val userDoc = db.collection("users").document(pUserId).get().await()
                            val shouldShowLeader = isTeam && index == 0
                            ParticipantItem(
                                userId = pUserId,
                                name = userDoc.getString("name") ?: "알 수 없음",
                                emoji = if (shouldShowLeader) "👑" else "👤",
                                isLeader = shouldShowLeader,
                                profileImage = (userDoc.get("profileImages") as? List<*>)?.firstOrNull()?.toString() ?: "",
                                isFriend = friendIds.contains(pUserId) || pUserId == myId
                            )
                        }
                    }.awaitAll()
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

    fun createOrGetDirectChat(friend: UserProfileData, onSuccess: (String, String) -> Unit, onFailure: (String) -> Unit) {
        val myId = currentUserIdOrNull ?: return
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()

                // 1. 현재 '진짜 1:1 방(direct)'이면서 '나'와 '이 친구' 딱 둘만 있는 방을 검색합니다.
                val snapshot = db.collection("chats")
                    .whereEqualTo("type", "direct")
                    .whereArrayContains("participants", myId)
                    .get()
                    .await()

                var existingChatId: String? = null
                for (doc in snapshot.documents) {
                    val participants = doc.get("participants") as? List<String> ?: emptyList()
                    // 참여자가 딱 2명이고, 그 중 한 명이 선택한 친구라면 그 방이 진짜 1:1 방!
                    if (participants.size == 2 && participants.contains(friend.userId)) {
                        existingChatId = doc.id
                        break
                    }
                }

                if (existingChatId != null) {
                    // 2-A. 기존에 온전한 1:1 방이 남아있다면 그 방으로 이동
                    withContext(Dispatchers.Main) { onSuccess(existingChatId, friend.name) }
                } else {
                    // 2-B. 방이 없거나, 예전 방이 단체톡으로 진화해버렸다면 완전 새로운 1:1 방 생성!
                    val newChatId = "direct_${UUID.randomUUID()}" // 고정 ID 대신 랜덤 ID 사용
                    val now = com.google.firebase.Timestamp.now()
                    val ids = listOf(myId, friend.userId).sorted()

                    db.collection("chats").document(newChatId).set(
                        mapOf(
                            "type" to "direct",
                            "participants" to ids,
                            "teamName" to friend.name,
                            "emoji" to "💬",
                            "createdAt" to now,
                            "lastMessage" to "",
                            "lastMessageAt" to now
                        )
                    ).await()

                    withContext(Dispatchers.Main) { onSuccess(newChatId, friend.name) }
                }
            } catch (e: Exception) {
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
                        "lastMessageAt" to now
                    )
                ).await()

                withContext(Dispatchers.Main) { onSuccess(chatId, finalRoomName) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onFailure("그룹 생성 실패") }
            }
        }
    }

    /**
     * 팀장 여부를 반환. UI에서 나가기 다이얼로그 분기에 활용.
     * (participants[0] == currentUser 이면 팀장)
     */
    fun isCurrentUserLeader(): Boolean {
        val myId = currentUserIdOrNull ?: return false
        return _participants.value.firstOrNull { it.isLeader }?.userId == myId
    }

    fun leaveChatRoom(chatId: String, onSuccess: () -> Unit = {}, onFailure: (String) -> Unit = {}) {
        val userId = currentUserIdOrNull ?: return
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val chatRef = db.collection("chats").document(chatId)

                val chatDoc = chatRef.get().await()
                @Suppress("UNCHECKED_CAST")
                val currentParticipants = (chatDoc.get("participants") as? List<String>) ?: emptyList()
                // teamId == chatId (팀 채팅은 생성 시 동일하게 설정됨)
                val teamId = chatDoc.getString("teamId") ?: chatId
                val now = com.google.firebase.Timestamp.now()

                if (currentParticipants.size <= 1) {
                    // ── 마지막 멤버 나가기: chats + teams 문서 모두 삭제 ──
                    chatRef.delete().await()

                    // teams 문서 삭제 (좀비 레코드 방지)
                    try { db.collection("teams").document(teamId).delete().await() } catch (e: Exception) {
                        android.util.Log.e("ChatVM", "teams 문서 삭제 실패: ${e.message}")
                    }
                    // 내 teamIds에서도 제거
                    try {
                        db.collection("users").document(userId)
                            .update("teamIds", FieldValue.arrayRemove(teamId)).await()
                    } catch (e: Exception) { }

                } else {
                    // ── 일반 나가기 ──
                    val userDoc = db.collection("users").document(userId).get().await()
                    val myName = userDoc.getString("name") ?: "알 수 없음"

                    val systemMsg = mapOf(
                        "senderId" to "system", "content" to "${myName}님이 나갔습니다.",
                        "type" to "system", "createdAt" to now
                    )
                    chatRef.collection("messages").add(systemMsg).await()
                    chatRef.update(mapOf(
                        "lastMessage"   to "${myName}님이 나갔습니다.",
                        "lastMessageAt" to now,
                        "participants"  to FieldValue.arrayRemove(userId)
                    )).await()

                    // teams.memberIds에서 제거
                    try {
                        db.collection("teams").document(teamId)
                            .update("memberIds", FieldValue.arrayRemove(userId)).await()
                    } catch (e: Exception) { }

                    // 내 teamIds에서도 제거
                    try {
                        db.collection("users").document(userId)
                            .update("teamIds", FieldValue.arrayRemove(teamId)).await()
                    } catch (e: Exception) { }
                }

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

    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _isLoadingProfile.value = true
            try {
                val doc = FirebaseFirestore.getInstance().collection("users").document(userId).get().await()
                _selectedUserProfile.value = UserProfileData(userId = doc.id, name = doc.getString("name") ?: "", mbti = doc.getString("mbti") ?: "", department = doc.getString("department") ?: "", age = doc.getLong("age")?.toInt() ?: 0, height = doc.getLong("height")?.toInt() ?: 0, location = doc.getString("location") ?: "", bio = doc.getString("bio") ?: "")
            } finally { _isLoadingProfile.value = false }
        }
    }

    fun clearUserProfile() { _selectedUserProfile.value = null }

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
                val updates = mutableMapOf<String, Any>(
                    "participants" to com.google.firebase.firestore.FieldValue.arrayUnion(*newParticipantIds.toTypedArray())
                )

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
}