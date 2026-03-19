package com.bugzero.meety.ui.admin

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// 학생증 인증 요청 데이터 클래스
data class VerificationRequest(
    val requestId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val studentIdImageUrl: String = "",
    val status: String = "pending"
)

// 승인/거절 액션 상태
sealed class AdminActionState {
    object Idle : AdminActionState()
    object Loading : AdminActionState()
    data class Success(val message: String) : AdminActionState()
    data class Error(val message: String) : AdminActionState()
}

class AdminViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    // 대기 중인 요청 목록
    private val _requests = MutableStateFlow<List<VerificationRequest>>(emptyList())
    val requests: StateFlow<List<VerificationRequest>> = _requests

    // 승인/거절 액션 상태
    private val _actionState = MutableStateFlow<AdminActionState>(AdminActionState.Idle)
    val actionState: StateFlow<AdminActionState> = _actionState

    init {
        fetchPendingRequests()
    }

    // 대기 중인 학생증 인증 요청 목록 가져오기
    fun fetchPendingRequests() {
        db.collection("adminQueue")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val list = snapshot?.documents?.map { doc ->
                    VerificationRequest(
                        requestId = doc.id,
                        userId = doc.getString("userId") ?: "",
                        userName = doc.getString("userName") ?: "",
                        userEmail = doc.getString("userEmail") ?: "",
                        studentIdImageUrl = doc.getString("studentIdImageUrl") ?: "",
                        status = doc.getString("status") ?: "pending"
                    )
                } ?: emptyList()

                _requests.value = list
            }
    }

    // 승인
    fun approveRequest(requestId: String, userId: String) {
        _actionState.value = AdminActionState.Loading

        // 1. users/{userId}.isVerified = true
        db.collection("users").document(userId)
            .update("isVerified", true)
            .addOnSuccessListener {
                // 2. adminQueue/{requestId}.status = "approved"
                db.collection("adminQueue").document(requestId)
                    .update("status", "approved")
                    .addOnSuccessListener {
                        _actionState.value = AdminActionState.Success("✅ 승인 완료했습니다")
                    }
                    .addOnFailureListener {
                        _actionState.value = AdminActionState.Error("상태 업데이트에 실패했습니다")
                    }
            }
            .addOnFailureListener {
                _actionState.value = AdminActionState.Error("승인에 실패했습니다")
            }
    }

    // 거절
    fun rejectRequest(requestId: String, userId: String) {
        _actionState.value = AdminActionState.Loading

        // adminQueue/{requestId}.status = "rejected"
        db.collection("adminQueue").document(requestId)
            .update("status", "rejected")
            .addOnSuccessListener {
                _actionState.value = AdminActionState.Success("❌ 거절 처리했습니다")
            }
            .addOnFailureListener {
                _actionState.value = AdminActionState.Error("거절 처리에 실패했습니다")
            }
    }

    fun resetActionState() {
        _actionState.value = AdminActionState.Idle
    }
}