package com.bugzero.meety.ui.feed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugzero.meety.ui.feed.FeedConstants
import com.bugzero.meety.ui.team.Team
import com.bugzero.meety.ui.theme.*

/**
 * 전체 목록 모드에서 사용하는 팀 리스트 아이템
 */
@Composable
fun TeamListItem(
    team: Team,
    onTeamClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onTeamClick(team.teamId) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(FeedConstants.LightPurpleBg),
                contentAlignment = Alignment.Center
            ) {
                Text("👥", fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(team.teamName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Gray900)
                Text(
                    "${team.memberIds.size}명 · ${team.tags.take(2).joinToString(", ")}",
                    fontSize = 13.sp,
                    color = Gray500
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Gray400)
        }
    }
}
