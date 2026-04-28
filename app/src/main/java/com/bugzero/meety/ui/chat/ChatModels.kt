package com.bugzero.meety.ui.chat

import com.google.firebase.Timestamp

/**
 * 팀원 초대 모델 (자동 매칭 기능)
 * Firestore 컬렉션: teamInvitations/{id}
 */
data class TeamInvitation(
    val id: String = "",
    val teamId: String = "",
    val chatId: String = "",
    val teamName: String = "",
    val teamEmoji: String = "👥",
    val fromUserId: String = "",
    val toUserId: String = "",
    val status: String = "pending",   // "pending" | "accepted" | "rejected"
    val createdAt: Timestamp? = null
)

/**
 * 팀원 자동 매칭 후보 (팀을 좋아요 한 사용자)
 */
data class MatchCandidate(
    val userId: String = "",
    val name: String = "",
    val profileImageUrl: String = "",
    val mbti: String = "",
    val department: String = "",
    val matchScore: Int = 0   // userPreferences 기반 매칭 점수
)

data class ChatPreview(
    val id: String = "",
    val teamId: String = "",
    val lastMessage: String = "",
    val lastMessageAt: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val teamName: String = "",
    val unreadCount: Int = 0,
    val emoji: String = "👥",
    val type: String = "team",
    val participantCount: Int = 0
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