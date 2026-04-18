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
    val isMe: Boolean = false,
    // ─── 통화 로그 전용 필드 (type == "call_log" 일 때 사용) ────
    val callType: String = "",           // "video" | "voice"
    val callStatus: String = "",         // "call_completed" | "call_missed" | "call_canceled"
    val callDurationSec: Int = 0,
    val callerId: String = "",
    // ─── 장소 공유 카드 전용 필드 (type == "place_card" 일 때 사용) ────
    val placeName: String = "",
    val placeCategory: String = "",
    val placeAddress: String = "",
    val placeImageUrl: String = "",
    val placeReviewCount: Int = 0,
    val placePlaceId: String = "",       // 네이버 지도 place ID (상세 페이지 딥링크용)
    val placeLat: Double = 0.0,
    val placeLng: Double = 0.0
)
