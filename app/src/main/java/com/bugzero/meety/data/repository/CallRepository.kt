package com.bugzero.meety.data.repository

interface CallRepository {

    /** 통화를 시작하고 Agora 채널 이름을 반환 (그룹 통화 지원). */
    suspend fun startCall(chatId: String, callType: String, callerId: String): String

    /**
     * 통화 종료.
     * @param chatId  채팅방 ID
     * @param userId  현재 사용자 UID (joinedUsers에서 제거)
     * @param forceEndForAll true이면 status를 "ended"로 강제 (발신자가 수락 전 취소 등)
     */
    suspend fun endCall(chatId: String, userId: String, forceEndForAll: Boolean = false)

    /** 수신 통화 수락 — joinedUsers에 추가하고 채널 이름 반환 */
    suspend fun acceptCall(chatId: String, userId: String): String

    /**
     * 특정 채팅방의 수신 통화를 실시간 감지.
     * @param currentUserId  현재 사용자 UID (이미 joinedUsers에 있으면 onIncomingCall을 호출하지 않음)
     * @param onIncomingCall status == "calling" 또는 "active" 이고 내가 참여자가 아닐 때 호출
     * @param onCallEnded    status == "ended" 일 때 호출 (전체 통화 종료 → 다이얼로그 닫기)
     */
    fun listenForIncomingCall(
        chatId: String,
        currentUserId: String,
        onIncomingCall: (callType: String, callerId: String) -> Unit,
        onCallEnded: () -> Unit = {}
    )

    /** 수신 통화 리스너 해제 */
    fun stopListeningForCalls(chatId: String)

    /**
     * 통화 로그 메시지를 한 번만 기록하기 위한 원자적 획득.
     * 성공(true)한 쪽만 실제로 call_log 메시지를 써야 한다.
     */
    suspend fun tryClaimCallLog(chatId: String): Boolean
}
