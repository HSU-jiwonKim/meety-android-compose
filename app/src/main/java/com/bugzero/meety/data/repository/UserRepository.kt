package com.bugzero.meety.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // =====================
    // 로그인
    // =====================
    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onBanned: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val user = auth.currentUser
                if (user?.isEmailVerified == true) {
                    db.collection("users").document(user.uid).get()
                        .addOnSuccessListener { doc ->
                            val isBanned = doc.getBoolean("isBanned") ?: false
                            if (isBanned) {
                                auth.signOut()
                                onBanned()
                            } else {
                                onSuccess()
                            }
                        }
                        .addOnFailureListener { onFailure("로그인에 실패했습니다") }
                } else {
                    auth.signOut()
                    onFailure("이메일 인증이 필요합니다\n받은 메일함을 확인해주세요")
                }
            }
            .addOnFailureListener { onFailure("이메일 또는 비밀번호가 틀렸습니다") }
    }

    // =====================
    // 회원가입
    // =====================
    fun signUp(
        email: String,
        password: String,
        name: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user ?: return@addOnSuccessListener
                val userId = user.uid

                user.sendEmailVerification()
                    .addOnSuccessListener {
                        val userMap = hashMapOf(
                            "name" to name,
                            "email" to email,
                            "isVerified" to false,
                            "isAdmin" to false,
                            "isBanned" to false,
                            "teamId" to "",
                            "fcmToken" to "",
                            "studentIdImageUrl" to "",
                            "profileImages" to listOf<String>(),
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                        db.collection("users").document(userId).set(userMap)
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { onFailure("유저 정보 저장에 실패했습니다") }
                    }
                    .addOnFailureListener { onFailure("인증 이메일 발송에 실패했습니다") }
            }
            .addOnFailureListener { e ->
                val message = when {
                    e.message?.contains("email address is already in use") == true ->
                        "이미 사용 중인 이메일입니다"
                    else -> "회원가입에 실패했습니다"
                }
                onFailure(message)
            }
    }

    // =====================
    // 이메일 인증 확인
    // =====================
    fun checkEmailVerified(
        onVerified: () -> Unit,
        onNotVerified: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val user = auth.currentUser ?: run {
            onFailure("로그인이 필요합니다")
            return
        }
        user.reload()
            .addOnSuccessListener {
                if (auth.currentUser?.isEmailVerified == true) onVerified()
                else onNotVerified()
            }
            .addOnFailureListener { onFailure("확인에 실패했습니다") }
    }

    fun resendVerificationEmail(
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val user = auth.currentUser ?: run {
            onFailure("로그인이 필요합니다")
            return
        }
        user.sendEmailVerification()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure("이메일 재전송에 실패했습니다") }
    }

    // =====================
    // 프로필 저장
    // =====================
    fun saveProfile(
        name: String,
        age: String,
        department: String,
        mbti: String,
        bio: String,
        height: String,
        location: String,
        interests: List<String>,
        foodLikes: List<String>,
        foodDislikes: List<String>,
        imageUris: List<android.net.Uri>,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: run {
            onFailure("로그인이 필요합니다")
            return
        }

        if (imageUris.isEmpty()) {
            saveProfileData(userId, name, age, department, mbti, bio, height, location, interests, foodLikes, foodDislikes, emptyList(), onSuccess, onFailure)
            return
        }

        val imageUrls = mutableListOf<String>()
        var uploadCount = 0

        imageUris.forEach { uri ->
            val fileName = "profile_${userId}_${System.currentTimeMillis()}_${uploadCount}.jpg"
            val ref = storage.reference.child("profiles/$userId/$fileName")

            ref.putFile(uri)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { downloadUri ->
                        imageUrls.add(downloadUri.toString())
                        uploadCount++
                        if (uploadCount == imageUris.size) {
                            saveProfileData(userId, name, age, department, mbti, bio, height, location, interests, foodLikes, foodDislikes, imageUrls, onSuccess, onFailure)
                        }
                    }
                }
                .addOnFailureListener { onFailure("이미지 업로드에 실패했습니다") }
        }
    }

    private fun saveProfileData(
        userId: String,
        name: String,
        age: String,
        department: String,
        mbti: String,
        bio: String,
        height: String,
        location: String,
        interests: List<String>,
        foodLikes: List<String>,
        foodDislikes: List<String>,
        profileImages: List<String>,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val profileMap = hashMapOf(
            "name" to name,
            "age" to (age.toIntOrNull() ?: 0),
            "department" to department,
            "mbti" to mbti,
            "bio" to bio,
            "height" to (height.toIntOrNull() ?: 0),
            "location" to location,
            "interests" to interests,
            "foodLikes" to foodLikes,
            "foodDislikes" to foodDislikes,
            "profileImages" to profileImages
        )
        db.collection("users").document(userId)
            .update(profileMap as Map<String, Any>)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure("프로필 저장에 실패했습니다") }
    }

    // =====================
    // 학생증 업로드
    // =====================
    fun requestStudentIdVerification(
        imageUri: android.net.Uri,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: run {
            onFailure("로그인이 필요합니다")
            return
        }
        val userEmail = auth.currentUser?.email ?: ""

        val fileName = "studentId_${userId}_${System.currentTimeMillis()}.jpg"
        val ref = storage.reference.child("studentIds/$userId/$fileName")

        ref.putFile(imageUri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUri ->
                    val imageUrl = downloadUri.toString()

                    db.collection("users").document(userId)
                        .update("studentIdImageUrl", imageUrl)

                    db.collection("users").document(userId).get()
                        .addOnSuccessListener { document ->
                            val userName = document.getString("name") ?: ""
                            val requestMap = hashMapOf(
                                "userId" to userId,
                                "userName" to userName,
                                "userEmail" to userEmail,
                                "studentIdImageUrl" to imageUrl,
                                "status" to "pending",
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                            db.collection("adminQueue").add(requestMap)
                                .addOnSuccessListener { onSuccess() }
                                .addOnFailureListener { onFailure("인증 요청에 실패했습니다") }
                        }
                }
            }
            .addOnFailureListener { onFailure("이미지 업로드에 실패했습니다") }
    }

    // =====================
    // 인증 상태 및 역할 확인
    // =====================
    fun checkVerificationAndRole(
        onAdmin: () -> Unit,
        onVerified: () -> Unit,
        onNotYet: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: run {
            onFailure("로그인이 필요합니다")
            return
        }
        db.collection("users").document(userId)
            .get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { document ->
                val isAdmin = document.getBoolean("isAdmin") ?: false
                val isVerified = document.getBoolean("isVerified") ?: false
                when {
                    isAdmin -> onAdmin()
                    isVerified -> onVerified()
                    else -> onNotYet()
                }
            }
            .addOnFailureListener { onFailure("확인에 실패했습니다") }
    }

    fun loadUserRole(onResult: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                onResult(doc.getBoolean("isAdmin") ?: false)
            }
    }

    // =====================
    // 비밀번호 찾기
    // =====================
    fun sendPasswordResetEmail(
        email: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure("이메일 전송에 실패했습니다") }
    }

    // =====================
    // 실시간 차단 감지
    // =====================
    fun startBanListener(onBanned: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .addSnapshotListener { doc, error ->
                if (error != null) return@addSnapshotListener
                val isBanned = doc?.getBoolean("isBanned") ?: false
                if (isBanned) {
                    auth.signOut()
                    onBanned()
                }
            }
    }

    // =====================
    // 기타
    // =====================
    fun checkAutoLogin(): Boolean = auth.currentUser?.isEmailVerified == true

    fun logout() = auth.signOut()

    fun getCurrentUserId(): String? = auth.currentUser?.uid
}