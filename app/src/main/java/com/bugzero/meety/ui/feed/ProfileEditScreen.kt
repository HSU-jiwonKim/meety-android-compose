package com.bugzero.meety.ui.feed

<<<<<<< HEAD
import androidx.compose.runtime.Composable

@Composable
fun ProfileEditScreen() {
    // TODO: B 담당 - 내 프로필 수정 화면 구현
=======
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(onBackClick: () -> Unit = {}) {
    var name by remember { mutableStateOf("김민지") }
    var department by remember { mutableStateOf("컴퓨터공학과") }
    var mbti by remember { mutableStateOf("ENFP") }
    var bio by remember { mutableStateOf("카페 투어 좋아해요 ☕️") }

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
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFFF9FAFB)).padding(padding)
                .padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            listOf(
                Triple("이름", name, { v: String -> name = v }),
                Triple("학과", department, { v: String -> department = v }),
                Triple("MBTI", mbti, { v: String -> mbti = v }),
                Triple("자기소개", bio, { v: String -> bio = v }),
            ).forEach { (label, value, onChange) ->
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
                            onValueChange = onChange,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = label != "자기소개",
                            minLines = if (label == "자기소개") 3 else 1
                        )
                    }
                }
            }
            Button(
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Purple)
            ) { Text("저장하기", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }
>>>>>>> ca178ecdf5d6d6fc1948225597641ad0fce1c061
}