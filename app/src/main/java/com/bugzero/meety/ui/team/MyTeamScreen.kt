package com.bugzero.meety.ui.team

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.bugzero.meety.ui.call.CallViewModel
import com.bugzero.meety.ui.feed.FeedConstants
import com.bugzero.meety.ui.theme.Gray100
import com.bugzero.meety.ui.theme.Gray200
import com.bugzero.meety.ui.theme.Gray400
import com.bugzero.meety.ui.theme.Gray500
import com.bugzero.meety.ui.theme.Gray900
import com.bugzero.meety.ui.theme.Purple
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType

private enum class MyTeamTab {
    FRIENDS, ADD_FRIEND
}

private enum class FriendScreenMode {
    LIST, ADD_FRIEND, PROFILE_DETAIL
}

private enum class FriendProfileTab {
    PHOTOS, INFO
}

private val friendScheduleDays = listOf("월", "화", "수", "목", "금")
private val friendScheduleTimes = listOf(
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

data class FriendUiState(
    val id: String,
    val name: String,
    val email: String = "",
    val profileImageUrl: String = "",
    val profileImages: List<String> = emptyList(),
    val department: String = "",
    val age: Int = 0,
    val mbti: String = "",
    val bio: String = "",
    val location: String = "",
    val height: Int = 0,
    val interests: List<String> = emptyList(),
    val foodLikes: List<String> = emptyList(),
    val foodDislikes: List<String> = emptyList(),
    val schedule: Map<String, List<String>> = emptyMap(),
    val isFavorite: Boolean = false
)

data class ProfilePreviewUiState(
    val id: String = "",
    val name: String = "",
    val profileImageUrl: String = "",
    val profileImages: List<String> = emptyList(),
    val department: String = "",
    val age: Int = 0,
    val mbti: String = "",
    val bio: String = "",
    val location: String = "",
    val height: Int = 0,
    val interests: List<String> = emptyList(),
    val foodLikes: List<String> = emptyList(),
    val foodDislikes: List<String> = emptyList(),
    val schedule: Map<String, List<String>> = emptyMap()
)

@Composable
fun MyTeamScreen(
    viewModel: TeamViewModel = viewModel(),
    onHomeClick: () -> Unit = {},
    onMatchingClick: () -> Unit = {},
    onCreateTeamClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onCancelSentClick: (String) -> Unit = {},
    onEditTeamClick: () -> Unit = {},
    onCreateNewTeamClick: () -> Unit = {},
    onFriendChatClick: (chatId: String, roomName: String) -> Unit = { _, _ -> },

    // 변경한 부분: 통화 시작 후 CallScreen으로 이동시키기 위한 콜백
    // NavGraph에서 route에 맞게 연결하면 됨
    onCallStarted: (chatId: String, callType: String) -> Unit = { _, _ -> }
) {
    var screenMode by remember { mutableStateOf(FriendScreenMode.LIST) }
    var friendEmail by remember { mutableStateOf("") }
    var selectedProfile by remember { mutableStateOf<ProfilePreviewUiState?>(null) }
    var friendSearchQuery by remember { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val friends by viewModel.friends.collectAsState()
    val friendAddMessage by viewModel.friendAddMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchedUser by viewModel.searchedUser.collectAsState()
    val searchMessage by viewModel.searchMessage.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadFriends()

    }

    val friendList = friends.map {
        FriendUiState(
            id = it.userId,
            name = if (it.name.isBlank()) "이름 없음" else it.name,
            email = it.email,
            profileImageUrl = it.profileImageUrl,
            profileImages = it.profileImages,
            department = it.department,
            age = it.age,
            mbti = it.mbti,
            bio = it.bio,
            location = it.location,
            height = it.height,
            interests = it.interests,
            foodLikes = it.foodLikes,
            foodDislikes = it.foodDislikes,
            schedule = it.schedule,
            isFavorite = it.isFavorite
        )
    }

    val filteredFriendList = friendList.filter { friend ->
        val query = friendSearchQuery.trim()
        query.isBlank() ||
                friend.name.contains(query, ignoreCase = true) ||
                friend.email.contains(query, ignoreCase = true)
    }

    val favoriteFriendList = filteredFriendList.filter { it.isFavorite }
    val normalFriendList = filteredFriendList.filter { !it.isFavorite }

    fun startProfileCall(
        targetUserId: String,
        callType: String
    ) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        if (currentUserId.isBlank()) {
            Toast.makeText(context, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        if (targetUserId.isBlank()) {
            Toast.makeText(context, "상대방 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentUserId == targetUserId) {
            Toast.makeText(context, "자기 자신에게는 통화할 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        coroutineScope.launch {
            runCatching {
                val chatId = findOrCreateOneToOneChat(
                    currentUserId = currentUserId,
                    targetUserId = targetUserId
                )

                onCallStarted(chatId, callType)
            }.onFailure {
                Toast.makeText(
                    context,
                    "통화를 시작하지 못했습니다: ${it.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun startFriendChat(friend: FriendUiState) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        if (currentUserId.isBlank()) {
            Toast.makeText(context, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        if (friend.id.isBlank()) {
            Toast.makeText(context, "상대방 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        coroutineScope.launch {
            runCatching {
                findOrCreateOneToOneChat(
                    currentUserId = currentUserId,
                    targetUserId = friend.id,
                    targetUserName = friend.name
                )
            }.onSuccess { chatId ->
                onFriendChatClick(chatId, friend.name)
            }.onFailure {
                Toast.makeText(
                    context,
                    "채팅방을 만들지 못했습니다: ${it.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Scaffold(
        topBar = {
            when (screenMode) {
                FriendScreenMode.LIST -> {} // 리스트 모드는 LazyColumn 내부에서 스크롤됨
                FriendScreenMode.ADD_FRIEND -> AddFriendTopBar(
                    onBackClick = {
                        screenMode = FriendScreenMode.LIST
                        friendEmail = ""
                        viewModel.clearFriendAddMessage()
                        viewModel.clearSearchedUser()
                    }
                )
                FriendScreenMode.PROFILE_DETAIL -> FriendPageTopBar(
                    title = selectedProfile?.name ?: "프로필",
                    onBackClick = {
                        selectedProfile = null
                        screenMode = FriendScreenMode.LIST
                    }
                )
            }
        },
        containerColor = Color(0xFFF4F4F8),
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        when (screenMode) {
            FriendScreenMode.LIST -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 4.dp,
                        bottom = 130.dp
                    )
                ) {
                    item {
                        FriendListTopBar(
                            onAddFriendClick = { screenMode = FriendScreenMode.ADD_FRIEND }
                        )
                    }

                    item {
                        FriendSearchSection(
                            query = friendSearchQuery,
                            onQueryChange = { friendSearchQuery = it }
                        )
                    }

                    // 친구 추가 배너 (목업 .add-friend)
                    item {
                        AddFriendBanner(onClick = { screenMode = FriendScreenMode.ADD_FRIEND })
                    }

                    if (isLoading) {
                        item { LoadingCard() }
                    }

                    if (friendList.isEmpty() && !isLoading) {
                        item { EmptyFriendCard(text = "아직 친구가 없습니다.") }
                    } else if (filteredFriendList.isEmpty() && !isLoading) {
                        item { EmptyFriendCard(text = "검색 결과가 없습니다.") }
                    } else {
                        // 즐겨찾기 가로 스트립 (목업 .fav-row)
                        if (favoriteFriendList.isNotEmpty()) {
                            item { SectionTitle(text = "즐겨찾기") }
                            item {
                                FavoriteFriendRow(
                                    favorites = favoriteFriendList,
                                    onClick = { friend ->
                                        selectedProfile = friend.toProfilePreview()
                                        screenMode = FriendScreenMode.PROFILE_DETAIL
                                    }
                                )
                            }
                        }

                        item {
                            SectionTitle(text = "전체 친구", trailing = "${filteredFriendList.size}명")
                        }

                        items(filteredFriendList) { friend ->
                            FriendListItem(
                                friend = friend,
                                onProfileClick = {
                                    selectedProfile = friend.toProfilePreview()
                                    screenMode = FriendScreenMode.PROFILE_DETAIL
                                },
                                onFavoriteClick = { viewModel.toggleFavoriteFriend(friend.id, friend.isFavorite) },
                                onChatClick = { startFriendChat(friend) },
                                onCallClick = { startProfileCall(friend.id, "voice") },
                                onRemoveClick = { viewModel.removeFriend(friend.id) }
                            )
                        }
                    }
                }
            }

            FriendScreenMode.ADD_FRIEND -> {
                AddFriendPage(
                    modifier = Modifier.padding(innerPadding),
                    emailId = friendEmail,
                    searchedUser = searchedUser,
                    searchMessage = searchMessage,
                    friendAddMessage = friendAddMessage,
                    isLoading = isLoading,
                    onEmailIdChange = {
                        friendEmail = it
                        viewModel.clearFriendAddMessage()
                        viewModel.clearSearchedUser()
                    },
                    onSearchClick = {
                        viewModel.searchFriendByEmail("$friendEmail@hansung.ac.kr")
                    },
                    onAddClick = {
                        viewModel.addFriendByEmail("$friendEmail@hansung.ac.kr")
                    }
                )
            }

            FriendScreenMode.PROFILE_DETAIL -> {
                selectedProfile?.let { profile ->
                    FriendProfileDetailPage(
                        modifier = Modifier.padding(innerPadding),
                        profile = profile
                    )
                }
            }
        }
    }
}

private fun FriendUiState.toProfilePreview(): ProfilePreviewUiState {
    return ProfilePreviewUiState(
        id = id,
        name = name,
        profileImageUrl = profileImageUrl,
        profileImages = profileImages,
        department = department,
        age = age,
        mbti = mbti,
        bio = bio,
        location = location,
        height = height,
        interests = interests,
        foodLikes = foodLikes,
        foodDislikes = foodDislikes,
        schedule = schedule
    )
}


@Composable
private fun FriendListTopBar(
    onAddFriendClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 0.dp, end = 0.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "친구",
            fontSize = 23.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.4).sp,
            color = Color(0xFF17161D)
        )
    }
}

@Composable
private fun FriendPageTopBar(
    title: String,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "뒤로가기",
                tint = Color(0xFF111111)
            )
        }

        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111111)
        )
    }
}

@Composable
private fun AddFriendTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF4F4F8))
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .shadow(1.dp, RoundedCornerShape(13.dp))
                .clip(RoundedCornerShape(13.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFECEAF1), RoundedCornerShape(13.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBackClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "뒤로",
                tint = Color(0xFF17161D),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                "친구 추가",
                fontSize = 23.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.4).sp,
                color = Color(0xFF17161D)
            )
            Text(
                "새로운 친구를 찾아보세요",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF9B98A6)
            )
        }
    }
}

