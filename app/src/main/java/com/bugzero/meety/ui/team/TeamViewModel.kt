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

    private val _memberNames = MutableStateFlow<List<String>>(emptyList())
    val memberNames: StateFlow<List<String>> = _memberNames

    private val _receivedLikes = MutableStateFlow<List<ReceivedLikeItem>>(emptyList())
    val receivedLikes: StateFlow<List<ReceivedLikeItem>> = _receivedLikes

    private val _sentLikes = MutableStateFlow<List<SentLikeItem>>(emptyList())
    val sentLikes: StateFlow<List<SentLikeItem>> = _sentLikes

    fun createTeam(
        teamName: String,
        description: String,
        tags: List<String>,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        _isLoading.value = true

        repository.createTeam(
            teamName = teamName,
            description = description,
            tags = tags,
            onSuccess = { teamId ->
                _message.value = "팀이 생성되었습니다."
                _isLoading.value = false
                onSuccess(teamId)
            },
            onFailure = {
                _message.value = it
                _isLoading.value = false
                onFailure(it)
            }
        )
    }

    fun loadMemberNames(memberIds: List<String>) {
        repository.loadMemberNames(
            memberIds = memberIds,
            onSuccess = { names -> _memberNames.value = names },
            onFailure = {
                _message.value = it
                _memberNames.value = emptyList()
            }
        )
    }

    fun loadReceivedLikes() {
        repository.loadReceivedLikes(
            onSuccess = { list -> _receivedLikes.value = list },
            onFailure = {
                _message.value = it
                _receivedLikes.value = emptyList()
            }
        )
    }


    fun loadMatchingTabData() {
        loadReceivedLikes()
    }

    fun acceptReceivedLike(likeId: String) {
        repository.acceptReceivedLike(
            likeId = likeId,
            onSuccess = { loadMatchingTabData() },
            onFailure = { _message.value = it }
        )
    }

    fun rejectReceivedLike(likeId: String) {
        repository.rejectReceivedLike(
            likeId = likeId,
            onSuccess = { loadMatchingTabData() },
            onFailure = { _message.value = it }
        )
    }


    fun leaveTeam(teamId: String) {
        repository.leaveTeam(
            teamId = teamId,
            onSuccess = {
                _myTeam.value = null
                _message.value = "팀에서 탈퇴했습니다."
            },
            onFailure = { _message.value = it }
        )
    }

    fun updateTeamProfileImage(teamId: String, imageUri: Uri) {
        repository.updateTeamProfileImage(
            teamId = teamId,
            imageUri = imageUri,
            onSuccess = {
                _message.value = "팀 대표 이미지가 변경되었습니다."

            },
            onFailure = { _message.value = it }
        )
    }
}