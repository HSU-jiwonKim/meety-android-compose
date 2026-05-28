package com.bugzero.meety.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugzero.meety.ui.theme.Brand1
import com.bugzero.meety.ui.theme.Brand2
import com.bugzero.meety.ui.theme.GradSoftEnd
import com.bugzero.meety.ui.theme.GradSoftStart
import com.bugzero.meety.ui.theme.Ink
import com.bugzero.meety.ui.theme.Ink2
import com.bugzero.meety.ui.theme.Ink3
import com.bugzero.meety.ui.theme.Ink4
import com.bugzero.meety.ui.theme.Line
import com.bugzero.meety.ui.theme.Line2
import com.bugzero.meety.ui.theme.MeetyBg
import com.bugzero.meety.ui.theme.MeetyGradient
import com.bugzero.meety.ui.theme.MeetySurface
import com.bugzero.meety.ui.theme.PinkSoft
import com.bugzero.meety.ui.theme.VioletSoft
import com.bugzero.meety.ui.theme.VioletText
import kotlinx.coroutines.delay

/**
 * 회원가입 단계의 밸런스 게임 화면.
 *
 * meety-balancegame-mockup.html 의 UI 를 그대로 옮긴 화면.
 * 인트로 → 6문항(양자택일) → 결과 요약 순으로 진행된다.
 *
 * 본 파일은 UI 렌더링만 담당한다. 답변 저장 / 추천 매칭 등 비즈니스 로직은
 * 별도 담당자가 추후 [BALANCE_QUESTIONS] 데이터와 onComplete 콜백을 활용해 연결할 예정.
 */
@Composable
fun BalanceGameScreen(
    onComplete: () -> Unit = {},
    onSkip: () -> Unit = {}
) {
    val total = BALANCE_QUESTIONS.size
    // step: -1 = intro, 0..total-1 = question, total = done
    var step by remember { mutableIntStateOf(-1) }
    val answers = remember {
        mutableStateListOf<String?>().apply { repeat(total) { add(null) } }
    }
    // 탭한 순간 잠시 하이라이트만 보여주고 다음으로 넘어가기 위한 임시 상태
    var pickedSide by remember { mutableStateOf<String?>(null) }

    // 진행률 계산
    val progressFraction = when {
        step < 0 -> 0f
        step >= total -> 1f
        else -> step.toFloat() / total
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(350),
        label = "balance_progress"
    )

    fun choose(side: String) {
        if (pickedSide != null) return
        pickedSide = side
    }

    // 선택 후 360ms 동안 하이라이트만 보여주고 다음 문항으로
    LaunchedEffect(pickedSide, step) {
        val side = pickedSide
        if (side != null && step in 0 until total) {
            delay(360)
            answers[step] = side
            pickedSide = null
            step += 1
        }
    }

    Scaffold(containerColor = MeetyBg) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── 상단 바 ──
            TopOnboardingBar(
                showBack = step > -1 && step < total,
                showSkip = step < total,
                onBack = {
                    if (pickedSide != null) return@TopOnboardingBar
                    if (step > -1) step -= 1
                },
                onSkip = onSkip
            )

            // ── 진행 바 ──
            Box(
                modifier = Modifier
                    .padding(horizontal = 18.dp)
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Line2)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(MeetyGradient)
                )
            }

            // ── 본문 (인트로 / 문항 / 결과) ──
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 12 })
                        .togetherWith(fadeOut(tween(180)))
                },
                label = "balance_step",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { currentStep ->
                when {
                    currentStep < 0 -> IntroView(onStart = { step = 0 })
                    currentStep >= total -> DoneView(
                        answers = answers.toList(),
                        onFinish = onComplete
                    )
                    else -> QuestionView(
                        index = currentStep,
                        total = total,
                        pickedSide = pickedSide,
                        onPick = ::choose
                    )
                }
            }
        }
    }
}

/* ────────────────────────────────────────────────────────────────────────── */
/* Top bar                                                                    */
/* ────────────────────────────────────────────────────────────────────────── */

@Composable
private fun TopOnboardingBar(
    showBack: Boolean,
    showSkip: Boolean,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 왼쪽 뒤로가기 (자리는 항상 차지하도록 placeholder)
        Box(modifier = Modifier.size(34.dp)) {
            if (showBack) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(11.dp))
                        .background(MeetySurface)
                        .border(1.dp, Line, RoundedCornerShape(11.dp))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "이전",
                        tint = Ink,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 가운데 단계 표시
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Ink3, fontWeight = FontWeight.Bold)) {
                        append("프로필 ✓ · ")
                    }
                    withStyle(SpanStyle(color = Brand1, fontWeight = FontWeight.Bold)) {
                        append("밸런스 게임")
                    }
                    withStyle(SpanStyle(color = Ink3, fontWeight = FontWeight.Bold)) {
                        append(" · 인증")
                    }
                },
                fontSize = 11.sp
            )
        }

        // 건너뛰기 (없으면 좌측 back 버튼과 폭을 맞추기 위해 placeholder)
        if (showSkip) {
            Text(
                text = "건너뛰기",
                color = Ink3,
                fontSize = 12.5f.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onSkip() }
            )
        } else {
            Spacer(Modifier.width(34.dp))
        }
    }
}

/* ────────────────────────────────────────────────────────────────────────── */
/* Intro                                                                      */
/* ────────────────────────────────────────────────────────────────────────── */

