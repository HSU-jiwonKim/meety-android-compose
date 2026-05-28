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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
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

private enum class MyTeamTab {
    FRIENDS, ADD_FRIEND
}

private enum class FriendScreenMode {
    LIST, ADD_FRIEND, PROFILE_DETAIL
}

private enum class FriendProfileTab {
    PHOTOS, INFO
}

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
    val schedule: Map<String, Any> = emptyMap()
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
                FriendScreenMode.LIST -> FriendListTopBar(
                    onAddFriendClick = { screenMode = FriendScreenMode.ADD_FRIEND }
                )
                FriendScreenMode.ADD_FRIEND -> FriendPageTopBar(
                    title = "친구 추가",
                    onBackClick = {
                        screenMode = FriendScreenMode.LIST
                        friendEmail = ""
                        viewModel.clearFriendAddMessage()
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
        containerColor = Color(0xFFF4F4F8)
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
                    email = friendEmail,
                    message = friendAddMessage,
                    onEmailChange = {
                        friendEmail = it
                        viewModel.clearFriendAddMessage()
                    },
                    onAddClick = { viewModel.addFriendByEmail(friendEmail) }
                )
            }

            FriendScreenMode.PROFILE_DETAIL -> {
                selectedProfile?.let { profile ->
                    FriendProfileDetailPage(
                        modifier = Modifier.padding(innerPadding),
                        profile = profile,
                        onVoiceCallClick = {
                            startProfileCall(
                                targetUserId = profile.id,
                                callType = "voice"
                            )
                        },
                        onVideoCallClick = {
                            startProfileCall(
                                targetUserId = profile.id,
                                callType = "video"
                            )
                        }
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
        foodLikes = foodLikes
    )
}


@Composable
private fun FriendListTopBar(
    onAddFriendClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF4F4F8))
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "친구",
                fontSize = 23.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.4).sp,
                color = Color(0xFF17161D)
            )
            Text(
                text = "함께할 사람들을 찾아보세요",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF9B98A6)
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .shadow(2.dp, RoundedCornerShape(13.dp))
                .background(Color.White, RoundedCornerShape(13.dp))
                .border(1.dp, Color(0xFFECEAF1), RoundedCornerShape(13.dp))
                .clickable(onClick = onAddFriendClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = "친구 추가",
                tint = Color(0xFF17161D),
                modifier = Modifier.size(20.dp)
            )
        }
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
private fun AddFriendPage(
    modifier: Modifier = Modifier,
    email: String,
    message: String,
    onEmailChange: (String) -> Unit,
    onAddClick: () -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 18.dp,
            bottom = 120.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { AddFriendGuideCard() }
        item { SectionTitle(text = "한성대 이메일로 친구 추가") }
        item {
            AddFriendInputCard(
                email = email,
                message = message,
                onEmailChange = onEmailChange,
                onAddClick = onAddClick
            )
        }
    }
}

@Composable
private fun FriendProfileDetailPage(
    modifier: Modifier = Modifier,
    profile: ProfilePreviewUiState,
    onVoiceCallClick: () -> Unit,
    onVideoCallClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(FriendProfileTab.PHOTOS) }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    val images = if (profile.profileImages.isNotEmpty()) profile.profileImages else listOf(profile.profileImageUrl).filter { it.isNotBlank() }

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
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 16.dp,
            bottom = 120.dp
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            FriendProfileHeader(profile = profile)
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
            item { FriendInfoSection(profile = profile) }
        }
    }
}

@Composable
private fun FriendProfileHeader(
    profile: ProfilePreviewUiState
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val colorIndex = (profile.id.hashCode() and Int.MAX_VALUE) % FeedConstants.CardColorPalette.size
        val avatarColors = FeedConstants.CardColorPalette[colorIndex]

        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(CircleShape)
                .background(Color(0xFFF2F3F5)),
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
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = profile.name,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Gray900
            )

            if (profile.age > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${profile.age}세",
                    fontSize = 14.sp,
                    color = Gray500
                )
            }
        }

        if (profile.bio.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = profile.bio,
                fontSize = 14.sp,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun FriendProfileTabRow(
    selectedTab: FriendProfileTab,
    onTabSelected: (FriendProfileTab) -> Unit
) {
    TabRow(
        selectedTabIndex = if (selectedTab == FriendProfileTab.PHOTOS) 0 else 1,
        containerColor = Color.White,
        contentColor = Color(0xFF111111)
    ) {
        Tab(
            selected = selectedTab == FriendProfileTab.PHOTOS,
            onClick = { onTabSelected(FriendProfileTab.PHOTOS) },
            text = { Text("사진", fontWeight = if (selectedTab == FriendProfileTab.PHOTOS) FontWeight.Bold else FontWeight.Medium) }
        )
        Tab(
            selected = selectedTab == FriendProfileTab.INFO,
            onClick = { onTabSelected(FriendProfileTab.INFO) },
            text = { Text("정보", fontWeight = if (selectedTab == FriendProfileTab.INFO) FontWeight.Bold else FontWeight.Medium) }
        )
    }
}

@Composable
private fun FriendPhotoGrid(
    imageUrls: List<String>,
    onPhotoClick: (String) -> Unit
) {
    if (imageUrls.isEmpty()) {
        EmptyFriendCard(text = "등록된 사진이 없습니다.")
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        imageUrls.chunked(3).forEach { rowImages ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowImages.forEach { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "친구 사진",
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onPhotoClick(imageUrl) },
                        contentScale = ContentScale.Crop
                    )
                }

                repeat(3 - rowImages.size) {
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
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    onDismiss()
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "친구 사진 크게 보기",
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp)),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun FriendInfoSection(profile: ProfilePreviewUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (profile.department.isNotBlank()) MemberInfoRow("학과", profile.department)
        if (profile.height > 0) MemberInfoRow("키", "${profile.height}cm")
        if (profile.location.isNotBlank()) MemberInfoRow("거주지", profile.location)
        if (profile.mbti.isNotBlank()) MemberInfoRow("MBTI", profile.mbti)
        if (profile.interests.isNotEmpty()) {
            HorizontalDivider(color = Gray200)
            Text("관심사", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Gray500)
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
                        Text(text = interest, fontSize = 12.sp, color = Purple)
                    }
                }
            }
        }

        if (profile.foodLikes.isNotEmpty()) {
            HorizontalDivider(color = Gray200)
            Text("좋아하는 음식", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Gray500)
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
                        Text(text = food, fontSize = 12.sp, color = Color(0xFF16A34A))
                    }
                }
            }
        }
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

@Composable
fun FriendProfileScreen(
    profile: ProfilePreviewUiState,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 상단 닫기 버튼
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "뒤로가기"
                    )
                }
            }

            // 프로필 헤더
            FriendProfileHeader(profile = profile)

            Spacer(modifier = Modifier.height(16.dp))

            // 프로필 상세
            FriendProfileDetailPage(
                profile = profile,
                onVoiceCallClick = {},
                onVideoCallClick = {}
            )
        }
    }
}
