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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
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
private val ScreenBackgroundColor = Color(0xFFF8F1F8)
private val MainPurpleColor = Color(0xFFA020F0)
private val LightPurpleColor = Color(0xFFEEDBFF)
private val DisabledPurpleColor = Color(0xFFC9A7E8)
private val ErrorColor = Color(0xFFE53935)
private val InputBackgroundColor = Color(0xFFF2F2F5)
private val PlaceholderColor = Color(0xFF9A9AA3)
private val BorderColor = Color(0xFFD5D6DE)

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
        topBar = {
            TeamCommonTopBar(
                onSearchClick = onSearchClick,
                onNotificationClick = onNotificationClick
            )
        },g
        containerColor = ScreenBackgroundColor
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 110.dp)
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
                    onAddCustomTag = { newTag ->
                        val normalizedTag = normalizeTag(newTag)

                        if (
                            normalizedTag.isNotBlank() &&
                            !selectedTags.contains(normalizedTag) &&
                            selectedTags.size < 5
                        ) {
                            selectedTags.add(normalizedTag)
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
        modifier = Modifier.padding(horizontal = 20.dp),
        color = MainPurpleColor,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
}

// 팀 이름 입력 영역
@Composable
private fun TeamNameSection(
    teamName: String,
    isError: Boolean,
    onTeamNameChange: (String) -> Unit
) {
    WhiteCardSection(title = "팀 이름") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(InputBackgroundColor)
                .border(
                    width = if (isError) 1.dp else 0.dp,
                    color = if (isError) ErrorColor else Color.Transparent,
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 14.dp, vertical = 14.dp)
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
    WhiteCardSection(title = "팀 소개") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(InputBackgroundColor)
                .padding(horizontal = 14.dp, vertical = 14.dp)
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
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 1.dp,
                    color = BorderColor,
                    shape = RoundedCornerShape(16.dp)
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
            .clip(RoundedCornerShape(16.dp)),
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
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = "사진 업로드",
            tint = Color(0xFF9AA1AE),
            modifier = Modifier.size(44.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "사진을 업로드하세요",
            color = Color(0xFF6D7483),
            style = MaterialTheme.typography.bodyLarge
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
    onAddCustomTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit
) {
    var customTagInput by remember { mutableStateOf("") }

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

        Spacer(modifier = Modifier.height(14.dp))

        CustomTagInputRow(
            customTagInput = customTagInput,
            selectedTags = selectedTags,
            onCustomTagInputChange = { customTagInput = it },
            onAddCustomTag = { tag ->
                onAddCustomTag(tag)
                customTagInput = ""
            }
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

// 직접 태그 입력 후 추가하는 영역
@Composable
private fun CustomTagInputRow(
    customTagInput: String,
    selectedTags: List<String>,
    onCustomTagInputChange: (String) -> Unit,
    onAddCustomTag: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(InputBackgroundColor)
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            BasicTextField(
                value = customTagInput,
                onValueChange = onCustomTagInputChange,
                textStyle = TextStyle(color = Color(0xFF222222)),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (customTagInput.isEmpty()) {
                        Text(
                            text = "직접 태그 입력 후 추가",
                            color = PlaceholderColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    innerTextField()
                }
            )
        }

        Surface(
            modifier = Modifier.clickable {
                val normalizedTag = normalizeTag(customTagInput)

                if (
                    normalizedTag.isNotBlank() &&
                    selectedTags.size < 5 &&
                    !selectedTags.contains(normalizedTag)
                ) {
                    onAddCustomTag(normalizedTag)
                }
            },
            shape = RoundedCornerShape(14.dp),
            color = LightPurpleColor
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "태그 추가",
                    tint = MainPurpleColor,
                    modifier = Modifier.size(16.dp)
                )

                Text(
                    text = "추가",
                    color = MainPurpleColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
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
    Button(
        onClick = onClick,
        enabled = !isCreatingTeam && !isCreateButtonLocked,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MainPurpleColor,
            disabledContainerColor = DisabledPurpleColor
        )
    ) {
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            isCreateButtonLocked -> {
                Text(
                    text = "잠시만 기다려주세요...",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            else -> {
                Text(
                    text = "팀 만들기",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// 각 입력 영역을 감싸는 공통 카드 UI
@Composable
private fun WhiteCardSection(
    title: String,
    titleRightText: String? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = Color.Black,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (titleRightText != null) {
                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = titleRightText,
                        color = Color(0xFF63708C),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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

                    Surface(
                        modifier = Modifier
                            .wrapContentHeight()
                            .clickable { onTagClick(tag) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (selected) LightPurpleColor else InputBackgroundColor
                    ) {
                        Text(
                            text = "#$tag",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            color = if (selected) MainPurpleColor else Color(0xFF444B5A),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
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
                        shape = RoundedCornerShape(20.dp),
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

// 사용자가 직접 입력한 태그에서 공백과 # 기호를 정리하는 함수
private fun normalizeTag(tag: String): String {
    return tag.trim().removePrefix("#")
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
