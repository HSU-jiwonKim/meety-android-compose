package com.bugzero.meety.data.repository

import com.bugzero.meety.ui.chat.ChatMessage
import com.bugzero.meety.ui.chat.ChatPreview
import kotlinx.coroutines.flow.Flow

interface ChatRepository {

    /** 내가 속한 채팅방 목록 실시간 스트림 */
    fun observeChatList(userId: String): Flow<List<ChatPreview>>

    /** 특정 채팅방 메시지 실시간 스트림 */
    fun observeMessages(chatId: String): Flow<List<ChatMessage>>

    /** 메시지 전송 */
    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        content: String,
        type: String = "text"
    )
}