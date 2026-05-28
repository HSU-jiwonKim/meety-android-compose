package com.bugzero.meety.ui.team

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.fillMaxHeight
import com.bugzero.meety.ui.auth.InfoRow
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
data class UserProfileUiState(
    val name: String,
    val age: Int,
    val school: String,
    val department: String,
    val height: Int,
    val location: String,
    val mbti: String,
    val bio: String,
    val interests: List<String>,
    val foodLikes: List<String>,
    val foodDislikes: List<String>,
    val profileImages: List<String>,
    val mainProfileImageUrl: String = "",
    val schedule: Map<String, List<String>> = emptyMap()
)

private enum class MyProfileTab { PHOTOS, INFO }

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
    onScheduleClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onAddPhotoClick(uri)
        }
    }

    val mainImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onAddMainPhotoClick(uri)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            MyProfileTopBar(
                handleName = uiState.name.ifBlank { "프로필" },
                onEditProfileClick = onEditProfileClick,
                onLogoutClick = onLogoutClick
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        MyPageBody(
            uiState = uiState,
            viewModel = viewModel,
            onEditProfileClick = onEditProfileClick,
            onScheduleClick = onScheduleClick,
            onAddPhotoClick = {
                imagePickerLauncher.launch("image/*")
            },
            onPickMainPhotoFromGalleryClick = {
                mainImagePickerLauncher.launch("image/*")
            },
            onLogoutClick = onLogoutClick,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun MyPageBody(
    uiState: UserProfileUiState,
    viewModel: MyPageViewModel,
    onEditProfileClick: () -> Unit,
    onScheduleClick: () -> Unit = {},
    onAddPhotoClick: () -> Unit,
    onPickMainPhotoFromGalleryClick: () -> Unit,
    onLogoutClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var showMainImageDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(MyProfileTab.PHOTOS) }

    selectedImageUrl?.let { imageUrl ->
        LargeImageDialog(
            imageUrl = imageUrl,
            onDismiss = { selectedImageUrl = null }
        )
    }

    if (showMainImageDialog) {
        SelectMainProfileImageDialog(
            imageUrls = uiState.profileImages,
            currentMainUrl = uiState.mainProfileImageUrl.ifBlank { uiState.profileImages.firstOrNull().orEmpty() },
            onDismiss = { showMainImageDialog = false },
            onSelect = { imageUrl ->
                showMainImageDialog = false
                viewModel.changeMainProfileImage(imageUrl)
            },
            onPickFromGallery = {
                showMainImageDialog = false
                onPickMainPhotoFromGalleryClick()
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ProfileHeaderSection(
                uiState = uiState,
                onImageClick = {
                    val imageUrl = if (uiState.mainProfileImageUrl.isNotBlank()) uiState.mainProfileImageUrl else uiState.profileImages.firstOrNull().orEmpty()
                    if (imageUrl.isNotBlank()) {
                        selectedImageUrl = imageUrl
                    }
                },
                onChangeMainImageClick = {
                    showMainImageDialog = true
                },
                onEditProfileClick = onEditProfileClick,
                onScheduleClick = onScheduleClick,
                onAddPhotoClick = onAddPhotoClick
            )
        }

        item {
            MyProfileTabRow(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }

        if (selectedTab == MyProfileTab.PHOTOS) {
            item {
                PhotoSection(
                    imageUrls = uiState.profileImages,
                    mainProfileImageUrl = uiState.mainProfileImageUrl.ifBlank { uiState.profileImages.firstOrNull().orEmpty() },
                    onAddPhotoClick = onAddPhotoClick,
                    onPhotoClick = { imageUrl ->
                        selectedImageUrl = imageUrl
                    },
                    onDeletePhotoClick = { imageUrl ->
                        viewModel.deleteProfileImage(imageUrl)
                    },
                    onSetMainPhotoClick = { imageUrl ->
                        viewModel.changeMainProfileImage(imageUrl)
                    }
                )
            }
        } else {
            item { ScheduleSection(schedule = uiState.schedule, onScheduleClick = onScheduleClick) }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun MyProfileTopBar(
    handleName: String,
    onEditProfileClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = handleName,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.2).sp,
            color = Color(0xFF17161D)
        )

        Spacer(modifier = Modifier.weight(1f))

        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "프로필 메뉴",
                    tint = Color(0xFF17161D)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = Color(0xFFF7F7F7),
                shape = RoundedCornerShape(18.dp)
            ) {
                DropdownMenuItem(
                    text = { Text("프로필 수정하기") },
                    onClick = {
                        expanded = false
                        onEditProfileClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("로그아웃", color = Color(0xFFE53935)) },
                    onClick = {
                        expanded = false
                        onLogoutClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun MyProfileTabRow(
    selectedTab: MyProfileTab,
    onTabSelected: (MyProfileTab) -> Unit
) {
    TabRow(
        selectedTabIndex = if (selectedTab == MyProfileTab.PHOTOS) 0 else 1,
        containerColor = Color.White,
        contentColor = Color(0xFF17161D),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Tab(
            selected = selectedTab == MyProfileTab.PHOTOS,
            onClick = { onTabSelected(MyProfileTab.PHOTOS) },
            icon = {
                Icon(
                    Icons.Outlined.GridView,
                    contentDescription = "사진",
                    tint = if (selectedTab == MyProfileTab.PHOTOS) Color(0xFF17161D) else Color(0xFFC4C2CD)
                )
            }
        )
        Tab(
            selected = selectedTab == MyProfileTab.INFO,
            onClick = { onTabSelected(MyProfileTab.INFO) },
            icon = {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = "정보·시간표",
                    tint = if (selectedTab == MyProfileTab.INFO) Color(0xFF17161D) else Color(0xFFC4C2CD)
                )
            }
        )
    }
}

private val IgGradient = Brush.linearGradient(
    0f to Color(0xFF7B5CFF), 0.45f to Color(0xFFA24BFF), 1f to Color(0xFFFF5C8A)
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProfileHeaderSection(
    uiState: UserProfileUiState,
    onImageClick: () -> Unit,
    onChangeMainImageClick: () -> Unit,
    onEditProfileClick: () -> Unit = {},
    onScheduleClick: () -> Unit = {},
    onAddPhotoClick: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // ── ig-top: 스토리 링 아바타 + 통계 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(88.dp),
                contentAlignment = Alignment.Center
            ) {
                // 그라데이션 링 + 프로필 사진 (탭 → 크게 보기)
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(IgGradient, CircleShape)
                        .padding(3.dp)
                        .clickable(onClick = onImageClick),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val profileImageUrl = if (uiState.mainProfileImageUrl.isNotBlank()) uiState.mainProfileImageUrl
                        else uiState.profileImages.firstOrNull().orEmpty()
                        if (profileImageUrl.isNotBlank()) {
                            AsyncImage(
                                model = profileImageUrl,
                                contentDescription = "프로필 이미지",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(Color(0xFFF2EEFF), Color(0xFFFFECF3)))),
                                contentAlignment = Alignment.Center
                            ) { Text("🙋", fontSize = 34.sp) }
                        }
                    }
                }
                // 카메라 배지 — 탭하면 프로필 사진 변경 다이얼로그 오픈
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = (-2).dp, y = (-2).dp)
                        .clip(CircleShape)
                        .background(Color(0xFF7B5CFF))
                        .border(2.dp, Color.White, CircleShape)
                        .clickable(onClick = onChangeMainImageClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "프로필 사진 변경",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.weight(1f).padding(start = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStat(uiState.profileImages.size.toString(), "사진")
                ProfileStat(uiState.interests.size.toString(), "관심사")
                ProfileStat(uiState.foodLikes.size.toString(), "취향")
            }
        }

        // ── ig-bio ──
        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(uiState.name, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF17161D))
                if (uiState.mbti.isNotBlank()) {
                    Spacer(Modifier.width(7.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF2EEFF), RoundedCornerShape(999.dp))
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    ) {
                        Text(uiState.mbti, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6D49E0))
                    }
                }
            }
            val deptLine = buildString {
                append("🎓 ")
                if (uiState.department.isNotBlank()) append(uiState.department)
                if (uiState.age > 0) { if (length > 2) append(" · "); append("${uiState.age}세") }
                if (uiState.height > 0) { if (length > 2) append(" · "); append("${uiState.height}cm") }
                if (uiState.school.isNotBlank()) { if (length > 2) append(" · "); append(uiState.school) }
            }
            Spacer(Modifier.height(3.dp))
            Text(deptLine, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF56535F))
            if (uiState.bio.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(uiState.bio, fontSize = 13.sp, color = Color(0xFF17161D), lineHeight = 20.sp)
            }
            if (uiState.interests.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    uiState.interests.joinToString(" ") { "#$it" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7B5CFF),
                    lineHeight = 20.sp
                )
            }
            if (uiState.location.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "사는곳",
                        tint = Color(0xFF9B98A6),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        uiState.location,
                        fontSize = 12.sp,
                        color = Color(0xFF9B98A6)
                    )
                }
            }
        }

        // ── ig-actions ──
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(IgGradient)
                    .clickable(onClick = onEditProfileClick)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("프로필 수정", fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFECEAF1), RoundedCornerShape(12.dp))
                    .clickable(onClick = onScheduleClick)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("시간표 공유", fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF17161D))
            }
            // + 버튼
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFECEAF1), RoundedCornerShape(12.dp))
                    .clickable(onClick = onAddPhotoClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "사진 추가",
                    tint = Color(0xFF17161D),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // ── ig-highlights (관심사 원형) ──
        if (uiState.interests.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 22.dp, end = 22.dp, top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                uiState.interests.take(8).forEachIndexed { idx, interest ->
                    val emojis = listOf("💜", "🍝", "✈️", "🎧", "☕", "🎬", "📚", "🌸")
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .background(if (idx == 0) IgGradient else Brush.linearGradient(listOf(Color(0xFFE7E4EF), Color(0xFFD9D6E3))), CircleShape)
                                .padding(2.5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color.White),
                                contentAlignment = Alignment.Center
                            ) { Text(emojis[idx % emojis.size], fontSize = 23.sp) }
                        }
                        Spacer(Modifier.height(7.dp))
                        Text(interest, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF56535F), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF17161D))
        Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = Color(0xFF56535F))
    }
}

