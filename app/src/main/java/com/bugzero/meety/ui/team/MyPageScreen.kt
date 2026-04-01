package com.bugzero.meety.ui.team

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.fillMaxHeight
import com.bugzero.meety.ui.auth.InfoRow
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

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
private val scheduleTimes = listOf(
    "09:00", "09:30",
    "10:00", "10:30",
    "11:00", "11:30",
    "12:00", "12:30",
    "13:00", "13:30",
    "14:00", "14:30",
    "15:00", "15:30",
    "16:00", "16:30",
    "17:00", "17:30",
    "18:00"
)

@Composable
fun MyPageScreen(
    uiState: UserProfileUiState,
    viewModel: MyPageViewModel,
    selectedBottomTab: TeamBottomTab = TeamBottomTab.PROFILE,
    onHomeClick: () -> Unit = {},
    onMatchingClick: () -> Unit = {},
    onCreateTeamClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onScheduleClick: () -> Unit = {}
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onAddPhotoClick(uri)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TeamCommonTopBar(
                onSearchClick = onSearchClick,
                onNotificationClick = onNotificationClick
            )
        },
        containerColor = Color(0xFFF8F1F8)
    ) { innerPadding ->
        MyPageBody(
            uiState = uiState,
            onEditProfileClick = onEditProfileClick,
            onScheduleClick = onScheduleClick,
            onAddPhotoClick = {
                imagePickerLauncher.launch("image/*")
            },
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun MyPageBody(
    uiState: UserProfileUiState,
    onEditProfileClick: () -> Unit,
    onScheduleClick: () -> Unit = {},
    onAddPhotoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ProfileHeaderSection(uiState = uiState) }
        item {
            SectionCard(title = "관심사") {
                TagWrapSection(tags = uiState.interests, chipColor = Color(0xFFF3E7FF), textColor = Color(0xFF8E24AA))
            }
        }
        item {
            SectionCard(title = "자기소개") {
                Text(text = uiState.bio, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF444444))
            }
        }
        item { PhotoSection(imageUrls = uiState.profileImages, onAddPhotoClick = onAddPhotoClick) }
        item { ScheduleSection(schedule = uiState.schedule, onScheduleClick = onScheduleClick) }
        item { FoodPreferenceSection(likes = uiState.foodLikes, dislikes = uiState.foodDislikes) }
        item { EditProfileButton(onClick = onEditProfileClick) }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun ProfileHeaderSection(uiState: UserProfileUiState) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(24.dp))
                    .background(brush = Brush.horizontalGradient(colors = listOf(Color(0xFFB842F5), Color(0xFFFF4FA3))))
            )
            Spacer(modifier = Modifier.height(54.dp))
            Card(
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(42.dp))
                    Text(text = uiState.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF222222))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "${uiState.age}세", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF666666))
                    Spacer(modifier = Modifier.height(16.dp))
                    InfoRow(icon = { Icon(Icons.Default.School, contentDescription = "학교", tint = Color(0xFF9C27B0)) }, text = "${uiState.school} · ${uiState.department}")
                    Spacer(modifier = Modifier.height(10.dp))
                    InfoRow(icon = { Icon(Icons.Default.Straighten, contentDescription = "키", tint = Color(0xFF9C27B0)) }, text = "${uiState.height}cm")
                    Spacer(modifier = Modifier.height(10.dp))
                    InfoRow(icon = { Icon(Icons.Default.LocationOn, contentDescription = "지역", tint = Color(0xFF9C27B0)) }, text = uiState.location)
                }
            }
        }
        Box(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 92.dp).size(116.dp).clip(CircleShape).background(Color.White).border(4.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val profileImageUrl = uiState.profileImages.firstOrNull().orEmpty()
            if (profileImageUrl.isNotBlank()) {
                AsyncImage(model = profileImageUrl, contentDescription = "프로필 이미지", modifier = Modifier.size(108.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            } else {
                Box(modifier = Modifier.size(108.dp).clip(CircleShape).background(Color(0xFFE8D6F7)), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = "프로필 이미지 없음", tint = Color(0xFF8E24AA), modifier = Modifier.size(44.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF222222))
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(icon: @Composable () -> Unit, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF444444))
    }
}

@Composable
private fun TagWrapSection(tags: List<String>, chipColor: Color, textColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tags.chunked(3).forEach { rowTags ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowTags.forEach { tag -> TagChip(text = tag, chipColor = chipColor, textColor = textColor) }
            }
        }
    }
}

