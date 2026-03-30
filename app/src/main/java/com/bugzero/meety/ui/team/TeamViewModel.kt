package com.bugzero.meety.ui.team

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class Team(
    val teamId: String = "",
    val leaderId: String = "",
    val memberIds: List<String> = emptyList(),
    val mbtiTags: List<String> = emptyList(),
    val profileImages: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val teamProfileImage: String = "",
    val status: String = "active",
    val teamName: String = "",
    val description: String = "",
    val createdAt: Long = 0L
)

data class Invitation(
    val invitationId: String = "",
    val teamId: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val status: String = "pending",
    val createdAt: Long = 0L
)

class TeamViewModel(
    private val repository: TeamRepository = FirebaseTeamRepository()
) : ViewModel() {

    private val _myTeam = MutableStateFlow<Team?>(null)
    val myTeam: StateFlow<Team?> = _myTeam

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun createTeam(
        teamName: String,
        description: String,
        tags: List<String>,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        _isLoading.value = true

        repository.createTeam(
            teamName = teamName,
            description = description,
            tags = tags,
            onSuccess = {
                _message.value = "팀이 생성되었습니다."
                _isLoading.value = false
                loadMyTeam()
                onSuccess()
            },
            onFailure = {
                _message.value = it
                _isLoading.value = false
                onFailure(it)
            }
        )
    }

    fun loadMyTeam() {
        _isLoading.value = true

        repository.loadMyTeam(
            onSuccess = { team ->
                _myTeam.value = team
                _isLoading.value = false
            },
            onFailure = {
                _message.value = it
                _isLoading.value = false
            }
        )
    }

    fun inviteMember(teamId: String, toUserId: String) {
        repository.inviteMember(
            teamId = teamId,
            toUserId = toUserId,
            onSuccess = {
                _message.value = "초대장을 보냈습니다."
            },
            onFailure = {
                _message.value = it
            }
        )
    }

    fun acceptInvitation(invitationId: String) {
        repository.acceptInvitation(
            invitationId = invitationId,
            onSuccess = {
                _message.value = "초대를 수락했습니다."
                loadMyTeam()
            },
            onFailure = {
                _message.value = it
            }
        )
    }

    fun rejectInvitation(invitationId: String) {
        repository.rejectInvitation(
            invitationId = invitationId,
            onSuccess = {
                _message.value = "초대를 거절했습니다."
            },
            onFailure = {
                _message.value = it
            }
        )
    }

    fun leaveTeam(teamId: String) {
        repository.leaveTeam(
            teamId = teamId,
            onSuccess = {
                _myTeam.value = null
                _message.value = "팀에서 탈퇴했습니다."
            },
            onFailure = {
                _message.value = it
            }
        )
    }

    fun updateTeamProfileImage(teamId: String, imageUri: Uri) {
        repository.updateTeamProfileImage(
            teamId = teamId,
            imageUri = imageUri,
            onSuccess = {
                _message.value = "팀 대표 이미지가 변경되었습니다."
                loadMyTeam()
            },
            onFailure = {
                _message.value = it
            }
        )
    }
}