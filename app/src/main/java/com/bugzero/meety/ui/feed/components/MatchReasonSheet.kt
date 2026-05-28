package com.bugzero.meety.ui.feed.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugzero.meety.ui.feed.FeedConstants
import com.bugzero.meety.ui.team.Team
import com.bugzero.meety.ui.theme.*
import kotlin.math.roundToInt

/**
 * 매칭 근거 바텀시트 (meety-feed-why-v3-mockup.html 이식)
 *
 * 추천 카드 위에 "?" 버튼으로 열리는 오버레이. 콜드스타트에서도 그 자리에서
 * 다시 셀 수 있는 "검증 가능한 수치"만 카드 형태로 한 장씩 넘겨 보여준다.
 *
 * 마지막 장 "자주 누른 태그 Top N"은 좋아요+패스 누적 10회를 채워야 해금되며,
 * 해금 전에는 적합도 점수 계산에서 제외(잠금)된다.
 */

// 해금 기준: 좋아요 + 패스 누적 횟수
private const val UNLOCK_THRESHOLD = 10
// 자주 누른 태그 상위 N개만 사용 (방법 2)
private const val TOP_TAG_N = 3

/**
 * 카드 배지용 적합도 점수 계산 (SwipeCard에서도 사용)
 * 기본 4항목 평균(80+88+80+75=81) + 해금 시 Top 태그 겹침 비율 추가
 */
fun calculateFitScore(userTopTags: List<String>, teamTags: List<String>, actionCount: Int): Int {
    val unlocked = actionCount >= UNLOCK_THRESHOLD
    val topTags  = userTopTags.take(TOP_TAG_N)
    val overlap  = topTags.count { it in teamTags }
    val topPct   = if (topTags.isNotEmpty()) (overlap * 100f / topTags.size).roundToInt() else 0
    val base     = listOf(80, 88, 80, 75)
    val vals     = base + if (unlocked) listOf(topPct) else emptyList()
    return if (vals.isNotEmpty()) vals.average().roundToInt() else 0
}

private val GradSoft = Brush.linearGradient(listOf(GradSoftStart, GradSoftEnd))

/** 근거 강도 배지 */
private enum class Strength(val bg: Color, val fg: Color, val filled: Int) {
    HI(Color(0xFFE7F8F0), Color(0xFF12A866), 3),
    MID(Color(0xFFFFF3DE), Color(0xFFC8860D), 2)
}

/** 한 장의 매칭 근거 */
private sealed class Reason {
    abstract val icon: String
    abstract val title: String
    abstract val source: String
    abstract val strength: Strength
    abstract val strengthLabel: String

    /** 적합도 종합 (4~5개 항목 평균) */
    data class Overview(
        override val icon: String,
        override val title: String,
        override val source: String,
        override val strength: Strength,
        override val strengthLabel: String,
        val fitScore: Int,
        val bars: List<Bar>,
        val calc: String
    ) : Reason()

    /** 관심사·태그 일치 (칩) */
    data class Chips(
        override val icon: String,
        override val title: String,
        override val source: String,
        override val strength: Strength,
        override val strengthLabel: String,
        val statNum: String,
        val statLabel: String,
        val statSub: String,
        val chips: List<Pair<String, Boolean>>,
        val why: String,
        val score: String,
        val scoreVal: String
    ) : Reason()

    /** 동네 근접도 (거리 행) */
    data class Distance(
        override val icon: String,
        override val title: String,
        override val source: String,
        override val strength: Strength,
        override val strengthLabel: String,
        val statNum: String,
        val statLabel: String,
        val statSub: String,
        val rows: List<DistRow>,
        val why: String,
        val score: String,
        val scoreVal: String
    ) : Reason()

    /** 일치/미일치 행 목록 (가치관·팀원 공통점) */
    data class Rows(
        override val icon: String,
        override val title: String,
        override val source: String,
        override val strength: Strength,
        override val strengthLabel: String,
        val statNum: String,
        val statLabel: String,
        val statSub: String,
        val rows: List<MatchRow>,
        val why: String,
        val score: String,
        val scoreVal: String,
        val caveat: String? = null
    ) : Reason()

