package com.bugzero.meety.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugzero.meety.data.repository.ChatRepository
import com.bugzero.meety.data.repository.FirebaseChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ChatViewModel(
    private val repository: ChatRepository = FirebaseChatRepository()
) : ViewModel() {

    private val currentUserId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // ── 채팅 목록 ──────────────────────────────────────────────
    private val _chatList = MutableStateFlow<List<ChatPreview>>(emptyList())
    val chatList: StateFlow<List<ChatPreview>> = _chatList.asStateFlow()

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

    // ✅ init 블록 하나로 합치기
    init {
        loadChatList()
        saveFcmToken()
    }

    // 채팅방 진입 시 호출
    fun enterChatRoom(chatId: String, roomName: String) {
        _roomName.value = roomName
        observeMessages(chatId)
    }

    // 채팅 목록 실시간 구독
    fun loadChatList() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            android.util.Log.d("ChatVM", "loadChatList 시작, userId: $currentUserId")
            try {
                repository.observeChatList(currentUserId)
                    .catch { e ->
                        android.util.Log.e("ChatVM", "에러: ${e.message}")
                        _errorMessage.value = "채팅 목록을 불러오지 못했어요"
                    }
                    .collect { list ->
                        android.util.Log.d("ChatVM", "받은 목록 수: ${list.size}")
                        _chatList.value = list
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "채팅 목록을 불러오지 못했어요"
            }
        }
    }

    // 특정 채팅방 메시지 실시간 구독
    fun observeMessages(chatId: String) {
        viewModelScope.launch {
            repository.observeMessages(chatId)
                .catch { _errorMessage.value = "메시지를 불러오지 못했어요" }
                .collect { _messages.value = it }
        }
    }

    // 메시지 전송
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

    // 시간 포맷
    fun formatTime(timestamp: com.google.firebase.Timestamp?): String {
        timestamp ?: return ""
        val date = timestamp.toDate()
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { time = date }

        return when {
            now.get(Calendar.DATE) == target.get(Calendar.DATE) ->
                SimpleDateFormat("HH:mm", Locale.KOREA).format(date)
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
}