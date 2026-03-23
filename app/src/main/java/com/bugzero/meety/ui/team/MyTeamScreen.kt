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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private enum class MyTeamTab {
    RECEIVED, SENT, MY_TEAM
}

data class InterestTeamUiState(
    val id: String,
    val teamName: String,
    val teamSize: String,
    val tags: List<String>
)

data class MyTeamUiState(
    val teamName: String,
    val teamSize: String,
    val tags: List<String>,
    val members: List<String>,
    val bio: String
)

@Composable
fun MyTeamScreen(
    onHomeClick: () -> Unit = {},
    onMatchingClick: () -> Unit = {},
    onCreateTeamClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onAcceptClick: (String) -> Unit = {},
    onRejectClick: (String) -> Unit = {},
    onCancelSentClick: (String) -> Unit = {},
    onEditTeamClick: () -> Unit = {},
    onCreateNewTeamClick: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(MyTeamTab.RECEIVED) }

    val receivedList = remember {
        listOf(
            InterestTeamUiState(
                id = "received_1",
                teamName = "컴공 개발자들",
                teamSize = "3:3",
                tags = listOf("#개발좋아", "#게임러버", "#운동좋아")
            ),
            InterestTeamUiState(
                id = "received_2",
                teamName = "디자인과 크루",
                teamSize = "2:2",
                tags = listOf("#예술좋아", "#전시회", "#감성적")
            )
        )
    }

    val sentList = remember {
        listOf(
            InterestTeamUiState(
                id = "sent_1",
                teamName = "한강 러너스",
                teamSize = "4:4",
                tags = listOf("#운동좋아", "#활발한", "#맛집탐방")
            ),
            InterestTeamUiState(
                id = "sent_2",
                teamName = "영화 보는 사람들",
                teamSize = "2:2",
                tags = listOf("#영화매니아", "#조용한", "#카페좋아")
            )
        )
    }

    val myTeam = remember {
        MyTeamUiState(
            teamName = "버그제로",
            teamSize = "3:3",
            tags = listOf("#개발좋아", "#카페좋아", "#게임러버"),
            members = listOf("이상혁", "김민수", "박지은"),
            bio = "같이 편하게 대화하고, 밥이나 카페 가면서 친해질 팀을 찾고 있어요."
        )
    }

    Scaffold(
        topBar = {
            TeamCommonTopBar(
                onSearchClick = onSearchClick,
                onNotificationClick = onNotificationClick
            )
        },
        bottomBar = {
            TeamCommonBottomBar(
                selectedTab = TeamBottomTab.MATCHING,
                onHomeClick = onHomeClick,
                onMatchingClick = onMatchingClick,
                onCreateTeamClick = onCreateTeamClick,
                onChatClick = onChatClick,
                onProfileClick = onProfileClick
            )
        },
        containerColor = Color(0xFFF8F1F8)
    ) { innerPadding ->

        when (selectedTab) {
            MyTeamTab.RECEIVED -> {
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
                        MyTeamTitleSection()
                    }

                    item {
                        MyTeamTabRow(
                            selectedTab = selectedTab,
                            receivedCount = receivedList.size,
                            sentCount = sentList.size,
                            onTabSelected = { selectedTab = it }
                        )
                    }

                    items(receivedList) { team ->
                        InterestTeamCard(
                            team = team,
                            showAcceptButton = true,
                            showRejectButton = true,
                            onAcceptClick = { onAcceptClick(team.id) },
                            onRejectClick = { onRejectClick(team.id) }
                        )
                    }
                }
            }

            MyTeamTab.SENT -> {
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
                        MyTeamTitleSection()
                    }

                    item {
                        MyTeamTabRow(
                            selectedTab = selectedTab,
                            receivedCount = receivedList.size,
                            sentCount = sentList.size,
                            onTabSelected = { selectedTab = it }
                        )
                    }

                    items(sentList) { team ->
                        SentInterestTeamCard(
                            team = team,
                            onCancelClick = { onCancelSentClick(team.id) }
                        )
                    }
                }
            }

            MyTeamTab.MY_TEAM -> {
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
                        MyTeamTitleSection()
                    }

                    item {
                        MyTeamTabRow(
                            selectedTab = selectedTab,
                            receivedCount = receivedList.size,
                            sentCount = sentList.size,
                            onTabSelected = { selectedTab = it }
                        )
                    }

                    item {
                        MyTeamInfoCard(
                            team = myTeam,
                            onEditTeamClick = onEditTeamClick
                        )
                    }

                    item {
                        CreateNewTeamButton(
                            onClick = onCreateNewTeamClick
                        )
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
            imageVector = Icons.Default.Favorite,
            contentDescription = "매칭",
            tint = Color(0xFFA020F0),
            modifier = Modifier.size(30.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = "매칭",
            color = Color(0xFFA020F0),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MyTeamTabRow(
    selectedTab: MyTeamTab,
    receivedCount: Int,
    sentCount: Int,
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
            text = "받은 관심",
            count = receivedCount,
            selected = selectedTab == MyTeamTab.RECEIVED,
            modifier = Modifier.weight(1f),
            onClick = { onTabSelected(MyTeamTab.RECEIVED) }
        )

        TeamTabButton(
            text = "보낸 관심",
            count = sentCount,
            selected = selectedTab == MyTeamTab.SENT,
            modifier = Modifier.weight(1f),
            onClick = { onTabSelected(MyTeamTab.SENT) }
        )

        TeamTabButton(
            text = "내 팀",
            count = null,
            selected = selectedTab == MyTeamTab.MY_TEAM,
            modifier = Modifier.weight(1f),
            onClick = { onTabSelected(MyTeamTab.MY_TEAM) }
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
    val backgroundBrush = if (selected) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFA020F0),
                Color(0xFFFF1493)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(Color.Transparent, Color.Transparent)
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundBrush)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color.Black,
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
                        if (selected) Color.White.copy(alpha = 0.25f)
                        else Color(0xFFF0E6FA)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    color = if (selected) Color.White else Color(0xFFA020F0),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun InterestTeamCard(
    team: InterestTeamUiState,
    showAcceptButton: Boolean,
    showRejectButton: Boolean,
    onAcceptClick: () -> Unit,
    onRejectClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(94.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFE8D6F7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = "팀 이미지",
                        tint = Color(0xFFA020F0),
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = team.teamName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111111)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = team.teamSize,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TagRow(tags = team.tags)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (showRejectButton) {
                    Button(
                        onClick = onRejectClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        border = BorderStroke(
                            1.dp,
                            Color(0xFFD8D8E0)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "거절"
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "거절",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (showAcceptButton) {
                    Button(
                        onClick = onAcceptClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFFA020F0),
                                            Color(0xFFFF1493)
                                        )
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = "수락",
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "수락 ✨",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SentInterestTeamCard(
    team: InterestTeamUiState,
    onCancelClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(94.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFFFE5F1)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = "팀 이미지",
                        tint = Color(0xFFFF1493),
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = team.teamName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111111)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = team.teamSize,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TagRow(tags = team.tags)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onCancelClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF4F4F8),
                    contentColor = Color(0xFF444444)
                )
            ) {
                Text(
                    text = "보낸 관심 취소",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun MyTeamInfoCard(
    team: MyTeamUiState,
    onEditTeamClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "내 팀 정보",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFA020F0)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = team.teamName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111111)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "팀 구성 ${team.teamSize}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666)
            )

            Spacer(modifier = Modifier.height(12.dp))

            TagRow(tags = team.tags)

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "팀원",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222)
            )

            Spacer(modifier = Modifier.height(10.dp))

            team.members.forEach { member ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1E2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "팀원",
                            tint = Color(0xFFA020F0),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = member,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF333333)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "팀 소개",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = team.bio,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF555555)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onEditTeamClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFA020F0),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "팀 정보 수정",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CreateNewTeamButton(
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color(0xFFA020F0)
        ),
        border = BorderStroke(
            1.dp,
            Color(0xFFD9C5EE)
        )
    ) {
        Text(
            text = "새 팀 만들기",
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TagRow(
    tags: List<String>
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.take(3).forEach { tag ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF3E7FF)
            ) {
                Text(
                    text = tag,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = Color(0xFFA020F0),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}