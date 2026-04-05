package com.bugzero.meety.ui.team

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import com.google.firebase.firestore.Query

class FirebaseTeamRepository : TeamRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    override fun createTeam(
        teamName: String,
        description: String,
        tags: List<String>,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) { onFailure("로그인된 사용자가 없습니다."); return }

        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val mbti = userDoc.getString("mbti") ?: ""
                val profileImage = userDoc.getString("profileImage") ?: ""

                val teamRef = db.collection("teams").document()
                val teamId = teamRef.id

                val team = Team(
                    teamId = teamId,
                    leaderId = userId,
                    memberIds = listOf(userId),
                    mbtiTags = listOf(mbti),
                    tags = tags,
                    profileImages = listOf(profileImage),
                    teamProfileImage = "",
                    status = "active",
                    teamName = teamName,
                    description = description,
                    createdAt = System.currentTimeMillis()
                )

                // 🔥 1. 팀 생성
                teamRef.set(team)
                    .addOnSuccessListener {

                        // 🔥 2. 팀 채팅방 생성 (핵심)
                        val chatData = mapOf(
                            "chatId" to teamId,
                            "teamId" to teamId,
                            "teamName" to teamName,
                            "leaderId" to userId,
                            "participants" to listOf(userId),
                            "type" to "team",
                            "createdAt" to com.google.firebase.Timestamp.now(),
                            "lastMessage" to "",
                            "lastMessageAt" to null,
                            "emoji" to "👥"
                        )

                        db.collection("chats").document(teamId)
                            .set(chatData)
                            .addOnSuccessListener {

                                // 🔥 3. 유저 teamId 설정
                                db.collection("users").document(userId)
                                    .update("teamId", teamId)
                                    .addOnSuccessListener { onSuccess(teamId) }
                                    .addOnFailureListener { onFailure(it.message ?: "users.teamId 업데이트 실패") }
                            }
                            .addOnFailureListener { onFailure(it.message ?: "팀 채팅방 생성 실패") }
                    }
                    .addOnFailureListener { onFailure(it.message ?: "teams 생성 실패") }
            }
            .addOnFailureListener { onFailure(it.message ?: "users 조회 실패") }
    }

    override fun loadReceivedLikes(onSuccess: (List<ReceivedLikeItem>) -> Unit, onFailure: (String) -> Unit) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) { onFailure("로그인된 사용자가 없습니다."); return }

        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { userDoc ->
                val myTeamId = userDoc.getString("teamId").orEmpty()
                if (myTeamId.isBlank()) { onSuccess(emptyList()); return@addOnSuccessListener }

                db.collection("likes")
                    .whereEqualTo("toTeamId", myTeamId)
                    .whereEqualTo("status", "pending")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { likeSnapshot ->
                        if (likeSnapshot.isEmpty) { onSuccess(emptyList()); return@addOnSuccessListener }

                        val results = mutableListOf<ReceivedLikeItem>()
                        val docs = likeSnapshot.documents
                        var completedCount = 0

                        for (likeDoc in docs) {
                            val likeId = likeDoc.getString("likeId").orEmpty().ifBlank { likeDoc.id }
                            val fromUserId = likeDoc.getString("fromUserId").orEmpty()
                            val fromTeamId = likeDoc.getString("fromTeamId").orEmpty()
                            val createdAt = likeDoc.getLong("createdAt") ?: 0L

                            if (fromUserId.isBlank()) {
                                completedCount++
                                if (completedCount == docs.size) onSuccess(results.sortedByDescending { it.createdAt })
                                continue
                            }

                            db.collection("users").document(fromUserId).get()
                                .addOnSuccessListener { senderDoc ->
                                    results.add(ReceivedLikeItem(
                                        likeId = likeId,
                                        fromUserId = fromUserId,
                                        fromUserName = senderDoc.getString("name").orEmpty().ifBlank { "이름 없음" },
                                        fromUserProfileImage = senderDoc.getString("profileImage").orEmpty(),
                                        fromUserMbti = senderDoc.getString("mbti").orEmpty(),
                                        fromUserDepartment = senderDoc.getString("department").orEmpty(),
                                        fromTeamId = fromTeamId,
                                        createdAt = createdAt
                                    ))
                                    completedCount++
                                    if (completedCount == docs.size) onSuccess(results.sortedByDescending { it.createdAt })
                                }
                                .addOnFailureListener {
                                    results.add(ReceivedLikeItem(likeId = likeId, fromUserId = fromUserId, fromUserName = "이름 없음", fromTeamId = fromTeamId, createdAt = createdAt))
                                    completedCount++
                                    if (completedCount == docs.size) onSuccess(results.sortedByDescending { it.createdAt })
                                }
                        }
                    }
                    .addOnFailureListener { e -> onFailure(e.message ?: "받은 관심 조회 실패") }
            }
            .addOnFailureListener { e -> onFailure(e.message ?: "사용자 정보 조회 실패") }
    }

    override fun acceptReceivedLike(likeId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        db.collection("likes").document(likeId).get()
            .addOnSuccessListener { likeDoc ->
                val fromUserId = likeDoc.getString("fromUserId").orEmpty()
                val toTeamId = likeDoc.getString("toTeamId").orEmpty()

                if (fromUserId.isBlank() || toTeamId.isBlank()) {
                    onFailure("좋아요 정보가 올바르지 않습니다.")
                    return@addOnSuccessListener
                }

                db.collection("users").document(fromUserId).get()
                    .addOnSuccessListener { userDoc ->
                        val mbti = userDoc.getString("mbti").orEmpty()
                        val profileImage = userDoc.getString("profileImage").orEmpty()

                        // 🔥 1. 팀에 추가
                        db.collection("teams").document(toTeamId)
                            .update(
                                mapOf(
                                    "memberIds" to FieldValue.arrayUnion(fromUserId),
                                    "mbtiTags" to FieldValue.arrayUnion(mbti),
                                    "profileImages" to FieldValue.arrayUnion(profileImage)
                                )
                            )
                            .addOnSuccessListener {

                                // 🔥 2. 유저 teamId 업데이트
                                db.collection("users").document(fromUserId)
                                    .update("teamId", toTeamId)
                                    .addOnSuccessListener {

                                        // 🔥 3. 좋아요 상태 변경
                                        db.collection("likes").document(likeId)
                                            .update(
                                                mapOf(
                                                    "status" to "accepted",
                                                    "respondedAt" to System.currentTimeMillis()
                                                )
                                            )
                                            .addOnSuccessListener {

                                                // 🔥🔥🔥 핵심: 기존 채팅방에 참가자 추가 (절대 새 채팅 생성 X)
                                                db.collection("chats").document(toTeamId)
                                                    .update(
                                                        "participants",
                                                        FieldValue.arrayUnion(fromUserId)
                                                    )
                                                    .addOnSuccessListener {

                                                        // 시스템 메시지
                                                        val systemMessage = mapOf(
                                                            "senderId" to "system",
                                                            "senderName" to "system",
                                                            "content" to "새 팀원이 입장했습니다.",
                                                            "type" to "system",
                                                            "createdAt" to com.google.firebase.Timestamp.now()
                                                        )

                                                        db.collection("chats")
                                                            .document(toTeamId)
                                                            .collection("messages")
                                                            .add(systemMessage)
                                                            .addOnSuccessListener { onSuccess() }
                                                            .addOnFailureListener { onSuccess() }
                                                    }
                                                    .addOnFailureListener { onFailure(it.message ?: "채팅방 참가자 추가 실패") }
                                            }
                                            .addOnFailureListener { onFailure(it.message ?: "좋아요 상태 업데이트 실패") }
                                    }
                                    .addOnFailureListener { onFailure(it.message ?: "사용자 teamId 업데이트 실패") }
                            }
                            .addOnFailureListener { onFailure(it.message ?: "팀원 추가 실패") }
                    }
                    .addOnFailureListener { onFailure(it.message ?: "사용자 정보 조회 실패") }
            }
            .addOnFailureListener { onFailure(it.message ?: "좋아요 조회 실패") }
    }

    override fun rejectReceivedLike(likeId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        db.collection("likes").document(likeId)
            .update(mapOf("status" to "rejected", "respondedAt" to System.currentTimeMillis()))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "거절 처리 실패") }
    }

    override fun loadMemberNames(memberIds: List<String>, onSuccess: (List<String>) -> Unit, onFailure: (String) -> Unit) {
        if (memberIds.isEmpty()) { onSuccess(emptyList()); return }

        db.collection("users")
            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), memberIds)
            .get()
            .addOnSuccessListener { snapshot ->
                val nameMap = mutableMapOf<String, String>()
                for (doc in snapshot.documents) {
                    nameMap[doc.id] = doc.getString("name").orEmpty().ifBlank { "이름 없음" }
                }
                onSuccess(memberIds.map { nameMap[it] ?: "이름 없음" })
            }
            .addOnFailureListener { e -> onFailure(e.message ?: "팀원 이름 조회 실패") }
    }

    override fun leaveTeam(teamId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) { onFailure("로그인된 사용자가 없습니다."); return }

        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val mbti = userDoc.getString("mbti") ?: ""
                val profileImage = userDoc.getString("profileImage") ?: ""

                db.collection("teams").document(teamId)
                    .update(
                        mapOf(
                            "memberIds" to FieldValue.arrayRemove(userId),
                            "mbtiTags" to FieldValue.arrayRemove(mbti),
                            "profileImages" to FieldValue.arrayRemove(profileImage)
                        )
                    )
                    .addOnSuccessListener {
                        db.collection("users").document(userId)
                            .update("teamId", "")
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { onFailure(it.message ?: "teamId 초기화 실패") }
                    }
                    .addOnFailureListener { onFailure(it.message ?: "팀 탈퇴 실패") }
            }
            .addOnFailureListener { onFailure(it.message ?: "사용자 조회 실패") }
    }

    override fun updateTeamProfileImage(teamId: String, imageUri: Uri, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        val fileName = "team_profile/${teamId}_${UUID.randomUUID()}.jpg"
        val imageRef = storage.reference.child(fileName)

        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl
                    .addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()
                        db.collection("teams").document(teamId)
                            .update("teamProfileImage", imageUrl)
                            .addOnSuccessListener { onSuccess(imageUrl) }
                            .addOnFailureListener { onFailure(it.message ?: "팀 이미지 저장 실패") }
                    }
                    .addOnFailureListener { onFailure(it.message ?: "URL 조회 실패") }
            }
            .addOnFailureListener { onFailure(it.message ?: "이미지 업로드 실패") }
    }
}