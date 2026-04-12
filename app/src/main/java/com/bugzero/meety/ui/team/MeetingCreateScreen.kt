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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

@Composable
fun MeetingCreateScreen(
    onHomeClick: () -> Unit = {},
    onMatchingClick: () -> Unit = {},
    onCreateTeamTabClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onCreateTeamClick: () -> Unit = {}
) {
    val teamViewModel: TeamViewModel = viewModel()
    var teamName by remember { mutableStateOf("") }
    var teamDescription by remember { mutableStateOf("") }
    val selectedTags = remember { mutableStateListOf<String>() }

    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri
        }
    }

    // 저장은 # 없이, 화면에는 # 붙여서 표시
    val allTags = listOf(
        "활발한", "조용한", "카페좋아", "술좋아", "운동좋아",
        "영화매니아", "게임러버", "음악좋아", "여행좋아",
        "맛집탐방", "예술좋아", "독서좋아", "춤", "노래", "요리"
    )

    Scaffold(
        topBar = {
            TeamCommonTopBar(
                onSearchClick = onSearchClick,
                onNotificationClick = onNotificationClick
            )
        },
        containerColor = Color(0xFFF8F1F8)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 110.dp)
        ) {
            item {
                Text(
                    text = "팀 만들기",
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = Color(0xFFA020F0),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                TeamNameSection(
                    teamName = teamName,
                    onTeamNameChange = { teamName = it }
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
                    allTags = allTags,
                    selectedTags = selectedTags,
                    onTagClick = { tag ->
                        if (selectedTags.contains(tag)) {
                            selectedTags.remove(tag)
                        } else if (selectedTags.size < 5) {
                            selectedTags.add(tag)
                        }
                    },
                    onAddCustomTag = { newTag ->
                        val normalizedTag = newTag.trim().removePrefix("#")
                        if (
                            normalizedTag.isNotBlank() &&
                            !selectedTags.contains(normalizedTag) &&
                            selectedTags.size < 5
                        ) {
                            selectedTags.add(normalizedTag)
                        }
                    },
                    onRemoveTag = { tag ->
                        selectedTags.remove(tag)
                    }
                )
            }

            item {
                Button(
                    onClick = {
                        teamViewModel.createTeam(
                            teamName = teamName.trim(),
                            description = teamDescription.trim(),
                            tags = selectedTags.toList(),
                            onSuccess = { teamId ->
                                selectedPhotoUri?.let { uri ->
                                    teamViewModel.updateTeamProfileImage(teamId, uri)
                                }
                                onCreateTeamClick()
                            },
                            onFailure = { errorMessage ->
                                println("팀 생성 실패: $errorMessage")
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA020F0))
                ) {
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
}

@Composable
private fun TeamNameSection(teamName: String, onTeamNameChange: (String) -> Unit) {
    WhiteCardSection(title = "팀 이름") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF2F2F5))
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
                            color = Color(0xFF9A9AA3),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

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
                .background(Color(0xFFF2F2F5))
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
                            color = Color(0xFF9A9AA3),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

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
                    color = Color(0xFFD5D6DE),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { onUploadPhotoClick() },
            contentAlignment = Alignment.Center
        ) {
            if (selectedPhotoUri != null) {
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
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "선택됨",
                        tint = Color(0xFFA020F0),
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else {
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
        }
    }
}

@Composable
private fun TeamTagSection(
    allTags: List<String>,
    selectedTags: List<String>,
    onTagClick: (String) -> Unit,
    onAddCustomTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit
) {
    var customTagInput by remember { mutableStateOf("") }

    WhiteCardSection(
        title = "팀 태그 (최대 5개)",
        titleRightText = "${selectedTags.size}/5"
    ) {
        TagWrapLayout(
            tags = allTags,
            selectedTags = selectedTags,
            onTagClick = onTagClick
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF2F2F5))
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                BasicTextField(
                    value = customTagInput,
                    onValueChange = { customTagInput = it },
                    textStyle = TextStyle(color = Color(0xFF222222)),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (customTagInput.isEmpty()) {
                            Text(
                                text = "직접 태그 입력 후 추가",
                                color = Color(0xFF9A9AA3),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        innerTextField()
                    }
                )
            }

            Surface(
                modifier = Modifier.clickable {
                    val normalizedTag = customTagInput.trim().removePrefix("#")
                    if (
                        normalizedTag.isNotBlank() &&
                        selectedTags.size < 5 &&
                        !selectedTags.contains(normalizedTag)
                    ) {
                        onAddCustomTag(normalizedTag)
                        customTagInput = ""
                    }
                },
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFEEDBFF)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "태그 추가",
                        tint = Color(0xFFA020F0),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "추가",
                        color = Color(0xFFA020F0),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (selectedTags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            SelectedTagList(
                selectedTags = selectedTags,
                onRemoveTag = onRemoveTag
            )
        }
    }
}

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
                        color = if (selected) Color(0xFFEEDBFF) else Color(0xFFF2F2F5)
                    ) {
                        Text(
                            text = "#$tag",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            color = if (selected) Color(0xFFA020F0) else Color(0xFF444B5A),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

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
                                color = Color(0xFFA020F0),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "태그 제거",
                                tint = Color(0xFFA020F0),
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