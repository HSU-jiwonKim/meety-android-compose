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
                }
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
            item { MyProfileInfoSection(uiState = uiState) }
            item { ScheduleSection(schedule = uiState.schedule, onScheduleClick = onScheduleClick) }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun MyProfileTopBar(
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFFB44FD3), Color(0xFFEC4899))),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Meety",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD946C7)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "프로필 메뉴",
                    tint = Color(0xFF4B4B4B)
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
        contentColor = Color(0xFF111111),
        modifier = Modifier.padding(horizontal = 20.dp)
    ) {
        Tab(
            selected = selectedTab == MyProfileTab.PHOTOS,
            onClick = { onTabSelected(MyProfileTab.PHOTOS) },
            text = { Text("사진", fontWeight = if (selectedTab == MyProfileTab.PHOTOS) FontWeight.Bold else FontWeight.Medium) }
        )
        Tab(
            selected = selectedTab == MyProfileTab.INFO,
            onClick = { onTabSelected(MyProfileTab.INFO) },
            text = { Text("정보", fontWeight = if (selectedTab == MyProfileTab.INFO) FontWeight.Bold else FontWeight.Medium) }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProfileHeaderSection(
    uiState: UserProfileUiState,
    onImageClick: () -> Unit,
    onChangeMainImageClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(Color(0xFFF2F3F5))
                .combinedClickable(
                    onClick = onImageClick,
                    onLongClick = onChangeMainImageClick
                ),
            contentAlignment = Alignment.Center
        ) {
            val profileImageUrl = if (uiState.mainProfileImageUrl.isNotBlank()) uiState.mainProfileImageUrl else uiState.profileImages.firstOrNull().orEmpty()

            if (profileImageUrl.isNotBlank()) {
                AsyncImage(
                    model = profileImageUrl,
                    contentDescription = "프로필 이미지",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "프로필 이미지 없음",
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(46.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = uiState.name,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222)
            )

            if (uiState.age > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${uiState.age}세",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
        }

        if (uiState.bio.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.bio,
                fontSize = 14.sp,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
private fun PhotoSection(
    imageUrls: List<String>,
    onAddPhotoClick: () -> Unit,
    onPhotoClick: (String) -> Unit,
    onDeletePhotoClick: (String) -> Unit,
    onSetMainPhotoClick: (String) -> Unit
) {
    val photos = imageUrls.filter { it.isNotBlank() }
    val items = mutableListOf<String>().apply {
        addAll(photos)
        if (photos.size < 9) add("ADD_BUTTON")
    }

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

    AsyncImage(
        model = imageUrl,
        contentDescription = "프로필 사진",
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            ),
        contentScale = ContentScale.Crop
    )
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
            Text(
                text = "프로필 사진 변경",
                color = Color.Black
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PhotoActionMenuItem(
                    text = "내 갤러리에서 선택",
                    onClick = onPickFromGallery
                )

                photos.chunked(3).forEach { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { imageUrl ->
                            val isSelected = selectedImageUrl == imageUrl

                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "선택할 사진",
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) Color.Black else Color.Transparent,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { selectedImageUrl = imageUrl },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                if (photos.isEmpty()) {
                    Text(
                        text = "선택할 사진이 없습니다.",
                        color = Color.Black
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedImageUrl != null,
                onClick = {
                    selectedImageUrl?.let { onSelect(it) }
                }
            ) {
                Text(
                    text = "확인",
                    color = Color.Black
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "취소", color = Color.Black)
            }
        }
    )
}