@Composable
private fun SectionCard(
    title: String,
    onClick: (() -> Unit)? = null,
    showTitleDot: Boolean = false,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            if (showTitleDot) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF7B5CFF), Color(0xFFFF5C8A))))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF17161D)
                    )
                }
            } else {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF222222))
            }
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
private fun MyProfileInfoSection(uiState: UserProfileUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (uiState.department.isNotBlank()) ProfileInfoRow("학과", uiState.department)
        if (uiState.height > 0) ProfileInfoRow("키", "${uiState.height}cm")
        if (uiState.location.isNotBlank()) ProfileInfoRow("거주지", uiState.location)
        if (uiState.mbti.isNotBlank()) ProfileInfoRow("MBTI", uiState.mbti)

        if (uiState.interests.isNotEmpty()) {
            Divider(color = Color(0xFFF0F0F0))
            Text("관심사", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF888888))
            TagWrapSection(
                tags = uiState.interests,
                chipColor = Color(0xFFF3E7FF),
                textColor = Color(0xFF8E24AA)
            )
        }

        if (uiState.foodLikes.isNotEmpty() || uiState.foodDislikes.isNotEmpty()) {
            Divider(color = Color(0xFFF0F0F0))
            FoodPreferenceInlineSection(likes = uiState.foodLikes, dislikes = uiState.foodDislikes)
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFF9CA3AF),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(end = 12.dp)
        )

        Text(
            text = value,
            fontSize = 13.sp,
            color = Color(0xFF222222),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FoodPreferenceInlineSection(likes: List<String>, dislikes: List<String>) {
    if (likes.isNotEmpty()) {
        Text(text = "좋아하는 음식", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF888888))
        TagWrapSection(tags = likes, chipColor = Color(0xFFE6F7E8), textColor = Color(0xFF2E7D32))
    }

    if (dislikes.isNotEmpty()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "싫어하는 음식", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF888888))
        TagWrapSection(tags = dislikes, chipColor = Color(0xFFFFE7E7), textColor = Color(0xFFC62828))
    }
}