@Composable
private fun AddFriendPage(
    modifier: Modifier = Modifier,
    emailId: String,
    searchedUser: SearchedUserUiState?,
    searchMessage: String,
    friendAddMessage: String,
    isLoading: Boolean,
    onEmailIdChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onAddClick: () -> Unit
) {
    var addRequested by remember { mutableStateOf(false) }

    LaunchedEffect(emailId) { addRequested = false }

    val gradBrush = Brush.linearGradient(
        0f to Color(0xFF7B5CFF),
        0.45f to Color(0xFFA24BFF),
        1f to Color(0xFFFF5C8A)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4F4F8))
    ) {
        EmailPanel(
            emailId = emailId,
            searchedUser = searchedUser,
            searchMessage = searchMessage,
            friendAddMessage = friendAddMessage,
            isLoading = isLoading,
            addRequested = addRequested,
            gradBrush = gradBrush,
            onEmailIdChange = onEmailIdChange,
            onSearchClick = onSearchClick,
            onAddClick = {
                addRequested = true
                onAddClick()
            }
        )
    }
}

@Composable
private fun FriendProfileDetailPage(
    modifier: Modifier = Modifier,
    profile: ProfilePreviewUiState
) {
    var selectedTab by remember { mutableStateOf(FriendProfileTab.PHOTOS) }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }

    // 친구 프로필은 조회 전용이므로 대표 사진 변경, 사진 추가, 사진 삭제 기능은 넣지 않는다.
    val images = remember(profile.profileImageUrl, profile.profileImages) {
        (listOf(profile.profileImageUrl) + profile.profileImages)
            .filter { it.isNotBlank() }
            .distinct()
    }

    selectedImageUrl?.let { imageUrl ->
        FriendLargeImageDialog(
            imageUrl = imageUrl,
            onDismiss = { selectedImageUrl = null }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            FriendProfileHeader(
                profile = profile,
                photoCount = images.size,
                onImageClick = {
                    val imageUrl = profile.profileImageUrl.ifBlank { images.firstOrNull().orEmpty() }
                    if (imageUrl.isNotBlank()) {
                        selectedImageUrl = imageUrl
                    }
                }
            )
        }

        item {
            FriendProfileTabRow(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }

        if (selectedTab == FriendProfileTab.PHOTOS) {
            item {
                FriendPhotoGrid(
                    imageUrls = images,
                    onPhotoClick = { imageUrl -> selectedImageUrl = imageUrl }
                )
            }
        } else {
            item { FriendScheduleSection(schedule = profile.schedule) }
        }
    }
}

