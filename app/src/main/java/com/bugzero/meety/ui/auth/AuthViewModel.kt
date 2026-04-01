package com.bugzero.meety.ui.auth

import androidx.lifecycle.ViewModel
import com.bugzero.meety.data.repository.UserRepository
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

sealed class PasswordResetState {
    object Idle : PasswordResetState()
    object Loading : PasswordResetState()
    object Success : PasswordResetState()
    data class Error(val message: String) : PasswordResetState()
}

class AuthViewModel : ViewModel() {

    private val userRepository = UserRepository()

    // =====================
    // isAdmin 상태 (bottom nav용)
    // =====================
    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin

    fun loadUserRole() {
        userRepository.loadUserRole { isAdmin ->
            _isAdmin.value = isAdmin
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
        userRepository.login(
            email = email,
            password = password,
            onSuccess = { _authState.value = AuthState.Success },
            onBanned = {
                _authState.value = AuthState.Error("이용이 제한된 계정입니다\n문의: meety@hansung.ac.kr")
            },
            onFailure = { _authState.value = AuthState.Error(it) }
        )
    }

    fun checkAutoLogin(): Boolean = userRepository.checkAutoLogin()

    fun logout() {
        userRepository.logout()
        _isAdmin.value = false
        _authState.value = AuthState.Idle
    }

    fun resetAuthState() { _authState.value = AuthState.Idle }

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
        userRepository.signUp(
            email = email,
            password = password,
            name = name,
            onSuccess = { _signUpState.value = SignUpState.Success },
            onFailure = { _signUpState.value = SignUpState.Error(it) }
        )
    }

    fun resetSignUpState() { _signUpState.value = SignUpState.Idle }

    // =====================
    // EmailVerification 용
    // =====================
    private val _emailVerificationState = MutableStateFlow<EmailVerificationState>(EmailVerificationState.Idle)
    val emailVerificationState: StateFlow<EmailVerificationState> = _emailVerificationState

    fun checkEmailVerified() {
        _emailVerificationState.value = EmailVerificationState.Loading
        userRepository.checkEmailVerified(
            onVerified = { _emailVerificationState.value = EmailVerificationState.Verified },
            onNotVerified = { _emailVerificationState.value = EmailVerificationState.NotVerified },
            onFailure = { _emailVerificationState.value = EmailVerificationState.Error(it) }
        )
    }

    fun resendVerificationEmail() {
        userRepository.resendVerificationEmail(
            onSuccess = { _emailVerificationState.value = EmailVerificationState.EmailSent },
            onFailure = { _emailVerificationState.value = EmailVerificationState.Error(it) }
        )
    }

    fun resetEmailVerificationState() { _emailVerificationState.value = EmailVerificationState.Idle }

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
        _profileSaveState.value = ProfileSaveState.Loading
        userRepository.saveProfile(
            name = name,
            age = age,
            department = department,
            mbti = mbti,
            bio = bio,
            height = height,
            location = location,
            interests = interests,
            foodLikes = foodLikes,
            foodDislikes = foodDislikes,
            imageUris = imageUris,
            onSuccess = { _profileSaveState.value = ProfileSaveState.Success },
            onFailure = { _profileSaveState.value = ProfileSaveState.Error(it) }
        )
    }

    fun resetProfileSaveState() { _profileSaveState.value = ProfileSaveState.Idle }

    // =====================
    // StudentIdUploadScreen 용
    // =====================
    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState

    fun requestStudentIdVerification(imageUri: android.net.Uri) {
        _uploadState.value = UploadState.Loading
        userRepository.requestStudentIdVerification(
            imageUri = imageUri,
            onSuccess = { _uploadState.value = UploadState.Success },
            onFailure = { _uploadState.value = UploadState.Error(it) }
        )
    }

    fun resetUploadState() { _uploadState.value = UploadState.Idle }

    // =====================
    // PendingVerificationScreen 용
    // =====================
    private val _verificationCheckState = MutableStateFlow<VerificationCheckState>(VerificationCheckState.Idle)
    val verificationCheckState: StateFlow<VerificationCheckState> = _verificationCheckState

    fun checkVerificationAndRole() {
        _verificationCheckState.value = VerificationCheckState.Loading
        userRepository.checkVerificationAndRole(
            onAdmin = {
                _isAdmin.value = true
                _verificationCheckState.value = VerificationCheckState.Admin
            },
            onVerified = {
                _verificationCheckState.value = VerificationCheckState.Verified
            },
            onNotYet = {
                _verificationCheckState.value = VerificationCheckState.NotYet
            },
            onFailure = {
                _verificationCheckState.value = VerificationCheckState.Error(it)
            }
        )
    }

    fun resetVerificationCheckState() { _verificationCheckState.value = VerificationCheckState.Idle }

    // =====================
    // 비밀번호 찾기 용
    // =====================
    private val _passwordResetState = MutableStateFlow<PasswordResetState>(PasswordResetState.Idle)
    val passwordResetState: StateFlow<PasswordResetState> = _passwordResetState

    fun sendPasswordResetEmail(email: String) {
        if (email.isEmpty()) {
            _passwordResetState.value = PasswordResetState.Error("이메일을 입력해주세요")
            return
        }
        if (!email.endsWith("@hansung.ac.kr")) {
            _passwordResetState.value = PasswordResetState.Error("한성대학교 이메일만 사용 가능합니다")
            return
        }
        _passwordResetState.value = PasswordResetState.Loading
        userRepository.sendPasswordResetEmail(
            email = email,
            onSuccess = { _passwordResetState.value = PasswordResetState.Success },
            onFailure = { _passwordResetState.value = PasswordResetState.Error(it) }
        )
    }

    fun resetPasswordResetState() { _passwordResetState.value = PasswordResetState.Idle }

    // =====================
    // 실시간 차단 감지
    // =====================
    fun startBanListener(onBanned: () -> Unit) {
        userRepository.startBanListener {
            _isAdmin.value = false
            _authState.value = AuthState.Idle
            onBanned()
        }
    }
}