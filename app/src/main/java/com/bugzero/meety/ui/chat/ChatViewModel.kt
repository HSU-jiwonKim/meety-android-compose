package com.bugzero.meety.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugzero.meety.data.repository.ChatRepository
import com.bugzero.meety.data.repository.FirebaseChatRepository
import com.bugzero.meety.ui.team.FirebaseTeamRepository
import com.bugzero.meety.ui.team.ReceivedLikeItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.bugzero.meety.ui.chat.UserProfileData

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

class ChatViewModel(
    private val repository: ChatRepository = FirebaseChatRepository(),
    private val teamRepository: FirebaseTeamRepository = FirebaseTeamRepository()
) : ViewModel() {

    private var likesListener: com.google.firebase.firestore.ListenerRegistration? = null
    private val currentUserId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // ── 채팅 목록 ──────────────────────────────────────────────
    private val _chatList = MutableStateFlow<List<ChatPreview>>(emptyList())
    val chatList: StateFlow<List<ChatPreview>> = _chatList.asStateFlow()

    // ── 요청 목록 ──────────────────────────────────────────────
    private val _requestList = MutableStateFlow<List<ReceivedLikeItem>>(emptyList())
    val requestList: StateFlow<List<ReceivedLikeItem>> = _requestList.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ── 현재 채팅방 이름 ─────────────────────────────────────────
    private val _roomName = MutableStateFlow("")
    val roomName: StateFlow<String> = _roomName.asStateFlow()

    // ── 채팅방 메시지 ───────────────────────────────────────────
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    // ── 대화상대 목록 ─────────────────────────────────────────────
    private val _participants = MutableStateFlow<List<ParticipantItem>>(emptyList())
    val participants: StateFlow<List<ParticipantItem>> = _participants.asStateFlow()

    // ── 선택된 유저 프로필 ─────────────────────────────────────────
    private val _selectedUserProfile = MutableStateFlow<UserProfileData?>(null)
    val selectedUserProfile: StateFlow<UserProfileData?> = _selectedUserProfile.asStateFlow()

    private val _isLoadingProfile = MutableStateFlow(false)
    val isLoadingProfile: StateFlow<Boolean> = _isLoadingProfile.asStateFlow()

    // ── 친구(매칭된 유저) 목록 ──────────────────────────────────────
    private val _friendList = MutableStateFlow<List<UserProfileData>>(emptyList())
    val friendList: StateFlow<List<UserProfileData>> = _friendList.asStateFlow()

    private val _isLoadingFriends = MutableStateFlow(false)
    val isLoadingFriends: StateFlow<Boolean> = _isLoadingFriends.asStateFlow()

    init {
        loadChatList()
        loadRequestList()
        saveFcmToken()
    }

    // ── 채팅방 진입 ───────────────────────────────────────────────
    fun enterChatRoom(chatId: String, roomName: String) {
        _roomName.value = roomName
        observeMessages(chatId)
        loadParticipants(chatId)
    }

    // ── 채팅 목록 실시간 구독 ──────────────────────────────────────
    fun loadChatList() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                repository.observeChatList(currentUserId)
                    .catch { e ->
                        android.util.Log.e("ChatVM", "채팅 목록 에러: ${e.message}")
                        _errorMessage.value = "채팅 목록을 불러오지 못했어요"
                        _isLoading.value = false
                    }
                    .collect { list ->
                        android.util.Log.d("ChatVM", "받은 채팅방 수: ${list.size}")
                        _chatList.value = list
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "채팅 목록을 불러오지 못했어요"
            }
        }
    }

    // ── 요청 목록 실시간 구독 ──────────────────────────────────────
    fun loadRequestList() {
        likesListener?.remove()
        likesListener = teamRepository.observeReceivedLikes(
            onUpdate = { list -> _requestList.value = list },
            onFailure = { message -> _errorMessage.value = message }
        )
    }

    // ── 요청 수락 ─────────────────────────────────────────────────
    fun acceptRequest(
        likeId: String,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        teamRepository.acceptReceivedLike(
            likeId = likeId,
            onSuccess = {
                loadRequestList()
                loadChatList()
                onSuccess()
            },
            onFailure = {
                _errorMessage.value = it
                onFailure(it)
            }
        )
    }

    // ── 요청 거절 ─────────────────────────────────────────────────
    fun rejectRequest(
        likeId: String,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        teamRepository.rejectReceivedLike(
            likeId = likeId,
            onSuccess = {
                loadRequestList()
                onSuccess()
            },
            onFailure = {
                _errorMessage.value = it
                onFailure(it)
            }
        )
    }

    // ── 메시지 실시간 구독 ─────────────────────────────────────────
    fun observeMessages(chatId: String) {
        viewModelScope.launch {
            repository.observeMessages(chatId)
                .catch { _errorMessage.value = "메시지를 불러오지 못했어요" }
                .collect { _messages.value = it }
        }
    }

    // ── 메시지 전송 ───────────────────────────────────────────────
    fun sendMessage(chatId: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            _isSending.value = true
            try {
                repository.sendMessage(
                    chatId = chatId,
                    senderId = currentUserId,
                    content = content.trim()
                )
            } catch (e: Exception) {
                _errorMessage.value = "메시지 전송에 실패했어요"
            } finally {
                _isSending.value = false
            }
        }
    }

    // ── 시간 포맷 ─────────────────────────────────────────────────
    fun formatTime(timestamp: com.google.firebase.Timestamp?): String {
        timestamp ?: return ""
        val date = timestamp.toDate()
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { time = date }
        return when {
            now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR) ->
                SimpleDateFormat("HH:mm", Locale.KOREA).format(date)
            now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                    now.get(Calendar.WEEK_OF_YEAR) == target.get(Calendar.WEEK_OF_YEAR) ->
                SimpleDateFormat("E", Locale.KOREA).format(date)
            else ->
                SimpleDateFormat("MM/dd", Locale.KOREA).format(date)
        }
    }

    // ── FCM 토큰 저장 ────────────────────────────────────────────
    fun saveFcmToken() {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (currentUserId.isEmpty()) return@addOnSuccessListener
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUserId)
                    .update("fcmToken", token)
            }
    }

    // ── 대화상대 목록 불러오기 ─────────────────────────────────────
    fun loadParticipants(chatId: String) {
        viewModelScope.launch {
            try {
                val chatDoc = FirebaseFirestore.getInstance()
                    .collection("chats")
                    .document(chatId)
                    .get()
                    .await()

                @Suppress("UNCHECKED_CAST")
                val participantIds = chatDoc.get("participants") as? List<String> ?: return@launch

                val items = mutableListOf<ParticipantItem>()
                participantIds.forEachIndexed { index, userId ->
                    try {
                        val userDoc = FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(userId)
                            .get()
                            .await()

                        val name = userDoc.getString("name") ?: "알 수 없음"
                        val profileImages = userDoc.get("profileImages") as? List<*>
                        val profileImage = profileImages?.firstOrNull()?.toString() ?: ""

                        items.add(
                            ParticipantItem(
                                userId = userId,
                                name = name,
                                emoji = if (index == 0) "👑" else "👤",
                                isLeader = index == 0,
                                profileImage = profileImage
                            )
                        )
                    } catch (e: Exception) {
                        items.add(
                            ParticipantItem(
                                userId = userId,
                                name = "알 수 없음",
                                emoji = "👤",
                                isLeader = index == 0
                            )
                        )
                    }
                }
                _participants.value = items
            } catch (e: Exception) {
                android.util.Log.e("ChatVM", "participants 불러오기 실패: ${e.message}")
            }
        }
    }

    // ── 채팅방 나가기 ─────────────────────────────────────────────
    fun leaveChatRoom(
        chatId: String,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()

                // 1. 시스템 메시지
                val systemMessage = mapOf(
                    "senderId" to "system",
                    "senderName" to "system",
                    "content" to "팀원이 나갔습니다.",
                    "type" to "system",
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
                db.collection("chats").document(chatId)
                    .collection("messages")
                    .add(systemMessage)
                    .await()

                // 2. 채팅방 participants에서 제거
                db.collection("chats").document(chatId)
                    .update("participants", FieldValue.arrayRemove(currentUserId))
                    .await()

                // 3. 팀 memberIds에서 제거
                db.collection("teams").document(chatId)
                    .update("memberIds", FieldValue.arrayRemove(currentUserId))
                    .await()

                // 4. 내 teamIds 배열에서 제거 (teamId 단일값 → teamIds 배열)
                db.collection("users").document(currentUserId)
                    .update("teamIds", FieldValue.arrayRemove(chatId))
                    .await()

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatVM", "채팅방 나가기 실패: ${e.message}")
                _errorMessage.value = "채팅방 나가기에 실패했어요"
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onFailure("채팅방 나가기에 실패했어요")
                }
            }
        }
    }

    // ── 유저 프로필 불러오기 ───────────────────────────────────────
    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _isLoadingProfile.value = true
            try {
                val doc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .get()
                    .await()

                _selectedUserProfile.value = UserProfileData(
                    userId = doc.id,
                    name = doc.getString("name") ?: "",
                    age = doc.getLong("age")?.toInt() ?: 0,
                    department = doc.getString("department") ?: "",
                    height = doc.getLong("height")?.toInt() ?: 0,
                    location = doc.getString("location") ?: "",
                    mbti = doc.getString("mbti") ?: "",
                    bio = doc.getString("bio") ?: "",
                    interests = doc.get("interests") as? List<String> ?: emptyList(),
                    foodLikes = doc.get("foodLikes") as? List<String> ?: emptyList(),
                    foodDislikes = doc.get("foodDislikes") as? List<String> ?: emptyList(),
                    profileImageUrl = ((doc.get("profileImages") as? List<*>)?.firstOrNull() as? String) ?: ""
                )
            } catch (e: Exception) {
                android.util.Log.e("ChatVM", "프로필 불러오기 실패: ${e.message}")
            } finally {
                _isLoadingProfile.value = false
            }
        }
    }

    fun clearUserProfile() {
        _selectedUserProfile.value = null
    }

    // ── 친구 목록 불러오기 ────────────────────────────────────────
    fun loadFriendList() {
        viewModelScope.launch {
            _isLoadingFriends.value = true
            try {
                val friendsSnapshot = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUserId)
                    .collection("friends")
                    .get()
                    .await()

                val friendIds = friendsSnapshot.documents
                    .mapNotNull { it.getString("friendUserId") }
                    .distinct()

                if (friendIds.isEmpty()) {
                    _friendList.value = emptyList()
                    _isLoadingFriends.value = false
                    return@launch
                }

                val friends = mutableListOf<UserProfileData>()
                friendIds.forEach { friendId ->
                    try {
                        val userDoc = FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(friendId)
                            .get()
                            .await()

                        if (userDoc.exists()) {
                            val profileImages = userDoc.get("profileImages") as? List<*>
                            val firstImage = profileImages?.firstOrNull()?.toString() ?: ""
                            friends.add(
                                UserProfileData(
                                    userId = userDoc.id,
                                    name = userDoc.getString("name") ?: "알 수 없음",
                                    age = userDoc.getLong("age")?.toInt() ?: 0,
                                    department = userDoc.getString("department") ?: "",
                                    height = userDoc.getLong("height")?.toInt() ?: 0,
                                    location = userDoc.getString("location") ?: "",
                                    mbti = userDoc.getString("mbti") ?: "",
                                    bio = userDoc.getString("bio") ?: "",
                                    interests = userDoc.get("interests") as? List<String> ?: emptyList(),
                                    foodLikes = userDoc.get("foodLikes") as? List<String> ?: emptyList(),
                                    foodDislikes = userDoc.get("foodDislikes") as? List<String> ?: emptyList(),
                                    profileImageUrl = firstImage
                                )
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ChatVM", "친구 프로필 불러오기 실패: ${e.message}")
                    }
                }
                _friendList.value = friends.sortedBy { it.name }
            } catch (e: Exception) {
                android.util.Log.e("ChatVM", "친구 목록 불러오기 실패: ${e.message}")
                _friendList.value = emptyList()
                _errorMessage.value = "친구 목록을 불러오지 못했어요"
            } finally {
                _isLoadingFriends.value = false
            }
        }
    }

    // ── 1:1 채팅방 생성 또는 조회 ──────────────────────────────────
    fun createOrGetDirectChat(
        friend: UserProfileData,
        onSuccess: (chatId: String, roomName: String) -> Unit,
        onFailure: (String) -> Unit = {}
    ) {
        if (currentUserId.isBlank()) { onFailure("로그인된 사용자가 없습니다."); return }
        if (friend.userId.isBlank()) { onFailure("상대 사용자 정보가 올바르지 않습니다."); return }

        viewModelScope.launch {
            try {
                val ids = listOf(currentUserId, friend.userId).sorted()
                val chatId = "direct_${ids[0]}_${ids[1]}"
                val chatRef = FirebaseFirestore.getInstance().collection("chats").document(chatId)
                val chatDoc = chatRef.get().await()

                if (!chatDoc.exists()) {
                    val now = com.google.firebase.Timestamp.now()
                    val chatData = hashMapOf(
                        "type" to "direct",
                        "participants" to ids,
                        "teamId" to "",
                        "teamName" to friend.name,
                        "emoji" to "💬",
                        "createdAt" to now,
                        "lastMessage" to "",
                        "lastMessageAt" to now,
                        "unreadCount" to 0
                    )
                    chatRef.set(chatData).await()
                } else {
                    val currentParticipants = chatDoc.get("participants") as? List<String> ?: emptyList()
                    val currentType = chatDoc.getString("type") ?: ""
                    if (currentType != "direct" || currentParticipants.sorted() != ids) {
                        onFailure("기존 채팅방 데이터가 올바르지 않습니다.")
                        return@launch
                    }
                }
                onSuccess(chatId, friend.name)
            } catch (e: Exception) {
                android.util.Log.e("ChatVM", "1:1 채팅방 생성 실패: ${e.message}")
                onFailure("채팅방 생성에 실패했어요")
            }
        }
    }

    // ── 현재 유저가 팀장인지 확인 ─────────────────────────────────────
    val isCurrentUserLeader: Boolean
        get() = _participants.value.firstOrNull { it.isLeader }?.userId == currentUserId

    // ── 팀장 양도 후 나가기 ───────────────────────────────────────────
    fun transferLeaderAndLeave(
        chatId: String,
        newLeaderUserId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()

                // 1. teams 팀장 변경
                db.collection("teams").document(chatId)
                    .update("leaderId", newLeaderUserId)
                    .await()

                // 2. participants 순서 변경 (새 팀장을 맨 앞으로, 나는 제거)
                val currentParticipants = _participants.value.map { it.userId }.toMutableList()
                currentParticipants.remove(newLeaderUserId)
                currentParticipants.remove(currentUserId)
                val newParticipants = mutableListOf(newLeaderUserId) + currentParticipants
                db.collection("chats").document(chatId)
                    .update("participants", newParticipants)
                    .await()

                // 3. 시스템 메시지
                val newLeaderName = _participants.value.find { it.userId == newLeaderUserId }?.name ?: "새 팀장"
                val systemMessage = mapOf(
                    "senderId" to "system",
                    "senderName" to "system",
                    "content" to "${newLeaderName}님이 팀장이 되었습니다.",
                    "type" to "system",
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
                db.collection("chats").document(chatId)
                    .collection("messages")
                    .add(systemMessage)
                    .await()

                // 4. 팀 memberIds에서 나 제거
                db.collection("teams").document(chatId)
                    .update("memberIds", FieldValue.arrayRemove(currentUserId))
                    .await()

                // 5. 내 teamIds 배열에서 제거 (teamId 단일값 → teamIds 배열)
                db.collection("users").document(currentUserId)
                    .update("teamIds", FieldValue.arrayRemove(chatId))
                    .await()

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatVM", "팀장 양도 실패: ${e.message}")
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onFailure("팀장 양도에 실패했어요")
                }
            }
        }
    }

    // ── 팀 해체 (팀장 혼자 남았을 때) ────────────────────────────────
    fun disbandTeam(
        chatId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()

                // 1. 시스템 메시지
                val systemMessage = mapOf(
                    "senderId" to "system",
                    "senderName" to "system",
                    "content" to "팀이 해체되었습니다.",
                    "type" to "system",
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
                db.collection("chats").document(chatId)
                    .collection("messages")
                    .add(systemMessage)
                    .await()

                // 2. 채팅방 participants 비우기
                db.collection("chats").document(chatId)
                    .update("participants", emptyList<String>())
                    .await()

                // 3. 팀 status 변경
                db.collection("teams").document(chatId)
                    .update("status", "disbanded")
                    .await()

                // 4. 내 teamIds 배열에서 제거 (teamId 단일값 → teamIds 배열)
                db.collection("users").document(currentUserId)
                    .update("teamIds", FieldValue.arrayRemove(chatId))
                    .await()

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatVM", "팀 해체 실패: ${e.message}")
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onFailure("팀 해체에 실패했어요")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        likesListener?.remove()
    }
}