@Composable
private fun FriendProfileHeader(
    profile: ProfilePreviewUiState,
    photoCount: Int,
    onImageClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 내 프로필과 같은 상단 프로필 UI. 친구 프로필은 조회 전용이므로 길게 누르기/수정 버튼은 넣지 않는다.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            0f to Color(0xFF7B5CFF),
                            0.45f to Color(0xFFA24BFF),
                            1f to Color(0xFFFF5C8A)
                        ),
                        CircleShape
                    )
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
                    if (profile.profileImageUrl.isNotBlank()) {
                        AsyncImage(
                            model = profile.profileImageUrl,
                            contentDescription = "프로필 이미지",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    androidx.compose.ui.graphics.Brush.linearGradient(
                                        listOf(Color(0xFFF2EEFF), Color(0xFFFFECF3))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🙋", fontSize = 34.sp)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FriendProfileStat(photoCount.toString(), "사진")
                FriendProfileStat(profile.interests.size.toString(), "관심사")
                FriendProfileStat(profile.foodLikes.size.toString(), "취향")
            }
        }

        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = profile.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF17161D)
                )
                if (profile.mbti.isNotBlank()) {
                    Spacer(Modifier.width(7.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF2EEFF), RoundedCornerShape(999.dp))
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = profile.mbti,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6D49E0)
                        )
                    }
                }
            }

            val deptLine = buildString {
                append("🎓 ")
                if (profile.department.isNotBlank()) append(profile.department)
                if (profile.age > 0) {
                    if (length > 2) append(" · ")
                    append("${profile.age}세")
                }
            }

            Spacer(Modifier.height(3.dp))
            Text(
                text = if (deptLine.length > 2) deptLine else "🎓 프로필 정보 없음",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF56535F)
            )

            if (profile.bio.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = profile.bio,
                    fontSize = 13.sp,
                    color = Color(0xFF17161D),
                    lineHeight = 20.sp
                )
            }

            if (profile.interests.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = profile.interests.joinToString(" ") { "#$it" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7B5CFF),
                    lineHeight = 20.sp
                )
            }
        }

        // 친구 프로필 조회 화면에서는 내 프로필의 수정/시간표 공유/+사진 버튼 영역을 표시하지 않는다.
        // 대신 나머지 하이라이트 영역은 내 프로필과 같은 모양으로 유지한다.
        if (profile.interests.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 22.dp, end = 22.dp, top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                profile.interests.take(8).forEachIndexed { idx, interest ->
                    val emojis = listOf("💜", "🍝", "✈️", "🎧", "☕", "🎬", "📚", "🌸")
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(64.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .background(
                                    if (idx == 0) {
                                        androidx.compose.ui.graphics.Brush.linearGradient(
                                            0f to Color(0xFF7B5CFF),
                                            0.45f to Color(0xFFA24BFF),
                                            1f to Color(0xFFFF5C8A)
                                        )
                                    } else {
                                        androidx.compose.ui.graphics.Brush.linearGradient(
                                            listOf(Color(0xFFE7E4EF), Color(0xFFD9D6E3))
                                        )
                                    },
                                    CircleShape
                                )
                                .padding(2.5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emojis[idx % emojis.size], fontSize = 23.sp)
                            }
                        }
                        Spacer(Modifier.height(7.dp))
                        Text(
                            text = interest,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF56535F),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF17161D))
        Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = Color(0xFF56535F))
    }
}

