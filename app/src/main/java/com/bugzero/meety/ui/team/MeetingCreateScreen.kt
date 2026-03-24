package com.bugzero.meety.ui.team

<<<<<<< HEAD
import androidx.compose.runtime.Composable

@Composable
fun MeetingCreateScreen() {
    // TODO: C 담당 - 새 팀 모임 생성 화면 구현
=======
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MeetingCreateScreen(
    onHomeClick: () -> Unit = {},
    onMatchingClick: () -> Unit = {},
    onCreateTeamTabClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onUploadPhotoClick: () -> Unit = {},
    onCreateTeamClick: () -> Unit = {}
) {
    var teamName by remember { mutableStateOf("") }
    var selectedTeamSize by remember { mutableIntStateOf(1) }
    val selectedTags = remember { mutableStateListOf<String>() }

    val allTags = listOf(
        "#활발한", "#조용한", "#카페좋아", "#술좋아", "#운동좋아",
        "#영화매니아", "#게임러버", "#음악좋아", "#여행좋아",
        "#맛집탐방", "#예술좋아", "#독서좋아", "#춤", "#노래", "#요리"
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
                TeamSizeSection(
                    selectedIndex = selectedTeamSize,
                    onSelect = { selectedTeamSize = it }
                )
            }

            item {
                TeamPhotoSection(
                    onUploadPhotoClick = onUploadPhotoClick
                )
            }

            item {
                TeamTagSection(
                    allTags = allTags,
                    selectedTags = selectedTags,
                    onTagClick = { tag ->
                        if (selectedTags.contains(tag)) {
                            selectedTags.remove(tag)
                        } else {
                            if (selectedTags.size < 5) {
                                selectedTags.add(tag)
                            }
                        }
                    }
                )
            }

            item {
                Button(
                    onClick = onCreateTeamClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFA020F0)
                    )
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
private fun TeamNameSection(
    teamName: String,
    onTeamNameChange: (String) -> Unit
) {
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
                textStyle = TextStyle(
                    color = Color(0xFF222222)
                ),
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
private fun TeamSizeSection(
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    val teamOptions = listOf("2:2", "3:3", "4:4")

    WhiteCardSection(title = "팀 구성") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            teamOptions.forEachIndexed { index, label ->
                val selected = selectedIndex == index

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (selected) Color(0xFFA020F0) else Color(0xFFF2F2F5)
                        )
                        .clickable { onSelect(index) }
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "👥",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = label,
                            color = if (selected) Color.White else Color(0xFF444444),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamPhotoSection(
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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

@Composable
private fun TeamTagSection(
    allTags: List<String>,
    selectedTags: List<String>,
    onTagClick: (String) -> Unit
) {
    WhiteCardSection(
        title = "팀 태그 (최대 5개)",
        titleRightText = "${selectedTags.size}/5"
    ) {
        TagWrapLayout(
            tags = allTags,
            selectedTags = selectedTags,
            onTagClick = onTagClick
        )
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
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
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
    val rows = tags.chunked(3)

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { rowTags ->
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
                            text = tag,
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
>>>>>>> ca178ecdf5d6d6fc1948225597641ad0fce1c061
}