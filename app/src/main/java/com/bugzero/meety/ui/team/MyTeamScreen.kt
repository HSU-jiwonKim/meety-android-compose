package com.bugzero.meety.ui.team

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private enum class MyTeamTab {
    FRIENDS, ADD_FRIEND
}

data class FriendUiState(
    val id: String,
    val name: String,
    val statusMessage: String,
    val department: String,
    val email: String,
    val profileEmoji: String = "🙂",
    val isFavorite: Boolean = false
)

data class RecommendedFriendUiState(
    val id: String,
    val name: String,
    val department: String,
    val email: String,
    val profileEmoji: String = "👤",
    val isAdded: Boolean = false
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

    val friendList = remember {
        mutableStateListOf(
            FriendUiState(
                id = "friend_1",
                name = "김민수",
                statusMessage = "오늘도 과제 화이팅",
                department = "컴퓨터공학부",
                email = "kms123@hansung.ac.kr",
                profileEmoji = "😄",
                isFavorite = true
            ),
            FriendUiState(
                id = "friend_2",
                name = "박지연",
                statusMessage = "회의 중입니다",
                department = "AI응용학과",
                email = "pjy22@hansung.ac.kr",
                profileEmoji = "🫶",
                isFavorite = false
            ),
            FriendUiState(
                id = "friend_3",
                name = "이서준",
                statusMessage = "점심 먹는 중",
                department = "경영학부",
                email = "sjlee@hansung.ac.kr",
                profileEmoji = "😎",
                isFavorite = false
            ),
            FriendUiState(
                id = "friend_4",
                name = "최은서",
                statusMessage = "카톡 확인 늦을 수 있어요",
                department = "디자인학부",
                email = "eschoi@hansung.ac.kr",
                profileEmoji = "🌸",
                isFavorite = true
            )
        )
    }

    val recommendedList = remember {
        mutableStateListOf(
            RecommendedFriendUiState(
                id = "recommend_1",
                name = "정하늘",
                department = "컴퓨터공학부",
                email = "skyjeong@hansung.ac.kr",
                profileEmoji = "☁️"
            ),
            RecommendedFriendUiState(
                id = "recommend_2",
                name = "윤도현",
                department = "기계전자공학부",
                email = "dhyoon@hansung.ac.kr",
                profileEmoji = "🚀"
            ),
            RecommendedFriendUiState(
                id = "recommend_3",
                name = "한유진",
                department = "AI응용학과",
                email = "yjhan@hansung.ac.kr",
                profileEmoji = "💡"
            )
        )
    }

    val favoriteFriends = friendList.filter { it.isFavorite }
    val normalFriends = friendList.filterNot { it.isFavorite }

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
            item { MyTeamTitleSection() }

            item {
                MyTeamTabRow(
                    selectedTab = selectedTab,
                    friendCount = friendList.size,
                    recommendCount = recommendedList.count { !it.isAdded },
                    onTabSelected = { selectedTab = it }
                )
            }

            when (selectedTab) {
                MyTeamTab.FRIENDS -> {
                    if (favoriteFriends.isNotEmpty()) {
                        item {
                            SectionTitle(text = "즐겨찾는 친구")
                        }

                        items(favoriteFriends) { friend ->
                            FriendListItem(friend = friend)
                        }
                    }

                    item {
                        SectionTitle(text = "친구")
                    }

                    if (normalFriends.isEmpty() && favoriteFriends.isEmpty()) {
                        item {
                            EmptyFriendCard(text = "아직 친구가 없습니다.")
                        }
                    } else {
                        items(normalFriends) { friend ->
                            FriendListItem(friend = friend)
                        }
                    }
                }

                MyTeamTab.ADD_FRIEND -> {
                    item {
                        AddFriendGuideCard()
                    }

                    item {
                        SectionTitle(text = "한성대 이메일 친구 추천")
                    }

                    if (recommendedList.isEmpty()) {
                        item {
                            EmptyFriendCard(text = "추천할 친구가 없습니다.")
                        }
                    } else {
                        items(recommendedList) { friend ->
                            RecommendedFriendCard(
                                friend = friend,
                                onAddClick = {
                                    val index = recommendedList.indexOfFirst { it.id == friend.id }
                                    if (index != -1) {
                                        recommendedList[index] = recommendedList[index].copy(isAdded = true)
                                    }
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
private fun MyTeamTitleSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "친구",
            tint = Color(0xFFA020F0),
            modifier = Modifier.size(30.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = "친구",
            color = Color(0xFFA020F0),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MyTeamTabRow(
    selectedTab: MyTeamTab,
    friendCount: Int,
    recommendCount: Int,
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
            count = recommendCount,
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
            .background(
                if (selected) Color(0xFFF3E7FF) else Color.Transparent
            )
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
                        if (selected) Color(0xFFE2C9FA)
                        else Color(0xFFF7EDFF)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    color = if (selected) Color(0xFFA020F0) else Color(0xFFA020F0),
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
    friend: FriendUiState
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
                    .background(
                        if (friend.isFavorite) Color(0xFFF3E7FF) else Color(0xFFF6F2FA)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = friend.profileEmoji,
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = friend.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF111111)
                    )

                    if (friend.isFavorite) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "즐겨찾기",
                            tint = Color(0xFFA020F0),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = friend.statusMessage,
                    color = Color(0xFF666666),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = friend.department,
                    color = Color(0xFF9CA3AF),
                    style = MaterialTheme.typography.bodySmall
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
private fun RecommendedFriendCard(
    friend: RecommendedFriendUiState,
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF6F2FA)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = friend.profileEmoji,
                        fontSize = 24.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = friend.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF111111)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = friend.department,
                        color = Color(0xFF666666),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = friend.email,
                        color = Color(0xFF9CA3AF),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onAddClick,
                enabled = !friend.isAdded,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (friend.isAdded) Color(0xFFE5E7EB) else Color(0xFFA020F0),
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
                    text = if (friend.isAdded) "추가 완료" else "친구 추가",
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