    /** 자주 누른 태그 Top N (잠금/해금) */
    data class TopTags(
        override val icon: String,
        override val title: String,
        override val source: String,
        override val strength: Strength,
        override val strengthLabel: String,
        val unlocked: Boolean,
        val actionCount: Int,
        val userTopTags: List<String>,
        val teamTags: List<String>,
        val overlap: Int,
        val percent: Int
    ) : Reason()
}

private data class Bar(val label: String, val value: Int, val locked: Boolean = false)
private data class DistRow(val label: String, val distance: String, val score: Int)
private data class MatchRow(val label: String, val value: String, val matched: Boolean)

/**
 * 추천 카드 위 매칭 근거 근거를 구성한다.
 * - 4개 기본 항목은 목업 값을 사용 (콜드스타트 UI 우선)
 * - "관심사 일치" 칩과 "Top N 태그"는 실제 team.tags / 사용자 태그 점수로 계산
 */
private fun buildReasons(
    team: Team,
    userTopTags: List<String>,
    actionCount: Int
): List<Reason> {
    val unlocked = actionCount >= UNLOCK_THRESHOLD

    // ── 방법 2: 자주 누른 태그 Top N ──
    val topTags = userTopTags.take(TOP_TAG_N)
    val overlap = topTags.count { it in team.tags }
    val topPercent = if (topTags.isNotEmpty()) (overlap * 100f / topTags.size).roundToInt() else 0

    // ── 적합도 종합 (기본 4항목 + 해금 시 Top 태그) ──
    val baseBars = listOf(
        Bar("관심사 일치", 80),
        Bar("동네 근접도", 88),
        Bar("가치관 일치", 80),
        Bar("팀원 공통점", 75)
    )
    val topBar = Bar("자주 누른 태그", if (unlocked) topPercent else 0, locked = !unlocked)
    val allBars = baseBars + topBar

    val activeValues = baseBars.map { it.value } + if (unlocked) listOf(topPercent) else emptyList()
    val fitScore = if (activeValues.isNotEmpty()) activeValues.average().roundToInt() else 0

    val calc = if (unlocked) {
        "$fitScore = (${activeValues.joinToString(" + ")}) ÷ ${activeValues.size} — 확률이 아니라 위 항목들의 평균이에요. " +
                "'자주 누른 태그' 근거가 해금되어 점수에 반영됐어요."
    } else {
        "$fitScore = (${baseBars.joinToString(" + ") { it.value.toString() }}) ÷ ${baseBars.size} — 확률이 아니라 위 항목들의 평균이에요. " +
                "'자주 누른 태그' 근거는 좋아요·패스 ${UNLOCK_THRESHOLD}회를 채우면 해금돼 점수에 반영돼요. (현재 ${actionCount}/${UNLOCK_THRESHOLD})"
    }

    return listOf(
        Reason.Overview(
            icon = "🎯",
            title = "적합도 ${fitScore}점",
            source = if (unlocked) "5개 항목의 평균 · 직접 계산" else "4개 항목의 평균 · 직접 계산",
            strength = Strength.HI,
            strengthLabel = "계산값",
            fitScore = fitScore,
            bars = allBars,
            calc = calc
        ),
        Reason.Chips(
            icon = "🏷️",
            title = "관심사·태그 일치",
            source = "회원님 프로필 ∩ 팀 태그",
            strength = Strength.HI,
            strengthLabel = "80%",
            statNum = "4/5",
            statLabel = "개 일치",
            statSub = "회원님 관심사 5개 중 4개가 이 팀과 겹쳐요",
            chips = listOf(
                "맛집탐방" to true, "카페" to true, "영화" to true, "여행" to true, "운동" to false
            ),
            why = "두 집합의 교집합으로 계산했어요. 화면의 칩을 직접 세면 그대로 4개예요 — 검증 가능합니다.",
            score = "일치 4개 ÷ 내 관심사 5개 × 100",
            scoreVal = "80점"
        ),
        Reason.Distance(
            icon = "📍",
            title = "동네 근접도",
            source = "회원님 ↔ 팀원 대중교통 거리",
            strength = Strength.HI,
            strengthLabel = "평균 15분",
            statNum = "약 15분",
            statLabel = "대중교통 평균",
            statSub = "회원님(성북구) 기준 · 평균 2.5km · 4명 중 3명 20분 내",
            rows = listOf(
                DistRow("멤버 A · 성북구(동일)", "0.5km · 5분", 100),
                DistRow("멤버 B · 동대문구", "1.8km · 12분", 92),
                DistRow("멤버 C · 종로구", "2.7km · 18분", 84),
                DistRow("멤버 D · 노원구", "5.0km · 25분", 76)
            ),
            why = "단순 거리(2.5km)보다 대중교통 소요 시간으로 보여줘 \"얼마나 가까운지\"를 바로 체감할 수 있어요. 가까울수록 높은 점수를 줍니다.",
            score = "팀원별 거리 점수 평균 (100+92+84+76)÷4",
            scoreVal = "88점"
        ),
        Reason.Rows(
            icon = "⚖️",
            title = "가치관 일치",
            source = "온보딩 밸런스 게임 응답",
            strength = Strength.HI,
            strengthLabel = "4/5",
            statNum = "4/5",
            statLabel = "문항 동일",
            statSub = "회원님과 이 팀 다수의 선택이 일치",
            rows = listOf(
                MatchRow("모임 성격", "친목 중심", true),
                MatchRow("활동 강도", "가볍게", true),
                MatchRow("만남 빈도", "주 1회", true),
                MatchRow("비용", "더치페이", true),
                MatchRow("분위기", "조용→활발", false)
            ),
            why = "가입할 때 답한 밸런스 게임 5문항을 팀 다수 응답과 비교했어요. 미리 수집한 값이라 사용자 없이도 즉시 계산돼요.",
            score = "동일 응답 4 ÷ 5문항 × 100",
            scoreVal = "80점"
        ),
        Reason.Rows(
            icon = "🧩",
            title = "팀원과의 공통점",
            source = "팀원 프로필 대조",
            strength = Strength.MID,
            strengthLabel = "3/4",
            statNum = "3/4",
            statLabel = "명과 공통점",
            statSub = "팀원 4명 중 3명과 겹치는 점이 있어요",
            rows = listOf(
                MatchRow("같은 디자인 계열", "2명", true),
                MatchRow("공통 관심사 · 영화", "1명", true),
                MatchRow("동갑(22세)", "3명", true),
                MatchRow("같은 동아리", "0명", false)
            ),
            why = "추상적 점수가 아니라 실제 팀원과 겹치는 사실이라 가장 와닿아요.",
            score = "공통점 있는 멤버 3 ÷ 팀원 4명 × 100",
            scoreVal = "75점",
            caveat = "아직 실제 활동·유지 데이터는 없어, 프로필·취향 기반으로만 계산했어요. 사용자가 늘면 결과 지표가 더해집니다."
        ),
        // ── 방법 2: 자주 누른 태그 Top N (마지막, 잠금/해금) ──
        Reason.TopTags(
            icon = "🔥",
            title = "자주 누른 태그 Top $TOP_TAG_N",
            source = "내가 자주 누른 태그 ∩ 팀 태그",
            strength = if (overlap >= 2) Strength.HI else Strength.MID,
            strengthLabel = if (unlocked) "$topPercent%" else "잠금",
            unlocked = unlocked,
            actionCount = actionCount,
            userTopTags = topTags,
            teamTags = team.tags,
            overlap = overlap,
            percent = topPercent
        )
    )
}

