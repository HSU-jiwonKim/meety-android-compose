package com.bugzero.meety.ui.team

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.HorizontalDivider
import com.bugzero.meety.ui.theme.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.style.TextAlign
import com.bugzero.meety.ui.feed.FeedConstants

private enum class MyTeamTab {
    FRIENDS, ADD_FRIEND
}

data class FriendUiState(
    val id: String,
    val name: String,
    val email: String = "",
    val profileImageUrl: String = "",
    val department: String = "",
    val age: Int = 0,
    val mbti: String = "",
    val bio: String = "",
    val location: String = "",
    val height: Int = 0,
    val interests: List<String> = emptyList(),
    val foodLikes: List<String> = emptyList()
)


data class ProfilePreviewUiState(
    val id: String = "",
    val name: String = "",
    val profileImageUrl: String = "",
    val department: String = "",
    val age: Int = 0,
    val mbti: String = "",
    val bio: String = "",
    val location: String = "",
    val height: Int = 0,
    val interests: List<String> = emptyList(),
    val foodLikes: List<String> = emptyList()
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
    onCreateNewTeamClick: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(MyTeamTab.FRIENDS) }
    var friendEmail by remember { mutableStateOf("") }
    var selectedProfile by remember { mutableStateOf<ProfilePreviewUiState?>(null) }
    var friendSearchQuery by remember { mutableStateOf("") }

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
            department = it.department,
            age = it.age,
            mbti = it.mbti,
            bio = it.bio,
            location = it.location,
            height = it.height,
            interests = it.interests,
            foodLikes = it.foodLikes
        )
    }

    val filteredFriendList = friendList.filter { friend ->
        val query = friendSearchQuery.trim()
        query.isBlank() ||
                friend.name.contains(query, ignoreCase = true) ||
                friend.email.contains(query, ignoreCase = true)
    }

    selectedProfile?.let { profile ->
        ProfilePreviewDialog(
            profile = profile,
            onDismiss = { selectedProfile = null }
        )
    }

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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 18.dp,
                bottom = 120.dp
            )
        ) {
            item {
                MyTeamTabRow(
                    selectedTab = selectedTab,
                    friendCount = friendList.size,
                    onTabSelected = {
                        selectedTab = it
                        if (it != MyTeamTab.FRIENDS) {
                            friendSearchQuery = ""
                        }
                    }
                )
            }

            if (selectedTab == MyTeamTab.FRIENDS) {
                item {
                    FriendSearchSection(
                        query = friendSearchQuery,
                        onQueryChange = { friendSearchQuery = it }
                    )
                }
            }

            if (isLoading) {
                item {
                    LoadingCard()
                }
            }

            when (selectedTab) {
                MyTeamTab.FRIENDS -> {
                    if (friendList.isEmpty() && !isLoading) {
                        item {
                            EmptyFriendCard(text = "아직 친구가 없습니다.")
                        }
                    } else if (filteredFriendList.isEmpty() && !isLoading) {
                        item {
                            EmptyFriendCard(text = "검색 결과가 없습니다.")
                        }
                    } else {
                        items(filteredFriendList) { friend ->
                            FriendListItem(
                                friend = friend,
                                onProfileClick = {
                                    selectedProfile = ProfilePreviewUiState(
                                        id = friend.id,
                                        name = friend.name,
                                        profileImageUrl = friend.profileImageUrl,
                                        department = friend.department,
                                        age = friend.age,
                                        mbti = friend.mbti,
                                        bio = friend.bio,
                                        location = friend.location,
                                        height = friend.height,
                                        interests = friend.interests,
                                        foodLikes = friend.foodLikes
                                    )
                                },
                                onRemoveClick = { viewModel.removeFriend(friend.id) }
                            )
                        }
                    }
                }

                MyTeamTab.ADD_FRIEND -> {
                    item {
                        AddFriendGuideCard()
                    }

                    item {
                        SectionTitle(text = "한성대 이메일로 친구 추가")
                    }

                    item {
                        AddFriendInputCard(
                            email = friendEmail,
                            message = friendAddMessage,
                            onEmailChange = {
                                friendEmail = it
                                viewModel.clearFriendAddMessage()
                            },
                            onAddClick = {
                                viewModel.addFriendByEmail(friendEmail)
                            }
                        )
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
        shape = RoundedCornerShape(18.dp),
        colors = androidx.compose.material3.TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        ),
        placeholder = {
            Text(
                text = "친구 이름 검색",
                color = Color(0xFF9CA3AF)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "검색",
                tint = Color(0xFFA020F0)
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
    text: String
) {
    Text(
        text = text,
        color = Color(0xFF666666),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 6.dp)
    )
}

@Composable
private fun FriendListItem(
    friend: FriendUiState,
    onProfileClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF2F3F5))
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                if (friend.profileImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = friend.profileImageUrl,
                        contentDescription = "프로필 이미지",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "기본 프로필",
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = friend.name,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF111111)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = onRemoveClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEDE9FE),
                    contentColor = Color(0xFFA020F0)
                )
            ) {
                Text(
                    text = "삭제",
                    fontWeight = FontWeight.Bold
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
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
private fun ReceivedFriendRequestCard(
    request: FriendRequestItem,
    onProfileClick: () -> Unit,
    onAcceptClick: () -> Unit,
    onRejectClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF2F3F5))
                        .clickable { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (request.profileImageUrl.isNotBlank()) {
                        AsyncImage(
                            model = request.profileImageUrl,
                            contentDescription = "프로필 이미지",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "기본 프로필",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (request.name.isBlank()) "이름 없음" else request.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF111111)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = request.email,
                        color = Color(0xFF9CA3AF),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onAcceptClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFA020F0),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "수락",
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onRejectClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE5E7EB),
                        contentColor = Color(0xFF4B5563)
                    )
                ) {
                    Text(
                        text = "거절",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfilePreviewDialog(
    profile: ProfilePreviewUiState,
    onDismiss: () -> Unit
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