@Composable
private fun FriendProfileTabRow(
    selectedTab: FriendProfileTab,
    onTabSelected: (FriendProfileTab) -> Unit
) {
    // 내 프로필 탭과 같은 아이콘 탭 UI
    TabRow(
        selectedTabIndex = if (selectedTab == FriendProfileTab.PHOTOS) 0 else 1,
        containerColor = Color.White,
        contentColor = Color(0xFF17161D),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Tab(
            selected = selectedTab == FriendProfileTab.PHOTOS,
            onClick = { onTabSelected(FriendProfileTab.PHOTOS) },
            icon = {
                Icon(
                    Icons.Outlined.GridView,
                    contentDescription = "사진",
                    tint = if (selectedTab == FriendProfileTab.PHOTOS) Color(0xFF17161D) else Color(0xFFC4C2CD)
                )
            }
        )
        Tab(
            selected = selectedTab == FriendProfileTab.INFO,
            onClick = { onTabSelected(FriendProfileTab.INFO) },
            icon = {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = "정보",
                    tint = if (selectedTab == FriendProfileTab.INFO) Color(0xFF17161D) else Color(0xFFC4C2CD)
                )
            }
        )
    }
}

@Composable
private fun FriendPhotoGrid(
    imageUrls: List<String>,
    onPhotoClick: (String) -> Unit
) {
    val photos = imageUrls.filter { it.isNotBlank() }

    if (photos.isEmpty()) {
        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
            EmptyFriendCard(text = "등록된 사진이 없습니다.")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        photos.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "프로필 사진",
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onPhotoClick(imageUrl) },
                        contentScale = ContentScale.Crop
                    )
                }

                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FriendLargeImageDialog(
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
private fun FriendScheduleSection(schedule: Map<String, List<String>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // 그라데이션 dot + 제목 (프로필탭 SectionCard 동일 스타일)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF7B5CFF), Color(0xFFFF5C8A))
                                )
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "시간표",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF17161D)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 요일 헤더
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Spacer(modifier = Modifier.width(36.dp))
                    friendScheduleDays.forEach { day ->
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

                friendScheduleTimes.forEach { time ->
                    val isHalfHour = time.endsWith(":30")
                    val timeIdx = friendScheduleTimes.indexOf(time)
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

                        friendScheduleDays.forEach { day ->
                            val isSelected = schedule[day]?.contains(time) == true
                            val prevTime = friendScheduleTimes.getOrNull(timeIdx - 1)
                            val nextTime = friendScheduleTimes.getOrNull(timeIdx + 1)
                            val isPrevSelected = prevTime != null && schedule[day]?.contains(prevTime) == true
                            val isNextSelected = nextTime != null && schedule[day]?.contains(nextTime) == true

                            val topRadius    = if (isPrevSelected) 0.dp else 4.dp
                            val bottomRadius = if (isNextSelected) 0.dp else 4.dp
                            val cellShape = RoundedCornerShape(
                                topStart = topRadius, topEnd = topRadius,
                                bottomStart = bottomRadius, bottomEnd = bottomRadius
                            )

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
                                                shape = cellShape
                                            )
                                        else
                                            Modifier.background(
                                                color = Color(0xFFF8F6FF),
                                                shape = cellShape
                                            )
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoRowForFriend(label: String, value: String) {
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
private fun FriendTagWrapSection(
    tags: List<String>,
    chipColor: Color,
    textColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tags.chunked(3).forEach { rowTags ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowTags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .background(chipColor, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = tag,
                            color = textColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendFoodPreferenceInlineSection(likes: List<String>, dislikes: List<String>) {
    if (likes.isNotEmpty()) {
        Text(text = "좋아하는 음식", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF888888))
        FriendTagWrapSection(tags = likes, chipColor = Color(0xFFE6F7E8), textColor = Color(0xFF2E7D32))
    }

    if (dislikes.isNotEmpty()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "싫어하는 음식", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF888888))
        FriendTagWrapSection(tags = dislikes, chipColor = Color(0xFFFFE7E7), textColor = Color(0xFFC62828))
    }
}

@Composable
private fun FriendSearchSection(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(15.dp),
        colors = androidx.compose.material3.TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedIndicatorColor = Color(0xFF7B5CFF),
            unfocusedIndicatorColor = Color(0xFFECEAF1),
            cursorColor = Color(0xFF7B5CFF)
        ),
        placeholder = {
            Text(
                text = "친구 이름 검색",
                color = Color(0xFF9B98A6),
                fontSize = 14.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "검색",
                tint = Color(0xFF9B98A6),
                modifier = Modifier.size(18.dp)
            )
        }
    )
}

@Composable
private fun MyTeamTabRow(
    selectedTab: MyTeamTab,
    friendCount: Int,
    onTabSelected: (MyTeamTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TeamTabButton(
            text = "친구",
            count = friendCount,
            selected = selectedTab == MyTeamTab.FRIENDS,
            modifier = Modifier.weight(1f),
            onClick = { onTabSelected(MyTeamTab.FRIENDS) }
        )

        TeamTabButton(
            text = "친구 추가",
            count = null,
            selected = selectedTab == MyTeamTab.ADD_FRIEND,
            modifier = Modifier.weight(1f),
            onClick = { onTabSelected(MyTeamTab.ADD_FRIEND) }
        )
    }
}

@Composable
private fun TeamTabButton(
    text: String,
    count: Int?,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color(0xFFF3E7FF) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = if (selected) Color(0xFFA020F0) else Color.Black,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )

        if (count != null) {
            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) Color(0xFFE2C9FA) else Color(0xFFF7EDFF)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    color = Color(0xFFA020F0),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(
    text: String,
    trailing: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            color = Color(0xFF17161D),
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold
        )
        if (trailing != null) {
            Text(
                text = trailing,
                color = Color(0xFF9B98A6),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** 친구 추가 배너 (목업 .add-friend) */
@Composable
private fun AddFriendBanner(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(Color(0xFFEFE9FF), Color(0xFFFFE8F1))
                )
            )
            .border(1.dp, Color(0xFFEADFFF), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        0f to Color(0xFF7B5CFF), 0.45f to Color(0xFFA24BFF), 1f to Color(0xFFFF5C8A)
                    ),
                    RoundedCornerShape(13.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("한성대 이메일로 친구 추가", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF17161D))
            Spacer(Modifier.height(2.dp))
            Text("@hansung.ac.kr 유저를 친구로 추가", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF56535F))
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF7B5CFF), modifier = Modifier.size(20.dp))
    }
}

