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
                        val images = (doc.get("profileImages") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                        val mainImage = doc.getString("mainProfileImageUrl").orEmpty()
                        val firstImage = if (mainImage.isNotBlank()) mainImage else images.firstOrNull().orEmpty()
                        val scheduleMap = mutableMapOf<String, List<String>>()
                        val rawSchedule = doc.get("schedule") as? Map<*, *> ?: emptyMap<Any, Any>()
                        rawSchedule.forEach { (key, value) ->
                            val day = key as? String ?: return@forEach
                            val timeList = value as? List<*> ?: return@forEach
                            scheduleMap[day] = timeList.mapNotNull { it as? String }
                        }

                        resultList.add(
                            FriendItem(
                                userId = doc.id,
                                name = doc.getString("name") ?: "",
                                email = doc.getString("email") ?: "",
                                profileImageUrl = firstImage,
                                profileImages = images,
                                department = doc.getString("department") ?: "",
                                age = (doc.getLong("age") ?: 0L).toInt(),
                                mbti = doc.getString("mbti") ?: "",
                                bio = doc.getString("bio") ?: "",
                                location = doc.getString("location") ?: "",
                                height = (doc.getLong("height") ?: 0L).toInt(),
                                interests = (doc.get("interests") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                                foodLikes = (doc.get("foodLikes") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                                foodDislikes = (doc.get("foodDislikes") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                                schedule = scheduleMap
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
}
