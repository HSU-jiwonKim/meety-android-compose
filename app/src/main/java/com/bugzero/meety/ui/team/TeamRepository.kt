package com.bugzero.meety.ui.team

import android.net.Uri

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