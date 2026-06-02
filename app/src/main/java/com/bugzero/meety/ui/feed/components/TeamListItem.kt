package com.bugzero.meety.ui.feed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugzero.meety.ui.feed.FeedConstants
import com.bugzero.meety.ui.feed.TeamActionStatus
import com.bugzero.meety.ui.team.Team
import com.bugzero.meety.ui.theme.*

private val SkyBlue = Color(0xFF0EA5E9)

/**
 * 목록 모드 팀 카드 (목업 .team-card)
 * 썸네일(멤버수 배지) + 이름/소개/태그 + 상태/좋아요 버튼
 */
@Composable
fun TeamListItem(
    team: Team,
    onTeamClick: (String) -> Unit,
    status: TeamActionStatus = TeamActionStatus.NONE,
    fitScore: Int? = null
) {
    val colorIndex = (team.teamId.hashCode() and Int.MAX_VALUE) % FeedConstants.CardColorPalette.size
    val thumbColors = FeedConstants.CardColorPalette[colorIndex]
    val initial = team.teamName.firstOrNull()?.toString() ?: "T"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp))
            .background(MeetySurface, RoundedCornerShape(20.dp))
            .border(1.dp, Line2, RoundedCornerShape(20.dp))
            .clickable { onTeamClick(team.teamId) }
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── 썸네일 + 멤버수 배지 ──
        Box(
            modifier = Modifier
                .size(74.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(Brush.verticalGradient(thumbColors)),
            contentAlignment = Alignment.Center
        ) {
            Text(initial, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White.copy(alpha = 0.9f))
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(7.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("${team.memberIds.size}명", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(Modifier.width(13.dp))

        // ── 내용 ──
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    team.teamName,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                fitScore?.let { score ->
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(VioletSoft, RoundedCornerShape(999.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "${score}점",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Brand1
                        )
                    }
                }
            }
            if (team.description.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    team.description,
                    fontSize = 12.5.sp,
                    color = Ink2,
                    lineHeight = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (team.tags.isNotEmpty()) {
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    team.tags.take(2).forEach { Chip(text = it) }
                }
            }
        }

        Spacer(Modifier.width(10.dp))

        // ── 오른쪽 상태 / 좋아요 버튼 ──
        if (status == TeamActionStatus.NONE) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(MeetySurface2, RoundedCornerShape(12.dp))
                    .border(1.dp, Line, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.FavoriteBorder, contentDescription = "좋아요", tint = Brand2, modifier = Modifier.size(18.dp))
            }
        } else {
            TeamStatusBadge(status)
        }
    }
}

/** 보라 소프트 칩 (목업 .chip) */
@Composable
private fun Chip(text: String) {
    Box(
        modifier = Modifier
            .background(VioletSoft, RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VioletText)
    }
}

@Composable
private fun TeamStatusBadge(status: TeamActionStatus) {
    when (status) {
        TeamActionStatus.NONE -> {}
        TeamActionStatus.LIKED -> StatusPill("좋아요 보냄", Icons.Default.Favorite, VioletText, VioletSoft)
        TeamActionStatus.PASSED -> StatusPill("패스함", Icons.Default.Close, Ink3, Gray200)
        TeamActionStatus.MY_TEAM -> StatusPill("내 팀", Icons.Default.Groups, SkyBlue, Color(0x1F0EA5E9))
        TeamActionStatus.INVITED -> StatusPill("초대 받음", Icons.Default.Mail, Color(0xFFD97706), Color(0xFFFEF3C7))
    }
}

@Composable
private fun StatusPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    fg: Color,
    bg: Color
) {
    Row(
        modifier = Modifier
            .background(bg, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(13.dp))
        Text(label, fontSize = 11.sp, color = fg, fontWeight = FontWeight.Bold)
    }
}
