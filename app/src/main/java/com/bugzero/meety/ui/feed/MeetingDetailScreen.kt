package com.bugzero.meety.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bugzero.meety.ui.team.Team
import com.bugzero.meety.ui.theme.*

private val SkyBlue = Color(0xFF0EA5E9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingDetailScreen(
    onBackClick: () -> Unit = {},
    team: Team? = null,
    userPreferences: Map<String, Int> = emptyMap(),
    status: TeamActionStatus = TeamActionStatus.NONE,
    memberProfiles: List<MemberProfile> = emptyList(),
    isMembersLoading: Boolean = false,
    onLikeClick: () -> Unit = {},           // NONE: 좋아요
    onPassClick: () -> Unit = {},           // NONE: 패스
    onCancelLike: () -> Unit = {},          // LIKED: 좋아요 취소
    onSendLikeFromPassed: () -> Unit = {}   // PASSED: 좋아요 전환
) {
    if (team == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("데이터를 불러올 수 없습니다.")
        }
        return
    }

    // 팀원 프로필 다이얼로그용 로컬 상태
    var selectedMember by remember { mutableStateOf<MemberProfile?>(null) }
    selectedMember?.let { member ->
        MemberProfileDialog(member = member, onDismiss = { selectedMember = null })
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
            DetailBottomBar(
                status             = status,
                onLikeClick        = onLikeClick,
                onPassClick        = onPassClick,
                onCancelLike       = onCancelLike,
                onSendLikeFromPassed = onSendLikeFromPassed
            )
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
            // ── 상태 배너 (NONE이 아닐 때만) ──
            if (status != TeamActionStatus.NONE) {
                item { StatusBanner(status) }
            }

            // ── 팀 기본 정보 카드 ──
            item { TeamInfoCard(team) }

            // ── AI 추천 이유 카드 ──
            item { AiReasonCard(aiReasonText) }

            // ── MBTI 태그 카드 ──
            if (team.mbtiTags.isNotEmpty()) {
                item { MbtiCard(team.mbtiTags) }
            }

            // ── 팀원 프로필 카드 ──
            item {
                MemberProfilesCard(
                    memberProfiles = memberProfiles,
                    isLoading      = isMembersLoading,
                    onMemberClick  = { selectedMember = it }
                )
            }
        }
    }
}

// ─────────────────────────────────────────
// 하단 바 — 상태별 분기
// ─────────────────────────────────────────

