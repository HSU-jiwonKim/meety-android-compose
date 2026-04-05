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

private enum class MyTeamTab {
    FRIENDS, ADD_FRIEND
}

data class FriendUiState(
    val id: String,
    val name: String,
    val profileImageUrl: String = "",
    val department: String = "",
    val age: Int = 0,
    val mbti: String = "",
    val bio: String = "",
    val location: String = ""
)

data class ProfilePreviewUiState(
    val name: String = "",
    val profileImageUrl: String = "",
    val department: String = "",
    val age: Int = 0,
    val mbti: String = "",
    val bio: String = "",
    val location: String = ""
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
    val receivedFriendRequests by viewModel.receivedFriendRequests.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadFriends()
        viewModel.loadReceivedFriendRequests()
    }

    val friendList = friends.map {
        FriendUiState(
            id = it.userId,
            name = if (it.name.isBlank()) "이름 없음" else it.name,
            profileImageUrl = it.profileImageUrl,
            department = it.department,
            age = it.age,
            mbti = it.mbti,
            bio = it.bio,
            location = it.location
        )
    }

    val filteredFriendList = friendList.filter { friend ->
        friend.name.contains(friendSearchQuery.trim(), ignoreCase = true)
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
                    addCount = receivedFriendRequests.size,
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
                                        name = friend.name,
                                        profileImageUrl = friend.profileImageUrl,
                                        department = friend.department,
                                        age = friend.age,
                                        mbti = friend.mbti,
                                        bio = friend.bio,
                                        location = friend.location
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

                    item {
                        SectionTitle(text = "받은 친구 요청")
                    }

                    if (receivedFriendRequests.isEmpty() && !isLoading) {
                        item {
                            EmptyFriendCard(text = "받은 친구 요청이 없습니다.")
                        }
                    } else {
                        items(receivedFriendRequests) { request ->
                            ReceivedFriendRequestCard(
                                request = request,
                                onProfileClick = {
                                    selectedProfile = ProfilePreviewUiState(
                                        name = request.name,
                                        profileImageUrl = request.profileImageUrl,
                                        department = request.department,
                                        age = request.age,
                                        mbti = request.mbti,
                                        bio = request.bio,
                                        location = request.location
                                    )
                                },
                                onAcceptClick = {
                                    viewModel.acceptFriendRequest(
                                        requestId = request.requestId,
                                        fromUserId = request.fromUserId
                                    )
                                },
                                onRejectClick = {
                                    viewModel.rejectFriendRequest(request.requestId)
                                }
                            )
                        }
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
    addCount: Int,
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
            count = addCount,
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
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = Color(0xFF666666),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { onDismiss() }
                    )
                }

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF2F3F5)),
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
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "기본 프로필",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (profile.name.isBlank()) "이름 없음" else profile.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF111111)
                )

                Spacer(modifier = Modifier.height(8.dp))

                val infoLine = buildList {
                    if (profile.department.isNotBlank()) add(profile.department)
                    if (profile.age > 0) add("${profile.age}세")
                    if (profile.mbti.isNotBlank()) add(profile.mbti)
                }.joinToString(" · ")

                if (infoLine.isNotBlank()) {
                    Text(
                        text = infoLine,
                        color = Color(0xFF666666),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (profile.bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = profile.bio,
                        color = Color(0xFF444444),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (profile.location.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = profile.location,
                        color = Color(0xFF888888),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}