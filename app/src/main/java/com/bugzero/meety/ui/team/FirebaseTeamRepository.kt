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
        if (userId == null) {
            onFailure("로그인된 사용자가 없습니다.")
            return
        }

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

                teamRef.set(team)
                    .addOnSuccessListener {
                        db.collection("users").document(userId)
                            .update("teamId", teamId)
                            .addOnSuccessListener {
                                onSuccess(teamId)
                            }
                            .addOnFailureListener {
                                onFailure(it.message ?: "users.teamId 업데이트 실패")
                            }
                    }
                    .addOnFailureListener {
                        onFailure(it.message ?: "teams 생성 실패")
                    }
            }
            .addOnFailureListener {
                onFailure(it.message ?: "users 조회 실패")
            }
    }

    override fun loadMyTeam(
        onSuccess: (Team?) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            onFailure("로그인된 사용자가 없습니다.")
            return
        }

        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { userSnapshot ->

                val teamId = userSnapshot.getString("teamId").orEmpty()

                // 사용자가 아직 팀이 없으면 null 반환
                if (teamId.isBlank()) {
                    onSuccess(null)
                    return@addOnSuccessListener
                }

                db.collection("teams")
                    .document(teamId)
                    .get()
                    .addOnSuccessListener { teamSnapshot ->

                        if (!teamSnapshot.exists()) {
                            onFailure("팀 정보를 찾을 수 없습니다.")
                            return@addOnSuccessListener
                        }

                        val team = Team(
                            teamId = teamSnapshot.getString("teamId").orEmpty(),
                            leaderId = teamSnapshot.getString("leaderId").orEmpty(),
                            memberIds = teamSnapshot.get("memberIds") as? List<String> ?: emptyList(),
                            mbtiTags = teamSnapshot.get("mbtiTags") as? List<String> ?: emptyList(),
                            profileImages = teamSnapshot.get("profileImages") as? List<String> ?: emptyList(),
                            tags = teamSnapshot.get("tags") as? List<String> ?: emptyList(),
                            teamProfileImage = teamSnapshot.getString("teamProfileImage").orEmpty(),
                            status = teamSnapshot.getString("status").orEmpty().ifBlank { "active" },
                            teamName = teamSnapshot.getString("teamName").orEmpty(),
                            description = teamSnapshot.getString("description").orEmpty(),
                            createdAt = teamSnapshot.getLong("createdAt") ?: 0L
                        )

                        onSuccess(team)
                    }
                    .addOnFailureListener { e ->
                        onFailure(e.message ?: "팀 정보 조회에 실패했습니다.")
                    }
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "사용자 정보 조회에 실패했습니다.")
            }
    }

    override fun loadReceivedLikes(
        onSuccess: (List<ReceivedLikeItem>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            onFailure("로그인된 사용자가 없습니다.")
            return
        }

        // 1. 현재 사용자의 teamId 조회
        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { userDoc ->
                val myTeamId = userDoc.getString("teamId").orEmpty()

                if (myTeamId.isBlank()) {
                    onSuccess(emptyList())
                    return@addOnSuccessListener
                }

                // 2. 내 팀이 받은 likes 조회
                db.collection("likes")
                    .whereEqualTo("toTeamId", myTeamId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { likeSnapshot ->

                        if (likeSnapshot.isEmpty) {
                            onSuccess(emptyList())
                            return@addOnSuccessListener
                        }

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
                                if (completedCount == docs.size) {
                                    onSuccess(results.sortedByDescending { it.createdAt })
                                }
                                continue
                            }

                            // 3. 보낸 사람 users 문서 조회
                            db.collection("users").document(fromUserId).get()
                                .addOnSuccessListener { senderDoc ->
                                    val item = ReceivedLikeItem(
                                        likeId = likeId,
                                        fromUserId = fromUserId,
                                        fromUserName = senderDoc.getString("name").orEmpty().ifBlank { "이름 없음" },
                                        fromUserProfileImage = senderDoc.getString("profileImage").orEmpty(),
                                        fromUserMbti = senderDoc.getString("mbti").orEmpty(),
                                        fromUserDepartment = senderDoc.getString("department").orEmpty(),
                                        fromTeamId = fromTeamId,
                                        createdAt = createdAt
                                    )

                                    results.add(item)

                                    completedCount++
                                    if (completedCount == docs.size) {
                                        onSuccess(results.sortedByDescending { it.createdAt })
                                    }
                                }
                                .addOnFailureListener {
                                    // users 조회 실패해도 목록 자체는 보여주기 위해 기본값 처리
                                    val item = ReceivedLikeItem(
                                        likeId = likeId,
                                        fromUserId = fromUserId,
                                        fromUserName = "이름 없음",
                                        fromUserProfileImage = "",
                                        fromUserMbti = "",
                                        fromUserDepartment = "",
                                        fromTeamId = fromTeamId,
                                        createdAt = createdAt
                                    )

                                    results.add(item)

                                    completedCount++
                                    if (completedCount == docs.size) {
                                        onSuccess(results.sortedByDescending { it.createdAt })
                                    }
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        onFailure(e.message ?: "받은 관심 조회 실패")
                    }
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "사용자 정보 조회 실패")
            }
    }

    override fun loadSentLikes(
        onSuccess: (List<SentLikeItem>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            onFailure("로그인된 사용자가 없습니다.")
            return
        }

        db.collection("likes")
            .whereEqualTo("fromUserId", currentUserId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val sentList = snapshot.documents.map { doc ->
                    SentLikeItem(
                        likeId = doc.getString("likeId").orEmpty().ifBlank { doc.id },
                        toTeamId = doc.getString("toTeamId").orEmpty(),
                        toTeamName = doc.getString("toTeamName").orEmpty(),
                        toTeamTags = doc.get("toTeamTags") as? List<String> ?: emptyList(),
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                }

                onSuccess(sentList)
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "보낸 관심 조회 실패")
            }
    }
    override fun inviteMember(
        teamId: String,
        toUserId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val fromUserId = auth.currentUser?.uid
        if (fromUserId == null) {
            onFailure("로그인된 사용자가 없습니다.")
            return
        }

        val invitationRef = db.collection("invitations").document()
        val invitation = Invitation(
            invitationId = invitationRef.id,
            teamId = teamId,
            fromUserId = fromUserId,
            toUserId = toUserId,
            status = "pending",
            createdAt = System.currentTimeMillis()
        )

        invitationRef.set(invitation)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener {
                onFailure(it.message ?: "초대장 생성 실패")
            }
    }

    override fun acceptInvitation(
        invitationId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onFailure("로그인된 사용자가 없습니다.")
            return
        }

        val invitationRef = db.collection("invitations").document(invitationId)

        invitationRef.get()
            .addOnSuccessListener { invitationDoc ->
                val teamId = invitationDoc.getString("teamId") ?: ""

                if (teamId.isEmpty()) {
                    onFailure("초대장에 teamId가 없습니다.")
                    return@addOnSuccessListener
                }

                db.collection("users").document(userId).get()
                    .addOnSuccessListener { userDoc ->
                        val mbti = userDoc.getString("mbti") ?: ""
                        val profileImage = userDoc.getString("profileImage") ?: ""

                        db.collection("teams").document(teamId)
                            .update(
                                mapOf(
                                    "memberIds" to FieldValue.arrayUnion(userId),
                                    "mbtiTags" to FieldValue.arrayUnion(mbti),
                                    "profileImages" to FieldValue.arrayUnion(profileImage)
                                )
                            )
                            .addOnSuccessListener {
                                db.collection("users").document(userId)
                                    .update("teamId", teamId)
                                    .addOnSuccessListener {
                                        invitationRef.update("status", "accepted")
                                            .addOnSuccessListener { onSuccess() }
                                            .addOnFailureListener {
                                                onFailure(it.message ?: "초대 상태 업데이트 실패")
                                            }
                                    }
                                    .addOnFailureListener {
                                        onFailure(it.message ?: "users.teamId 업데이트 실패")
                                    }
                            }
                            .addOnFailureListener {
                                onFailure(it.message ?: "teams 멤버 추가 실패")
                            }
                    }
                    .addOnFailureListener {
                        onFailure(it.message ?: "사용자 정보 조회 실패")
                    }
            }
            .addOnFailureListener {
                onFailure(it.message ?: "초대장 조회 실패")
            }
    }
    override fun loadMemberNames(
        memberIds: List<String>,
        onSuccess: (List<String>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (memberIds.isEmpty()) {
            onSuccess(emptyList())
            return
        }

        db.collection("users")
            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), memberIds)
            .get()
            .addOnSuccessListener { snapshot ->
                val nameMap = mutableMapOf<String, String>()

                for (doc in snapshot.documents) {
                    val name = doc.getString("name").orEmpty().ifBlank { "이름 없음" }
                    nameMap[doc.id] = name
                }

                // memberIds 순서대로 이름 리스트 만들기
                val memberNames = memberIds.map { userId ->
                    nameMap[userId] ?: "이름 없음"
                }

                onSuccess(memberNames)
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "팀원 이름 조회에 실패했습니다.")
            }
    }
    override fun rejectInvitation(
        invitationId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("invitations").document(invitationId)
            .update("status", "rejected")
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener {
                onFailure(it.message ?: "초대 거절 실패")
            }
    }
    override fun leaveTeam(
        teamId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onFailure("로그인된 사용자가 없습니다.")
            return
        }

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
                            .addOnFailureListener {
                                onFailure(it.message ?: "users.teamId 초기화 실패")
                            }
                    }
                    .addOnFailureListener {
                        onFailure(it.message ?: "팀 탈퇴 처리 실패")
                    }
            }
            .addOnFailureListener {
                onFailure(it.message ?: "사용자 조회 실패")
            }
    }

    override fun updateTeamProfileImage(
        teamId: String,
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val fileName = "team_profile/${teamId}_${UUID.randomUUID()}.jpg"
        val imageRef = storage.reference.child(fileName)

        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl
                    .addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()
                        db.collection("teams").document(teamId)
                            .update("teamProfileImage", imageUrl)
                            .addOnSuccessListener {
                                onSuccess(imageUrl)
                            }
                            .addOnFailureListener {
                                onFailure(it.message ?: "팀 대표 사진 URL 저장 실패")
                            }
                    }
                    .addOnFailureListener {
                        onFailure(it.message ?: "다운로드 URL 조회 실패")
                    }
            }
            .addOnFailureListener {
                onFailure(it.message ?: "이미지 업로드 실패")
            }
    }
}