@Composable
private fun IntroView(onStart: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .graphicsLayer { rotationZ = -4f }
                    .shadow(18.dp, RoundedCornerShape(36.dp))
                    .clip(RoundedCornerShape(36.dp))
                    .background(MeetyGradient),
                contentAlignment = Alignment.Center
            ) {
                Text("🎲", fontSize = 60.sp)
            }
            Spacer(Modifier.height(26.dp))
            Text(
                "밸런스 게임으로\n시작해요",
                fontSize = 25.sp,
                lineHeight = 33.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Ink,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "둘 중 더 끌리는 쪽을 고르면 돼요.\n정답은 없어요 — 회원님 취향이 정답이에요!",
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = Ink2,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Brand1,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    "6문항 · 약 30초",
                    fontSize = 12.5f.sp,
                    color = Ink3,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        CtaButton(
            text = "시작하기",
            onClick = onStart
        )
    }
}

/* ────────────────────────────────────────────────────────────────────────── */
/* Question                                                                   */
/* ────────────────────────────────────────────────────────────────────────── */

@Composable
private fun QuestionView(
    index: Int,
    total: Int,
    pickedSide: String?,
    onPick: (String) -> Unit
) {
    val question = BALANCE_QUESTIONS[index]
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Q${index + 1} / $total",
            color = Brand1,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = question.question,
            color = Ink,
            fontSize = 21.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // 두 카드 + 가운데 VS
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 10.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OptionCard(
                    option = question.optionA,
                    isViolet = true,
                    selected = pickedSide == "a",
                    onClick = { onPick("a") }
                )
                OptionCard(
                    option = question.optionB,
                    isViolet = false,
                    selected = pickedSide == "b",
                    onClick = { onPick("b") }
                )
            }
            // VS 뱃지
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(46.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Line, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "VS",
                    color = Brand1,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Text(
            "더 끌리는 쪽을 탭하세요",
            color = Ink4,
            fontSize = 11.5f.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        )
    }
}

@Composable
private fun ColumnScope.OptionCard(
    option: BalanceOption,
    isViolet: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accent = if (isViolet) Brand1 else Brand2
    val bg = if (isViolet) VioletSoft else PinkSoft
    val borderColor = if (selected) accent else Color.Transparent
    val elevation by animateDpAsState(
        targetValue = if (selected) 14.dp else 0.dp,
        label = "card_elevation"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.01f else 1f,
        label = "card_scale"
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(elevation, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .border(2.dp, borderColor, RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Text(option.emoji, fontSize = 42.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    option.label,
                    color = Ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    option.subtitle,
                    color = Ink2,
                    fontSize = 12.5f.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 우상단 픽 배지
        PickBadge(
            selected = selected,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 14.dp, end = 16.dp)
        )
    }
}

@Composable
private fun PickBadge(selected: Boolean, modifier: Modifier = Modifier) {
    if (selected) {
        Box(
            modifier = modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MeetyGradient),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    } else {
        Box(
            modifier = modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(2.dp, Ink4, CircleShape)
        )
    }
}

/* ────────────────────────────────────────────────────────────────────────── */
/* Done                                                                       */
/* ────────────────────────────────────────────────────────────────────────── */

@Composable
private fun DoneView(
    answers: List<String?>,
    onFinish: () -> Unit
) {
    // 안전한 fallback: 미선택이면 a 로 간주
    val safeAnswers = answers.map { it ?: "a" }
    val chips = safeAnswers.mapIndexed { i, side ->
        val q = BALANCE_QUESTIONS[i]
        if (side == "a") q.optionA.tag else q.optionB.tag
    }
    // 타입 라벨: 1번(친목/목표) + 5번(차분/활발)
    val typeLeft = if (safeAnswers.getOrNull(0) == "a") "친목" else "목표"
    val typeRight = if (safeAnswers.getOrNull(4) == "b") "활발" else "차분"
    val typeLabel = "${typeLeft}·${typeRight}형"

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(14.dp))
            // 큰 체크 링
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(GradSoftStart, GradSoftEnd))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .shadow(10.dp, CircleShape)
                        .clip(CircleShape)
                        .background(MeetyGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "취향 분석 끝!",
                color = Ink,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "\"$typeLabel\" 스타일이네요",
                color = Brand1,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(18.dp))

            // 칩 행
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chips.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(VioletSoft)
                            .padding(horizontal = 13.dp, vertical = 7.dp)
                    ) {
                        Text(
                            tag,
                            color = VioletText,
                            fontSize = 12.5f.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // 정보 박스
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(MeetySurface)
                    .border(1.dp, Line2, RoundedCornerShape(16.dp))
                    .padding(15.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Brand1,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "이 답변은 이렇게 쓰여요",
                        color = Ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    buildAnnotatedString {
                        append("인증이 끝나면 피드에서, 회원님 답변과 ")
                        withStyle(SpanStyle(color = Ink, fontWeight = FontWeight.Bold)) {
                            append("팀원들의 답변을 모은 팀 가치관")
                        }
                        append("을 비교해 \"가치관 일치\" 근거로 보여드려요. (예: 5문항 중 4개 일치)")
                    },
                    color = Ink2,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }

            Spacer(Modifier.height(20.dp))
        }

        CtaButton(
            text = "학생증 인증하러 가기",
            trailingArrow = true,
            onClick = onFinish
        )
    }
}

/* ────────────────────────────────────────────────────────────────────────── */
/* CTA                                                                        */
/* ────────────────────────────────────────────────────────────────────────── */

@Composable
private fun CtaButton(
    text: String,
    trailingArrow: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(MeetyGradient)
                .clickable { onClick() }
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
            if (trailingArrow) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