/** 즐겨찾기 가로 스트립 (목업 .fav-row) */
@Composable
private fun FavoriteFriendRow(
    favorites: List<FriendUiState>,
    onClick: (FriendUiState) -> Unit
) {
    val palette = listOf(
        Color(0xFF7B5CFF), Color(0xFFFF5C8A), Color(0xFF26A69A), Color(0xFF1E88E5), Color(0xFFF5A623)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        favorites.forEach { friend ->
            val c = palette[(friend.id.hashCode() and Int.MAX_VALUE) % palette.size]
            Column(
                modifier = Modifier
                    .width(62.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onClick(friend) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                0f to Color(0xFF7B5CFF), 0.45f to Color(0xFFA24BFF), 1f to Color(0xFFFF5C8A)
                            ),
                            CircleShape
                        )
                        .padding(2.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFFF4F4F8))
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (friend.profileImageUrl.isNotBlank()) {
                            AsyncImage(
                                model = friend.profileImageUrl,
                                contentDescription = friend.name,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize().clip(CircleShape).background(c),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    friend.name.firstOrNull()?.toString() ?: "?",
                                    color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    friend.name,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF56535F),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FriendListItem(
    friend: FriendUiState,
    onProfileClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onChatClick: () -> Unit,
    onCallClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Color.White,
            title = { Text(text = "친구 삭제", color = Color.Black) },
            text = { Text(text = "${friend.name}님을 친구 목록에서 삭제할까요?", color = Color.Black) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onRemoveClick()
                    }
                ) { Text(text = "삭제", color = Color(0xFFE53935)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(text = "취소", color = Color.Black)
                }
            }
        )
    }

    val avatarColor = listOf(
        Color(0xFF7B5CFF), Color(0xFFFF5C8A), Color(0xFF26A69A), Color(0xFF1E88E5), Color(0xFFF5A623)
    )[(friend.id.hashCode() and Int.MAX_VALUE) % 5]

    val meta = buildString {
        if (friend.department.isNotBlank()) append(friend.department)
        if (friend.age > 0) {
            if (isNotEmpty()) append(" · ")
            append("${friend.age}세")
        }
        if (isEmpty() && friend.bio.isNotBlank()) append(friend.bio)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp))
            .background(Color.White, RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFFF1EFF5), RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onProfileClick,
                onLongClick = { showDeleteDialog = true }
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 아바타 (라운드 사각, 그라데이션/이미지)
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onProfileClick)
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(listOf(avatarColor, avatarColor.copy(alpha = 0.8f)))
                ),
            contentAlignment = Alignment.Center
        ) {
            if (friend.profileImageUrl.isNotBlank()) {
                AsyncImage(
                    model = friend.profileImageUrl,
                    contentDescription = "프로필 이미지",
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    friend.name.firstOrNull()?.toString() ?: "?",
                    color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(modifier = Modifier.width(13.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = friend.name,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = Color(0xFF17161D),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (meta.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = meta,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF9B98A6),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 인라인 액션: 채팅 / 통화 / 즐겨찾기
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            FriendActionButton(
                icon = Icons.Default.ChatBubble,
                tint = Color(0xFF7B5CFF),
                bg = Color(0xFFF2EEFF),
                border = false,
                onClick = onChatClick
            )
            FriendActionButton(
                icon = Icons.Default.Call,
                tint = Color(0xFF19C37D),
                bg = Color(0xFFFAFAFD),
                border = true,
                onClick = onCallClick
            )
            FriendActionButton(
                icon = if (friend.isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                tint = if (friend.isFavorite) Color(0xFFF5A623) else Color(0xFF9B98A6),
                bg = Color(0xFFFAFAFD),
                border = true,
                onClick = onFavoriteClick
            )
        }
    }
}

/** 친구 카드 인라인 액션 버튼 (목업 .f-act) */
@Composable
private fun FriendActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    bg: Color,
    border: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .then(if (border) Modifier.border(1.dp, Color(0xFFECEAF1), RoundedCornerShape(12.dp)) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun FriendActionMenuItem(
    text: String,
    textColor: Color = Color(0xFF111111),
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
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ===== 이메일 패널 =====
@Composable
private fun EmailPanel(
    emailId: String,
    searchedUser: SearchedUserUiState?,
    searchMessage: String,
    friendAddMessage: String,
    isLoading: Boolean,
    addRequested: Boolean,
    gradBrush: Brush,
    onEmailIdChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onAddClick: () -> Unit
) {
    val isFocused = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 120.dp)
    ) {
        // 히어로 카드
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(listOf(Color(0xFFEFE9FF), Color(0xFFFFE8F1)))
                )
                .border(1.5.dp, Color(0xFFC9B6FF), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(gradBrush),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    "학교 이메일로 친구 찾기",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF17161D)
                )
                Text(
                    "@hansung.ac.kr 이메일로 가입한\n같은 학교 친구를 바로 찾을 수 있어요",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF56535F),
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )
            }
        }

        // 입력 레이블
        Text(
            "이메일 아이디",
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF17161D),
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 8.dp)
        )

        // 입력 행
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 14.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(15.dp))
                .background(Color.White)
                .border(
                    1.5.dp,
                    if (isFocused.value || emailId.isNotBlank()) Color(0xFF7B5CFF) else Color(0xFFECEAF1),
                    RoundedCornerShape(15.dp)
                )
                .height(50.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 텍스트 입력 (왼쪽)
            BasicTextField(
                value = emailId,
                onValueChange = onEmailIdChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, end = 8.dp)
                    .onFocusChanged { isFocused.value = it.isFocused },
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = Color(0xFF17161D)
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                decorationBox = { innerTextField ->
                    Box {
                        if (emailId.isEmpty()) {
                            Text(
                                "아이디 입력",
                                fontSize = 14.sp,
                                color = Color(0xFFC4C2CD)
                            )
                        }
                        innerTextField()
                    }
                }
            )
            // X 버튼
            if (emailId.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFECEAF1))
                        .clickable { onEmailIdChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "지우기",
                        tint = Color(0xFF9B98A6),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            // 구분선
            Box(
                modifier = Modifier
                    .width(1.5.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFECEAF1))
            )
            // 서픽스 (오른쪽)
            Box(
                modifier = Modifier
                    .background(Color(0xFFFAFAFD))
                    .padding(horizontal = 12.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "@hansung.ac.kr",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9B98A6),
                    maxLines = 1
                )
            }
        }

        // 검색 버튼
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(gradBrush)
                .clickable { if (!isLoading) onSearchClick() }
                .padding(15.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(19.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "친구 검색하기",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }

        // 결과 영역
        val hasResult = searchedUser != null || searchMessage.isNotBlank() || friendAddMessage.isNotBlank()
        if (hasResult) {
            Text(
                "검색 결과",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF17161D),
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 10.dp, top = 4.dp)
            )

            if (searchedUser != null) {
                EmailResultCard(
                    user = searchedUser,
                    addRequested = addRequested || friendAddMessage == "친구 추가가 완료되었습니다.",
                    gradBrush = gradBrush,
                    onAddClick = onAddClick
                )
            }

            val feedbackMsg = when {
                friendAddMessage.isNotBlank() && searchedUser == null -> friendAddMessage
                searchMessage.isNotBlank() -> searchMessage
                friendAddMessage.isNotBlank() -> friendAddMessage
                else -> ""
            }
            if (feedbackMsg.isNotBlank()) {
                val isSuccess = feedbackMsg.contains("완료")
                Box(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSuccess) Color(0xFFF2EEFF) else Color(0xFFFFECEC))
                        .border(
                            1.dp,
                            if (isSuccess) Color(0xFFC9B6FF) else Color(0xFFFFCCCC),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(14.dp)
                ) {
                    Text(
                        feedbackMsg,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSuccess) Color(0xFF6D49E0) else Color(0xFFD32F2F)
                    )
                }
            }
        }
    }
}

