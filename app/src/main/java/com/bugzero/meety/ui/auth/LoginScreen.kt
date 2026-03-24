package com.bugzero.meety.ui.auth

<<<<<<< HEAD
import androidx.compose.runtime.Composable

@Composable
fun LoginScreen() {
    // TODO: A 담당 - 로그인 화면 구현
=======
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
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onSignUpClick: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }

    val authState by viewModel.authState.collectAsState()
    val passwordResetState by viewModel.passwordResetState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) onLoginSuccess()
    }

    val logoScale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        logoScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }

    // 비밀번호 찾기 다이얼로그
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = {
                showResetDialog = false
                resetEmail = ""
                viewModel.resetPasswordResetState()
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "비밀번호 찾기",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Gray900
                    )
                    IconButton(
                        onClick = {
                            showResetDialog = false
                            resetEmail = ""
                            viewModel.resetPasswordResetState()
                        }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "닫기", tint = Gray500)
                    }
                }
            },
            text = {
                Column {
                    Text(
                        "가입한 한성대 이메일을 입력하면\n비밀번호 재설정 링크를 보내드려요",
                        fontSize = 14.sp,
                        color = Gray500,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("name@hansung.ac.kr", color = Gray400) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = Gray400)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Purple,
                            unfocusedContainerColor = Color(0xFFF9FAFB),
                            focusedContainerColor = Color(0xFFF9FAFB)
                        )
                    )
                    when (passwordResetState) {
                        is PasswordResetState.Error -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                (passwordResetState as PasswordResetState.Error).message,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp
                            )
                        }
                        is PasswordResetState.Success -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "✅ 이메일을 전송했습니다\n받은 메일함을 확인해주세요",
                                color = Color(0xFF22C55E),
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            )
                        }
                        else -> {}
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.sendPasswordResetEmail(resetEmail) },
                    enabled = passwordResetState !is PasswordResetState.Loading,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Purple)
                ) {
                    if (passwordResetState is PasswordResetState.Loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("전송", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        resetEmail = ""
                        viewModel.resetPasswordResetState()
                    }
                ) {
                    Text("취소", color = Gray500)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899), Color(0xFF7C3AED))))
    ) {
        Box(
            modifier = Modifier.size(280.dp).offset(x = (-40).dp, y = 80.dp)
                .background(Color(0x4DF9A8D4), androidx.compose.foundation.shape.CircleShape)
        )
        Box(
            modifier = Modifier.size(380.dp).align(Alignment.BottomEnd).offset(x = 40.dp, y = 80.dp)
                .background(Color(0x4DC084FC), androidx.compose.foundation.shape.CircleShape)
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .scale(logoScale.value)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))),
                                RoundedCornerShape(24.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Meety 💜",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Transparent,
                        style = LocalTextStyle.current.copy(
                            brush = Brush.horizontalGradient(listOf(Purple, Color(0xFFF472B6)))
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("다시 만나서 반가워요!", fontSize = 16.sp, color = Color(0xFF6B7280))

                    Spacer(modifier = Modifier.height(32.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("이메일", fontSize = 14.sp, color = Gray700, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("name@hansung.ac.kr", color = Gray400) },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = Gray400)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Purple,
                                unfocusedContainerColor = Color(0xFFF9FAFB),
                                focusedContainerColor = Color(0xFFF9FAFB)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("비밀번호", fontSize = 14.sp, color = Gray700, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("비밀번호를 입력하세요", color = Gray400) },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Gray400)
                            },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = Gray400
                                    )
                                }
                            },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Purple,
                                unfocusedContainerColor = Color(0xFFF9FAFB),
                                focusedContainerColor = Color(0xFFF9FAFB)
                            )
                        )
                    }

                    if (authState is AuthState.Error) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            (authState as AuthState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(
                            onClick = {
                                resetEmail = email
                                viewModel.resetPasswordResetState()
                                showResetDialog = true
                            }
                        ) {
                            Text(
                                "비밀번호를 잊으셨나요?",
                                color = Purple,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.login(email, password) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                        enabled = authState !is AuthState.Loading
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(listOf(Purple, Color(0xFFF472B6))),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (authState is AuthState.Loading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("로그인 ✨", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("아직 계정이 없으신가요? ", color = Gray500, fontSize = 14.sp)
                        TextButton(onClick = onSignUpClick, contentPadding = PaddingValues(0.dp)) {
                            Text("회원가입", color = Purple, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "🎓 한성대학교 학생만 가입 가능합니다",
                fontSize = 13.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
>>>>>>> ca178ecdf5d6d6fc1948225597641ad0fce1c061
}