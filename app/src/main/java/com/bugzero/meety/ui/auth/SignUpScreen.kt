package com.bugzero.meety.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bugzero.meety.ui.theme.*

@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    viewModel: AuthViewModel = viewModel()
) {
    var currentStep by remember { mutableIntStateOf(1) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val signUpState by viewModel.signUpState.collectAsState()
    val emailVerificationState by viewModel.emailVerificationState.collectAsState()

    // 회원가입 성공 시 Step2로 이동
    LaunchedEffect(signUpState) {
        if (signUpState is SignUpState.Success) {
            currentStep = 2
            viewModel.resetSignUpState()
        }
    }

    // 이메일 인증 완료 시 Step3으로 이동
    LaunchedEffect(emailVerificationState) {
        if (emailVerificationState is EmailVerificationState.Verified) {
            currentStep = 3
            viewModel.resetEmailVerificationState()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF9333EA), Color(0xFFEC4899)))),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(24.dp)
        ) {
            Column(modifier = Modifier.padding(28.dp)) {
                // 헤더
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currentStep > 1) {
                        IconButton(onClick = { currentStep-- }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(3) { step ->
                                Box(
                                    modifier = Modifier.weight(1f).height(4.dp)
                                        .background(
                                            if (step + 1 <= currentStep) Purple else Color(0xFFE5E7EB),
                                            RoundedCornerShape(2.dp)
                                        )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            when (currentStep) {
                                1 -> "기본 정보"
                                2 -> "이메일 인증"
                                else -> "가입 완료"
                            },
                            fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Gray900
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    },
                    label = "stepContent"
                ) { step ->
                    when (step) {
                        1 -> SignUpStep1(
                            name = name, onNameChange = { name = it },
                            email = email, onEmailChange = { email = it },
                            password = password, onPasswordChange = { password = it },
                            confirmPassword = confirmPassword, onConfirmChange = { confirmPassword = it },
                            showPassword = showPassword, onTogglePassword = { showPassword = !showPassword },
                            signUpState = signUpState,
                            onNext = { viewModel.signUp(email, password, name) }
                        )
                        2 -> SignUpStep2(
                            email = email,
                            verificationState = emailVerificationState,
                            onVerifyClick = { viewModel.checkEmailVerified() },
                            onResendClick = { viewModel.resendVerificationEmail() }
                        )
                        else -> SignUpStep3(onComplete = onSignUpSuccess)
                    }
                }

                if (currentStep == 1) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("이미 계정이 있으신가요? ", color = Gray500, fontSize = 14.sp)
                        TextButton(onClick = onLoginClick, contentPadding = PaddingValues(0.dp)) {
                            Text("로그인", color = Purple, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SignUpStep1(
    name: String, onNameChange: (String) -> Unit,
    email: String, onEmailChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    confirmPassword: String, onConfirmChange: (String) -> Unit,
    showPassword: Boolean, onTogglePassword: () -> Unit,
    signUpState: SignUpState,
    onNext: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            Text("이름", fontSize = 14.sp, color = Gray700, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = name, onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("홍길동", color = Gray400) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Gray400) },
                shape = RoundedCornerShape(12.dp), singleLine = true
            )
        }
        Column {
            Text("한성대 이메일", fontSize = 14.sp, color = Gray700, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = email, onValueChange = onEmailChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("name@hansung.ac.kr", color = Gray400) },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Gray400) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(12.dp), singleLine = true
            )
            Text("@hansung.ac.kr 이메일만 가입 가능합니다", fontSize = 12.sp, color = Gray400)
        }
        Column {
            Text("비밀번호", fontSize = 14.sp, color = Gray700, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = password, onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("8자 이상 입력하세요", color = Gray400) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Gray400) },
                trailingIcon = {
                    IconButton(onClick = onTogglePassword) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null, tint = Gray400
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(12.dp), singleLine = true
            )
        }
        Column {
            Text("비밀번호 확인", fontSize = 14.sp, color = Gray700, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = confirmPassword, onValueChange = onConfirmChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("비밀번호를 다시 입력하세요", color = Gray400) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Gray400) },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(12.dp), singleLine = true
            )
        }

        // 에러 메시지
        if (signUpState is SignUpState.Error) {
            Text(
                (signUpState as SignUpState.Error).message,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp
            )
        }

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Purple),
            enabled = signUpState !is SignUpState.Loading
        ) {
            if (signUpState is SignUpState.Loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Text("다음", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun SignUpStep2(
    email: String,
    verificationState: EmailVerificationState,
    onVerifyClick: () -> Unit,
    onResendClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(72.dp)
                .background(Color(0xFFEDE9FE), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Email, contentDescription = null, tint = Purple, modifier = Modifier.size(36.dp))
        }

        Text(
            "${email}로\n인증 링크를 전송했습니다",
            fontSize = 14.sp, color = Gray500,
            textAlign = TextAlign.Center
        )

        Text(
            "받은 메일함에서 인증 링크를 클릭한 후\n아래 버튼을 눌러주세요",
            fontSize = 13.sp, color = Gray400,
            textAlign = TextAlign.Center
        )

        when (verificationState) {
            is EmailVerificationState.NotVerified -> {
                Text(
                    "아직 인증이 완료되지 않았습니다\n메일함을 확인해주세요",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
            is EmailVerificationState.EmailSent -> {
                Text("인증 이메일을 재전송했습니다 ✓", color = Purple, fontSize = 13.sp)
            }
            is EmailVerificationState.Error -> {
                Text(verificationState.message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
            else -> {}
        }

        TextButton(onClick = onResendClick) {
            Text("인증 이메일 재전송", color = Purple, fontSize = 13.sp)
        }

        Button(
            onClick = onVerifyClick,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Purple),
            enabled = verificationState !is EmailVerificationState.Loading
        ) {
            if (verificationState is EmailVerificationState.Loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Text("인증 완료했어요 ✓", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun SignUpStep3(onComplete: () -> Unit) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200)
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(96.dp)
                .background(Color(0xFFEDE9FE), androidx.compose.foundation.shape.CircleShape)
                .scale(scale.value),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Favorite, contentDescription = null,
                tint = Purple, modifier = Modifier.size(48.dp)
            )
        }
        Text("환영합니다!", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Gray900)
        Text(
            "이메일 인증이 완료되었습니다\n프로필을 설정하고 Meety를 시작해보세요",
            fontSize = 15.sp, color = Gray500,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Purple)
        ) {
            Text("프로필 설정하기", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}