// ===== 이메일 검색 결과 카드 =====
@Composable
private fun EmailResultCard(
    user: SearchedUserUiState,
    addRequested: Boolean,
    gradBrush: Brush,
    onAddClick: () -> Unit
) {
    val palette = listOf(
        Color(0xFF7B5CFF), Color(0xFFFF5C8A), Color(0xFF26A69A), Color(0xFF1E88E5), Color(0xFFF5A623)
    )
    val avatarColor = palette[(user.userId.hashCode() and Int.MAX_VALUE) % palette.size]

    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF2EEFF))
            .border(1.dp, Color(0xFFC9B6FF), RoundedCornerShape(20.dp))
            .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 아바타
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(listOf(avatarColor, avatarColor.copy(alpha = 0.8f)))
                ),
            contentAlignment = Alignment.Center
        ) {
            if (user.profileImageUrl.isNotBlank()) {
                AsyncImage(
                    model = user.profileImageUrl,
                    contentDescription = user.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    user.name.firstOrNull()?.toString() ?: "?",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(Modifier.width(13.dp))

        // 정보
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    user.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF17161D)
                )
                Spacer(Modifier.width(7.dp))
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFFECF3), RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        "같은 학교",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFE0457A)
                    )
                }
            }
            val metaText = buildString {
                if (user.department.isNotBlank()) append(user.department)
                if (user.age > 0) {
                    if (isNotEmpty()) append(" · ")
                    append("${user.age}세")
                }
            }
            if (metaText.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    metaText,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF9B98A6)
                )
            }
        }

        // 추가 버튼
        if (addRequested) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color(0xFFEEE9FF))
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF7B5CFF),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    "요청됨",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF7B5CFF)
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(11.dp))
                    .background(gradBrush)
                    .clickable { onAddClick() }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    "추가",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun AddFriendGuideCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "친구 추가",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFFA020F0)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "한성대 이메일을 사용하는 유저를 친구로 추가할 수 있어요.",
                color = Color(0xFF666666),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun AddFriendInputCard(
    email: String,
    message: String,
    onEmailChange: (String) -> Unit,
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("한성대 이메일") },
                placeholder = { Text("example@hansung.ac.kr") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            if (message.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onAddClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFA020F0),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "친구 추가",
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "친구 추가",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptyFriendCard(
    text: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun LoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.5.dp,
                color = Color(0xFFA020F0)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "불러오는 중...",
                color = Color(0xFF666666),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ProfilePreviewDialog(
    profile: ProfilePreviewUiState,
    onDismiss: () -> Unit,
    onVoiceCallClick: () -> Unit,
    onVideoCallClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val colorIndex = (profile.id.hashCode() and Int.MAX_VALUE) % FeedConstants.CardColorPalette.size
                val avatarColors = FeedConstants.CardColorPalette[colorIndex]

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (profile.profileImageUrl.isNotBlank()) {
                        AsyncImage(
                            model = profile.profileImageUrl,
                            contentDescription = "${profile.name} 프로필 사진",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(androidx.compose.ui.graphics.Brush.verticalGradient(avatarColors)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = profile.name.firstOrNull()?.toString() ?: "?",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = profile.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Gray900
                    )

                    if (profile.age > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Gray100, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${profile.age}세",
                                fontSize = 12.sp,
                                color = Gray500
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3E7FF))
                            .clickable { onVoiceCallClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "통화",
                            tint = Purple,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3E7FF))
                            .clickable { onVideoCallClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "영상통화",
                            tint = Purple,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Gray200)
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (profile.department.isNotBlank()) MemberInfoRow("학과", profile.department)
                    if (profile.height > 0) MemberInfoRow("키", "${profile.height}cm")
                    if (profile.location.isNotBlank()) MemberInfoRow("거주지", profile.location)
                    if (profile.mbti.isNotBlank()) MemberInfoRow("MBTI", profile.mbti)
                    if (profile.bio.isNotBlank()) MemberInfoRow("소개", profile.bio)
                }

                if (profile.interests.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "관심사",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Gray500
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            profile.interests.forEach { interest ->
                                Box(
                                    modifier = Modifier
                                        .background(FeedConstants.LightPurpleBg, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = interest,
                                        fontSize = 12.sp,
                                        color = Purple
                                    )
                                }
                            }
                        }
                    }
                }

                if (profile.foodLikes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "좋아하는 음식",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Gray500
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            profile.foodLikes.forEach { food ->
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFDCFCE7), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = food,
                                        fontSize = 12.sp,
                                        color = Color(0xFF16A34A)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Purple)
                ) {
                    Text(
                        text = "닫기",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Gray400,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(end = 12.dp)
        )

        Text(
            text = value,
            fontSize = 13.sp,
            color = Gray900,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

// 변경한 부분: 친구와의 1:1 채팅방을 찾거나 생성하는 함수
private suspend fun findOrCreateOneToOneChat(
    currentUserId: String,
    targetUserId: String,
    targetUserName: String = ""
): String {
    val db = FirebaseFirestore.getInstance()

    val participants = listOf(currentUserId, targetUserId).sorted()
    val directKey = participants.joinToString("_")

    val existingChat = db.collection("chats")
        .whereEqualTo("directKey", directKey)
        .limit(1)
        .get()
        .await()
        .documents
        .firstOrNull()

    if (existingChat != null) {
        return existingChat.id
    }

    val oldDirectChat = db.collection("chats")
        .whereArrayContains("participants", currentUserId)
        .get()
        .await()
        .documents
        .firstOrNull { doc ->
            val savedParticipants = doc.get("participants") as? List<String> ?: emptyList()
            doc.getString("type") == "direct" &&
                    savedParticipants.size == 2 &&
                    savedParticipants.contains(targetUserId)
        }

    if (oldDirectChat != null) {
        if (oldDirectChat.getString("directKey").isNullOrBlank()) {
            oldDirectChat.reference.update("directKey", directKey).await()
        }
        return oldDirectChat.id
    }

    val newChatRef = db.collection("chats").document()
    val now = Timestamp.now()

    val chatData = mapOf(
        "chatId" to newChatRef.id,
        "type" to "direct",
        "directKey" to directKey,
        "participants" to participants,
        "teamName" to targetUserName,
        "emoji" to "💬",
        "createdAt" to now,
        "lastMessage" to "",
        "lastMessageAt" to now,
        // ✨ 원년 멤버 모두에게 생성 시점 기록 → 재입장 시 acceptInvitation/inviteFriendsToChat 에서 덮어쓰기
        "memberJoinedAt" to participants.associateWith { now }
    )

    newChatRef.set(chatData).await()

    return newChatRef.id
}

// ═══════════════════════════════════════════════════════════════════════════════
// 공개 래퍼 — 좋아요 카드 등 외부에서 인스타그램 스타일 프로필을 표시할 때 사용
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * [FriendProfileDetailPage] 를 뒤로가기 버튼이 달린 전체화면으로 감싼 공개 컴포저블.
 * ChatListScreen 등 외부 화면에서 좋아요 발신자 프로필을 인스타그램 스타일로 보여줄 때 사용.
 */
@Composable
fun FriendProfileScreen(
    profile: ProfilePreviewUiState,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 상단 내비게이션 바 (뒤로가기 + 이름)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = Color(0xFF17161D)
                )
            }
            Text(
                text = profile.name,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF17161D)
            )
        }
        // 구분선
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFF1EFF5))
        )
        // 인스타그램 스타일 프로필 본문
        FriendProfileDetailPage(profile = profile)
    }
}
