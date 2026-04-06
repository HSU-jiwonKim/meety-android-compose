package com.bugzero.meety.ui.feed

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bugzero.meety.data.repository.FeedRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── MyPageScreen과 동일한 컬러 팔레트 ──────────────────────────────────────────
private val EditPurplePrimary = Color(0xFF9C27B0)
private val EditPurpleLight   = Color(0xFFF3E7FF)
private val EditGradientStart = Color(0xFFB842F5)
private val EditGradientEnd   = Color(0xFFFF4FA3)
private val EditBgColor       = Color(0xFFF8F1F8)
private val EditTextPrimary   = Color(0xFF222222)
private val EditTextSecondary = Color(0xFF666666)
private val EditTextHint      = Color(0xFFBBBBBB)
private val EditDivider       = Color(0xFFF0F0F0)
private val EditUnderlineIdle = Color(0xFFEADFF5)

/**
 * 프로필 수정 화면
 *
 * MyPageScreen의 시각 구조를 그대로 유지하면서 모든 필드를 편집 가능하게 한다.
 *   - 그라데이션 배너 + 프로필 사진 (카메라 배지) + 정보 카드 → 인라인 편집
 *   - 관심사 / 음식 취향 → 칩 에디터 (추가 / 삭제)
 *   - 자기소개 → 멀티라인 OutlinedTextField
 *   - TopAppBar 우측 "저장" 버튼으로 Firestore 업데이트
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    onBackClick: () -> Unit = {},
    repository: FeedRepository = remember { FeedRepository() }
) {
    // ── 편집 상태 ──────────────────────────────────────────────────────────────
    var name          by remember { mutableStateOf("") }
    var age           by remember { mutableStateOf("") }
    var school        by remember { mutableStateOf("") }
    var department    by remember { mutableStateOf("") }
    var height        by remember { mutableStateOf("") }
    var location      by remember { mutableStateOf("") }
    var bio           by remember { mutableStateOf("") }
    var mbti          by remember { mutableStateOf("") }
    var interests     by remember { mutableStateOf(emptyList<String>()) }
    var foodLikes     by remember { mutableStateOf(emptyList<String>()) }
    var foodDislikes  by remember { mutableStateOf(emptyList<String>()) }
    var profileImages by remember { mutableStateOf(emptyList<String>()) }
    var pickedImageUri by remember { mutableStateOf<Uri?>(null) }

    var isLoading    by remember { mutableStateOf(true) }
    var isSaving     by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    // 갤러리 이미지 선택
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> if (uri != null) pickedImageUri = uri }

    // 초기 데이터 로딩
    LaunchedEffect(Unit) {
        repository.fetchMyProfile()
            .onSuccess { data ->
                name       = data["name"]       as? String ?: ""
                age        = ((data["age"]    as? Long)?.toString()
                    ?: (data["age"]    as? Int)?.toString()) ?: ""
                school     = data["school"]     as? String ?: ""
                department = data["department"] as? String ?: ""
                height     = ((data["height"] as? Long)?.toString()
                    ?: (data["height"] as? Int)?.toString()) ?: ""
                location   = data["location"]   as? String ?: ""
                bio        = data["bio"]        as? String ?: ""
                mbti       = data["mbti"]       as? String ?: ""
                interests    = (data["interests"]    as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                foodLikes    = (data["foodLikes"]    as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                foodDislikes = (data["foodDislikes"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                profileImages = (data["profileImages"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            }
            .onFailure { errorMessage = it.message }
        isLoading = false
    }

    // 저장
    fun save() {
        val mbtiTrimmed = mbti.trim().uppercase()
        if (mbtiTrimmed.isNotBlank()) {
            val validMbtiRegex = Regex("^[EI][NS][FT][JP]$")
            if (!validMbtiRegex.matches(mbtiTrimmed)) {
                errorMessage = "MBTI는 4자리 형식으로 입력해주세요 (예: ENFP, INTJ)"
                return
            }
        }

        scope.launch {
            isSaving = true
            errorMessage = null

            val updates = buildMap<String, Any> {
                put("name",         name.trim())
                put("department",   department.trim())
                put("bio",          bio.trim())
                put("mbti",         mbtiTrimmed)
                put("interests",    interests)
                put("foodLikes",    foodLikes)
                put("foodDislikes", foodDislikes)
                school.trim().takeIf { it.isNotBlank() }?.let { put("school", it) }
                location.trim().takeIf { it.isNotBlank() }?.let { put("location", it) }
                age.toIntOrNull()?.let    { put("age",    it) }
                height.toIntOrNull()?.let { put("height", it) }
                // TODO: pickedImageUri를 Firebase Storage에 업로드 후 profileImages에 URL 추가
            }

            repository.updateMyProfile(updates)
                .onSuccess {
                    delay(400)
                    onBackClick()
                }
                .onFailure { errorMessage = it.message ?: "저장에 실패했습니다." }

            isSaving = false
        }
    }

    // ── Scaffold ───────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("프로필 수정", fontWeight = FontWeight.Bold, color = EditTextPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로", tint = EditPurplePrimary)
                    }
                },
                actions = {
                    if (isSaving) {
                        Box(
                            modifier         = Modifier.padding(end = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color       = EditPurplePrimary
                            )
                        }
                    } else {
                        TextButton(onClick = { save() }) {
                            Text(
                                "저장",
                                color      = EditPurplePrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 16.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = EditBgColor
    ) { padding ->

        if (isLoading) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = EditPurplePrimary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding      = PaddingValues(bottom = 40.dp)
        ) {

            // ── 에러 배너 ──
            if (errorMessage != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .background(Color(0xFFFFEBEE), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(errorMessage!!, color = Color(0xFFC62828), fontSize = 13.sp)
                    }
                }
            }

            // ── 헤더: 배너 + 프로필 사진 + 기본 정보 카드 ──
            item {
                EditableHeaderSection(
                    name              = name,          onNameChange       = { name = it },
                    age               = age,           onAgeChange        = { age = it },
                    school            = school,        onSchoolChange     = { school = it },
                    department        = department,    onDepartmentChange = { department = it },
                    height            = height,        onHeightChange     = { height = it },
                    location          = location,      onLocationChange   = { location = it },
                    mbti              = mbti,          onMbtiChange       = { mbti = it.uppercase() },
                    profileImageUrl   = profileImages.firstOrNull() ?: "",
                    pickedImageUri    = pickedImageUri,
                    onProfileImageClick = { imagePickerLauncher.launch("image/*") }
                )
            }

            // ── 관심사 ──
            item {
                EditSectionCard(title = "관심사") {
                    ChipEditor(
                        chips       = interests,
                        onAdd       = { tag -> if (tag !in interests) interests = interests + tag },
                        onRemove    = { tag -> interests = interests.filter { it != tag } },
                        chipColor   = Color(0xFFF3E7FF),
                        textColor   = Color(0xFF8E24AA),
                        addColor    = Color(0xFFE8D6F7),
                        placeholder = "관심사 입력 후 확인"
                    )
                }
            }

            // ── 자기소개 ──
            item {
                EditSectionCard(title = "자기소개") {
                    OutlinedTextField(
                        value         = bio,
                        onValueChange = { bio = it },
                        modifier      = Modifier.fillMaxWidth(),
                        placeholder   = {
                            Text("자신을 자유롭게 소개해보세요", color = EditTextHint, fontSize = 14.sp)
                        },
                        minLines      = 3,
                        shape         = RoundedCornerShape(12.dp),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = EditPurplePrimary,
                            unfocusedBorderColor = EditUnderlineIdle,
                            focusedLabelColor    = EditPurplePrimary
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            color    = EditTextPrimary,
                            fontSize = 14.sp
                        )
                    )
                }
            }

            // ── 음식 취향 ──
            item {
                EditSectionCard(title = "음식 취향") {
                    Text("좋아하는 음식", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    ChipEditor(
                        chips       = foodLikes,
                        onAdd       = { tag -> if (tag !in foodLikes) foodLikes = foodLikes + tag },
                        onRemove    = { tag -> foodLikes = foodLikes.filter { it != tag } },
                        chipColor   = Color(0xFFE6F7E8),
                        textColor   = Color(0xFF2E7D32),
                        addColor    = Color(0xFFD0F0D3),
                        placeholder = "좋아하는 음식 입력"
                    )
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = EditDivider)
                    Spacer(Modifier.height(16.dp))
                    Text("싫어하는 음식", fontWeight = FontWeight.Bold, color = Color(0xFFC62828), fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    ChipEditor(
                        chips       = foodDislikes,
                        onAdd       = { tag -> if (tag !in foodDislikes) foodDislikes = foodDislikes + tag },
                        onRemove    = { tag -> foodDislikes = foodDislikes.filter { it != tag } },
                        chipColor   = Color(0xFFFFE7E7),
                        textColor   = Color(0xFFC62828),
                        addColor    = Color(0xFFFFD0D0),
                        placeholder = "싫어하는 음식 입력"
                    )
                }
            }
        }
    }
}

// ── 헤더 섹션 ─────────────────────────────────────────────────────────────────

@Composable
private fun EditableHeaderSection(
    name: String,           onNameChange: (String) -> Unit,
    age: String,            onAgeChange: (String) -> Unit,
    school: String,         onSchoolChange: (String) -> Unit,
    department: String,     onDepartmentChange: (String) -> Unit,
    height: String,         onHeightChange: (String) -> Unit,
    location: String,       onLocationChange: (String) -> Unit,
    mbti: String,           onMbtiChange: (String) -> Unit,
    profileImageUrl: String,
    pickedImageUri: Uri?,
    onProfileImageClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Column {
            // 그라데이션 배너
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.horizontalGradient(listOf(EditGradientStart, EditGradientEnd)))
            )
            Spacer(modifier = Modifier.height(54.dp))

            // 정보 카드 (인라인 편집)
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(24.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(42.dp))

                    // 이름
                    InlineEditField(
                        value         = name,
                        onValueChange = onNameChange,
                        placeholder   = "이름",
                        textStyle     = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, color = EditTextPrimary),
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true
                    )
                    Spacer(Modifier.height(4.dp))

                    // 나이
                    InlineEditField(
                        value         = age,
                        onValueChange = { if (it.length <= 3 && (it.isEmpty() || it.all(Char::isDigit))) onAgeChange(it) },
                        placeholder   = "나이",
                        textStyle     = TextStyle(fontSize = 14.sp, color = EditTextSecondary),
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true,
                        keyboardType  = KeyboardType.Number,
                        suffix        = if (age.isNotBlank()) "세" else ""
                    )
                    Spacer(Modifier.height(18.dp))

                    // 학교 · 학과
                    EditableInfoRow(
                        icon = {
                            Icon(Icons.Default.School, contentDescription = "학교",
                                tint = EditPurplePrimary, modifier = Modifier.size(18.dp))
                        }
                    ) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            InlineEditField(
                                value = school, onValueChange = onSchoolChange,
                                placeholder = "학교", modifier = Modifier.weight(1f), singleLine = true
                            )
                            Text("·", color = EditTextSecondary, fontSize = 14.sp)
                            InlineEditField(
                                value = department, onValueChange = onDepartmentChange,
                                placeholder = "학과", modifier = Modifier.weight(1f), singleLine = true
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    // 키 + MBTI
                    EditableInfoRow(
                        icon = {
                            Icon(Icons.Default.Straighten, contentDescription = "키",
                                tint = EditPurplePrimary, modifier = Modifier.size(18.dp))
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            InlineEditField(
                                value = height,
                                onValueChange = { if (it.length <= 3 && (it.isEmpty() || it.all(Char::isDigit))) onHeightChange(it) },
                                placeholder = "키", modifier = Modifier.width(70.dp), singleLine = true,
                                keyboardType = KeyboardType.Number, suffix = if (height.isNotBlank()) "cm" else ""
                            )
                            Text("·", color = EditTextSecondary, fontSize = 14.sp)
                            Icon(Icons.Default.Person, contentDescription = "MBTI",
                                tint = EditPurplePrimary, modifier = Modifier.size(18.dp))
                            InlineEditField(
                                value = mbti,
                                onValueChange = { if (it.length <= 4) onMbtiChange(it) },
                                placeholder = "MBTI", modifier = Modifier.width(80.dp), singleLine = true
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    // 지역
                    EditableInfoRow(
                        icon = {
                            Icon(Icons.Default.LocationOn, contentDescription = "지역",
                                tint = EditPurplePrimary, modifier = Modifier.size(18.dp))
                        }
                    ) {
                        InlineEditField(
                            value = location, onValueChange = onLocationChange,
                            placeholder = "지역", modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                    }
                }
            }
        }

        // 프로필 사진 (카메라 배지 포함)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 92.dp)
                .size(116.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(4.dp, Color.White, CircleShape)
                .clickable { onProfileImageClick() },
            contentAlignment = Alignment.Center
        ) {
            val imageModel: Any? = pickedImageUri ?: profileImageUrl.takeIf { it.isNotBlank() }

            if (imageModel != null) {
                AsyncImage(
                    model              = imageModel,
                    contentDescription = "프로필 이미지",
                    modifier           = Modifier.size(108.dp).clip(CircleShape),
                    contentScale       = ContentScale.Crop
                )
            } else {
                Box(
                    modifier         = Modifier.size(108.dp).clip(CircleShape).background(Color(0xFFE8D6F7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null,
                        tint = EditPurplePrimary, modifier = Modifier.size(44.dp))
                }
            }

            // 카메라 배지
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-6).dp, y = (-6).dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(EditPurplePrimary)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "사진 변경",
                    tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ── 공통 컴포넌트 ──────────────────────────────────────────────────────────────

@Composable
private fun EditableInfoRow(
    icon: @Composable () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(modifier = Modifier.width(10.dp))
        content()
    }
}

@Composable
private fun InlineEditField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle(fontSize = 14.sp, color = EditTextPrimary),
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    suffix: String = ""
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            BasicTextField(
                value             = value,
                onValueChange     = onValueChange,
                modifier          = Modifier.weight(1f),
                textStyle         = textStyle,
                singleLine        = singleLine,
                cursorBrush       = SolidColor(EditPurplePrimary),
                keyboardOptions   = KeyboardOptions(keyboardType = keyboardType),
                interactionSource = interactionSource,
                decorationBox     = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(placeholder, style = textStyle.copy(color = EditTextHint))
                        }
                        innerTextField()
                    }
                }
            )
            if (suffix.isNotEmpty()) {
                Text(suffix, style = textStyle.copy(color = EditTextSecondary),
                    modifier = Modifier.padding(start = 2.dp, bottom = 1.dp))
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(if (isFocused) EditPurplePrimary else EditUnderlineIdle)
        )
    }
}

@Composable
private fun EditSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold, color = EditTextPrimary, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun ChipEditor(
    chips: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    chipColor: Color,
    textColor: Color,
    addColor: Color,
    placeholder: String
) {
    var isAdding    by remember { mutableStateOf(false) }
    var newChipText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    fun confirmAdd() {
        val text = newChipText.trim()
        if (text.isNotBlank()) onAdd(text)
        newChipText = ""
        isAdding = false
    }

    LaunchedEffect(isAdding) {
        if (isAdding) focusRequester.requestFocus()
    }

    val allChips = chips + if (isAdding) listOf("__INPUT__") else listOf("__ADD__")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        allChips.chunked(3).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { item ->
                    when (item) {
                        "__ADD__" -> {
                            Surface(
                                shape    = RoundedCornerShape(20.dp),
                                color    = addColor.copy(alpha = 0.7f),
                                modifier = Modifier.clickable { isAdding = true }
                            ) {
                                Row(
                                    modifier              = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "추가",
                                        tint = textColor, modifier = Modifier.size(13.dp))
                                    Text("추가", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        "__INPUT__" -> {
                            Surface(shape = RoundedCornerShape(20.dp), color = addColor) {
                                Row(
                                    modifier          = Modifier
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .widthIn(min = 80.dp, max = 180.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BasicTextField(
                                        value         = newChipText,
                                        onValueChange = { newChipText = it },
                                        modifier      = Modifier.weight(1f).focusRequester(focusRequester),
                                        singleLine    = true,
                                        textStyle     = TextStyle(fontSize = 12.sp, color = textColor),
                                        cursorBrush   = SolidColor(textColor),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = { confirmAdd() }),
                                        decorationBox = { innerTextField ->
                                            Box {
                                                if (newChipText.isEmpty()) {
                                                    Text(placeholder, fontSize = 12.sp,
                                                        color = textColor.copy(alpha = 0.45f))
                                                }
                                                innerTextField()
                                            }
                                        }
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.Check, contentDescription = "추가 완료",
                                        tint = textColor,
                                        modifier = Modifier.size(14.dp).clickable { confirmAdd() }
                                    )
                                }
                            }
                        }
                        else -> {
                            Surface(shape = RoundedCornerShape(20.dp), color = chipColor) {
                                Row(
                                    modifier          = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(item, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.width(5.dp))
                                    Icon(
                                        Icons.Default.Close, contentDescription = "삭제",
                                        tint = textColor.copy(alpha = 0.65f),
                                        modifier = Modifier.size(13.dp).clickable { onRemove(item) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