@Composable
private fun TagChip(text: String, chipColor: Color, textColor: Color) {
    Surface(shape = RoundedCornerShape(20.dp), color = chipColor) {
        Text(text = text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = textColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PhotoSection(imageUrls: List<String>, onAddPhotoClick: () -> Unit) {
    val photos = imageUrls.filter { it.isNotBlank() }
    val items = mutableListOf<String>().apply { addAll(photos); if (photos.size < 9) add("ADD_BUTTON") }
    SectionCard(title = "사진") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.chunked(3).forEach { rowItems ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowItems.forEach { item ->
                        if (item == "ADD_BUTTON") AddPhotoItem(onClick = onAddPhotoClick, modifier = Modifier.weight(1f))
                        else PhotoItem(imageUrl = item, modifier = Modifier.weight(1f))
                    }
                    repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun ScheduleSection(
    schedule: Map<String, List<String>>,
    onScheduleClick: () -> Unit = {}
) {
    SectionCard(title = "시간표", onClick = onScheduleClick) {
        Column {
            // 요일 헤더 고정
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Spacer(modifier = Modifier.width(36.dp))
                scheduleDays.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFF1E7F7), RoundedCornerShape(6.dp))
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6F3D8A)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 시간별 행
            scheduleTimes.forEach { time ->
                val isHalfHour = time.endsWith(":30")
                val timeIdx = scheduleTimes.indexOf(time)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(modifier = Modifier.width(36.dp)) {
                        if (!isHalfHour) {
                            Text(
                                text = time.substring(0, 5),
                                fontSize = 8.sp,
                                color = Color(0xFF9C79A8)
                            )
                        }
                    }

                    scheduleDays.forEach { day ->
                        val isSelected = schedule[day]?.contains(time) == true
                        val prevTime = scheduleTimes.getOrNull(timeIdx - 1)
                        val nextTime = scheduleTimes.getOrNull(timeIdx + 1)
                        val isPrevSelected = prevTime != null && schedule[day]?.contains(prevTime) == true
                        val isNextSelected = nextTime != null && schedule[day]?.contains(nextTime) == true

                        // 연속 블록 처리 - 위아래 연결 여부에 따라 모서리 조정
                        val topRadius = if (isPrevSelected) 0.dp else 4.dp
                        val bottomRadius = if (isNextSelected) 0.dp else 4.dp

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .then(
                                    if (isHalfHour && !isPrevSelected)
                                        Modifier.drawBehind {
                                            drawLine(
                                                color = Color(0xFFE0D0EA),
                                                start = Offset(0f, 0f),
                                                end = Offset(size.width, 0f),
                                                strokeWidth = 1f,
                                                pathEffect = PathEffect.dashPathEffect(
                                                    floatArrayOf(3f, 3f), 0f
                                                )
                                            )
                                        }
                                    else Modifier
                                )
                                .background(
                                    color = if (isSelected) Color(0xFFCE93D8) else Color(0xFFF7F2F9),
                                    shape = RoundedCornerShape(
                                        topStart = topRadius,
                                        topEnd = topRadius,
                                        bottomStart = bottomRadius,
                                        bottomEnd = bottomRadius
                                    )
                                )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // 편집 안내
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color(0xFFAB47BC),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "탭하여 시간표 편집",
                    fontSize = 11.sp,
                    color = Color(0xFFAB47BC)
                )
            }
        }
    }
}

@Composable
private fun FoodPreferenceSection(likes: List<String>, dislikes: List<String>) {
    SectionCard(title = "음식 취향") {
        Text(text = "좋아하는 음식", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
        Spacer(modifier = Modifier.height(10.dp))
        TagWrapSection(tags = likes, chipColor = Color(0xFFE6F7E8), textColor = Color(0xFF2E7D32))
        Spacer(modifier = Modifier.height(18.dp))
        Divider(color = Color(0xFFF0F0F0))
        Spacer(modifier = Modifier.height(18.dp))
        Text(text = "싫어하는 음식", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
        Spacer(modifier = Modifier.height(10.dp))
        TagWrapSection(tags = dislikes, chipColor = Color(0xFFFFE7E7), textColor = Color(0xFFC62828))
    }
}

@Composable
private fun EditProfileButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
    ) {
        Icon(imageVector = Icons.Default.Edit, contentDescription = "프로필 수정", tint = Color.White)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "프로필 수정하기", color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PhotoItem(imageUrl: String, modifier: Modifier = Modifier) {
    AsyncImage(model = imageUrl, contentDescription = "프로필 사진", modifier = modifier.aspectRatio(1f).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
}

@Composable
private fun AddPhotoItem(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick, modifier = modifier.aspectRatio(1f), shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1E7F7)),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(text = "+", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF8E24AA), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "사진 추가", style = MaterialTheme.typography.bodySmall, color = Color(0xFF8E24AA))
        }
    }
}