@Composable
private fun ScheduleSection(
    schedule: Map<String, List<String>>,
    onScheduleClick: () -> Unit = {}
) {
    SectionCard(title = "시간표", onClick = onScheduleClick, showTitleDot = true) {
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
                            .background(Color(0xFFEFE9FF), RoundedCornerShape(6.dp))
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6D49E0)
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
                                color = Color(0xFF9B98A6)
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
                                                color = Color(0xFFE4DEFF),
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
                                .then(
                                    if (isSelected)
                                        Modifier.background(
                                            brush = Brush.linearGradient(
                                                listOf(
                                                    Color(0xFF7B5CFF).copy(alpha = 0.35f),
                                                    Color(0xFFFF5C8A).copy(alpha = 0.28f)
                                                )
                                            ),
                                            shape = RoundedCornerShape(
                                                topStart = topRadius,
                                                topEnd = topRadius,
                                                bottomStart = bottomRadius,
                                                bottomEnd = bottomRadius
                                            )
                                        )
                                    else
                                        Modifier.background(
                                            color = Color(0xFFF8F6FF),
                                            shape = RoundedCornerShape(
                                                topStart = topRadius,
                                                topEnd = topRadius,
                                                bottomStart = bottomRadius,
                                                bottomEnd = bottomRadius
                                            )
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
                    tint = Color(0xFF7B5CFF),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "탭하여 시간표 편집",
                    fontSize = 11.sp,
                    color = Color(0xFF7B5CFF)
                )
            }
        }
    }
}

