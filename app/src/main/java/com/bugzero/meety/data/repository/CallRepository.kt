package com.bugzero.meety.data.repository

interface CallRepository {

    /** 통화를 시작하고 Agora 채널 이름을 반환 */
    suspend fun startCall(chatId: String, callType: String, callerId: String): String

    /** 통화 종료 (Firestore 상태를 "ended"로 업데이트) */
    suspend fun endCall(chatId: String)

    /** 수신 통화 수락 — Firestore 상태를 "accepted"로 바꾸고 채널 이름 반환 */
    suspend fun acceptCall(chatId: String): String

    /** 특정 채팅방의 수신 통화를 실시간 감지 */
    fun listenForIncomingCall(chatId: String, onIncomingCall: (callType: String, callerId: String) -> Unit)

    /** 수신 통화 리스너 해제 */
    fun stopListeningForCalls(chatId: String)
}
