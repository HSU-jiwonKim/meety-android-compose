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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bugzero.meety.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun PendingVerificationScreen(
    onCheckVerification: () -> Unit = {},
    onLogout: () -> Unit = {},
    authViewModel: AuthViewModel  // ← viewModel() 기본값 제거!
) {
    val verificationState by authViewModel.verificationCheckState.collectAsState()

    // ⭐ 10초마다 자동으로 인증 상태 확인
    LaunchedEffect(Unit) {
        while (true) {
            delay(10000) // 10초 대기
            android.util.Log.d("PENDING", "🔄 Auto-checking verification status...")
            onCheckVerification()
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
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(Color(0xFFF5F3FF), androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.HourglassEmpty,
                contentDescription = null,
                tint = Purple,
                modifier = Modifier.size(50.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

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

        // 안내 카드
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

        // 인증 확인하기 버튼
        Button(
            onClick = {
                android.util.Log.d("PENDING", "🔄 Manual check button clicked")
                onCheckVerification()
            },
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

        Spacer(modifier = Modifier.height(12.dp))

        // 로그아웃
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