package com.bugzero.meety.ui.auth

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

sealed class SignUpState {
    object Idle : SignUpState()
    object Loading : SignUpState()
    object Success : SignUpState()
    data class Error(val message: String) : SignUpState()
}

sealed class UploadState {
    object Idle : UploadState()
    object Loading : UploadState()
    object Success : UploadState()
    data class Error(val message: String) : UploadState()
}

sealed class EmailVerificationState {
    object Idle : EmailVerificationState()
    object Loading : EmailVerificationState()
    object Verified : EmailVerificationState()
    object NotVerified : EmailVerificationState()
    object EmailSent : EmailVerificationState()
    data class Error(val message: String) : EmailVerificationState()
}

sealed class ProfileSaveState {
    object Idle : ProfileSaveState()
    object Loading : ProfileSaveState()
    object Success : ProfileSaveState()
    data class Error(val message: String) : ProfileSaveState()
}

sealed class VerificationCheckState {
    object Idle : VerificationCheckState()
    object Loading : VerificationCheckState()
    object Verified : VerificationCheckState()
    object NotYet : VerificationCheckState()
    object Admin : VerificationCheckState()
    data class Error(val message: String) : VerificationCheckState()
}

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // =====================
    // isAdmin 상태 (bottom nav용)
    // =====================
    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin

    fun loadUserRole() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                _isAdmin.value = doc.getBoolean("isAdmin") ?: false
            }
    }

    // =====================
    // LoginScreen 용
    // =====================
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun login(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error("이메일과 비밀번호를 입력해주세요")
            return
        }
        if (!email.endsWith("@hansung.ac.kr")) {
            _authState.value = AuthState.Error("한성대학교 이메일만 사용 가능합니다")
            return
        }

        _authState.value = AuthState.Loading

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val user = auth.currentUser
                if (user?.isEmailVerified == true) {
                    _authState.value = AuthState.Success
                } else {
                    auth.signOut()
                    _authState.value = AuthState.Error("이메일 인증이 필요합니다\n받은 메일함을 확인해주세요")
                }
            }
            .addOnFailureListener {
                _authState.value = AuthState.Error("이메일 또는 비밀번호가 틀렸습니다")
            }
    }

    fun checkAutoLogin(): Boolean {
        return auth.currentUser?.isEmailVerified == true
    }

    fun logout() {
        auth.signOut()
        _isAdmin.value = false
        _authState.value = AuthState.Idle
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    // =====================
    // SignUpScreen 용
    // =====================
    private val _signUpState = MutableStateFlow<SignUpState>(SignUpState.Idle)
    val signUpState: StateFlow<SignUpState> = _signUpState

    fun signUp(email: String, password: String, name: String) {
        if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
            _signUpState.value = SignUpState.Error("모든 항목을 입력해주세요")
            return
        }
        if (!email.endsWith("@hansung.ac.kr")) {
            _signUpState.value = SignUpState.Error("한성대학교 이메일만 사용 가능합니다")
            return
        }
        if (password.length < 8) {
            _signUpState.value = SignUpState.Error("비밀번호는 8자 이상이어야 합니다")
            return
        }

        _signUpState.value = SignUpState.Loading

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
                            "teamId" to "",
                            "fcmToken" to "",
                            "studentIdImageUrl" to "",
                            "profileImages" to listOf<String>(),
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                        db.collection("users").document(userId).set(userMap)
                            .addOnSuccessListener {
                                _signUpState.value = SignUpState.Success
                            }
                            .addOnFailureListener {
                                _signUpState.value = SignUpState.Error("유저 정보 저장에 실패했습니다")
                            }
                    }
                    .addOnFailureListener {
                        _signUpState.value = SignUpState.Error("인증 이메일 발송에 실패했습니다")
                    }
            }
            .addOnFailureListener { e ->
                val message = when {
                    e.message?.contains("email address is already in use") == true ->
                        "이미 사용 중인 이메일입니다"
                    else -> "회원가입에 실패했습니다"
                }
                _signUpState.value = SignUpState.Error(message)
            }
    }

    fun resetSignUpState() {
        _signUpState.value = SignUpState.Idle
    }

    // =====================
    // EmailVerification 용
    // =====================
    private val _emailVerificationState = MutableStateFlow<EmailVerificationState>(EmailVerificationState.Idle)
    val emailVerificationState: StateFlow<EmailVerificationState> = _emailVerificationState

    fun checkEmailVerified() {
        val user = auth.currentUser
        if (user == null) {
            _emailVerificationState.value = EmailVerificationState.Error("로그인이 필요합니다")
            return
        }

        _emailVerificationState.value = EmailVerificationState.Loading

        user.reload()
            .addOnSuccessListener {
                if (auth.currentUser?.isEmailVerified == true) {
                    _emailVerificationState.value = EmailVerificationState.Verified
                } else {
                    _emailVerificationState.value = EmailVerificationState.NotVerified
                }
            }
            .addOnFailureListener {
                _emailVerificationState.value = EmailVerificationState.Error("확인에 실패했습니다")
            }
    }

    fun resendVerificationEmail() {
        val user = auth.currentUser
        if (user == null) {
            _emailVerificationState.value = EmailVerificationState.Error("로그인이 필요합니다")
            return
        }

        user.sendEmailVerification()
            .addOnSuccessListener {
                _emailVerificationState.value = EmailVerificationState.EmailSent
            }
            .addOnFailureListener {
                _emailVerificationState.value = EmailVerificationState.Error("이메일 재전송에 실패했습니다")
            }
    }

    fun resetEmailVerificationState() {
        _emailVerificationState.value = EmailVerificationState.Idle
    }

    // =====================
    // SetupProfileScreen 용
    // =====================
    private val _profileSaveState = MutableStateFlow<ProfileSaveState>(ProfileSaveState.Idle)
    val profileSaveState: StateFlow<ProfileSaveState> = _profileSaveState

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
        context: android.content.Context
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _profileSaveState.value = ProfileSaveState.Error("로그인이 필요합니다")
            return
        }

        _profileSaveState.value = ProfileSaveState.Loading

        if (imageUris.isEmpty()) {
            // 이미지 없으면 바로 저장
            saveProfileData(userId, name, age, department, mbti, bio, height, location, interests, foodLikes, foodDislikes, emptyList())
            return
        }

        // 이미지 있으면 Storage 업로드 후 저장
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
                            saveProfileData(userId, name, age, department, mbti, bio, height, location, interests, foodLikes, foodDislikes, imageUrls)
                        }
                    }
                }
                .addOnFailureListener {
                    _profileSaveState.value = ProfileSaveState.Error("이미지 업로드에 실패했습니다")
                }
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
        profileImages: List<String>
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
            .addOnSuccessListener {
                _profileSaveState.value = ProfileSaveState.Success
            }
            .addOnFailureListener {
                _profileSaveState.value = ProfileSaveState.Error("프로필 저장에 실패했습니다")
            }
    }

    fun resetProfileSaveState() {
        _profileSaveState.value = ProfileSaveState.Idle
    }

    // =====================
    // StudentIdUploadScreen 용
    // =====================
    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState

    fun requestStudentIdVerification(imageUri: android.net.Uri) {
        val userId = auth.currentUser?.uid
        val userEmail = auth.currentUser?.email ?: ""
        if (userId == null) {
            _uploadState.value = UploadState.Error("로그인이 필요합니다")
            return
        }

        _uploadState.value = UploadState.Loading

        // 1. Storage에 학생증 이미지 업로드
        val fileName = "studentId_${userId}_${System.currentTimeMillis()}.jpg"
        val ref = storage.reference.child("studentIds/$userId/$fileName")

        ref.putFile(imageUri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUri ->
                    val imageUrl = downloadUri.toString()

                    // 2. users 문서에 studentIdImageUrl 업데이트
                    db.collection("users").document(userId)
                        .update("studentIdImageUrl", imageUrl)

                    // 3. adminQueue에 저장
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
                                .addOnSuccessListener {
                                    _uploadState.value = UploadState.Success
                                }
                                .addOnFailureListener {
                                    _uploadState.value = UploadState.Error("인증 요청에 실패했습니다")
                                }
                        }
                }
            }
            .addOnFailureListener {
                _uploadState.value = UploadState.Error("이미지 업로드에 실패했습니다")
            }
    }

    fun resetUploadState() {
        _uploadState.value = UploadState.Idle
    }

    // =====================
    // PendingVerificationScreen 용
    // =====================
    private val _verificationCheckState = MutableStateFlow<VerificationCheckState>(VerificationCheckState.Idle)
    val verificationCheckState: StateFlow<VerificationCheckState> = _verificationCheckState

    fun checkVerificationAndRole() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _verificationCheckState.value = VerificationCheckState.Error("로그인이 필요합니다")
            return
        }

        _verificationCheckState.value = VerificationCheckState.Loading

        db.collection("users").document(userId)
            .get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { document ->
                val isAdmin = document.getBoolean("isAdmin") ?: false
                val isVerified = document.getBoolean("isVerified") ?: false

                _isAdmin.value = isAdmin

                _verificationCheckState.value = when {
                    isAdmin -> VerificationCheckState.Admin
                    isVerified -> VerificationCheckState.Verified
                    else -> VerificationCheckState.NotYet
                }
            }
            .addOnFailureListener {
                _verificationCheckState.value = VerificationCheckState.Error("확인에 실패했습니다")
            }
    }

    fun resetVerificationCheckState() {
        _verificationCheckState.value = VerificationCheckState.Idle
    }
}