package com.bugzero.meety.data.repository

import com.bugzero.meety.ui.team.FriendItem
interface FriendRepository {

    // 🔹 이메일로 유저 찾기
    fun findUserByEmail(
        email: String,
        onSuccess: (userId: String) -> Unit,
        onFailure: (String) -> Unit
    )

    // 🔹 친구 추가
    fun addFriend(
        myUserId: String,
        friendUserId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    )

    // 🔹 내 친구 목록 조회
    fun loadMyFriends(
        myUserId: String,
        onSuccess: (List<String>) -> Unit,
        onFailure: (String) -> Unit
    )

    // 🔹 친구 삭제
    fun removeFriend(
        myUserId: String,
        friendUserId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    )
    fun loadFriendProfiles(
        friendIds: List<String>,
        onSuccess: (List<FriendItem>) -> Unit,
        onFailure: (String) -> Unit
    )
}