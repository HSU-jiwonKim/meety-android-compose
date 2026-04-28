package com.bugzero.meety.data.repository

import com.bugzero.meety.ui.chat.ChatMessage
import com.bugzero.meety.ui.chat.ChatPreview
import com.bugzero.meety.ui.chat.MatchCandidate
import com.bugzero.meety.ui.chat.TeamInvitation
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
    suspend fun transferLeadershipAndLeave(chatId: String, currentUserId: String, newLeaderId: String)

    // ── 팀원 자동 매칭 / 초대 ──────────────────────────────

    /** 나에게 온 대기 중인 팀 초대 실시간 스트림 */
    fun observePendingInvitations(userId: String): Flow<List<TeamInvitation>>

    /** 팀원 초대 전송 */
    suspend fun sendTeamInvitation(
        teamId: String,
        chatId: String,
        teamName: String,
        teamEmoji: String,
        fromUserId: String,
        toUserId: String
    )

    /** 초대 수락: 팀 + 채팅방 참여자 추가 */
    suspend fun acceptInvitation(invitationId: String, userId: String, teamId: String, chatId: String)

    /** 초대 거절 */
    suspend fun rejectInvitation(invitationId: String)

    /** 해당 팀을 좋아요한 후보자 목록 (자동 매칭용) */
    suspend fun loadMatchCandidates(teamId: String): List<MatchCandidate>
}