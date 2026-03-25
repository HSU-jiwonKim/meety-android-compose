package com.bugzero.meety.ui.chat

import com.google.firebase.Timestamp

// chats/{chatId} 문서 구조
data class ChatPreview(
    val id: String = "",
    val teamAId: String = "",
    val teamBId: String = "",
    val meetingId: String = "",
    val lastMessage: String = "",
    val lastMessageAt: Timestamp? = null,
    val createdAt: Timestamp? = null,

    // UI 표시용 (Firestore에서 별도 조회)
    val teamName: String = "",
    val unreadCount: Int = 0,
    val emoji: String = "💬"
)

// chats/{chatId}/messages/{messageId} 문서 구조
data class ChatMessage(
    val id: String = "",
    val senderId: String = "",      // Firestore 필드명 그대로
    val senderName: String = "",    // Firestore 필드명 그대로
    val content: String = "",       // Firestore 필드명 그대로
    val type: String = "text",      // "text" | "image"
    val createdAt: Timestamp? = null,

    // UI 렌더링용 (Firestore 저장 안 함)
    val isMe: Boolean = false
)