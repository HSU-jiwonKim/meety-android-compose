package com.bugzero.meety.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bugzero.meety.ui.team.Team
import com.bugzero.meety.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingDetailScreen(
    onBackClick: () -> Unit = {},
    team: Team? = null,
    userPreferences: Map<String, Int> = emptyMap(),
    onLikeClick: () -> Unit = {},
    onPassClick: () -> Unit = {}
) {
    if (team == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("데이터를 불러올 수 없습니다.")
        }
        return
    }

    // userPreferences 기반으로 이 팀과 매칭되는 상위 태그를 찾아 AI 추천 이유 생성
    val aiReasonText = remember(team.teamId, userPreferences) {
        val matchingTags = (team.tags + team.mbtiTags)
            .filter { (userPreferences[it] ?: 0) > 0 }
            .sortedByDescending { userPreferences[it] ?: 0 }
            .take(2)

        when {
            matchingTags.isNotEmpty() -> {
                val tagStr = matchingTags.joinToString(", ") { "#$it" }
                "$tagStr 취향 점수가 높아 추천된 팀이에요!"
            }
            else -> "좋아요를 누를수록 당신에게 딱 맞는 팀을 추천해드려요!"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(team.teamName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로", tint = Purple)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onPassClick() },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("패스", color = Gray500, fontWeight = FontWeight.Bold) }

                Button(
                    onClick = { onLikeClick() },
                    modifier = Modifier.weight(2f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Purple)
                ) { Text("💜 좋아요!", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(FeedConstants.BackgroundGray)
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 팀 기본 정보 카드
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 팀 프로필 아바타 — 사진이 있으면 사진, 없으면 이니셜 그라데이션
                        val colorIndex = (team.teamId.hashCode() and Int.MAX_VALUE) % FeedConstants.CardColorPalette.size
                        val avatarColors = FeedConstants.CardColorPalette[colorIndex]
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (team.teamProfileImage.isNotBlank()) {
                                AsyncImage(
                                    model              = team.teamProfileImage,
                                    contentDescription = "팀 대표 사진",
                                    modifier           = Modifier.fillMaxSize(),
                                    contentScale       = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            androidx.compose.ui.graphics.Brush.verticalGradient(avatarColors)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text       = team.teamName.firstOrNull()?.toString() ?: "T",
                                        fontSize   = 40.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = Color.White
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(team.teamName, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Gray900)
                        Text("${team.memberIds.size}명", fontSize = 14.sp, color = Gray500)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (team.description.isNotEmpty()) {
                            Text(team.description, fontSize = 14.sp, color = Gray700)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement   = Arrangement.spacedBy(6.dp)
                        ) {
                            team.tags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .background(FeedConstants.LightPurpleBg, RoundedCornerShape(20.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) { Text(tag, fontSize = 12.sp, color = Purple) }
                            }
                        }
                    }
                }
            }

            // AI 추천 이유 카드 (userPreferences 기반 동적 메시지)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = FeedConstants.AiCardBg)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Purple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "AI 추천 이유",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Purple
                            )
                            Text(aiReasonText, fontSize = 13.sp, color = Gray700)
                        }
                    }
                }
            }

            // MBTI 태그 카드
            if (team.mbtiTags.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "팀원 MBTI",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Gray900
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement   = Arrangement.spacedBy(6.dp)
                            ) {
                                team.mbtiTags.forEach { mbti ->
                                    Box(
                                        modifier = Modifier
                                            .background(FeedConstants.LightPurpleBg, RoundedCornerShape(12.dp))
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            mbti,
                                            fontSize   = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color      = Purple
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
