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
import com.bugzero.meety.ui.auth.BALANCE_QUESTIONS
import com.bugzero.meety.ui.feed.CurrentUserProfile
import com.bugzero.meety.ui.feed.FeedConstants
import com.bugzero.meety.ui.feed.MemberDistanceResult
import com.bugzero.meety.ui.feed.MemberProfile
import com.bugzero.meety.ui.team.Team
import com.bugzero.meety.ui.theme.*
import kotlin.math.abs
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

/**
 * 카드 배지용 적합도 점수 계산 (레거시 — SwipeCard에서 캐시된 값으로 교체 예정)
 *
 * FeedViewModel.computeFitScore()가 실제 계산을 담당하며,
 * 이 함수는 캐시가 아직 준비되지 않았을 때의 폴백용으로만 사용된다.
 */
@Deprecated("FeedUiState.cardFitScoreCache 에서 미리 계산된 값을 사용하세요")
fun calculateFitScore(userTopTags: List<String>, teamTags: List<String>, actionCount: Int): Int {
    val unlocked = actionCount >= FeedConstants.MATCH_UNLOCK_THRESHOLD
    val topTags  = userTopTags.take(FeedConstants.MATCH_TOP_TAG_N)
    val overlap  = topTags.count { it in teamTags }
    val topPct   = if (topTags.isNotEmpty()) (overlap * 100f / topTags.size).roundToInt() else 0
    val base     = listOf(80, 88, 80, 75)
    val vals     = base + if (unlocked) listOf(topPct) else emptyList()
    return if (vals.isNotEmpty()) vals.average().roundToInt() else 0
}

private val GradSoft = Brush.linearGradient(listOf(GradSoftStart, GradSoftEnd))

/**
 * 밸런스 게임 axis 키 → 화면 표시용 짧은 레이블.
 * BalanceQuestions.kt 의 axis 값과 1:1 대응해야 한다.
 * axis가 추가/변경되면 여기도 같이 업데이트.
 */
private val AXIS_DISPLAY_LABELS = mapOf(
    "meeting_purpose" to "모임 목적",
    "intensity"       to "활동 강도",
    "frequency"       to "만남 빈도",
    "cost"            to "비용 정산",
    "vibe"            to "분위기",
    "planning"        to "약속 스타일"
)

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

/** locked: 해금 전(자물쇠). pending: 데이터 로딩/분석 중(숫자 표시 안 함). */
private data class Bar(val label: String, val value: Int, val locked: Boolean = false, val pending: Boolean = false)
private data class DistRow(val label: String, val distance: String, val score: Int)
private data class MatchRow(val label: String, val value: String, val matched: Boolean)

/**
 * 매칭 근거 카드 목록을 구성한다.
 *
 * - 관심사·태그: currentUserProfile.interests/foodLikes ∩ team.tags
 * - 동네 근접도: distanceResults (FeedViewModel이 사전 계산한 대중교통 소요시간)
 * - 가치관 일치: currentUserProfile.balanceAnswers ↔ team.balanceProfile
 * - 팀원 공통점: memberProfiles ∩ currentUserProfile (관심사·음식취향)
 * - 자주 누른 태그: userTopTags(선호도 누적 Top N) ∩ team.tags
 */