@Composable
fun MatchReasonSheet(
    team: Team,
    userTopTags: List<String>,
    actionCount: Int,
    onClose: () -> Unit,
    onApply: () -> Unit
) {
    val reasons = remember(team.teamId, userTopTags, actionCount) {
        buildReasons(team, userTopTags, actionCount)
    }
    var idx by remember(team.teamId) { mutableStateOf(0) }
    // 슬라이드 방향: 1 = 다음(왼쪽에서 오른쪽으로 밀려남), -1 = 이전
    var slideDir by remember { mutableStateOf(1) }
    val isLast = idx == reasons.lastIndex

    // 시트 슬라이드업 입장 애니메이션 (HTML의 translateY 20px → 0)
    // LaunchedEffect로 20dp → 0dp 트리거해 실제 슬라이드업 효과 발생
    var sheetTarget by remember { mutableStateOf(20.dp) }
    val sheetOffsetY by animateDpAsState(
        targetValue = sheetTarget,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "sheetSlideUp"
    )
    LaunchedEffect(Unit) { sheetTarget = 0.dp }

    // 카드 위를 덮는 반투명 그라데이션 (시트 바깥 탭하면 닫힘)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color(0x1A140C2E),
                    0.38f to Color(0x80140C2E),
                    1f to Color(0xDB140C2E)
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClose() }
    ) {
        // 바텀시트: 최대 화면 75% 높이, 내부 스크롤 가능
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                // 시트 높이: 카드 영역의 최대 93% (답답하지 않게 크게)
                .fillMaxHeight(0.93f)
                .padding(8.dp)
                .offset(y = sheetOffsetY)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* 바깥 탭 이벤트 흡수 */ }
                .padding(horizontal = 15.dp, vertical = 15.dp)
        ) {
            // ── 상단: 타이틀 + 카운터 + 닫기 ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✦", color = Brand1, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(6.dp))
                Text("매칭 근거", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Spacer(Modifier.weight(1f))
                Row {
                    Text("${idx + 1}", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Brand1)
                    Text(" / ${reasons.size}", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Ink3)
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MeetySurface2)
                        .border(1.dp, Line, CircleShape)
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, "닫기", tint = Ink2, modifier = Modifier.size(15.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── 근거 본문: 스크롤 가능 + 페이지 전환 애니메이션 ──
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = idx,
                    transitionSpec = {
                        // slideDir에 따라 좌우 슬라이드 + 페이드 (HTML rin 애니메이션과 동일)
                        (slideInHorizontally(
                            animationSpec = tween(280, easing = FastOutSlowInEasing),
                            initialOffsetX = { fullWidth -> if (slideDir > 0) fullWidth else -fullWidth }
                        ) + fadeIn(tween(200))) togetherWith
                        (slideOutHorizontally(
                            animationSpec = tween(280, easing = FastOutSlowInEasing),
                            targetOffsetX = { fullWidth -> if (slideDir > 0) -fullWidth else fullWidth }
                        ) + fadeOut(tween(200)))
                    },
                    label = "reason_slide"
                ) { currentIdx ->
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        ReasonContent(reasons[currentIdx])
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── 하단 네비게이션: 이전 / dots / 다음(or CTA) ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                NavButton(Icons.Default.ChevronLeft) {
                    slideDir = -1
                    idx = (idx - 1 + reasons.size) % reasons.size
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    reasons.forEachIndexed { i, _ ->
                        // dot 너비 애니메이션
                        val dotWidth by animateDpAsState(
                            targetValue = if (i == idx) 20.dp else 7.dp,
                            animationSpec = tween(200),
                            label = "dot_width_$i"
                        )
                        Box(
                            modifier = Modifier
                                .height(7.dp)
                                .width(dotWidth)
                                .clip(RoundedCornerShape(999.dp))
                                .then(
                                    if (i == idx)
                                        Modifier.background(FeedConstants.GradientPurplePink, RoundedCornerShape(999.dp))
                                    else
                                        Modifier.background(Line, RoundedCornerShape(999.dp))
                                )
                        )
                    }
                }
                if (isLast) {
                    Row(
                        modifier = Modifier
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(FeedConstants.GradientPurplePink)
                            .clickable { onApply() }
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Login, null, tint = Color.White, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("가입 신청하기", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    }
                } else {
                    NavButton(Icons.Default.ChevronRight) {
                        slideDir = 1
                        idx = (idx + 1) % reasons.size
                    }
                }
            }
        }
    }
}

