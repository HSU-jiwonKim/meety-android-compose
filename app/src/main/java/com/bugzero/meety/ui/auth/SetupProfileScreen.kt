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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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

val DEPARTMENT_OPTIONS = listOf(
    // 학부·학과
    "컴퓨터공학부", "AI응용학과", "융합보안학과", "호텔외식경영학과",
    "문학문화콘텐츠학과", "예술학부",
    // 트랙
    "경영트랙", "경제트랙", "부동산트랙", "취미무역학트랙", "행정트랙",
    "사회트랙", "상담심리트랙",
    "한국어문학트랙", "역사문화트랙", "영어영문학트랙",
    "빅데이터트랙", "전자트랙", "기계설계트랙", "기계자동화트랙",
    "산업공학트랙", "신소재화학트랙", "식품영양학트랙",
    "미디어디자인트랙", "커뮤니케이션디자인트랙", "인테리어디자인트랙",
    "제품서비스디자인트랙", "뷰티디자인매니지먼트트랙", "스포츠미디어트랙"
)

/** 시/도 → 시/군/구 계층 목록. ProfileEditScreen 에서도 공유한다. */
val LOCATION_MAP: Map<String, List<String>> = mapOf(
    "서울" to listOf(
        "강남구", "강동구", "강북구", "강서구", "관악구",
        "광진구", "구로구", "금천구", "노원구", "도봉구",
        "동대문구", "동작구", "마포구", "서대문구", "서초구",
        "성동구", "성북구", "송파구", "양천구", "영등포구",
        "용산구", "은평구", "종로구", "중구", "중랑구"
    ),
    "경기도" to listOf(
        "수원시", "성남시", "고양시", "용인시", "부천시",
        "안산시", "안양시", "남양주시", "화성시", "평택시",
        "의정부시", "시흥시", "파주시", "광명시", "김포시",
        "광주시", "군포시", "하남시", "오산시", "양주시",
        "구리시", "안성시", "포천시", "의왕시", "여주시",
        "동두천시", "과천시", "가평군", "양평군", "연천군"
    ),
    "인천" to listOf(
        "중구", "동구", "미추홀구", "연수구", "남동구",
        "부평구", "계양구", "서구", "강화군", "옹진군"
    ),
    "강원" to listOf(
        "춘천시", "원주시", "강릉시", "동해시", "태백시",
        "속초시", "삼척시", "홍천군", "횡성군", "영월군",
        "평창군", "정선군", "철원군", "화천군", "양구군",
        "인제군", "고성군", "양양군"
    ),
    "충청북도" to listOf(
        "청주시", "충주시", "제천시", "보은군", "옥천군",
        "영동군", "증평군", "진천군", "괴산군", "음성군", "단양군"
    ),
    "충청남도" to listOf(
        "천안시", "공주시", "보령시", "아산시", "서산시",
        "논산시", "계룡시", "당진시", "금산군", "부여군",
        "서천군", "청양군", "홍성군", "예산군", "태안군"
    ),
    "세종" to listOf("세종시"),
    "전북" to listOf(
        "전주시", "군산시", "익산시", "정읍시", "남원시",
        "김제시", "완주군", "진안군", "무주군", "장수군",
        "임실군", "순창군", "고창군", "부안군"
    ),
    "전라남도" to listOf(
        "목포시", "여수시", "순천시", "나주시", "광양시",
        "담양군", "곡성군", "구례군", "고흥군", "보성군",
        "화순군", "장흥군", "강진군", "해남군", "영암군",
        "무안군", "함평군", "영광군", "장성군", "완도군",
        "진도군", "신안군"
    ),
    "광주" to listOf("동구", "서구", "남구", "북구", "광산구"),
    "대전" to listOf("동구", "중구", "서구", "유성구", "대덕구"),
    "부산" to listOf(
        "중구", "서구", "동구", "영도구", "부산진구",
        "동래구", "남구", "북구", "해운대구", "사하구",
        "금정구", "강서구", "연제구", "수영구", "사상구", "기장군"
    ),
    "대구" to listOf(
        "중구", "동구", "서구", "남구", "북구",
        "수성구", "달서구", "달성군"
    ),
    "울산" to listOf("중구", "남구", "동구", "북구", "울주군"),
    "경상북도" to listOf(
        "포항시", "경주시", "김천시", "안동시", "구미시",
        "영주시", "영천시", "상주시", "문경시", "경산시",
        "의성군", "청송군", "영양군", "영덕군", "청도군",
        "고령군", "성주군", "칠곡군", "예천군", "봉화군",
        "울진군", "울릉군"
    ),
    "경상남도" to listOf(
        "창원시", "진주시", "통영시", "사천시", "김해시",
        "밀양시", "거제시", "양산시", "의령군", "함안군",
        "창녕군", "고성군", "남해군", "하동군", "산청군",
        "함양군", "거창군", "합천군"
    ),
    "제주" to listOf("제주시", "서귀포시")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupProfileScreen(
    onComplete: () -> Unit = {},
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var selectedMbti by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var locationProvince by remember { mutableStateOf("") }
    var locationDistrict by remember { mutableStateOf("") }
    var provinceExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    var selectedInterests by remember { mutableStateOf(setOf<String>()) }
    var selectedFoodLikes by remember { mutableStateOf(setOf<String>()) }
    var selectedFoodDislikes by remember { mutableStateOf(setOf<String>()) }
    var mbtiExpanded by remember { mutableStateOf(false) }
    var departmentExpanded by remember { mutableStateOf(false) }
    var selectedImages by remember { mutableStateOf(listOf<Uri>()) }

    val profileSaveState by viewModel.profileSaveState.collectAsState()

    // 저장 성공 시 다음 화면으로
    LaunchedEffect(profileSaveState) {
        if (profileSaveState is ProfileSaveState.Success) {
            viewModel.resetProfileSaveState()
            onComplete()
        }
    }

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
                    onClick = {
                        viewModel.saveProfile(
                            name = name,
                            age = age,
                            department = department,
                            mbti = selectedMbti,
                            bio = bio,
                            height = height,
                            location = listOf(locationProvince, locationDistrict)
                                .filter { it.isNotBlank() }.joinToString(" "),
                            interests = selectedInterests.toList(),
                            foodLikes = selectedFoodLikes.toList(),
                            foodDislikes = selectedFoodDislikes.toList(),
                            imageUris = selectedImages,
                            context = context
                        )
                    },
                    enabled = isFormValid && profileSaveState !is ProfileSaveState.Loading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Purple)
                ) {
                    if (profileSaveState is ProfileSaveState.Loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("완료하고 시작하기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
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

            // 에러 메시지
            if (profileSaveState is ProfileSaveState.Error) {
                Text(
                    (profileSaveState as ProfileSaveState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
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
                        // 학과 드롭다운
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = department,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { departmentExpanded = true },
                                label = { Text("학과") },
                                placeholder = { Text("학과 선택", color = Gray400) },
                                trailingIcon = {
                                    Icon(
                                        if (departmentExpanded) Icons.Default.KeyboardArrowUp
                                        else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null, tint = Gray400
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = Gray900,
                                    disabledBorderColor = if (departmentExpanded) Purple else Gray400,
                                    disabledLabelColor = if (departmentExpanded) Purple else Gray400,
                                    disabledPlaceholderColor = Gray400,
                                    disabledTrailingIconColor = Gray400,
                                    disabledContainerColor = Color.White
                                )
                            )
                            DropdownMenu(
                                expanded = departmentExpanded,
                                onDismissRequest = { departmentExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.45f)
                                    .heightIn(max = 280.dp)
                                    .background(Color.White)
                            ) {
                                DEPARTMENT_OPTIONS.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option, color = Color(0xFF111111), fontSize = 15.sp) },
                                        onClick = { department = option; departmentExpanded = false },
                                        modifier = Modifier.background(Color.White)
                                    )
                                }
                            }
                        }
                    }

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
                    OutlinedTextField(
                        value = height, onValueChange = { height = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("키 (cm)") },
                        placeholder = { Text("170", color = Gray400) },
                        shape = RoundedCornerShape(12.dp), singleLine = true
                    )
                    // 지역 선택 — 시/도 → 시/군/구 2단계
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // 시/도 드롭다운
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = locationProvince,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth().clickable { provinceExpanded = true },
                                label = { Text("시/도") },
                                placeholder = { Text("선택", color = Gray400) },
                                trailingIcon = {
                                    Icon(
                                        if (provinceExpanded) Icons.Default.KeyboardArrowUp
                                        else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null, tint = Gray400
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = Gray900,
                                    disabledBorderColor = if (provinceExpanded) Purple else Gray400,
                                    disabledLabelColor = if (provinceExpanded) Purple else Gray400,
                                    disabledPlaceholderColor = Gray400,
                                    disabledTrailingIconColor = Gray400,
                                    disabledContainerColor = Color.White
                                )
                            )
                            DropdownMenu(
                                expanded = provinceExpanded,
                                onDismissRequest = { provinceExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.45f).heightIn(max = 280.dp).background(Color.White)
                            ) {
                                LOCATION_MAP.keys.forEach { province ->
                                    DropdownMenuItem(
                                        text = { Text(province, color = Color(0xFF111111), fontSize = 15.sp) },
                                        onClick = {
                                            locationProvince = province
                                            locationDistrict = "" // 시/도 바뀌면 시/군/구 초기화
                                            provinceExpanded = false
                                        },
                                        modifier = Modifier.background(Color.White)
                                    )
                                }
                            }
                        }
                        // 시/군/구 드롭다운
                        Box(modifier = Modifier.weight(1f)) {
                            val districtList = LOCATION_MAP[locationProvince] ?: emptyList()
                            OutlinedTextField(
                                value = locationDistrict,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth().clickable {
                                    if (locationProvince.isNotBlank()) districtExpanded = true
                                },
                                label = { Text("시/군/구") },
                                placeholder = { Text(if (locationProvince.isBlank()) "시/도 먼저" else "선택", color = Gray400) },
                                trailingIcon = {
                                    Icon(
                                        if (districtExpanded) Icons.Default.KeyboardArrowUp
                                        else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null, tint = Gray400
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = Gray900,
                                    disabledBorderColor = if (districtExpanded) Purple else Gray400,
                                    disabledLabelColor = if (districtExpanded) Purple else Gray400,
                                    disabledPlaceholderColor = Gray400,
                                    disabledTrailingIconColor = Gray400,
                                    disabledContainerColor = Color.White
                                )
                            )
                            DropdownMenu(
                                expanded = districtExpanded,
                                onDismissRequest = { districtExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.45f).heightIn(max = 280.dp).background(Color.White)
                            ) {
                                districtList.forEach { district ->
                                    DropdownMenuItem(
                                        text = { Text(district, color = Color(0xFF111111), fontSize = 15.sp) },
                                        onClick = { locationDistrict = district; districtExpanded = false },
                                        modifier = Modifier.background(Color.White)
                                    )
                                }
                            }
                        }
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