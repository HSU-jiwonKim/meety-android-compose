package com.bugzero.meety.data.repository

import com.bugzero.meety.ui.team.FriendItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.bugzero.meety.ui.team.FriendRequestItem

class FirebaseFriendRepository : FriendRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // 🔹 이메일로 유저 찾기
    override fun findUserByEmail(
        email: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val userId = result.documents[0].id
                    onSuccess(userId)
                } else {
                    onFailure("해당 이메일의 사용자가 없습니다.")
                }
            }
            .addOnFailureListener {
                onFailure(it.message ?: "유저 조회 실패")
            }
    }

    // 🔹 친구 추가 (단방향)
    override fun addFriend(
        myUserId: String,
        friendUserId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (myUserId == friendUserId) {
            onFailure("자기 자신은 추가할 수 없습니다.")
            return
        }

        val data = hashMapOf(
            "friendUserId" to friendUserId,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(myUserId)
            .collection("friends")
            .document(friendUserId)
            .set(data)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onFailure(it.message ?: "친구 추가 실패")
            }
    }

    // 🔹 내 친구 목록 조회
    override fun loadMyFriends(
        myUserId: String,
        onSuccess: (List<String>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("users")
            .document(myUserId)
            .collection("friends")
            .get()
            .addOnSuccessListener { result ->
                val list = mutableListOf<String>()

                for (doc in result.documents) {
                    val friendId = doc.getString("friendUserId")
                    if (friendId != null) {
                        list.add(friendId)
                    }
                }

                onSuccess(list)
            }
            .addOnFailureListener {
                onFailure(it.message ?: "친구 목록 조회 실패")
            }
    }

    // 🔹 친구 삭제
    override fun removeFriend(
        myUserId: String,
        friendUserId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("users")
            .document(myUserId)
            .collection("friends")
            .document(friendUserId)
            .delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onFailure(it.message ?: "친구 삭제 실패")
            }
    }
    override fun loadFriendProfiles(
        friendIds: List<String>,
        onSuccess: (List<FriendItem>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (friendIds.isEmpty()) {
            onSuccess(emptyList())
            return
        }

        val resultList = mutableListOf<FriendItem>()
        var loadedCount = 0

        for (friendId in friendIds) {
            db.collection("users")
                .document(friendId)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val images = doc.get("profileImages") as? List<*>
                        val firstImage = images?.firstOrNull() as? String ?: ""

                        resultList.add(
                            FriendItem(
                                userId = doc.id,
                                name = doc.getString("name") ?: "",
                                email = doc.getString("email") ?: "",
                                profileImageUrl = firstImage,
                                department = doc.getString("department") ?: "",
                                age = (doc.getLong("age") ?: 0L).toInt(),
                                mbti = doc.getString("mbti") ?: "",
                                bio = doc.getString("bio") ?: "",
                                location = doc.getString("location") ?: ""
                            )
                        )
                    }
                    loadedCount++
                    if (loadedCount == friendIds.size) {
                        onSuccess(resultList)
                    }
                }
                .addOnFailureListener {
                    loadedCount++
                    if (loadedCount == friendIds.size) {
                        onSuccess(resultList)
                    }
                }
        }
    }
    override fun sendFriendRequest(
        fromUserId: String,
        toUserId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val data = hashMapOf(
            "fromUserId" to fromUserId,
            "toUserId" to toUserId,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("friendRequests")
            .add(data)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onFailure(it.message ?: "친구 요청 생성 실패")
            }
    }

    override fun loadReceivedFriendRequests(
        myUserId: String,
        onSuccess: (List<FriendRequestItem>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("friendRequests")
            .whereEqualTo("toUserId", myUserId)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    onSuccess(emptyList())
                    return@addOnSuccessListener
                }

                val requestDocs = result.documents
                val requestList = mutableListOf<FriendRequestItem>()
                var loadedCount = 0

                for (doc in requestDocs) {
                    val requestId = doc.id
                    val fromUserId = doc.getString("fromUserId").orEmpty()

                    if (fromUserId.isBlank()) {
                        loadedCount++
                        if (loadedCount == requestDocs.size) {
                            onSuccess(requestList)
                        }
                        continue
                    }

                    db.collection("users")
                        .document(fromUserId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            if (userDoc.exists()) {
                                val images = userDoc.get("profileImages") as? List<*>
                                val firstImage = images?.firstOrNull() as? String ?: ""

                                requestList.add(
                                    FriendRequestItem(
                                        requestId = requestId,
                                        fromUserId = fromUserId,
                                        name = userDoc.getString("name") ?: "",
                                        email = userDoc.getString("email") ?: "",
                                        profileImageUrl = firstImage,
                                        department = userDoc.getString("department") ?: "",
                                        age = (userDoc.getLong("age") ?: 0L).toInt(),
                                        mbti = userDoc.getString("mbti") ?: "",
                                        bio = userDoc.getString("bio") ?: "",
                                        location = userDoc.getString("location") ?: ""
                                    )
                                )
                            }

                            loadedCount++
                            if (loadedCount == requestDocs.size) {
                                onSuccess(requestList)
                            }
                        }
                        .addOnFailureListener {
                            loadedCount++
                            if (loadedCount == requestDocs.size) {
                                onSuccess(requestList)
                            }
                        }
                }
            }
            .addOnFailureListener {
                onFailure(it.message ?: "받은 친구 요청 조회 실패")
            }
    }

    override fun acceptFriendRequest(
        requestId: String,
        myUserId: String,
        fromUserId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val data = hashMapOf(
            "friendUserId" to fromUserId,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(myUserId)
            .collection("friends")
            .document(fromUserId)
            .set(data)
            .addOnSuccessListener {
                db.collection("friendRequests")
                    .document(requestId)
                    .delete()
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener {
                        onFailure(it.message ?: "친구 요청 삭제 실패")
                    }
            }
            .addOnFailureListener {
                onFailure(it.message ?: "친구 수락 실패")
            }
    }

    override fun rejectFriendRequest(
        requestId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("friendRequests")
            .document(requestId)
            .delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onFailure(it.message ?: "친구 요청 거절 실패")
            }
    }
}