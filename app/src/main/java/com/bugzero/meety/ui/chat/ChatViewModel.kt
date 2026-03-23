package com.bugzero.meety.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // 채팅 목록 상태
    private val _chatList = MutableStateFlow<List<ChatPreview>>(emptyList())
    val chatList: StateFlow<List<ChatPreview>> = _chatList
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    // 채팅방 메시지 목록 상태
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    // 채팅방 이름 상태
    private val _roomName = MutableStateFlow("")
    val roomName: StateFlow<String> = _roomName

    // 메시지 전송 중 상태
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    // Firestore 실시간 리스너
    private var chatListListener: ListenerRegistration? = null
    private var messageListener: ListenerRegistration? = null

    init {
        updateFcmToken()
        loadChatList()
    }

    // FCM 토큰 갱신 → users/{userId}.fcmToken 업데이트
    private fun updateFcmToken() {
        val userId = auth.currentUser?.uid ?: return
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                db.collection("users")
                    .document(userId)
                    .update("fcmToken", token)
            }
    }

    // chats 컬렉션에서 내가 속한 채팅방 목록 실시간 구독
    private fun loadChatList() {
        val userId = auth.currentUser?.uid ?: return
        _isLoading.value = true

        chatListListener = db.collection("chats")
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _isLoading.value = false
                    _errorMessage.value = "채팅 목록을 불러오지 못했습니다."
                    return@addSnapshotListener
                }

                val rawList = snapshot.documents.mapNotNull { doc ->
                    ChatPreview(
                        id = doc.id,
                        teamAId = doc.getString("teamAId") ?: "",
                        teamBId = doc.getString("teamBId") ?: "",
                        meetingId = doc.getString("meetingId") ?: "",
                        lastMessage = doc.getString("lastMessage") ?: "",
                        lastMessageAt = doc.getTimestamp("lastMessageAt"),
                        createdAt = doc.getTimestamp("createdAt")
                    )
                }

                viewModelScope.launch {
                    val enriched = enrichWithTeamName(rawList, userId)
                    _chatList.value = enriched
                    _isLoading.value = false
                    _errorMessage.value = null
                }
            }
    }

    // 상대 팀 이름 조회해서 ChatPreview에 추가
    private suspend fun enrichWithTeamName(
        list: List<ChatPreview>,
        userId: String
    ): List<ChatPreview> {
        // 내 teamId 조회
        val myTeamId = runCatching {
            db.collection("users").document(userId).get().await()
                .getString("teamId") ?: ""
        }.getOrDefault("")

        return list.map { preview ->
            runCatching {
                // 상대 팀 ID 판별
                val opponentTeamId = if (preview.teamAId == myTeamId) {
                    preview.teamBId
                } else {
                    preview.teamAId
                }

                // teams/{teamId}.teamName 조회
                val teamName = db.collection("teams")
                    .document(opponentTeamId)
                    .get()
                    .await()
                    .getString("teamName") ?: "알 수 없는 팀"

                // 안읽은 메시지 수 조회
                val unreadCount = db.collection("chats")
                    .document(preview.id)
                    .collection("messages")
                    .whereEqualTo("isRead", false)
                    .get()
                    .await()
                    .documents
                    .count { it.getString("senderId") != userId }

                preview.copy(
                    teamName = teamName,
                    unreadCount = unreadCount
                )
            }.getOrDefault(preview)
        }
    }

    // 채팅방 진입 시 호출
    // chats/{chatId}/messages 실시간 구독
    fun enterChatRoom(chatId: String, roomName: String) {
        _roomName.value = roomName

        // 기존 메시지 리스너 해제
        messageListener?.remove()
        _messages.value = emptyList()

        val myUserId = auth.currentUser?.uid ?: return

        // messages 서브컬렉션 실시간 구독
        // createdAt 기준 오름차순 (오래된 메시지가 위)
        messageListener = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val messageList = snapshot.documents.mapNotNull { doc ->
                    val senderId = doc.getString("senderId") ?: return@mapNotNull null
                    ChatMessage(
                        id = doc.id,
                        senderId = senderId,
                        senderName = doc.getString("senderName") ?: "",
                        content = doc.getString("content") ?: "",
                        type = doc.getString("type") ?: "text",
                        createdAt = doc.getTimestamp("createdAt"),
                        isMe = senderId == myUserId  // 내 메시지 여부 판별
                    )
                }
                _messages.value = messageList

                // 읽음 처리 (내가 읽은 메시지 isRead → true)
                markMessagesAsRead(chatId, myUserId, snapshot.documents)
            }
    }

    // 읽음 처리: 상대방이 보낸 메시지 중 isRead=false인 것만 업데이트
    private fun markMessagesAsRead(
        chatId: String,
        myUserId: String,
        docs: List<com.google.firebase.firestore.DocumentSnapshot>
    ) {
        val batch = db.batch()
        var hasUnread = false

        docs.forEach { doc ->
            val senderId = doc.getString("senderId") ?: return@forEach
            val isRead = doc.getBoolean("isRead") ?: false

            if (senderId != myUserId && !isRead) {
                batch.update(doc.reference, "isRead", true)
                hasUnread = true
            }
        }

        if (hasUnread) {
            batch.commit()
        }
    }

    // 메시지 전송
    // chats/{chatId}/messages에 새 문서 추가
    // chats/{chatId}.lastMessage, lastMessageAt 업데이트
    fun sendMessage(chatId: String, content: String) {
        if (content.isBlank()) return
        val userId = auth.currentUser?.uid ?: return
        val userName = auth.currentUser?.displayName ?: ""

        viewModelScope.launch {
            _isSending.value = true
            try {
                // Firestore 필드명 팀장님 구조 그대로 사용
                val message = hashMapOf(
                    "senderId" to userId,
                    "senderName" to userName,
                    "content" to content,
                    "type" to "text",
                    "createdAt" to Timestamp.now()
                )

                // messages 서브컬렉션에 추가
                db.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .add(message)
                    .await()

                // chats 문서 lastMessage 업데이트
                db.collection("chats")
                    .document(chatId)
                    .update(
                        mapOf(
                            "lastMessage" to content,
                            "lastMessageAt" to Timestamp.now()
                        )
                    )
                    .await()

            } catch (e: Exception) {
                // 전송 실패 시 추후 에러 상태 처리
            } finally {
                _isSending.value = false
            }
        }
    }

    // Timestamp → 화면 표시용 시간 문자열 변환
    fun formatTime(timestamp: Timestamp?): String {
        val date = timestamp?.toDate() ?: return ""
        val now = Date()
        val diffMs = now.time - date.time
        val diffDays = diffMs / (1000 * 60 * 60 * 24)
        return when {
            diffDays < 1 -> SimpleDateFormat("a h:mm", Locale.KOREAN).format(date)
            diffDays < 7 -> SimpleDateFormat("E요일", Locale.KOREAN).format(date)
            else -> SimpleDateFormat("M월 d일", Locale.KOREAN).format(date)
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatListListener?.remove()
        messageListener?.remove()
    }
}