package com.bugzero.meety.ui.team

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 화면에서 반복해서 사용하는 색상값을 한 곳에 모아둔 부분
private val ScreenBackgroundColor = Color(0xFFF4F4F8) // 친구 탭과 동일한 배경색
private val MainPurpleColor = Color(0xFF7B5CFF)
private val LightPurpleColor = Color(0xFFF2EEFF)
private val DisabledPurpleColor = Color(0xFFDAD7E2)
private val ErrorColor = Color(0xFFFF4D7D)
private val InputBackgroundColor = Color(0xFFF6F4FB)
private val PlaceholderColor = Color(0xFF9B98A6)
private val BorderColor = Color(0xFFECEAF1)

// 시그니처 그라데이션 (보라 → 마젠타 → 핑크)
private val CreateGradient = androidx.compose.ui.graphics.Brush.linearGradient(
    0f to Color(0xFF7B5CFF), 0.45f to Color(0xFFA24BFF), 1f to Color(0xFFFF5C8A)
)
private val GradientSoft = androidx.compose.ui.graphics.Brush.linearGradient(
    listOf(Color(0xFFEFE9FF), Color(0xFFFFE8F1))
)

// LazyColumn 안에서 특정 입력칸으로 이동할 때 사용하는 item 위치
private const val TEAM_NAME_ITEM_INDEX = 1
private const val TEAM_TAG_ITEM_INDEX = 4

// 팀 만들기 버튼을 누른 뒤 중복 클릭을 막기 위한 잠금 시간
private const val CREATE_BUTTON_LOCK_TIME = 3000L

// 기본으로 보여줄 팀 태그 목록
private val DefaultTeamTags = listOf(
    "활발한", "조용한", "카페좋아", "술좋아", "운동좋아",
    "영화매니아", "게임러버", "음악좋아", "여행좋아",
    "맛집탐방", "예술좋아", "독서좋아", "춤", "노래", "요리"
)

@Composable
fun MeetingCreateScreen(
    onHomeClick: () -> Unit = {},
    onMatchingClick: () -> Unit = {},
    onCreateTeamTabClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},

    // 팀 생성 성공 후 NavGraph로 teamId와 teamName을 넘기는 콜백
    // NavGraph에서는 이 값을 이용해서 바로 팀 채팅방으로 이동함
    onCreateTeamClick: (String, String) -> Unit = { _, _ -> }
) {
    val teamViewModel: TeamViewModel = viewModel()

    // 팀 만들기 화면에서 입력받는 값들
    var teamName by remember { mutableStateOf("") }
    var teamDescription by remember { mutableStateOf("") }
    val selectedTags = remember { mutableStateListOf<String>() }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // 팀 생성 버튼 상태 관리
    var isCreatingTeam by remember { mutableStateOf(false) }
    var isCreateButtonLocked by remember { mutableStateOf(false) }

    // 필수 입력값 에러 표시 상태
    var showTeamNameError by remember { mutableStateOf(false) }
    var showTeamTagError by remember { mutableStateOf(false) }

    // 필수 입력 누락 시 해당 영역으로 자동 스크롤하기 위한 상태
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 대표 사진 선택 런처
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri
        }
    }

    // 팀 만들기 버튼을 누르면 3초 동안 다시 누르지 못하게 잠그는 함수
    fun lockCreateButtonForThreeSeconds() {
        isCreateButtonLocked = true

        coroutineScope.launch {
            delay(CREATE_BUTTON_LOCK_TIME)
            isCreateButtonLocked = false
        }
    }

    Scaffold(
        containerColor = ScreenBackgroundColor,
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 0.dp, bottom = 110.dp)
        ) {
            item {
                ScreenTitle()
            }

            item {
                TeamNameSection(
                    teamName = teamName,
                    isError = showTeamNameError,
                    onTeamNameChange = {
                        teamName = it

                        // 팀 이름을 입력하면 에러 표시 제거
                        if (it.trim().isNotBlank()) {
                            showTeamNameError = false
                        }
                    }
                )
            }

            item {
                TeamDescriptionSection(
                    teamDescription = teamDescription,
                    onTeamDescriptionChange = { teamDescription = it }
                )
            }

            item {
                TeamPhotoSection(
                    selectedPhotoUri = selectedPhotoUri,
                    onUploadPhotoClick = { imagePickerLauncher.launch("image/*") }
                )
            }

            item {
                TeamTagSection(
                    allTags = DefaultTeamTags,
                    selectedTags = selectedTags,
                    isError = showTeamTagError,
                    onTagClick = { tag ->
                        if (selectedTags.contains(tag)) {
                            selectedTags.remove(tag)
                        } else if (selectedTags.size < 5) {
                            selectedTags.add(tag)
                        }

                        // 태그를 하나라도 선택하면 에러 표시 제거
                        if (selectedTags.isNotEmpty()) {
                            showTeamTagError = false
                        }
                    },
                    onRemoveTag = { tag ->
                        selectedTags.remove(tag)
                    }
                )
            }

            item {
                CreateTeamButton(
                    isCreatingTeam = isCreatingTeam,
                    isCreateButtonLocked = isCreateButtonLocked,
                    onClick = {
                        if (isCreatingTeam || isCreateButtonLocked) return@CreateTeamButton

                        // 빠른 연속 클릭으로 팀이 중복 생성되는 문제를 막기 위해 버튼 잠금
                        lockCreateButtonForThreeSeconds()

                        val trimmedTeamName = teamName.trim()
                        val trimmedDescription = teamDescription.trim()

                        val isTeamNameBlank = trimmedTeamName.isBlank()
                        val isTeamTagBlank = selectedTags.isEmpty()

                        showTeamNameError = isTeamNameBlank
                        showTeamTagError = isTeamTagBlank

                        // 팀 이름이 비어 있으면 팀 이름 입력칸으로 이동
                        if (isTeamNameBlank) {
                            scrollToItem(
                                coroutineScope = coroutineScope,
                                listState = listState,
                                itemIndex = TEAM_NAME_ITEM_INDEX
                            )
                            return@CreateTeamButton
                        }

                        // 팀 태그가 비어 있으면 태그 영역으로 이동
                        if (isTeamTagBlank) {
                            scrollToItem(
                                coroutineScope = coroutineScope,
                                listState = listState,
                                itemIndex = TEAM_TAG_ITEM_INDEX
                            )
                            return@CreateTeamButton
                        }

                        isCreatingTeam = true

                        // 실제 팀 생성 요청
                        teamViewModel.createTeam(
                            teamName = trimmedTeamName,
                            description = trimmedDescription,
                            tags = selectedTags.toList(),
                            imageUri = selectedPhotoUri,
                            onSuccess = { teamId ->
                                isCreatingTeam = false

                                // 팀 생성 성공 후 NavGraph로 teamId와 teamName 전달
                                // 이 값을 이용해 생성된 팀 채팅방 화면으로 바로 이동함
                                onCreateTeamClick(teamId, trimmedTeamName)
                            },
                            onFailure = { errorMessage ->
                                isCreatingTeam = false
                                println("팀 생성 실패: $errorMessage")
                            }
                        )
                    }
                )
            }
        }
    }
}