@Composable
private fun NavButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Ink, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun ReasonContent(reason: Reason) {
    Column {
        // 공통 헤더: 아이콘 + 제목 + 출처 + 강도배지
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(GradSoft),
                contentAlignment = Alignment.Center
            ) {
                Text(reason.icon, fontSize = 21.sp)
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(reason.title, fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Spacer(Modifier.height(2.dp))
                Text(reason.source, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Ink3)
            }
            StrengthBadge(reason.strength, reason.strengthLabel)
        }

        Spacer(Modifier.height(12.dp))

        when (reason) {
            is Reason.Overview -> OverviewBody(reason)
            is Reason.Chips -> ChipsBody(reason)
            is Reason.Distance -> DistanceBody(reason)
            is Reason.Rows -> RowsBody(reason)
            is Reason.TopTags -> TopTagsBody(reason)
        }
    }
}

@Composable
private fun StrengthBadge(strength: Strength, label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(strength.bg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = strength.fg)
        Spacer(Modifier.width(5.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(3) { i ->
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(strength.fg.copy(alpha = if (i < strength.filled) 1f else 0.32f))
                )
            }
        }
    }
}

@Composable
private fun StatHero(num: String, label: String, sub: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(GradSoft)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(num, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Brand1, letterSpacing = (-0.6).sp)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            Spacer(Modifier.height(3.dp))
            Text(sub, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Ink2)
        }
    }
}

