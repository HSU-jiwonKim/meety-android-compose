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

    fun loadMemberNames(
        memberIds: List<String>,
        onSuccess: (List<String>) -> Unit,
        onFailure: (String) -> Unit
    )

    fun loadReceivedLikes(
        onSuccess: (List<ReceivedLikeItem>) -> Unit,
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