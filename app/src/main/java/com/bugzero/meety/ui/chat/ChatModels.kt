package com.bugzero.meety.ui.chat

import com.google.firebase.Timestamp

data class ChatPreview(
    val id: String = "",
    val teamId: String = "",
    val lastMessage: String = "",
    val lastMessageAt: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val teamName: String = "",
    val unreadCount: Int = 0,
    val emoji: String = "👥",
    val type: String = "team"
)

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderProfileImage: String = "",
    val content: String = "",
    val type: String = "text",
    val createdAt: Timestamp? = null,
    val isMe: Boolean = false
)