@Composable
private fun WhyText(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MeetySurface2)
            .padding(horizontal = 13.dp, vertical = 11.dp)
    ) {
        Text(text, fontSize = 13.sp, color = Ink2, lineHeight = 20.sp)
    }
}

@Composable
private fun ScoreBox(score: String, scoreVal: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MeetySurface2)
            .border(1.dp, Line, RoundedCornerShape(10.dp))
            .padding(horizontal = 11.dp, vertical = 8.dp)
    ) {
        Row {
            Text("📊 이 항목 점수 = $score = ", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Ink3)
            Text(scoreVal, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Brand1)
        }
    }
}

@Composable
private fun OverviewBody(r: Reason.Overview) {
    Column {
        StatHero("${r.fitScore}", "점 / 100", "회원님 데이터로 직접 계산")
        Spacer(Modifier.height(11.dp))
        r.bars.forEach { bar ->
            Row(
                modifier = Modifier.padding(bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.width(108.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (bar.locked) {
                        Icon(Icons.Default.Lock, null, tint = Ink4, modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(3.dp))
                    }
                    Text(
                        bar.label,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (bar.locked) Ink4 else Ink2
                    )
                }
                Spacer(Modifier.width(9.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(9.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Line2)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(if (bar.locked) 0f else bar.value / 100f)
                            .clip(RoundedCornerShape(999.dp))
                            .background(FeedConstants.GradientPurplePink)
                    )
                }
                Spacer(Modifier.width(9.dp))
                Text(
                    if (bar.locked) "잠금" else "${bar.value}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (bar.locked) Ink4 else Brand1,
                    modifier = Modifier.width(38.dp)
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        CalcBox(r.calc)
    }
}

@Composable
private fun CalcBox(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MeetySurface2)
            .border(1.dp, Line, RoundedCornerShape(10.dp))
            .padding(horizontal = 11.dp, vertical = 8.dp)
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Ink3, lineHeight = 16.5.sp)
    }
}

@Composable
private fun ChipsBody(r: Reason.Chips) {
    Column {
        StatHero(r.statNum, r.statLabel, r.statSub)
        Spacer(Modifier.height(11.dp))
        FlowRowChips(r.chips)
        Spacer(Modifier.height(11.dp))
        WhyText(r.why)
        Spacer(Modifier.height(10.dp))
        ScoreBox(r.score, r.scoreVal)
    }
}

@Composable
private fun FlowRowChips(chips: List<Pair<String, Boolean>>) {
    // 간단한 줄바꿈 칩 배치
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        chips.chunked(3).forEach { rowChips ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                rowChips.forEach { (label, on) ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (on) VioletSoft else MeetySurface2)
                            .then(
                                if (on) Modifier else Modifier.border(1.dp, Line, RoundedCornerShape(999.dp))
                            )
                            .padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (on) {
                            Icon(Icons.Default.Check, null, tint = OkGreen, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(5.dp))
                        }
                        Text(
                            label,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (on) VioletText else Ink4
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DistanceBody(r: Reason.Distance) {
    Column {
        StatHero(r.statNum, r.statLabel, r.statSub)
        Spacer(Modifier.height(11.dp))
        Column {
            r.rows.forEachIndexed { i, row ->
                if (i > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Line)
                    )
                }
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(row.label, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Ink2, modifier = Modifier.weight(1f))
                    Text(row.distance, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Ink3)
                    Spacer(Modifier.width(10.dp))
                    Text("${row.score}점", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Brand1)
                }
            }
        }
        Spacer(Modifier.height(11.dp))
        WhyText(r.why)
        Spacer(Modifier.height(10.dp))
        ScoreBox(r.score, r.scoreVal)
    }
}

@Composable
private fun RowsBody(r: Reason.Rows) {
    Column {
        StatHero(r.statNum, r.statLabel, r.statSub)
        Spacer(Modifier.height(11.dp))
        Column {
            r.rows.forEachIndexed { i, row ->
                if (i > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Line)
                    )
                }
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(row.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Ink2, modifier = Modifier.weight(1f))
                    Text(row.value, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    Spacer(Modifier.width(8.dp))
                    if (row.matched) {
                        Icon(Icons.Default.Check, null, tint = OkGreen, modifier = Modifier.size(17.dp))
                    } else {
                        Text("미일치", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Ink4)
                    }
                }
            }
        }
        Spacer(Modifier.height(11.dp))
        WhyText(r.why)
        Spacer(Modifier.height(10.dp))
        ScoreBox(r.score, r.scoreVal)
        r.caveat?.let {
            Spacer(Modifier.height(9.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color(0xFFEAF9F2))
                    .border(1.dp, Color(0xFFC7EEDD), RoundedCornerShape(11.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Text(it, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0B7A52), lineHeight = 16.5.sp)
            }
        }
    }
}

