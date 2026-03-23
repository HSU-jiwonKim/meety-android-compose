package com.bugzero.meety.ui.team

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 마이페이지 화면에 사용할 UI 상태 모델
 * Firestore users/{userId} 문서값을 ViewModel에서 읽어서 이 형태로 넘겨주면 됨
 */
data class UserProfileUiState(
    val name: String,
    val age: Int,
    val school: String,
    val department: String,
    val height: Int,
    val location: String,
    val bio: String,
    val interests: List<String>,
    val foodLikes: List<String>,
    val foodDislikes: List<String>,
    val profileImages: List<String>,
    val schedule: Map<String, List<String>>
)

private val scheduleDays = listOf("월", "화", "수", "목", "금")
private val scheduleTimes = listOf("09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00")

/**
 * 실제 마이페이지 화면
 * 이 파일은 UI만 담당하고,
 * 데이터는 MyPageRoute / MyPageViewModel 쪽에서 받아와서 넣어줌
 */
@Composable
fun MyPageScreen(
    uiState: UserProfileUiState,
    selectedBottomTab: TeamBottomTab = TeamBottomTab.PROFILE,
    onHomeClick: () -> Unit = {},
    onMatchingClick: () -> Unit = {},
    onCreateTeamClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onEditProfileClick: () -> Unit = {}
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TeamCommonTopBar(
                onSearchClick = onSearchClick,
                onNotificationClick = onNotificationClick
            )
        },
        bottomBar = {
            TeamCommonBottomBar(
                selectedTab = selectedBottomTab,
                onHomeClick = onHomeClick,
                onMatchingClick = onMatchingClick,
                onCreateTeamClick = onCreateTeamClick,
                onChatClick = onChatClick,
                onProfileClick = onProfileClick
            )
        },
        containerColor = Color(0xFFF8F1F8)
    ) { innerPadding ->
        MyPageBody(
            uiState = uiState,
            onEditProfileClick = onEditProfileClick,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

/**
 * 실제 마이페이지 본문
 */
@Composable
fun MyPageBody(
    uiState: UserProfileUiState,
    onEditProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ProfileHeaderSection(uiState = uiState)
        }

        item {
            SectionCard(title = "관심사") {
                TagWrapSection(
                    tags = uiState.interests,
                    chipColor = Color(0xFFF3E7FF),
                    textColor = Color(0xFF8E24AA)
                )
            }
        }

        item {
            SectionCard(title = "자기소개") {
                Text(
                    text = uiState.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF444444)
                )
            }
        }

        item {
            PhotoSection(imageUrls = uiState.profileImages)
        }

        item {
            ScheduleSection(schedule = uiState.schedule)
        }

        item {
            FoodPreferenceSection(
                likes = uiState.foodLikes,
                dislikes = uiState.foodDislikes
            )
        }

        item {
            EditProfileButton(
                onClick = onEditProfileClick
            )
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ProfileHeaderSection(
    uiState: UserProfileUiState
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFB842F5),
                                Color(0xFFFF4FA3)
                            )
                        )
                    )
            )

            Spacer(modifier = Modifier.height(54.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(42.dp))

                    Text(
                        text = uiState.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF222222)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${uiState.age}세",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    InfoRow(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = "학교",
                                tint = Color(0xFF9C27B0)
                            )
                        },
                        text = "${uiState.school} · ${uiState.department}"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    InfoRow(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Straighten,
                                contentDescription = "키",
                                tint = Color(0xFF9C27B0)
                            )
                        },
                        text = "${uiState.height}cm"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    InfoRow(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "지역",
                                tint = Color(0xFF9C27B0)
                            )
                        },
                        text = uiState.location
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 92.dp)
                .size(116.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(4.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8D6F7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "프로필 이미지",
                    tint = Color(0xFF8E24AA),
                    modifier = Modifier.size(44.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222)
            )

            Spacer(modifier = Modifier.height(14.dp))

            content()
        }
    }
}

@Composable
private fun InfoRow(
    icon: @Composable () -> Unit,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF444444)
        )
    }
}

@Composable
private fun TagWrapSection(
    tags: List<String>,
    chipColor: Color,
    textColor: Color
) {
    val chunkedTags = tags.chunked(3)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chunkedTags.forEach { rowTags ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowTags.forEach { tag ->
                    TagChip(
                        text = tag,
                        chipColor = chipColor,
                        textColor = textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun TagChip(
    text: String,
    chipColor: Color,
    textColor: Color
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = chipColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = textColor,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PhotoSection(
    imageUrls: List<String>
) {
    SectionCard(title = "사진") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val photos = if (imageUrls.isEmpty()) listOf("", "", "") else imageUrls.take(3)

            photos.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            when (index) {
                                0 -> Color(0xFFFFE3EE)
                                1 -> Color(0xFFEAD9FF)
                                else -> Color(0xFFFFF0D9)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "사진 ${index + 1}",
                        color = Color(0xFF666666),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleSection(
    schedule: Map<String, List<String>>
) {
    SectionCard(title = "시간표") {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TimeTableHeaderCell(
                    text = "",
                    modifier = Modifier.width(58.dp)
                )

                scheduleDays.forEach { day ->
                    TimeTableHeaderCell(
                        text = day,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            scheduleTimes.forEach { time ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimeTableHeaderCell(
                        text = time,
                        modifier = Modifier.width(58.dp)
                    )

                    scheduleDays.forEach { day ->
                        val isSelected = schedule[day]?.contains(time) == true

                        TimeTableBodyCell(
                            selected = isSelected,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeTableHeaderCell(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF1E7F7)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6F3D8A),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TimeTableBodyCell(
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) Color(0xFFCE93D8) else Color(0xFFF7F2F9)
            )
            .border(
                width = 1.dp,
                color = if (selected) Color(0xFFAB47BC) else Color(0xFFE6DDEA),
                shape = RoundedCornerShape(10.dp)
            )
    )
}

@Composable
private fun FoodPreferenceSection(
    likes: List<String>,
    dislikes: List<String>
) {
    SectionCard(title = "음식 취향") {
        Text(
            text = "좋아하는 음식",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E7D32)
        )

        Spacer(modifier = Modifier.height(10.dp))

        TagWrapSection(
            tags = likes,
            chipColor = Color(0xFFE6F7E8),
            textColor = Color(0xFF2E7D32)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Divider(color = Color(0xFFF0F0F0))

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "싫어하는 음식",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFC62828)
        )

        Spacer(modifier = Modifier.height(10.dp))

        TagWrapSection(
            tags = dislikes,
            chipColor = Color(0xFFFFE7E7),
            textColor = Color(0xFFC62828)
        )
    }
}

@Composable
private fun EditProfileButton(
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF9C27B0)
        )
    ) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "프로필 수정",
            tint = Color.White
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "프로필 수정하기",
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}