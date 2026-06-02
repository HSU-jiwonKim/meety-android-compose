package com.bugzero.meety.data.model

/**
 * 앱 내(상단 알림 버튼)에서 보여줄 알림 한 건.
 *
 * - type: "call" | "video_call" | "message" | "like"
 * - relatedId: 알림 종류에 따라 chatId(전화·메시지) 또는 teamId(좋아요)
 * - 읽음 처리는 별도 플래그 없이 "삭제 = 읽음"으로 처리한다.
 */
data class InAppNotification(
    val id: String = "",
    val toUserId: String = "",
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val relatedId: String = "",
    // 좋아요(like) 알림의 대상 팀 ID. 내 팀의 좋아요만 노출하기 위한 필터용.
    // 기존(레거시) 알림에는 비어 있을 수 있다.
    val teamId: String = "",
    val fromUserId: String = "",
    val fromUserName: String = "",
    val timestamp: Long = 0L
) {
    companion object {
        const val TYPE_CALL = "call"
        const val TYPE_VIDEO_CALL = "video_call"
        const val TYPE_MESSAGE = "message"
        const val TYPE_LIKE = "like"
    }
}
