package com.bugzero.meety.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bugzero.meety.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onLogout: () -> Unit = {},
    viewModel: AdminViewModel = viewModel()
) {
    val requests by viewModel.requests.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionState) {
        when (actionState) {
            is AdminActionState.Success -> {
                snackbarHostState.showSnackbar((actionState as AdminActionState.Success).message)
                viewModel.resetActionState()
            }
            is AdminActionState.Error -> {
                snackbarHostState.showSnackbar((actionState as AdminActionState.Error).message)
                viewModel.resetActionState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "관리자",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color.Transparent,
                        style = LocalTextStyle.current.copy(
                            brush = Brush.horizontalGradient(listOf(Purple, Color(0xFFF472B6)))
                        )
                    )
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "로그아웃", tint = Gray500)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        if (requests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF9FAFB))
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✅", fontSize = 56.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("대기 중인 인증 요청이 없습니다", fontSize = 16.sp, color = Gray500)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF9FAFB))
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "대기 중인 요청",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Gray900
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Purple, RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "${requests.size}",
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                items(requests) { request ->
                    AdminRequestCard(
                        request = request,
                        onApprove = {
                            viewModel.approveRequest(request.requestId, request.userId)
                        },
                        onReject = {
                            viewModel.rejectRequest(request.requestId, request.userId)
                        },
                        isLoading = actionState is AdminActionState.Loading
                    )
                }
            }
        }
    }
}

@Composable
fun AdminRequestCard(
    request: VerificationRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFA78BFA), Color(0xFFF472B6))),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        request.userName.ifEmpty { "이름 없음" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Gray900
                    )
                    Text(request.userEmail, fontSize = 13.sp, color = Gray500)
                }
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFFF7ED), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "대기 중",
                        fontSize = 11.sp,
                        color = Color(0xFFB45309),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 학생증 사진 영역 (Storage 연결 전)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color(0xFFF5F3FF), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        tint = Gray400,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Storage 연결 후 사진 표시",
                        fontSize = 12.sp,
                        color = Gray400,
                        textAlign = TextAlign.Center
                    )
                }
                // TODO: Storage 연결 후 아래로 교체
                // if (request.studentIdImageUrl.isNotEmpty()) {
                //     AsyncImage(model = request.studentIdImageUrl, ...)
                // }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onReject,
                    enabled = !isLoading,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEF4444)
                    )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("거절", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onApprove,
                    enabled = !isLoading,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Purple)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("승인", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}