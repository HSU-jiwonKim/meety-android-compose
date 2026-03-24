package com.bugzero.meety.ui.auth

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.bugzero.meety.ui.theme.*

@Composable
fun StudentIdUploadScreen(
    onUploadSuccess: () -> Unit = {},
    viewModel: AuthViewModel = viewModel()
) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val uploadState by viewModel.uploadState.collectAsState()

    // 업로드 성공 시 다음 화면으로
    LaunchedEffect(uploadState) {
        if (uploadState is UploadState.Success) {
            viewModel.resetUploadState()
            onUploadSuccess()
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) selectedImageUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text("학생증 인증", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Gray900)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "한성대학교 재학생임을 인증해주세요\n학생증 사진을 업로드하면 됩니다",
            fontSize = 14.sp, color = Gray500, textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(40.dp))

        // 미리보기 영역
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (selectedImageUri != null) Color.Transparent else Gray100)
                .border(
                    2.dp,
                    if (selectedImageUri != null) Color(0xFF22C55E) else Gray200,
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selectedImageUri != null) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Gray400,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "아래 버튼으로 사진을 선택해주세요",
                        color = Gray500,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 선택 완료 표시
        if (selectedImageUri != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "사진이 선택되었습니다",
                    color = Color(0xFF22C55E),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 에러 메시지
        if (uploadState is UploadState.Error) {
            Text(
                (uploadState as UploadState.Error).message,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 사진 선택 버튼
        OutlinedButton(
            onClick = { imagePicker.launch("image/*") },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Upload, contentDescription = null, tint = Purple)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (selectedImageUri != null) "사진 다시 선택하기" else "사진 선택하기",
                color = Purple,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 안내사항
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("📌 안내사항", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFB45309))
                Spacer(modifier = Modifier.height(4.dp))
                listOf(
                    "학생증 앞면이 선명하게 보여야 합니다",
                    "개인정보는 인증 목적으로만 사용됩니다",
                    "검토까지 최대 24시간 소요될 수 있습니다"
                ).forEach { text ->
                    Text("• $text", fontSize = 12.sp, color = Color(0xFF78350F), lineHeight = 20.sp)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 인증 요청 버튼
        Button(
            onClick = {
                selectedImageUri?.let { uri ->
                    viewModel.requestStudentIdVerification(uri)
                }
            },
            enabled = selectedImageUri != null && uploadState !is UploadState.Loading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Purple)
        ) {
            if (uploadState is UploadState.Loading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("인증 요청하기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}