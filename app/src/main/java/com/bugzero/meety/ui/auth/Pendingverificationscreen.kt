package com.bugzero.meety.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.bugzero.meety.R
import com.bugzero.meety.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun PendingVerificationScreen(
    onCheckVerification: () -> Unit = {},
    onLogout: () -> Unit = {},
    onReupload: () -> Unit = {},
    authViewModel: AuthViewModel
) {
    val verificationState by authViewModel.verificationCheckState.collectAsState()
    var isRejected by remember { mutableStateOf(false) }

    // Lottie 애니메이션
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.please_wait)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    LaunchedEffect(Unit) {
        while (true) {
            delay(10000)
            onCheckVerification()
        }
    }

    LaunchedEffect(Unit) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(userId)
                .get(com.google.firebase.firestore.Source.SERVER)
                .addOnSuccessListener { doc ->
                    val status = doc.getString("verificationStatus")
                    if (status == "rejected") {
                        isRejected = true
                    }
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isRejected) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFFFEE2E2), androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Cancel,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "인증이 거절되었어요",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Gray900
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "학생증 사진이 불명확하거나\n재학생 정보가 확인되지 않았어요",
                fontSize = 14.sp,
                color = Gray500,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoRow(Icons.Default.Info, "학생증 앞면이 선명하게 보여야 합니다")
                    InfoRow(Icons.Default.Info, "이름과 학번이 잘 보여야 합니다")
                    InfoRow(Icons.Default.Email, "문의: meety@hansung.ac.kr")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onReupload,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899))),
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Upload,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "학생증 다시 올리기",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

        } else {
            // Lottie 모래시계 애니메이션
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(180.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "인증 심사 중이에요",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Gray900
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "제출하신 학생증을 확인하고 있어요\n승인까지 최대 24시간 소요될 수 있어요",
                fontSize = 14.sp,
                color = Gray500,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoRow(Icons.Default.Schedule, "평일 오전 9시 ~ 오후 6시 처리")
                    InfoRow(Icons.Default.Email, "승인 시 가입 이메일로 알림 발송")
                    InfoRow(Icons.Default.Info, "문의: meety@hansung.ac.kr")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onCheckVerification() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Purple),
                enabled = verificationState !is VerificationCheckState.Loading
            ) {
                if (verificationState is VerificationCheckState.Loading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("인증 확인하기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onLogout) {
            Text("로그아웃", color = Gray500, fontSize = 14.sp)
        }
    }
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Purple, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, fontSize = 13.sp, color = Gray700)
    }
}