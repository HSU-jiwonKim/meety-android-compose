package com.bugzero.meety.ui.team

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class TeamBottomTab {
    HOME, MATCHING, CREATE_TEAM, CHAT, PROFILE
}

@Composable
fun TeamCommonTopBar(
    onSearchClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFEEDBFF)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "M",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFA020F0)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = "Meety",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.weight(1f))

        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "검색",
            modifier = Modifier
                .size(24.dp)
                .clickable { onSearchClick() },
            tint = Color.Black
        )

        Spacer(modifier = Modifier.width(16.dp))

        Box(
            modifier = Modifier.clickable { onNotificationClick() }
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsNone,
                contentDescription = "알림",
                modifier = Modifier.size(24.dp),
                tint = Color.Black
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
            )
        }
    }
}

@Composable
fun TeamCommonBottomBar(
    selectedTab: TeamBottomTab,
    onHomeClick: () -> Unit = {},
    onMatchingClick: () -> Unit = {},
    onCreateTeamClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(Color.White)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomBarItem(
            icon = Icons.Default.Home,
            label = "홈",
            selected = selectedTab == TeamBottomTab.HOME,
            onClick = onHomeClick
        )

        BottomBarItem(
            icon = Icons.Default.VolunteerActivism,
            label = "매칭",
            selected = selectedTab == TeamBottomTab.MATCHING,
            onClick = onMatchingClick
        )

        BottomBarItem(
            icon = Icons.Default.Add,
            label = "팀 만들기",
            selected = selectedTab == TeamBottomTab.CREATE_TEAM,
            onClick = onCreateTeamClick
        )

        BottomBarItem(
            icon = Icons.Default.ChatBubbleOutline,
            label = "채팅",
            selected = selectedTab == TeamBottomTab.CHAT,
            onClick = onChatClick
        )

        BottomBarItem(
            icon = Icons.Default.PersonOutline,
            label = "프로필",
            selected = selectedTab == TeamBottomTab.PROFILE,
            onClick = onProfileClick
        )
    }
}

@Composable
private fun BottomBarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val itemColor = if (selected) Color(0xFFA020F0) else Color(0xFF8A8A8A)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = itemColor,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            color = itemColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}