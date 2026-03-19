package com.bugzero.meety.ui.auth

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bugzero.meety.ui.theme.*

val MBTI_TYPES = listOf(
    "ISTJ", "ISFJ", "INFJ", "INTJ",
    "ISTP", "ISFP", "INFP", "INTP",
    "ESTP", "ESFP", "ENFP", "ENTP",
    "ESTJ", "ESFJ", "ENFJ", "ENTJ"
)

val INTEREST_TAGS = listOf(
    "카페투어", "영화감상", "독서", "운동", "게임",
    "음악감상", "여행", "맛집탐방", "사진", "그림",
    "춤", "노래", "요리", "술", "산책",
    "등산", "캠핑", "드라이브", "쇼핑", "전시회"
)

val FOOD_OPTIONS = listOf(
    "한식", "중식", "일식", "양식", "분식",
    "치킨", "피자", "햄버거", "카페", "디저트",
    "고기", "해산물", "샐러드", "면요리", "찜/탕"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupProfileScreen(onComplete: () -> Unit = {}) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var selectedMbti by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedInterests by remember { mutableStateOf(setOf<String>()) }
    var selectedFoodLikes by remember { mutableStateOf(setOf<String>()) }
    var selectedFoodDislikes by remember { mutableStateOf(setOf<String>()) }
    var mbtiExpanded by remember { mutableStateOf(false) }
    var selectedImages by remember { mutableStateOf(listOf<Uri>()) }

    // 갤러리에서 여러 장 선택
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val combined = (selectedImages + uris).distinct().take(6)
        selectedImages = combined
    }

    val isFormValid = name.isNotEmpty() && age.isNotEmpty() &&
            department.isNotEmpty() && selectedMbti.isNotEmpty()

    Scaffold(
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
                Button(
                    onClick = onComplete,
                    enabled = isFormValid,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Purple)
                ) {
                    Text("완료하고 시작하기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9FAFB))
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 헤더
            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 20.dp)) {
                Text("프로필 설정", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Gray900)
                Text("회원님을 소개해주세요", fontSize = 14.sp, color = Gray500)
            }

            // 사진 업로드
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("프로필 사진", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Gray900)
                        Text(" (최대 6장)", fontSize = 13.sp, color = Gray500)
                        Spacer(modifier = Modifier.weight(1f))
                        Text("${selectedImages.size}/6", fontSize = 13.sp, color = Gray500)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 선택된 사진 미리보기
                        selectedImages.forEach { uri ->
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                // X 버튼 (삭제)
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.TopEnd)
                                        .background(Color(0x99000000), RoundedCornerShape(10.dp))
                                        .clickable { selectedImages = selectedImages - uri },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "삭제",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                        // 추가 버튼 (6장 미만일 때만)
                        if (selectedImages.size < 6) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(2.dp, Gray200, RoundedCornerShape(12.dp))
                                    .background(Gray100)
                                    .clickable { imagePicker.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = Gray400,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text("사진 추가", fontSize = 10.sp, color = Gray400)
                                }
                            }
                        }
                    }
                }
            }

            // 기본 정보
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("기본 정보", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Gray900)
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("이름") },
                        placeholder = { Text("이름을 입력하세요", color = Gray400) },
                        shape = RoundedCornerShape(12.dp), singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = age, onValueChange = { age = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("나이") },
                            placeholder = { Text("23", color = Gray400) },
                            shape = RoundedCornerShape(12.dp), singleLine = true
                        )
                        OutlinedTextField(
                            value = department, onValueChange = { department = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("학과") },
                            placeholder = { Text("컴퓨터공학과", color = Gray400) },
                            shape = RoundedCornerShape(12.dp), singleLine = true
                        )
                    }

                    // MBTI - 커스텀 드롭다운 (흰 배경 보장)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedMbti,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { mbtiExpanded = true },
                            label = { Text("MBTI") },
                            placeholder = { Text("MBTI를 선택하세요", color = Gray400) },
                            trailingIcon = {
                                Icon(
                                    if (mbtiExpanded) Icons.Default.KeyboardArrowUp
                                    else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Gray400
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Gray900,
                                disabledBorderColor = if (mbtiExpanded) Purple else Gray400,
                                disabledLabelColor = if (mbtiExpanded) Purple else Gray400,
                                disabledPlaceholderColor = Gray400,
                                disabledTrailingIconColor = Gray400,
                                disabledContainerColor = Color.White
                            )
                        )
                        DropdownMenu(
                            expanded = mbtiExpanded,
                            onDismissRequest = { mbtiExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(Color.White)
                        ) {
                            MBTI_TYPES.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type, color = Color(0xFF111111), fontSize = 15.sp) },
                                    onClick = { selectedMbti = type; mbtiExpanded = false },
                                    modifier = Modifier.background(Color.White)
                                )
                            }
                        }
                    }
                }
            }

            // 추가 정보
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column {
                        Text("추가 정보", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Gray900)
                        Text("선택 사항이며, 입력 시 다른 사용자에게 공개됩니다", fontSize = 12.sp, color = Gray400)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = height, onValueChange = { height = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("키 (cm)") },
                            placeholder = { Text("170", color = Gray400) },
                            shape = RoundedCornerShape(12.dp), singleLine = true
                        )
                        OutlinedTextField(
                            value = location, onValueChange = { location = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("거주 지역") },
                            placeholder = { Text("성북구", color = Gray400) },
                            shape = RoundedCornerShape(12.dp), singleLine = true
                        )
                    }
                }
            }

            // 관심사
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row {
                        Text("관심사", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Gray900)
                        Text(" (최대 10개)", fontSize = 13.sp, color = Gray500)
                        Spacer(modifier = Modifier.weight(1f))
                        Text("${selectedInterests.size}/10", fontSize = 13.sp, color = Gray500)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        INTEREST_TAGS.forEach { tag ->
                            val isSelected = tag in selectedInterests
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) Purple else Gray100)
                                    .clickable {
                                        selectedInterests = if (isSelected) selectedInterests - tag
                                        else if (selectedInterests.size < 10) selectedInterests + tag
                                        else selectedInterests
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(tag, fontSize = 13.sp, color = if (isSelected) Color.White else Gray700)
                            }
                        }
                    }
                }
            }

            // 자기소개
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("자기소개", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Gray900)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = bio, onValueChange = { if (it.length <= 200) bio = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("자신을 소개해주세요", color = Gray400) },
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3, maxLines = 5
                    )
                    Text(
                        "${bio.length}/200",
                        fontSize = 12.sp,
                        color = Gray400,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }

            // 좋아하는 음식
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row {
                        Text("😋 좋아하는 음식", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Gray900)
                        Spacer(modifier = Modifier.weight(1f))
                        Text("${selectedFoodLikes.size}/5", fontSize = 13.sp, color = Gray500)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FOOD_OPTIONS.forEach { food ->
                            val isSelected = food in selectedFoodLikes
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) Color(0xFF22C55E) else Gray100)
                                    .clickable {
                                        selectedFoodLikes = if (isSelected) selectedFoodLikes - food
                                        else if (selectedFoodLikes.size < 5) selectedFoodLikes + food
                                        else selectedFoodLikes
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(food, fontSize = 13.sp, color = if (isSelected) Color.White else Gray700)
                            }
                        }
                    }
                }
            }

            // 싫어하는 음식
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row {
                        Text("😣 싫어하는 음식", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Gray900)
                        Spacer(modifier = Modifier.weight(1f))
                        Text("${selectedFoodDislikes.size}/5", fontSize = 13.sp, color = Gray500)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FOOD_OPTIONS.forEach { food ->
                            val isSelected = food in selectedFoodDislikes
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) Color(0xFFEF4444) else Gray100)
                                    .clickable {
                                        selectedFoodDislikes = if (isSelected) selectedFoodDislikes - food
                                        else if (selectedFoodDislikes.size < 5) selectedFoodDislikes + food
                                        else selectedFoodDislikes
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(food, fontSize = 13.sp, color = if (isSelected) Color.White else Gray700)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}