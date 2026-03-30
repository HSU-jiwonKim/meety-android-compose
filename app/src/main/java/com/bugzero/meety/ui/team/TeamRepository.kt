package com.bugzero.meety.ui.team

import android.net.Uri

data class ReceivedLikeItem(
    val likeId: String = "",
    val fromUserId: String = "",
    val fromUserName: String = "",
    val fromUserProfileImage: String = "",
    val fromUserMbti: String = "",
    val fromUserDepartment: String = "",
    val fromTeamId: String = "",
    val createdAt: Long = 0L
)

data class SentLikeItem(
    val likeId: String = "",
    val toTeamId: String = "",
    val toTeamName: String = "",
    val toTeamTags: List<String> = emptyList(),
    val createdAt: Long = 0L
)
interface TeamRepository {

    fun createTeam(
        teamName: String,
        description: String,
        tags: List<String>,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    )

    fun loadMyTeam(
        onSuccess: (Team?) -> Unit,
        onFailure: (String) -> Unit
    )
    fun loadMemberNames(
        memberIds: List<String>,
        onSuccess: (List<String>) -> Unit,
        onFailure: (String) -> Unit
    )
    // 받은 관심 목록
    fun loadReceivedLikes(
        onSuccess: (List<ReceivedLikeItem>) -> Unit,
        onFailure: (String) -> Unit
    )

    fun loadSentLikes(
        onSuccess: (List<SentLikeItem>) -> Unit,
        onFailure: (String) -> Unit
    )
    fun acceptReceivedLike(
        likeId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    )

    fun rejectReceivedLike(
        likeId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    )
    fun inviteMember(
        teamId: String,
        toUserId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    )

    fun acceptInvitation(
        invitationId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    )

    fun rejectInvitation(
        invitationId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    )

    fun leaveTeam(
        teamId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    )

    fun updateTeamProfileImage(
        teamId: String,
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    )
}