private fun buildReasons(
    team: Team,
    userTopTags: List<String>,
    actionCount: Int,
    currentUserProfile: CurrentUserProfile?,
    memberProfiles: List<MemberProfile>,
    distanceResults: List<MemberDistanceResult>,
    fitScore: Int           // 카드에 표시된 점수와 동일한 값 (FeedViewModel.computeFitScore)
): List<Reason> {
    val unlocked = actionCount >= FeedConstants.MATCH_UNLOCK_THRESHOLD

    // ── 자주 누른 태그 Top N ──
    val topTags = userTopTags.take(FeedConstants.MATCH_TOP_TAG_N)
    val overlap = topTags.count { it in team.tags }
    // 분모를 MATCH_TOP_TAG_N(3)으로 고정 — topTags 실제 크기와 무관하게 일관된 점수.
    // 0개 → 0점, 1개 → 33점, 2개 → 67점, 3개 → 100점
    val topPercent = (overlap * 100f / FeedConstants.MATCH_TOP_TAG_N).roundToInt()

    // ── 관심사·태그 일치 ──
    val userInterests = currentUserProfile?.interests ?: emptyList()
    val userFoodLikes = currentUserProfile?.foodLikes ?: emptyList()
    val allUserTags = (userInterests + userFoodLikes).distinct().take(8)
    val matchedTags = allUserTags.filter { it in team.tags }
    val unmatchedTags = allUserTags.filter { it !in team.tags }
    val displayChips: List<Pair<String, Boolean>> = (matchedTags + unmatchedTags.take(3))
        .map { it to (it in team.tags) }
        .ifEmpty { team.tags.take(5).map { it to false } }
    val interestMatchCount = matchedTags.size
    val interestTotalCount = allUserTags.size.coerceAtLeast(1)
    val interestScore = if (allUserTags.isNotEmpty()) {
        (interestMatchCount.toDouble() / interestTotalCount * 100).toInt().coerceIn(0, 100)
    } else 70

    // ── 동네 근접도 ──
    val hasDistance = distanceResults.isNotEmpty()
    val avgMinutes = if (hasDistance) distanceResults.map { it.transitMinutes }.average().toInt() else 0
    val avgDistScore = if (hasDistance) distanceResults.map { it.score }.average().toInt() else 0
    val distRows = if (hasDistance) {
        distanceResults.map { r ->
            val distStr = if (r.distanceKm < 1.0) "${(r.distanceKm * 1000).toInt()}m"
                          else "${"%.1f".format(r.distanceKm)}km"
            DistRow(
                label    = "${r.memberName} · ${r.memberLocation}",
                distance = "$distStr · 약 ${r.transitMinutes}분",
                score    = r.score
            )
        }
    } else {
        listOf(DistRow("계산 중…", "잠시 후 다시 열어보세요", 0))
    }

    // ── 가치관 일치 (balanceAnswers ↔ team.balanceProfile) ──
    val userAnswers  = currentUserProfile?.balanceAnswers ?: emptyMap()
    val teamProfile  = team.balanceProfile
    // axis → (행 레이블, 유저가 고른 옵션 태그)
    // 저장 규칙: -1 = optionA("a" 선택), +1 = optionB("b" 선택)
    val axisInfo: Map<String, Pair<String, String>> = BALANCE_QUESTIONS.associate { q ->
        val chosenTag = when (userAnswers[q.axis]) {
            -1   -> q.optionA.tag
            1    -> q.optionB.tag
            else -> "—"
        }
        q.axis to ((AXIS_DISPLAY_LABELS[q.axis] ?: q.axis) to chosenTag)
    }
    val availableAxes = userAnswers.keys.intersect(teamProfile.keys)
        .sortedByDescending { axis -> abs(teamProfile[axis] ?: 0f) }
        .take(5)
    val balanceRows = availableAxes.mapNotNull { axis ->
        val userVal  = userAnswers[axis]  ?: return@mapNotNull null
        val teamAvg  = teamProfile[axis]  ?: return@mapNotNull null
        val (category, answerLabel) = axisInfo[axis] ?: return@mapNotNull null
        val matched = when {
            abs(teamAvg) < 0.1f         -> false  // 팀이 반반 → 절반 점수
            userVal < 0 && teamAvg < -0.1f -> true
            userVal > 0 && teamAvg > 0.1f  -> true
            else                            -> false
        }
        MatchRow(label = category, value = answerLabel, matched = matched)
    }
    val balanceMatchCount = balanceRows.count { it.matched }
    val balanceTotal = balanceRows.size.coerceAtLeast(1)
    val balanceScore = if (availableAxes.isNotEmpty()) {
        availableAxes.mapNotNull { axis ->
            val u = userAnswers[axis]?.toFloat() ?: return@mapNotNull null
            val t = teamProfile[axis]             ?: return@mapNotNull null
            ((u * t + 1f) / 2f * 100f).coerceIn(0f, 100f)
        }.average().toInt().coerceIn(0, 100)
    } else 0
    val hasBalance = balanceRows.isNotEmpty()

    // ── 팀원 공통점 (computeCommonalityScore와 동일한 공식) ──
    val userMbti = currentUserProfile?.mbti?.uppercase() ?: ""
    val hasUserMbti = userMbti.length == 4

    val hasMemberData = memberProfiles.isNotEmpty() && currentUserProfile != null
    val commonRows: List<MatchRow>
    val membersWithCommon: Int
    val commonScore: Int

    if (hasMemberData) {
        // 관심사 행: 내 관심사 항목별로 몇 명과 겹치는지
        val interestMatchRows = userInterests.map { interest ->
            val cnt = memberProfiles.count { interest in it.interests }
            MatchRow("공통 관심사 · $interest", "${cnt}명", cnt > 0)
        }.take(3)
        // 음식 행
        val foodMatchRows = userFoodLikes.map { food ->
            val cnt = memberProfiles.count { food in it.foodLikes }
            MatchRow("음식 취향 · $food", "${cnt}명", cnt > 0)
        }.take(2)
        // MBTI 행: 팀원마다 4축 일치도 표시
        val mbtiRows = if (hasUserMbti) {
            memberProfiles.mapNotNull { member ->
                val memberMbti = member.mbti.uppercase()
                if (memberMbti.length != 4) return@mapNotNull null
                val axisMatch = userMbti.zip(memberMbti).count { (a, b) -> a == b }
                MatchRow(
                    label   = "MBTI · ${member.name} ($memberMbti)",
                    value   = "${axisMatch}/4축 일치",
                    matched = axisMatch >= 3
                )
            }
        } else emptyList()

        commonRows = (mbtiRows + interestMatchRows + foodMatchRows).ifEmpty {
            listOf(MatchRow("공통 프로필 항목 없음", "현재 데이터 기준", false))
        }
        membersWithCommon = memberProfiles.count { member ->
            userInterests.any { it in member.interests } || userFoodLikes.any { it in member.foodLikes }
        }

        // 점수: 팀원별 [관심사 정밀도 + 음식 정밀도 + MBTI 축 일치도] 평균 + 커버리지 보너스
        val memberScores = memberProfiles.map { member ->
            val signals = mutableListOf<Double>()
            if (currentUserProfile!!.interests.isNotEmpty()) {
                val matched = currentUserProfile.interests.count { it in member.interests.toSet() }
                signals.add(matched.toDouble() / currentUserProfile.interests.size * 100)
            }
            if (currentUserProfile.foodLikes.isNotEmpty()) {
                val matched = currentUserProfile.foodLikes.count { it in member.foodLikes.toSet() }
                signals.add(matched.toDouble() / currentUserProfile.foodLikes.size * 100)
            }
            val memberMbti = member.mbti.uppercase()
            if (hasUserMbti && memberMbti.length == 4) {
                val axisMatch = userMbti.zip(memberMbti).count { (a, b) -> a == b }
                signals.add(axisMatch.toDouble() / 4 * 100)
            }
            if (signals.isEmpty()) 50.0 else signals.average()
        }
        val memberAvg = if (memberScores.isEmpty()) 50.0 else memberScores.average()
        val coverageBonus = if (currentUserProfile!!.interests.isNotEmpty()) {
            val allMemberInterests = memberProfiles.flatMap { it.interests }.toSet()
            val covered = currentUserProfile.interests.count { it in allMemberInterests }
            covered.toDouble() / currentUserProfile.interests.size * 20
        } else 0.0
        commonScore = (memberAvg + coverageBonus).toInt().coerceIn(0, 100)
    } else {
        commonRows = listOf(MatchRow("팀원 프로필 로딩 중", "잠시 후 다시 열어보세요", false))
        membersWithCommon = 0
        commonScore = 50
    }

    // 팀원 평균 일치도 표시용 — 커버리지 보너스 포함한 최종 점수를 % 표기로 변환
    val commonScorePct = commonScore  // 0~100 → 그대로 % 로 표기

    // ── 적합도 종합 bars ──
    // 관심사 일치: 프로필 기반 정적 데이터 (관심사/음식취향 ∩ 팀 태그)
    // 자주 누른 태그: 스와이프 누적 행동 기반 동적 데이터 — 둘은 출처가 달라 별도 표시
    // pending=true: 데이터 미수집 → "—" 표시
    val allBars = listOf(
        Bar("관심사 일치",   interestScore),
        Bar("가치관 일치",   if (hasBalance)  balanceScore        else 0,  pending = !hasBalance),
        Bar("동네 근접도",   if (hasDistance) avgDistScore        else 0,  pending = !hasDistance),
        Bar("팀원 공통점",   if (hasMemberData) commonScore else 50, pending = !hasMemberData),
        Bar("자주 누른 태그", if (unlocked) topPercent else 0, locked = !unlocked)
    )

    // fitScore: 카드에 표시된 값과 동일 (FeedViewModel.computeFitScore 가중 평균)
    // bars 4개가 computeFitScore 컴포넌트와 1:1 대응하므로 단순 평균과의 차이는 재정규화 때문
    val calc = buildString {
        append("${fitScore}점 — 태그선호도(30%)·가치관(30%)·거리(20%)·팀원공통점(20%) 가중 평균.")
        append(" 데이터가 없는 항목은 제외 후 재정규화돼요.")
        if (!unlocked) {
            append(" '자주 누른 태그'는 좋아요·패스 ${FeedConstants.MATCH_UNLOCK_THRESHOLD}회를 채우면 점수에 반영돼요.")
            append(" (현재 $actionCount/${FeedConstants.MATCH_UNLOCK_THRESHOLD})")
        }
    }

    return buildList {
        add(Reason.Overview(
            icon = "🎯",
            title = "적합도 ${fitScore}점",
            source = "태그·가치관·거리·공통점 가중 평균",
            strength = Strength.HI,
            strengthLabel = "계산값",
            fitScore = fitScore,
            bars = allBars,
            calc = calc
        ))
        add(Reason.Chips(
            icon = "🏷️",
            title = "관심사·태그 일치",
            source = "회원님 관심사 ∩ 팀 태그",
            strength = if (interestMatchCount > 0) Strength.HI else Strength.MID,
            strengthLabel = if (allUserTags.isNotEmpty()) "${interestScore}%" else "분석 중",
            statNum = "$interestMatchCount/${allUserTags.size}",
            statLabel = "개 일치",
            statSub = "회원님 관심사 ${allUserTags.size}개 중 ${interestMatchCount}개가 이 팀과 겹쳐요",
            chips = displayChips,
            why = "회원님 관심사·음식취향과 팀 태그의 교집합으로 계산했어요. 화면의 칩을 직접 세면 같은 수예요 — 검증 가능합니다.",
            score = "일치 ${interestMatchCount}개 ÷ 내 관심사 ${allUserTags.size}개 × 100",
            scoreVal = "${interestScore}점"
        ))
        add(Reason.Distance(
            icon = "📍",
            title = "동네 근접도",
            source = "회원님 ↔ 팀원 대중교통 소요시간 기반",
            strength = if (hasDistance && avgDistScore >= 75) Strength.HI else Strength.MID,
            strengthLabel = if (hasDistance) "${avgDistScore}점" else "계산 중",
            statNum = if (hasDistance) "${avgDistScore}점" else "—",
            statLabel = "팀원 평균 근접도",
            statSub = if (hasDistance) {
                val loc = currentUserProfile?.location?.ifBlank { "내 동네" } ?: "내 동네"
                "회원님($loc) 기준 · 대중교통 평균 ${avgMinutes}분 · ${distanceResults.size}명 대상"
            } else "거리 데이터를 불러오는 중이에요",
            rows = distRows,
            why = buildString {
                append("대중교통 소요시간을 점수로 변환했어요. ")
                append("0분 → 100점 · 30분 → 76점 · 60분 → 52점 · 125분 이상 → 0점. ")
                append("공식: 100 − (소요시간 × 0.8), 최저 0점. ")
                append("팀원마다 계산한 뒤 평균을 냅니다. ")
                append("아래 목록에서 팀원별 소요시간과 점수를 직접 확인할 수 있어요.")
            },
            score = if (hasDistance) "100 − (대중교통 소요시간 × 0.8) 팀원 평균" else "거리 계산 대기 중",
            scoreVal = if (hasDistance) "${avgDistScore}점" else "분석 중"
        ))
        add(Reason.Rows(
            icon = "⚖️",
            title = "가치관 일치",
            source = "온보딩 밸런스 게임 응답",
            strength = if (hasBalance && balanceScore >= 70) Strength.HI else Strength.MID,
            strengthLabel = if (hasBalance) "$balanceMatchCount/$balanceTotal" else "분석 중",
            statNum = if (hasBalance) "$balanceMatchCount/$balanceTotal" else "—",
            statLabel = "문항 동일",
            statSub = if (hasBalance) "회원님과 이 팀 다수의 선택이 일치하는 문항" else "밸런스 게임 응답 분석 중",
            rows = balanceRows.ifEmpty {
                listOf(MatchRow("밸런스 게임 데이터 없음", "팀·유저 답변 대기 중", false))
            },
            why = "가입할 때 답한 밸런스 게임 문항을 팀 다수 응답과 비교했어요. 팀원 의견이 반반일 때는 절반 점수만 부여합니다 — 억지로 일치로 처리하지 않아요.",
            score = if (hasBalance) "(유저답변 × 팀평균 + 1) ÷ 2 × 100의 평균" else "밸런스 게임 답변 대기 중",
            scoreVal = if (hasBalance) "${balanceScore}점" else "분석 중"
        ))
        add(Reason.Rows(
            icon = "🧩",
            title = "팀원과의 공통점",
            source = "관심사 · 음식취향 · MBTI 4축 비교",
            strength = if (commonScore >= 60) Strength.HI else Strength.MID,
            strengthLabel = if (hasMemberData) "${commonScorePct}%" else "분석 중",
            statNum = if (hasMemberData) "${commonScorePct}%" else "—",
            statLabel = "팀원 평균 일치도",
            statSub = if (hasMemberData)
                "팀원 ${memberProfiles.size}명 각각과 관심사·음식·MBTI를 비교해 평균 낸 값이에요"
            else
                "팀원 프로필 분석 중",
            rows = commonRows,
            why = buildString {
                append("팀원 한 명 한 명과 세 가지를 비교합니다. ")
                append("① 내 관심사 중 팀원과 겹치는 비율, ")
                append("② 내 음식취향 중 겹치는 비율, ")
                append("③ MBTI 4개 축(E/I·N/S·F/T·J/P) 중 일치하는 수. ")
                append("데이터가 없는 항목은 제외하고 있는 것만 평균 냅니다. ")
                append("추가로, 팀 전체가 내 관심사를 얼마나 커버하는지도 최대 +20%p 보너스로 반영돼요.")
            },
            score = "팀원별 세 신호 평균 → 팀원 전체 평균 + 커버리지 보너스(최대 +20)",
            scoreVal = if (hasMemberData) "${commonScorePct}점" else "분석 중",
            caveat = "아직 실제 활동·유지 데이터는 없어, 프로필·취향 기반으로만 계산했어요. 사용자가 늘면 결과 지표가 더해집니다."
        ))
        add(Reason.TopTags(
            icon = "🔥",
            title = "자주 누른 태그 Top ${FeedConstants.MATCH_TOP_TAG_N}",
            source = "내가 자주 누른 태그 ∩ 팀 태그",
            strength = if (overlap >= 2) Strength.HI else Strength.MID,
            strengthLabel = if (unlocked) "${topPercent}%" else "잠금",
            unlocked = unlocked,
            actionCount = actionCount,
            userTopTags = topTags,
            teamTags = team.tags,
            overlap = overlap,
            percent = topPercent
        ))
    }
}