@Composable
private fun FoodPreferenceSection(likes: List<String>, dislikes: List<String>) {
    SectionCard(title = "싫어하는 음식", showTitleDot = true) {
        if (dislikes.isNotEmpty()) {
            TagWrapSection(tags = dislikes, chipColor = Color(0xFFFFE7E7), textColor = Color(0xFFC62828))
        }
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
private fun PhotoSection(
    imageUrls: List<String>,
    mainProfileImageUrl: String = "",
    onAddPhotoClick: () -> Unit,
    onPhotoClick: (String) -> Unit,
    onDeletePhotoClick: (String) -> Unit,
    onSetMainPhotoClick: (String) -> Unit
) {
    val photos = imageUrls.filter { it.isNotBlank() }
    val items = photos

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { item ->
                    if (item == "ADD_BUTTON") {
                        AddPhotoItem(
                            onClick = onAddPhotoClick,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        PhotoItem(
                            imageUrl = item,
                            isMainPhoto = item == mainProfileImageUrl,
                            modifier = Modifier.weight(1f),
                            onClick = { onPhotoClick(item) },
                            onDeleteClick = { onDeletePhotoClick(item) },
                            onSetMainClick = { onSetMainPhotoClick(item) }
                        )
                    }
                }

                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoItem(
    imageUrl: String,
    isMainPhoto: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSetMainClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    if (showMenu) {
        AlertDialog(
            onDismissRequest = { showMenu = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "사진 관리",
                    color = Color.Black
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    PhotoActionMenuItem(
                        text = "프로필 사진으로 변경",
                        onClick = {
                            showMenu = false
                            onSetMainClick()
                        }
                    )

                    PhotoActionMenuItem(
                        text = "삭제",
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    Box(modifier = modifier.aspectRatio(1f)) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "프로필 사진",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .then(
                    if (isMainPhoto)
                        Modifier.border(2.5.dp, Color(0xFF7B5CFF), RoundedCornerShape(16.dp))
                    else
                        Modifier
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                ),
            contentScale = ContentScale.Crop
        )
        // 대표 사진 배지
        if (isMainPhoto) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF7B5CFF))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "대표",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
@Composable
private fun PhotoActionMenuItem(
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isPressed) Color(0xFFEDEDED)
                else Color.Transparent
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Text(
            text = text,
            color = Color.Black,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
@Composable
private fun AddPhotoItem(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick, modifier = modifier.aspectRatio(1f), shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1E7F7)),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(text = "+", style = MaterialTheme.typography.headlineLarge, color = Color(0xFF8E24AA), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LogoutButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFF5F5F5),
            contentColor = Color(0xFF888888)
        )
    ) {
        Icon(
            imageVector = Icons.Default.ExitToApp,
            contentDescription = "로그아웃",
            tint = Color(0xFF888888)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "로그아웃",
            fontWeight = FontWeight.Medium
        )
    }
}
@Composable
private fun LargeImageDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "큰 사진",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun SelectMainProfileImageDialog(
    imageUrls: List<String>,
    currentMainUrl: String = "",
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onPickFromGallery: () -> Unit
) {
    val photos = imageUrls.filter { it.isNotBlank() }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(text = "프로필 사진 변경", color = Color(0xFF17161D), fontWeight = FontWeight.ExtraBold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PhotoActionMenuItem(
                    text = "📷  갤러리에서 새 사진 선택",
                    onClick = onPickFromGallery
                )

                if (photos.isNotEmpty()) {
                    Text(
                        text = "내 사진에서 선택",
                        fontSize = 12.sp,
                        color = Color(0xFF9B98A6),
                        fontWeight = FontWeight.SemiBold
                    )
                    photos.chunked(3).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowItems.forEach { imageUrl ->
                                val isPicked = selectedImageUrl == imageUrl
                                val isCurrent = imageUrl == currentMainUrl && selectedImageUrl == null
                                val isHighlighted = isPicked || isCurrent

                                Box(modifier = Modifier.size(72.dp)) {
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = "선택할 사진",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(14.dp))
                                            .border(
                                                width = if (isHighlighted) 2.5.dp else 0.dp,
                                                color = if (isHighlighted) Color(0xFF7B5CFF) else Color.Transparent,
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .clickable { selectedImageUrl = imageUrl },
                                        contentScale = ContentScale.Crop
                                    )
                                    // 선택됨 체크 또는 현재 대표 표시
                                    if (isHighlighted) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .align(Alignment.TopEnd)
                                                .offset(x = (-4).dp, y = 4.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF7B5CFF))
                                                .border(2.dp, Color.White, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                    // 현재 대표 사진 "현재" 라벨
                                    if (isCurrent) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(4.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF7B5CFF).copy(alpha = 0.85f))
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text("현재", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(text = "선택할 사진이 없습니다.", color = Color(0xFF9B98A6), fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedImageUrl != null,
                onClick = { selectedImageUrl?.let { onSelect(it) } }
            ) {
                Text(
                    text = "확인",
                    color = if (selectedImageUrl != null) Color(0xFF7B5CFF) else Color(0xFFD1D5DB),
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "취소", color = Color(0xFF6B7280))
            }
        }
    )
}