@Composable
private fun TopTagsBody(r: Reason.TopTags) {
    if (!r.unlocked) {
        // ── 잠금 상태 ──
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(15.dp))
                    .background(MeetySurface2)
                    .border(1.dp, Line, RoundedCornerShape(15.dp))
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Line2),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lock, null, tint = Ink3, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("아직 잠겨 있어요", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "좋아요·패스 ${UNLOCK_THRESHOLD}회를 채우면 해금돼 적합도 점수에 반영돼요",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink2,
                        lineHeight = 16.sp
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            // 진행도
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("진행도", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Ink2)
                Spacer(Modifier.width(9.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(9.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Line2)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((r.actionCount.coerceAtMost(UNLOCK_THRESHOLD).toFloat() / UNLOCK_THRESHOLD))
                            .clip(RoundedCornerShape(999.dp))
                            .background(FeedConstants.GradientPurplePink)
                    )
                }
                Spacer(Modifier.width(9.dp))
                Text("${r.actionCount}/${UNLOCK_THRESHOLD}", fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = Brand1)
            }
            Spacer(Modifier.height(11.dp))
            WhyText(
                "내가 제일 좋아하는 태그 ${TOP_TAG_N}개 중 몇 개가 이 팀에 있는지 보는 근거예요. " +
                        "자주 누른 태그를 \"순위\"로만 써서 복잡한 가중치 없이 직관적으로 계산해요. " +
                        "충분히 둘러봐야 내 취향 순위가 정확해지기 때문에 ${UNLOCK_THRESHOLD}회 이후 해금됩니다."
            )
        }
    } else {
        // ── 해금 상태 ──
        Column {
            StatHero("${r.overlap}/${r.userTopTags.size}", "개 겹침", "내 Top ${r.userTopTags.size} 태그 중 ${r.overlap}개가 이 팀에 있어요")
            Spacer(Modifier.height(11.dp))
            Text("내가 자주 누른 태그 Top ${r.userTopTags.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Ink3)
            Spacer(Modifier.height(6.dp))
            FlowRowChips(r.userTopTags.map { it to (it in r.teamTags) })
            Spacer(Modifier.height(10.dp))
            Text("이 팀 태그", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Ink3)
            Spacer(Modifier.height(6.dp))
            FlowRowChips(r.teamTags.take(6).map { it to (it in r.userTopTags) })
            Spacer(Modifier.height(11.dp))
            WhyText(
                "내가 제일 좋아하는 태그 ${r.userTopTags.size}개 중 ${r.overlap}개가 이 팀에 있어요. " +
                        "횟수를 \"순위\"로만 쓰고 복잡한 가중치 계산은 버려, 화면에서 직접 셀 수 있어요."
            )
            Spacer(Modifier.height(10.dp))
            ScoreBox("겹친 ${r.overlap}개 ÷ Top ${r.userTopTags.size}개 × 100", "${r.percent}점")
        }
    }
}