// 화면 상단의 제목 영역
@Composable
private fun ScreenTitle() {
    Text(
        text = "팀 만들기",
        fontSize = 23.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.4).sp,
        color = Color(0xFF17161D),
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 14.dp)
    )
}

// 팀 이름 입력 영역
@Composable
private fun TeamNameSection(
    teamName: String,
    isError: Boolean,
    onTeamNameChange: (String) -> Unit
) {
    WhiteCardSection(
        title = "팀 이름",
        required = true,
        titleRightText = "${teamName.length} / 20"
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(15.dp))
                .background(InputBackgroundColor)
                .border(
                    width = if (isError) 1.dp else 0.dp,
                    color = if (isError) ErrorColor else Color.Transparent,
                    shape = RoundedCornerShape(15.dp)
                )
                .padding(horizontal = 15.dp, vertical = 14.dp)
        ) {
            BasicTextField(
                value = teamName,
                onValueChange = onTeamNameChange,
                textStyle = TextStyle(color = Color(0xFF222222)),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (teamName.isEmpty()) {
                        Text(
                            text = "예: 경영학과 프렌즈",
                            color = PlaceholderColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    innerTextField()
                }
            )
        }

        if (isError) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "팀 이름을 입력해주세요.",
                color = ErrorColor,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// 팀 소개 입력 영역
@Composable
private fun TeamDescriptionSection(
    teamDescription: String,
    onTeamDescriptionChange: (String) -> Unit
) {
    WhiteCardSection(
        title = "팀 소개",
        required = true,
        titleRightText = "${teamDescription.length} / 60"
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 74.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(InputBackgroundColor)
                .padding(horizontal = 15.dp, vertical = 14.dp)
        ) {
            BasicTextField(
                value = teamDescription,
                onValueChange = onTeamDescriptionChange,
                textStyle = TextStyle(color = Color(0xFF222222)),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (teamDescription.isEmpty()) {
                        Text(
                            text = "예: 함께 카페 가고 영화 볼 팀원을 구해요",
                            color = PlaceholderColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    innerTextField()
                }
            )
        }
    }
}

// 팀 대표 사진 선택 영역
@Composable
private fun TeamPhotoSection(
    selectedPhotoUri: Uri?,
    onUploadPhotoClick: () -> Unit
) {
    WhiteCardSection(title = "대표 사진") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(24.dp))
                .then(
                    if (selectedPhotoUri == null) Modifier.background(GradientSoft)
                    else Modifier.background(InputBackgroundColor)
                )
                .clickable { onUploadPhotoClick() },
            contentAlignment = Alignment.Center
        ) {
            if (selectedPhotoUri != null) {
                SelectedTeamPhoto(selectedPhotoUri = selectedPhotoUri)
            } else {
                EmptyTeamPhotoPlaceholder()
            }
        }
    }
}

