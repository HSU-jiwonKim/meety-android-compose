package com.bugzero.meety.ui.chat

import com.google.firebase.Timestamp

data class ChatPreview(
    val id: String = "",
    val teamAId: String = "",
    val teamBId: String = "",
    val meetingId: String = "",
    val lastMessage: String = "",
    val lastMessageAt: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val teamName: String = "",
    val unreadCount: Int = 0,
    val emoji: String = "💬"
)

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderProfileImage: String = "",  // 추가
    val content: String = "",
    val type: String = "text",
    val createdAt: Timestamp? = null,
    val isMe: Boolean = false
)