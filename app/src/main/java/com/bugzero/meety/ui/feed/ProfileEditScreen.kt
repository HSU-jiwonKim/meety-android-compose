package com.bugzero.meety.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugzero.meety.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    onBackClick: () -> Unit = {},
    repository: FeedRepository = remember { FeedRepository() }
) {
    var name by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var mbti by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var saveSuccess by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // 화면 진입 시 Firebase에서 프로필 로드
    LaunchedEffect(Unit) {
        val result = repository.fetchMyProfile()
        result.onSuccess { data ->
            name = (data["name"] as? String) ?: ""
            department = (data["department"] as? String) ?: ""
            mbti = (data["mbti"] as? String) ?: ""
            bio = (data["bio"] as? String) ?: ""
            isLoading = false
        }.onFailure {
            errorMessage = it.message
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("프로필 수정", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로", tint = Purple)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator(color = Purple)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FeedConstants.BackgroundGray)
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 에러 메시지
                if (errorMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            errorMessage ?: "",
                            modifier = Modifier.padding(12.dp),
                            color = FeedConstants.ErrorRed,
                            fontSize = 13.sp
                        )
                    }
                }

                // 저장 성공 메시지
                if (saveSuccess) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "프로필이 저장되었습니다!",
                            modifier = Modifier.padding(12.dp),
                            color = Color(0xFF16A34A),
                            fontSize = 13.sp
                        )
                    }
                }

                // 입력 필드들
                ProfileField(label = "이름", value = name, onValueChange = { name = it })
                ProfileField(label = "학과", value = department, onValueChange = { department = it })
                ProfileField(label = "MBTI", value = mbti, onValueChange = { mbti = it })
                ProfileField(
                    label = "자기소개",
                    value = bio,
                    onValueChange = { bio = it },
                    singleLine = false,
                    minLines = 3
                )

                // 저장 버튼
                Button(
                    onClick = {
                        scope.launch {
                            isSaving = true
                            saveSuccess = false
                            errorMessage = null

                            val fields = mapOf(
                                "name" to name,
                                "department" to department,
                                "mbti" to mbti,
                                "bio" to bio
                            )

                            repository.updateMyProfile(fields)
                                .onSuccess { saveSuccess = true }
                                .onFailure { errorMessage = it.message ?: "저장에 실패했습니다." }

                            isSaving = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Purple),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("저장하기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Gray700)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = singleLine,
                minLines = minLines
            )
        }
    }
}
