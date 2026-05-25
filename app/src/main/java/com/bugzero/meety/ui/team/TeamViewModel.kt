package com.bugzero.meety.ui.team

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.bugzero.meety.data.repository.FriendRepository
import com.bugzero.meety.data.repository.FirebaseFriendRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


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
    val profileImages: List<String> = emptyList(),
    val department: String = "",
    val age: Int = 0,
    val mbti: String = "",
    val bio: String = "",
    val location: String = "",
    val height: Int = 0,
    val interests: List<String> = emptyList(),
    val foodLikes: List<String> = emptyList(),
    val isFavorite: Boolean = false
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
    val location: String = "",
    val height: Int = 0,
    val interests: List<String> = emptyList(),
    val foodLikes: List<String> = emptyList()
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
    private val db = FirebaseFirestore.getInstance()

    private val _friends = MutableStateFlow<List<FriendItem>>(emptyList())
    val friends: StateFlow<List<FriendItem>> = _friends

    private val _friendAddMessage = MutableStateFlow("")
    val friendAddMessage: StateFlow<String> = _friendAddMessage


    fun createTeam(
        teamName: String,
        description: String,
        tags: List<String>,
        imageUri: Uri?, // ✨ 1. 사진(Uri)을 받을 수 있는 바구니(파라미터)를 추가했어요!
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        _isLoading.value = true

        repository.createTeam(
            teamName = teamName,
            description = description,
            tags = tags,
            onSuccess = { teamId ->
                // ✨ 2. 방이 성공적으로 만들어졌다면, 선택한 사진이 있는지 확인합니다!
                if (imageUri != null) {
                    // 사진이 있다면, 만들어진 팀(teamId)에 바로 사진 업로드 콤보 공격!
                    repository.updateTeamProfileImage(
                        teamId = teamId,
                        imageUri = imageUri,
                        onSuccess = {
                            _message.value = "팀과 사진이 성공적으로 생성되었습니다!"
                            _isLoading.value = false
                            onSuccess(teamId)
                        },
                        onFailure = { errorMsg ->
                            _message.value = "팀은 만들어졌으나 사진 업로드에 실패했습니다: $errorMsg"
                            _isLoading.value = false
                            onSuccess(teamId) // 그래도 팀은 만들어졌으니 다음 화면으로 넘겨줍니다.
                        }
                    )
                } else {
                    // 사진을 선택하지 않고 그냥 만들었을 때
                    _message.value = "팀이 생성되었습니다."
                    _isLoading.value = false
                    onSuccess(teamId)
                }
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
                        db.collection("users")
                            .document(myUserId)
                            .collection("friends")
                            .get()
                            .addOnSuccessListener { result ->
                                val favoriteIds = result.documents
                                    .filter { it.getBoolean("isFavorite") == true }
                                    .map { it.id }
                                    .toSet()

                                _friends.value = friendList.map { friend ->
                                    friend.copy(isFavorite = favoriteIds.contains(friend.userId))
                                }
                                _isLoading.value = false
                            }
                            .addOnFailureListener {
                                _friends.value = friendList
                                _isLoading.value = false
                            }
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

    fun toggleFavoriteFriend(friendUserId: String, currentFavorite: Boolean) {
        val myUserId = auth.currentUser?.uid
        if (myUserId == null) {
            _message.value = "로그인된 사용자가 없습니다."
            return
        }

        db.collection("users")
            .document(myUserId)
            .collection("friends")
            .document(friendUserId)
            .update("isFavorite", !currentFavorite)
            .addOnSuccessListener {
                _message.value = if (currentFavorite) "즐겨찾기를 해제했습니다." else "즐겨찾기에 추가했습니다."
                loadFriends()
            }
            .addOnFailureListener {
                _message.value = it.message ?: "즐겨찾기 변경 실패"
            }
    }

    fun clearFriendAddMessage() {
        _friendAddMessage.value = ""
    }
}
