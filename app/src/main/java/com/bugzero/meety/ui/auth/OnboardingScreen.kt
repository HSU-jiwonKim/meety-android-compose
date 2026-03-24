package com.bugzero.meety.ui.auth

<<<<<<< HEAD
import androidx.compose.runtime.Composable

@Composable
fun OnboardingScreen() {
    // TODO: A 담당 - 회원가입 · 프로필 초기 설정 화면 구현
}
=======
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class OnboardingSlide(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val gradientStart: Color,
    val gradientEnd: Color
)

val onboardingSlides = listOf(
    OnboardingSlide("안전한 대학 인증", "한성대 이메일 인증으로\n신뢰할 수 있는 만남을 시작하세요",
        Icons.Default.Shield, Color(0xFF8B5CF6), Color(0xFF6D28D9)),
    OnboardingSlide("친구들과 함께", "N:N 팀 매칭으로\n친구들과 함께하는 즐거운 과팅",
        Icons.Default.Group, Color(0xFF9333EA), Color(0xFFEC4899)),
    OnboardingSlide("똑똑한 매칭", "MBTI, 관심사 기반으로\n나와 잘 맞는 팀을 찾아보세요",
        Icons.Default.Favorite, Color(0xFFEC4899), Color(0xFFBE185D)),
    OnboardingSlide("간편한 일정 조율", "모두가 가능한 날짜를\n자동으로 찾아드려요",
        Icons.Default.CalendarToday, Color(0xFF7C3AED), Color(0xFF4C1D95)),
)

@Composable
fun OnboardingScreen(
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit
) {
    var currentSlide by remember { mutableStateOf(0) }
    val slide = onboardingSlides[currentSlide]

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Color(0xFFF5F3FF))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 헤더
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Meety 💜",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Transparent,
                    style = LocalTextStyle.current.copy(
                        brush = Brush.horizontalGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899)))
                    )
                )
                TextButton(onClick = onLoginClick) {
                    Text("건너뛰기", color = Color(0xFF6B7280), fontWeight = FontWeight.Medium, fontSize = 15.sp)
                }
            }

            // 슬라이드 컨텐츠
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AnimatedContent(
                    targetState = currentSlide,
                    transitionSpec = {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    },
                    label = "slideContent"
                ) { index ->
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // 아이콘 박스
                        val scale = remember { Animatable(0f) }
                        val rotation = remember { Animatable(-180f) }
                        // 수정 - 이걸로 교체
                        LaunchedEffect(Unit) {
                            launch {
                                scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                            }
                            launch {
                                rotation.animateTo(0f, tween(500))
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .scale(scale.value)
                                .rotate(rotation.value)
                                .clip(RoundedCornerShape(36.dp))
                                .background(Brush.linearGradient(listOf(onboardingSlides[index].gradientStart, onboardingSlides[index].gradientEnd))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(onboardingSlides[index].icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(80.dp))
                        }

                        Spacer(modifier = Modifier.height(48.dp))

                        Text(
                            onboardingSlides[index].title,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = Color.Transparent,
                            style = LocalTextStyle.current.copy(
                                brush = Brush.horizontalGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899)))
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            onboardingSlides[index].description,
                            fontSize = 18.sp,
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center,
                            lineHeight = 28.sp
                        )
                    }
                }
            }

            // 하단 네비게이션
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 도트 인디케이터
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    onboardingSlides.forEachIndexed { index, _ ->
                        val width by animateDpAsState(
                            targetValue = if (index == currentSlide) 40.dp else 10.dp,
                            animationSpec = tween(300), label = "dot"
                        )
                        Box(
                            modifier = Modifier
                                .height(10.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(
                                    if (index == currentSlide)
                                        Brush.horizontalGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899)))
                                    else Brush.horizontalGradient(listOf(Color(0xFFD1D5DB), Color(0xFFD1D5DB)))
                                )
                                .clickable { currentSlide = index }
                        )
                    }
                }

                // 다음/시작 버튼
                Button(
                    onClick = {
                        if (currentSlide < onboardingSlides.size - 1) currentSlide++
                        else onSignUpClick()
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                    elevation = ButtonDefaults.buttonElevation(8.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(Brush.horizontalGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899))), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (currentSlide == onboardingSlides.size - 1) "시작하기 🚀" else "다음",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (currentSlide < onboardingSlides.size - 1) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
>>>>>>> ca178ecdf5d6d6fc1948225597641ad0fce1c061
