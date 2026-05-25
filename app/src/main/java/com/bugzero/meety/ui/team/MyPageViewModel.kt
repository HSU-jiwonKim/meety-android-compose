package com.bugzero.meety.ui.team

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class MyPageScreenState(
    val isLoading: Boolean = true,
    val uiState: UserProfileUiState? = null,
    val errorMessage: String? = null
)

class MyPageViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _screenState = MutableStateFlow(MyPageScreenState())
    val screenState: StateFlow<MyPageScreenState> = _screenState

    private var listenerRegistration: ListenerRegistration? = null

    init {
        loadMyProfile()
    }

    fun loadMyProfile() {
        listenerRegistration?.remove()
        listenerRegistration = null

        val userId = auth.currentUser?.uid

        if (userId == null) {
            _screenState.value = MyPageScreenState(
                isLoading = false,
                uiState = null,
                errorMessage = null
            )
            return
        }

        _screenState.value = MyPageScreenState(isLoading = true)

        listenerRegistration = firestore.collection("users")
            .document(userId)
            .addSnapshotListener { document, error ->
                if (auth.currentUser == null) {
                    _screenState.value = MyPageScreenState(
                        isLoading = false,
                        uiState = null,
                        errorMessage = null
                    )
                    return@addSnapshotListener
                }

                if (error != null) {
                    _screenState.value = MyPageScreenState(
                        isLoading = false,
                        uiState = null,
                        errorMessage = error.message ?: "프로필 정보를 불러오지 못했습니다."
                    )
                    return@addSnapshotListener
                }

                if (document == null || !document.exists()) {
                    _screenState.value = MyPageScreenState(
                        isLoading = false,
                        uiState = null,
                        errorMessage = "사용자 정보가 없습니다."
                    )
                    return@addSnapshotListener
                }

                val name = document.getString("name") ?: ""
                val age = document.getLong("age")?.toInt() ?: 0
                val department = document.getString("department") ?: ""
                val height = document.getLong("height")?.toInt() ?: 0
                val location = document.getString("location") ?: ""
                val mbti = document.getString("mbti") ?: ""
                val bio = document.getString("bio") ?: ""

                val interests = document.get("interests") as? List<String> ?: emptyList()
                val foodLikes = document.get("foodLikes") as? List<String> ?: emptyList()
                val foodDislikes = document.get("foodDislikes") as? List<String> ?: emptyList()
                val profileImages = (document.get("profileImages") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                val mainProfileImageUrl = document.getString("mainProfileImageUrl") ?: profileImages.firstOrNull().orEmpty()

                val scheduleMap = mutableMapOf<String, List<String>>()
                val rawSchedule = document.get("schedule") as? Map<*, *> ?: emptyMap<Any, Any>()

                for ((key, value) in rawSchedule) {
                    val day = key as? String ?: continue
                    val timeList = value as? List<*> ?: emptyList<Any>()
                    scheduleMap[day] = timeList.mapNotNull { it as? String }
                }

                val uiState = UserProfileUiState(
                    name = name,
                    age = age,
                    school = "한성대학교",
                    department = department,
                    height = height,
                    location = location,
                    mbti = mbti,
                    bio = bio,
                    interests = interests,
                    foodLikes = foodLikes,
                    foodDislikes = foodDislikes,
                    profileImages = profileImages,
                    mainProfileImageUrl = mainProfileImageUrl,
                    schedule = scheduleMap
                )

                _screenState.value = MyPageScreenState(
                    isLoading = false,
                    uiState = uiState,
                    errorMessage = null
                )
            }
    }

    fun onAddPhotoClick(imageUri: Uri) {
        uploadProfileImage(imageUri = imageUri, setAsMain = false)
    }

    fun onAddMainPhotoClick(imageUri: Uri) {
        uploadProfileImage(imageUri = imageUri, setAsMain = true)
    }

    private fun uploadProfileImage(imageUri: Uri, setAsMain: Boolean) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            _screenState.value = _screenState.value.copy(
                isLoading = false,
                errorMessage = null
            )
            return
        }

        _screenState.value = _screenState.value.copy(isLoading = true)

        val fileName = "profile_${System.currentTimeMillis()}.jpg"
        val imageRef = storage.reference
            .child("users")
            .child(userId)
            .child("profileImages")
            .child(fileName)

        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl
                    .addOnSuccessListener { uri ->
                        val newUrl = uri.toString()
                        val currentImages = _screenState.value.uiState?.profileImages ?: emptyList()
                        val newImages = listOf(newUrl) + currentImages.filter { it.isNotBlank() && it != newUrl }
                        val updateData = mutableMapOf<String, Any>(
                            "profileImages" to newImages
                        )

                        if (setAsMain) {
                            updateData["mainProfileImageUrl"] = newUrl
                        }

                        firestore.collection("users")
                            .document(userId)
                            .update(updateData)
                            .addOnFailureListener { e ->
                                _screenState.value = _screenState.value.copy(
                                    isLoading = false,
                                    errorMessage = e.message ?: "프로필 사진 저장에 실패했습니다."
                                )
                            }
                    }
                    .addOnFailureListener { e ->
                        _screenState.value = _screenState.value.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "이미지 URL을 가져오지 못했습니다."
                        )
                    }
            }
            .addOnFailureListener { e ->
                _screenState.value = _screenState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "이미지 업로드에 실패했습니다."
                )
            }
    }
    fun deleteProfileImage(imageUrl: String) {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(userId)
            .update("profileImages", FieldValue.arrayRemove(imageUrl))
            .addOnFailureListener { e ->
                _screenState.value = _screenState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "사진 삭제에 실패했습니다."
                )
            }
    }

    fun changeMainProfileImage(imageUrl: String) {
        val userId = auth.currentUser?.uid ?: return

        if (imageUrl.isBlank()) return

        firestore.collection("users")
            .document(userId)
            .update("mainProfileImageUrl", imageUrl)
            .addOnFailureListener { e ->
                _screenState.value = _screenState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "프로필 사진 변경에 실패했습니다."
                )
            }
    }
    fun clearListener() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    override fun onCleared() {
        super.onCleared()
        clearListener()
    }
}
