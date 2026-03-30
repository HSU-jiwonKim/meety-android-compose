package com.bugzero.meety.ui.team

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

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

        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val teamId = userDoc.getString("teamId") ?: ""

                if (teamId.isEmpty()) {
                    onSuccess(null)
                    return@addOnSuccessListener
                }

                db.collection("teams").document(teamId).get()
                    .addOnSuccessListener { teamDoc ->
                        val team = teamDoc.toObject(Team::class.java)
                        onSuccess(team)
                    }
                    .addOnFailureListener {
                        onFailure(it.message ?: "teams 조회 실패")
                    }
            }
            .addOnFailureListener {
                onFailure(it.message ?: "users 조회 실패")
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