@Composable
fun MatchReasonSheet(
    team: Team,
    userTopTags: List<String>,
    actionCount: Int,
    currentUserProfile: CurrentUserProfile?,
    memberProfiles: List<MemberProfile>,
    distanceResults: List<MemberDistanceResult>,
    fitScore: Int = 70,     // 카드에 표시된 점수와 일치 (FeedUiState.cardFitScoreCache)
    onClose: () -> Unit,
    onApply: () -> Unit
) {
    val reasons = remember(
        team.teamId,
        userTopTags,
        actionCount,
        currentUserProfile,
        memberProfiles.size,
        distanceResults.size,
        fitScore
    ) {
        buildReasons(
            team = team,
            userTopTags = userTopTags,
            actionCount = actionCount,
            currentUserProfile = currentUserProfile,
            memberProfiles = memberProfiles,
            distanceResults = distanceResults,
            fitScore = fitScore
        )
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
                val isFirst = idx == 0
                if (isFirst) {
                    // 첫 번째 카드에서는 좌측 화살표 숨김 (wrap-around 방지)
                    Spacer(Modifier.size(46.dp))
                } else {
                    NavButton(Icons.Default.ChevronLeft) {
                        slideDir = -1
                        idx -= 1
                    }
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
                        Text("좋아요 보내기", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
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
            val dimmed = bar.locked || bar.pending
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
                        color = if (dimmed) Ink4 else Ink2
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
                            .fillMaxWidth(if (dimmed) 0f else bar.value / 100f)
                            .clip(RoundedCornerShape(999.dp))
                            .background(FeedConstants.GradientPurplePink)
                    )
                }
                Spacer(Modifier.width(9.dp))
                Text(
                    when {
                        bar.locked  -> "잠금"
                        bar.pending -> "—"
                        else        -> "${bar.value}"
                    },
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (dimmed) Ink4 else Brand1,
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
                        "좋아요·패스 ${FeedConstants.MATCH_UNLOCK_THRESHOLD}회를 채우면 해금돼 적합도 점수에 반영돼요",
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
                            .fillMaxWidth((r.actionCount.coerceAtMost(FeedConstants.MATCH_UNLOCK_THRESHOLD).toFloat() / FeedConstants.MATCH_UNLOCK_THRESHOLD))
                            .clip(RoundedCornerShape(999.dp))
                            .background(FeedConstants.GradientPurplePink)
                    )
                }
                Spacer(Modifier.width(9.dp))
                Text("${r.actionCount}/${FeedConstants.MATCH_UNLOCK_THRESHOLD}", fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = Brand1)
            }
            Spacer(Modifier.height(11.dp))
            WhyText(
                "내가 제일 좋아하는 태그 ${FeedConstants.MATCH_TOP_TAG_N}개 중 몇 개가 이 팀에 있는지 보는 근거예요. " +
                        "자주 누른 태그를 \"순위\"로만 써서 복잡한 가중치 없이 직관적으로 계산해요. " +
                        "충분히 둘러봐야 내 취향 순위가 정확해지기 때문에 ${FeedConstants.MATCH_UNLOCK_THRESHOLD}회 이후 해금됩니다."
            )
        }
    } else {
        // ── 해금 상태 ──
        val topN = FeedConstants.MATCH_TOP_TAG_N  // 항상 3 — 표시용 기준값
        Column {
            if (r.userTopTags.isEmpty()) {
                // userTagScores가 아직 없는 상태 (첫 세션, 데이터 미수집)
                StatHero("—", "분석 중", "좋아요·패스를 더 하면 내 취향 태그가 쌓여요")
                Spacer(Modifier.height(11.dp))
                Text(
                    "아직 누적된 태그가 없어요. 카드를 더 둘러보면 Top ${topN}이 채워집니다.",
                    fontSize = 12.sp,
                    color = Ink3,
                    lineHeight = 17.sp
                )
            } else {
                StatHero("${r.overlap}/${topN}", "개 겹침", "내 Top ${topN} 태그 중 ${r.overlap}개가 이 팀에 있어요")
                Spacer(Modifier.height(11.dp))
                Text("내가 자주 누른 태그 Top ${topN}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Ink3)
                Spacer(Modifier.height(6.dp))
                FlowRowChips(r.userTopTags.map { it to (it in r.teamTags) })
                Spacer(Modifier.height(10.dp))
                Text("이 팀 태그", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Ink3)
                Spacer(Modifier.height(6.dp))
                FlowRowChips(r.teamTags.take(6).map { it to (it in r.userTopTags) })
                Spacer(Modifier.height(11.dp))
                WhyText(
                    "내가 제일 좋아하는 태그 ${topN}개 중 ${r.overlap}개가 이 팀에 있어요. " +
                            "횟수를 \"순위\"로만 쓰고 복잡한 가중치 계산은 버려, 화면에서 직접 셀 수 있어요."
                )
                Spacer(Modifier.height(10.dp))
                ScoreBox("겹친 ${r.overlap}개 ÷ Top ${topN}개 × 100", "${r.percent}점")
            }
        }
    }
}