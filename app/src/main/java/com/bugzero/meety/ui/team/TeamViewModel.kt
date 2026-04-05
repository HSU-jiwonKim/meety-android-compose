package com.bugzero.meety.ui.team

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.bugzero.meety.data.repository.FriendRepository
import com.bugzero.meety.data.repository.FirebaseFriendRepository
import com.google.firebase.auth.FirebaseAuth

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
data class FriendItem(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val department: String = "",
    val age: Int = 0,
    val mbti: String = "",
    val bio: String = "",
    val location: String = ""
)
data class FriendRequestItem(
    val requestId: String = "",
    val fromUserId: String = "",
    val name: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val department: String = "",
    val age: Int = 0,
    val mbti: String = "",
    val bio: String = "",
    val location: String = ""
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
    private val repository: TeamRepository = FirebaseTeamRepository(),
    private val friendRepository: FriendRepository = FirebaseFriendRepository()
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

    private val auth = FirebaseAuth.getInstance()

    private val _friends = MutableStateFlow<List<FriendItem>>(emptyList())
    val friends: StateFlow<List<FriendItem>> = _friends

    private val _friendAddMessage = MutableStateFlow("")
    val friendAddMessage: StateFlow<String> = _friendAddMessage
    private val _receivedFriendRequests = MutableStateFlow<List<FriendRequestItem>>(emptyList())
    val receivedFriendRequests: StateFlow<List<FriendRequestItem>> = _receivedFriendRequests


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
    fun loadFriends() {
        val myUserId = auth.currentUser?.uid
        if (myUserId == null) {
            _message.value = "로그인된 사용자가 없습니다."
            _friends.value = emptyList()
            return
        }

        _isLoading.value = true

        friendRepository.loadMyFriends(
            myUserId = myUserId,
            onSuccess = { friendIds ->
                if (friendIds.isEmpty()) {
                    _friends.value = emptyList()
                    _isLoading.value = false
                    return@loadMyFriends
                }

                friendRepository.loadFriendProfiles(
                    friendIds = friendIds,
                    onSuccess = { friendList ->
                        _friends.value = friendList
                        _isLoading.value = false
                    },
                    onFailure = {
                        _message.value = it
                        _friends.value = emptyList()
                        _isLoading.value = false
                    }
                )
            },
            onFailure = {
                _message.value = it
                _friends.value = emptyList()
                _isLoading.value = false
            }
        )
    }
    fun addFriendByEmail(email: String) {
        val myUser = auth.currentUser
        val myUserId = myUser?.uid
        val myEmail = myUser?.email

        if (myUserId == null || myEmail == null) {
            _friendAddMessage.value = "로그인된 사용자가 없습니다."
            return
        }

        val trimmedEmail = email.trim()

        if (trimmedEmail.isBlank()) {
            _friendAddMessage.value = "이메일을 입력해주세요."
            return
        }

        if (!trimmedEmail.endsWith("@hansung.ac.kr")) {
            _friendAddMessage.value = "한성대 이메일만 추가할 수 있습니다."
            return
        }

        if (trimmedEmail == myEmail) {
            _friendAddMessage.value = "자기 자신은 친구 추가할 수 없습니다."
            return
        }

        _isLoading.value = true
        _friendAddMessage.value = ""

        friendRepository.findUserByEmail(
            email = trimmedEmail,
            onSuccess = { friendUserId ->

                friendRepository.loadMyFriends(
                    myUserId = myUserId,
                    onSuccess = { currentFriends ->
                        if (currentFriends.contains(friendUserId)) {
                            _friendAddMessage.value = "이미 친구로 추가된 사용자입니다."
                            _isLoading.value = false
                            return@loadMyFriends
                        }

                        friendRepository.addFriend(
                            myUserId = myUserId,
                            friendUserId = friendUserId,
                            onSuccess = {
                                friendRepository.sendFriendRequest(
                                    fromUserId = myUserId,
                                    toUserId = friendUserId,
                                    onSuccess = {
                                        _friendAddMessage.value = "친구 추가가 완료되었습니다."
                                        _isLoading.value = false
                                        loadFriends()
                                    },
                                    onFailure = {
                                        _friendAddMessage.value = it
                                        _isLoading.value = false
                                    }
                                )
                            },
                            onFailure = {
                                _friendAddMessage.value = it
                                _isLoading.value = false
                            }
                        )
                    },
                    onFailure = {
                        _friendAddMessage.value = it
                        _isLoading.value = false
                    }
                )
            },
            onFailure = {
                _friendAddMessage.value = "존재하지 않는 이메일입니다."
                _isLoading.value = false
            }
        )
    }

    fun removeFriend(friendUserId: String) {
        val myUserId = auth.currentUser?.uid
        if (myUserId == null) {
            _message.value = "로그인된 사용자가 없습니다."
            return
        }

        _isLoading.value = true

        friendRepository.removeFriend(
            myUserId = myUserId,
            friendUserId = friendUserId,
            onSuccess = {
                _message.value = "친구가 삭제되었습니다."
                _isLoading.value = false
                loadFriends()
            },
            onFailure = {
                _message.value = it
                _isLoading.value = false
            }
        )
    }
    fun loadReceivedFriendRequests() {
        val myUserId = auth.currentUser?.uid
        if (myUserId == null) {
            _message.value = "로그인된 사용자가 없습니다."
            _receivedFriendRequests.value = emptyList()
            return
        }

        friendRepository.loadReceivedFriendRequests(
            myUserId = myUserId,
            onSuccess = { list ->
                _receivedFriendRequests.value = list
            },
            onFailure = {
                _message.value = it
                _receivedFriendRequests.value = emptyList()
            }
        )
    }
    fun acceptFriendRequest(requestId: String, fromUserId: String) {
        val myUserId = auth.currentUser?.uid
        if (myUserId == null) {
            _message.value = "로그인된 사용자가 없습니다."
            return
        }

        _isLoading.value = true

        friendRepository.acceptFriendRequest(
            requestId = requestId,
            myUserId = myUserId,
            fromUserId = fromUserId,
            onSuccess = {
                _isLoading.value = false
                loadFriends()
                loadReceivedFriendRequests()
            },
            onFailure = {
                _message.value = it
                _isLoading.value = false
            }
        )
    }
    fun rejectFriendRequest(requestId: String) {
        _isLoading.value = true

        friendRepository.rejectFriendRequest(
            requestId = requestId,
            onSuccess = {
                _isLoading.value = false
                loadReceivedFriendRequests()
            },
            onFailure = {
                _message.value = it
                _isLoading.value = false
            }
        )
    }
    fun clearFriendAddMessage() {
        _friendAddMessage.value = ""
    }
}