@Composable
private fun DetailBottomBar(
    status: TeamActionStatus,
    onLikeClick: () -> Unit,
    onPassClick: () -> Unit,
    onCancelLike: () -> Unit,
    onSendLikeFromPassed: () -> Unit
) {
    Surface(
        modifier  = Modifier.fillMaxWidth(),
        color     = Color.White,
        shadowElevation = 8.dp
    ) {
        when (status) {
            TeamActionStatus.NONE -> {
                // 기본: 패스 / 좋아요
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick  = onPassClick,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape    = RoundedCornerShape(14.dp)
                    ) { Text("패스", color = Gray500, fontWeight = FontWeight.Bold) }

                    Button(
                        onClick  = onLikeClick,
                        modifier = Modifier.weight(2f).height(52.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = Purple)
                    ) { Text("💜 좋아요!", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                }
            }

            TeamActionStatus.LIKED -> {
                // 이미 좋아요 → 취소 버튼
                Row(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment   = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null,
                            tint = Purple, modifier = Modifier.size(18.dp))
                        Text("좋아요를 보낸 팀이에요", fontSize = 13.sp, color = Gray700,
                            fontWeight = FontWeight.Medium)
                    }
                    OutlinedButton(
                        onClick = onCancelLike,
                        shape   = RoundedCornerShape(12.dp),
                        border  = androidx.compose.foundation.BorderStroke(1.dp, Gray400),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("좋아요 취소", fontSize = 13.sp, color = Gray500,
                            fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            TeamActionStatus.PASSED -> {
                // 패스했던 팀 → 좋아요 전환 버튼
                Row(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment   = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null,
                            tint = Gray400, modifier = Modifier.size(18.dp))
                        Text("패스했던 팀이에요", fontSize = 13.sp, color = Gray700,
                            fontWeight = FontWeight.Medium)
                    }
                    Button(
                        onClick = onSendLikeFromPassed,
                        shape   = RoundedCornerShape(12.dp),
                        colors  = ButtonDefaults.buttonColors(containerColor = Purple),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("💜 좋아요 보내기", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            TeamActionStatus.MY_TEAM -> {
                // 내 팀 → 액션 없음
                Row(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment   = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Groups, contentDescription = null,
                        tint = SkyBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("내가 소속된 팀이에요", fontSize = 14.sp, color = SkyBlue,
                        fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────
// 상태 배너
// ─────────────────────────────────────────

@Composable
private fun StatusBanner(status: TeamActionStatus) {
    val (icon, text, tint, bg) = when (status) {
        TeamActionStatus.LIKED   ->
            BannerStyle(Icons.Default.Favorite,  "이미 좋아요를 보낸 팀이에요",  Purple,  Purple.copy(alpha = 0.08f))
        TeamActionStatus.PASSED  ->
            BannerStyle(Icons.Default.Close,     "패스했던 팀이에요",            Gray500, Gray200)
        TeamActionStatus.MY_TEAM ->
            BannerStyle(Icons.Default.Groups,    "내가 소속된 팀이에요",         SkyBlue, SkyBlue.copy(alpha = 0.08f))
        TeamActionStatus.NONE    -> return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Text(text, fontSize = 13.sp, color = tint, fontWeight = FontWeight.SemiBold)
    }
}

private data class BannerStyle(
    val icon: ImageVector,
    val text: String,
    val tint: Color,
    val bg: Color
)

// ─────────────────────────────────────────
// 팀 기본 정보 카드
// ─────────────────────────────────────────

@Composable
private fun TeamInfoCard(team: Team) {
    val colorIndex   = (team.teamId.hashCode() and Int.MAX_VALUE) % FeedConstants.CardColorPalette.size
    val avatarColors = FeedConstants.CardColorPalette[colorIndex]
    val initial      = team.teamName.firstOrNull()?.toString() ?: "T"

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // ── 배너 + 아바타 오버랩 ──
            // 배너 100dp, 아바타 반지름 48dp → 총 높이 148dp
            Box(
                modifier         = Modifier.fillMaxWidth().height(148.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                // 배너 영역 (상단 100dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .align(Alignment.TopCenter)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                ) {
                    if (team.teamProfileImage.isNotBlank()) {
                        AsyncImage(
                            model              = team.teamProfileImage,
                            contentDescription = "팀 배너 사진",
                            modifier           = Modifier.fillMaxSize(),
                            contentScale       = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(avatarColors))
                        )
                    }
                }

                // 아바타 (96dp 원형, 흰 테두리 3dp)
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .border(3.dp, Color.White, CircleShape)
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(avatarColors)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = initial,
                        fontSize   = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // 팀 이름
            Text(
                text       = team.teamName,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = Gray900
            )

            Spacer(Modifier.height(6.dp))

            // 팀원 수 배지 (보라 pill)
            Box(
                modifier = Modifier
                    .background(FeedConstants.LightPurpleBg, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text       = "${team.memberIds.size}명",
                    fontSize   = 13.sp,
                    color      = Purple,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // 팀 소개 (중앙 정렬)
            if (team.description.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text      = team.description,
                    fontSize  = 14.sp,
                    color     = Gray700,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.padding(horizontal = 20.dp)
                )
            }

            // 태그 (중앙 정렬, # 접두사)
            if (team.tags.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                FlowRow(
                    modifier              = Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                ) {
                    team.tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .background(FeedConstants.LightPurpleBg, RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) { Text("#$tag", fontSize = 12.sp, color = Purple, fontWeight = FontWeight.Medium) }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

// ─────────────────────────────────────────
// AI 추천 이유 카드
// ─────────────────────────────────────────

@Composable
private fun AiReasonCard(aiReasonText: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = FeedConstants.AiCardBg)
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null,
                tint = Purple, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("AI 추천 이유", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Purple)
                Text(aiReasonText, fontSize = 13.sp, color = Gray700)
            }
        }
    }
}

// ─────────────────────────────────────────
// MBTI 태그 카드
// ─────────────────────────────────────────

@Composable
private fun MbtiCard(mbtiTags: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("팀원 MBTI", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Gray900)
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(6.dp)
            ) {
                mbtiTags.forEach { mbti ->
                    Box(
                        modifier = Modifier
                            .background(FeedConstants.LightPurpleBg, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(mbti, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Purple)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────
// 팀원 프로필 카드
// ─────────────────────────────────────────

@Composable
private fun MemberProfilesCard(
    memberProfiles: List<MemberProfile>,
    isLoading: Boolean,
    onMemberClick: (MemberProfile) -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("팀원 소개", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Gray900)
            Spacer(Modifier.height(12.dp))

            when {
                isLoading -> {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Purple, strokeWidth = 2.dp,
                            modifier = Modifier.size(28.dp))
                    }
                }
                memberProfiles.isEmpty() -> {
                    Text("팀원 정보를 불러올 수 없어요", fontSize = 13.sp, color = Gray400,
                        modifier = Modifier.padding(vertical = 8.dp))
                }
                else -> {
                    memberProfiles.forEachIndexed { index, member ->
                        MemberRow(member = member, onClick = { onMemberClick(member) })
                        if (index < memberProfiles.lastIndex) {
                            HorizontalDivider(
                                modifier  = Modifier.padding(vertical = 12.dp),
                                color     = Gray200,
                                thickness = 1.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberRow(member: MemberProfile, onClick: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 아바타
        val colorIndex   = (member.userId.hashCode() and Int.MAX_VALUE) % FeedConstants.CardColorPalette.size
        val avatarColors = FeedConstants.CardColorPalette[colorIndex]
        Box(
            modifier         = Modifier.size(52.dp).clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val firstImage = member.profileImages.firstOrNull()
            if (!firstImage.isNullOrBlank()) {
                AsyncImage(
                    model              = firstImage,
                    contentDescription = "${member.name} 프로필 사진",
                    modifier           = Modifier.fillMaxSize(),
                    contentScale       = ContentScale.Crop
                )
            } else {
                Box(
                    modifier         = Modifier.fillMaxSize()
                        .background(Brush.verticalGradient(avatarColors)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = member.name.firstOrNull()?.toString() ?: "?",
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                }
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            // 이름 + 나이 + MBTI
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(member.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Gray900)
                if (member.age > 0) {
                    Text("${member.age}세", fontSize = 12.sp, color = Gray500)
                }
                if (member.mbti.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .background(FeedConstants.LightPurpleBg, RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(member.mbti, fontSize = 11.sp, color = Purple, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // 학과
            if (member.department.isNotBlank()) {
                Text(member.department, fontSize = 12.sp, color = Gray500)
            }

            // 자기소개
            if (member.bio.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text     = member.bio,
                    fontSize = 13.sp,
                    color    = Gray700,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 관심사 태그 (최대 3개)
            if (member.interests.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    member.interests.take(3).forEach { interest ->
                        Box(
                            modifier = Modifier
                                .background(Gray100, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(interest, fontSize = 11.sp, color = Gray500)
                        }
                    }
                    if (member.interests.size > 3) {
                        Text("+${member.interests.size - 3}", fontSize = 11.sp, color = Gray400,
                            modifier = Modifier.align(Alignment.CenterVertically))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────
// 팀원 상세 프로필 다이얼로그 (채팅방 팝업과 동일 스타일)
// ─────────────────────────────────────────

@Composable
private fun MemberProfileDialog(
    member: MemberProfile,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── 아바타 (실제 프로필 사진 or 이니셜 그라데이션) ──
                val colorIndex   = (member.userId.hashCode() and Int.MAX_VALUE) % FeedConstants.CardColorPalette.size
                val avatarColors = FeedConstants.CardColorPalette[colorIndex]
                Box(
                    modifier         = Modifier.size(80.dp).clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val firstImage = member.profileImages.firstOrNull()
                    if (!firstImage.isNullOrBlank()) {
                        AsyncImage(
                            model              = firstImage,
                            contentDescription = "${member.name} 프로필 사진",
                            modifier           = Modifier.fillMaxSize(),
                            contentScale       = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier         = Modifier.fillMaxSize()
                                .background(Brush.verticalGradient(avatarColors)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = member.name.firstOrNull()?.toString() ?: "?",
                                fontSize   = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Color.White
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── 이름 + 나이 뱃지 ──
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(member.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Gray900)
                    if (member.age > 0) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Gray100, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) { Text("${member.age}세", fontSize = 12.sp, color = Gray500) }
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Gray200)
                Spacer(Modifier.height(16.dp))

                // ── 기본 정보 rows ──
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (member.department.isNotBlank()) MemberInfoRow("학과",  member.department)
                    if (member.height > 0)              MemberInfoRow("키",    "${member.height}cm")
                    if (member.location.isNotBlank())   MemberInfoRow("거주지", member.location)
                    if (member.mbti.isNotBlank())       MemberInfoRow("MBTI",  member.mbti)
                    if (member.bio.isNotBlank())        MemberInfoRow("소개",   member.bio)
                }

                // ── 관심사 태그 ──
                if (member.interests.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("관심사", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Gray500)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement   = Arrangement.spacedBy(6.dp)
                        ) {
                            member.interests.forEach { interest ->
                                Box(
                                    modifier = Modifier
                                        .background(FeedConstants.LightPurpleBg, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) { Text(interest, fontSize = 12.sp, color = Purple) }
                            }
                        }
                    }
                }

                // ── 좋아하는 음식 태그 ──
                if (member.foodLikes.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("좋아하는 음식", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Gray500)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement   = Arrangement.spacedBy(6.dp)
                        ) {
                            member.foodLikes.forEach { food ->
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFDCFCE7), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) { Text(food, fontSize = 12.sp, color = Color(0xFF16A34A)) }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── 닫기 버튼 ──
                Button(
                    onClick  = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Purple)
                ) {
                    Text("닫기", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MemberInfoRow(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.Top
    ) {
        Text(
            text       = label,
            fontSize   = 13.sp,
            color      = Gray400,
            fontWeight = FontWeight.Medium,
            modifier   = Modifier.padding(end = 12.dp)
        )
        Text(
            text      = value,
            fontSize  = 13.sp,
            color     = Gray900,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier  = Modifier.weight(1f)
        )
    }
}
