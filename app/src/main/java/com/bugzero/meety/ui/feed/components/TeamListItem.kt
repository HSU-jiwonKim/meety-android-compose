package com.bugzero.meety.ui.feed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugzero.meety.ui.feed.FeedConstants
import com.bugzero.meety.ui.feed.TeamActionStatus
import com.bugzero.meety.ui.team.Team
import com.bugzero.meety.ui.theme.*

/** 내 팀 배지에 사용하는 스카이블루 색상 */
private val SkyBlue = Color(0xFF0EA5E9)

/**
 * 전체 목록 모드에서 사용하는 팀 리스트 아이템
 *
 * [status]에 따라 카드 배경 틴트와 상태 배지가 달라진다:
 *  - LIKED   : 연보라 배경 + 💜 "좋아요 보냄" 배지
 *  - PASSED  : 연회색 배경 + ✕ "패스함" 배지
 *  - MY_TEAM : 연파랑 배경 + 👥 "내 팀" 배지
 *  - NONE    : 흰 배경 + 오른쪽 화살표 (기존 스타일)
 */
@Composable
fun TeamListItem(
    team: Team,
    onTeamClick: (String) -> Unit,
    status: TeamActionStatus = TeamActionStatus.NONE
) {
    // teamId 해시값으로 아바타 색상 결정 (SwipeCard와 동일한 로직)
    val colorIndex = (team.teamId.hashCode() and Int.MAX_VALUE) % FeedConstants.CardColorPalette.size
    val avatarColors = FeedConstants.CardColorPalette[colorIndex]
    val initial = team.teamName.firstOrNull()?.toString() ?: "T"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTeamClick(team.teamId) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 아바타: 실제 프로필 이미지 우선, 없으면 이니셜 그라데이션
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (team.teamProfileImage.isNotBlank()) {
                    AsyncImage(
                        model              = team.teamProfileImage,
                        contentDescription = "${team.teamName} 프로필 사진",
                        modifier           = Modifier.fillMaxSize(),
                        contentScale       = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(avatarColors)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = initial,
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )
                    }
                }
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

            Spacer(modifier = Modifier.width(8.dp))

            // 상태 배지
            TeamStatusBadge(status)
        }
    }
}

/**
 * 상태에 따라 아이콘 + 텍스트 배지 또는 화살표를 표시한다.
 */
@Composable
private fun TeamStatusBadge(status: TeamActionStatus) {
    when (status) {
        TeamActionStatus.NONE -> {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Gray400
            )
        }

        TeamActionStatus.LIKED -> {
            Row(
                modifier = Modifier
                    .background(Purple.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "좋아요 보냄",
                    tint = Purple,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "좋아요 보냄",
                    fontSize = 11.sp,
                    color = Purple,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        TeamActionStatus.PASSED -> {
            Row(
                modifier = Modifier
                    .background(Gray200, RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "패스함",
                    tint = Gray500,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "패스함",
                    fontSize = 11.sp,
                    color = Gray500,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        TeamActionStatus.MY_TEAM -> {
            Row(
                modifier = Modifier
                    .background(SkyBlue.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = "내 팀",
                    tint = SkyBlue,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "내 팀",
                    fontSize = 11.sp,
                    color = SkyBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        TeamActionStatus.INVITED -> {
            Row(
                modifier = Modifier
                    .background(Color(0xFFFEF3C7), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mail,
                    contentDescription = "초대 받음",
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "초대 받음",
                    fontSize = 11.sp,
                    color = Color(0xFFD97706),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