// 대표 사진이 선택된 경우 보여주는 이미지 영역
@Composable
private fun SelectedTeamPhoto(
    selectedPhotoUri: Uri
) {
    AsyncImage(
        model = selectedPhotoUri,
        contentDescription = "팀 대표 사진",
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(22.dp)),
        contentScale = ContentScale.Crop
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "선택됨",
            tint = MainPurpleColor,
            modifier = Modifier.size(28.dp)
        )
    }
}

// 대표 사진이 선택되지 않았을 때 보여주는 안내 영역
@Composable
private fun EmptyTeamPhotoPlaceholder() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 흰 원 안의 보라 카메라 (목업 .photo-up .cam)
        Box(
            modifier = Modifier
                .size(54.dp)
                .shadow(2.dp, CircleShape)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "사진 업로드",
                tint = MainPurpleColor,
                modifier = Modifier.size(25.dp)
            )
        }

        Spacer(modifier = Modifier.height(9.dp))

        Text(
            text = "팀 대표 사진 추가",
            color = Color(0xFF17161D),
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "팀을 가장 잘 보여주는 사진 한 장",
            color = Color(0xFF56535F),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// 팀 태그 선택 및 직접 입력 영역
@Composable
private fun TeamTagSection(
    allTags: List<String>,
    selectedTags: List<String>,
    isError: Boolean,
    onTagClick: (String) -> Unit,
    onRemoveTag: (String) -> Unit
) {

    WhiteCardSection(
        title = "팀 태그 (최대 5개)",
        titleRightText = "${selectedTags.size}/5"
    ) {
        if (isError) {
            Text(
                text = "팀 태그를 최소 1개 이상 선택해주세요.",
                color = ErrorColor,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(10.dp))
        }

        TagWrapLayout(
            tags = allTags,
            selectedTags = selectedTags,
            onTagClick = onTagClick
        )

        if (selectedTags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))

            SelectedTagList(
                selectedTags = selectedTags,
                onRemoveTag = onRemoveTag
            )
        }
    }
}

// 팀 만들기 버튼 영역
@Composable
private fun CreateTeamButton(
    isCreatingTeam: Boolean,
    isCreateButtonLocked: Boolean,
    onClick: () -> Unit
) {
    val disabled = isCreatingTeam || isCreateButtonLocked
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .then(
                if (disabled) Modifier.background(DisabledPurpleColor)
                else Modifier.background(CreateGradient)
            )
            .clickable(enabled = !disabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            when {
                isCreatingTeam -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "팀 생성 중...",
                        color = Color.White,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                isCreateButtonLocked -> {
                    Text(
                        text = "잠시만 기다려주세요...",
                        color = Color.White,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                else -> {
                    Text(
                        text = "팀 만들기 완료",
                        color = Color.White,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

// 각 입력 영역을 감싸는 공통 카드 UI
@Composable
private fun WhiteCardSection(
    title: String,
    titleRightText: String? = null,
    required: Boolean = false,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        color = Color.White,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = Color(0xFF17161D),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                if (required) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "*", color = Color(0xFFFF5C8A), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }

                Spacer(modifier = Modifier.weight(1f))

                if (titleRightText != null) {
                    Text(
                        text = titleRightText,
                        color = Color(0xFFC4C2CD),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            content()
        }
    }
}

// 기본 태그 목록을 3개씩 끊어서 보여주는 영역
@Composable
private fun TagWrapLayout(
    tags: List<String>,
    selectedTags: List<String>,
    onTagClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tags.chunked(3).forEach { rowTags ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowTags.forEach { tag ->
                    val selected = selectedTags.contains(tag)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .then(
                                if (selected) Modifier.background(CreateGradient)
                                else Modifier
                                    .background(Color.White)
                                    .border(1.dp, BorderColor, RoundedCornerShape(999.dp))
                            )
                            .clickable { onTagClick(tag) }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = tag,
                            color = if (selected) Color.White else Color(0xFF56535F),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// 선택된 태그 목록을 보여주고, X 버튼으로 삭제할 수 있는 영역
@Composable
private fun SelectedTagList(
    selectedTags: List<String>,
    onRemoveTag: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        selectedTags.chunked(3).forEach { rowTags ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowTags.forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFFF7ECFF)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .widthIn(min = 72.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#$tag",
                                color = MainPurpleColor,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "태그 제거",
                                tint = MainPurpleColor,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { onRemoveTag(tag) }
                            )
                        }
                    }
                }
            }
        }
    }
}


// 필수 입력이 비어 있을 때 해당 입력 영역으로 스크롤하는 함수
private fun scrollToItem(
    coroutineScope: CoroutineScope,
    listState: LazyListState,
    itemIndex: Int
) {
    coroutineScope.launch {
        listState.animateScrollToItem(itemIndex)
    }
}
