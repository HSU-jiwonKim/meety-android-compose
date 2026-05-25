package com.bugzero.meety.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bugzero.meety.ui.theme.Gray400
import com.bugzero.meety.ui.theme.Gray500
import com.bugzero.meety.ui.theme.Gray700
import com.bugzero.meety.ui.theme.Gray900
import com.bugzero.meety.ui.theme.Purple

// 패널 그라데이션 (왼쪽 = 보라, 오른쪽 = 핑크)
private val LeftGradient = Brush.verticalGradient(
    listOf(Color(0xFF9F7AEA), Color(0xFF7C3AED), Color(0xFF6D28D9))
)
private val RightGradient = Brush.verticalGradient(
    listOf(Color(0xFFF472B6), Color(0xFFEC4899), Color(0xFFBE185D))
)

/**
 * 회원가입 단계의 밸런스 게임 화면 (풀스크린 분할 UI).
 *
 * 인트로 → 16문항(화면을 꽉 채우는 두 패널 + 가운데 VS) → 마지막 답변 시 저장 → onComplete.
 * 답변은 axis별 -1/+1 로 모아 users/{uid}.balanceProfile 에 저장된다.
 */
@Composable
fun BalanceGameScreen(
    onComplete: () -> Unit = {},
    onSkip: () -> Unit = {},
    viewModel: AuthViewModel = viewModel()
) {
    var started by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableIntStateOf(0) }
    val answers = remember { mutableStateMapOf<String, Int>() }

    val total = BALANCE_QUESTIONS.size
    val saveState by viewModel.balanceSaveState.collectAsState()
    val isSaving = saveState is BalanceSaveState.Loading

    LaunchedEffect(saveState) {
        if (saveState is BalanceSaveState.Success) {
            viewModel.resetBalanceSaveState()
            onComplete()
        }
    }

    fun selectAnswer(value: Int) {
        if (isSaving) return
        val q = BALANCE_QUESTIONS[currentIndex]
        answers[q.axis] = value
        if (currentIndex < total - 1) {
            currentIndex++
        } else {
            viewModel.saveBalanceProfile(answers.toMap())
        }
    }

    Scaffold(containerColor = Color(0xFFF9FAFB)) { padding ->
        if (!started) {
            BalanceIntro(
                modifier = Modifier.padding(padding),
                onStart = { started = true },
                onSkip = onSkip
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // ── 상단 바: 이전 / 진행률 / 건너뛰기 ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (currentIndex > 0) currentIndex-- },
                    enabled = currentIndex > 0 && !isSaving
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "이전 질문",
                        tint = if (currentIndex > 0) Gray700 else Color.Transparent
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "${currentIndex + 1} / $total",
                    color = Gray700,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onSkip, enabled = !isSaving) {
                    Text("건너뛰기", color = Gray400, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── 진행 바 (그라데이션) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEDE9FE))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((currentIndex + 1).toFloat() / total)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)))
                        )
                )
            }

            // ── 질문 + 패널 (전환 애니메이션) ──
            AnimatedContent(
                targetState = currentIndex,
                transitionSpec = {
                    (slideInHorizontally(tween(280)) { it } + fadeIn(tween(280))) togetherWith
                            (slideOutHorizontally(tween(280)) { -it } + fadeOut(tween(180)))
                },
                label = "question_transition",
                modifier = Modifier.weight(1f)
            ) { index ->
                val q = BALANCE_QUESTIONS[index]
                Column(modifier = Modifier.fillMaxSize()) {
                    // 질문 헤더
                    Spacer(Modifier.height(20.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFEDE9FE))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "${q.emoji}  ${q.category}",
                                color = Purple,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            q.question,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Gray900,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // 두 패널 + 가운데 VS 뱃지
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            ChoicePanel(
                                label = q.leftLabel,
                                gradient = LeftGradient,
                                enabled = !isSaving,
                                onClick = { selectAnswer(q.leftValue) }
                            )
                            ChoicePanel(
                                label = q.rightLabel,
                                gradient = RightGradient,
                                enabled = !isSaving,
                                onClick = { selectAnswer(q.rightValue) }
                            )
                        }
                        // 가운데 VS 뱃지
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(58.dp)
                                .shadow(8.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(3.dp, Color(0xFFF3F4F6), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "VS",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Purple
                            )
                        }
                    }
                }
            }

            // ── 저장 중 / 에러 표시 ──
            when (val s = saveState) {
                is BalanceSaveState.Loading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = Purple, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("결과 저장 중...", color = Gray500, fontSize = 14.sp)
                    }
                }
                is BalanceSaveState.Error -> {
                    Text(
                        s.message,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun ColumnScope.ChoicePanel(
    label: String,
    gradient: Brush,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        label = "panel_scale"
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(12.dp, RoundedCornerShape(30.dp), clip = false)
            .clip(RoundedCornerShape(30.dp))
            .background(gradient)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled
            ) { onClick() }
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BalanceIntro(
    modifier: Modifier = Modifier,
    onStart: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .shadow(16.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("⚖️", fontSize = 52.sp)
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "밸런스 게임",
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Gray900
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "둘 중 하나만 골라주세요!\n나와 잘 맞는 동아리·팀을 찾는 데 쓰여요.",
            fontSize = 15.sp,
            color = Gray500,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFEDE9FE))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                "총 ${BALANCE_QUESTIONS.size}문항 · 1분이면 충분해요",
                fontSize = 13.sp,
                color = Purple,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(36.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Purple)
        ) {
            Text("시작하기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onSkip) {
            Text("나중에 할게요", color = Gray400, fontSize = 13.sp)
        }
    }
}
