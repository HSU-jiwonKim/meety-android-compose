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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.bugzero.meety.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onLogout: () -> Unit = {},
    viewModel: AdminViewModel = viewModel()
) {
    val requests by viewModel.requests.collectAsState()
    val users by viewModel.users.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "인증 대기 ${if (requests.isNotEmpty()) "(${requests.size})" else ""}",
        "유저 목록",
        "신고 처리 ${if (reports.isNotEmpty()) "(${reports.size})" else ""}"
    )

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
            Column {
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
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = Purple
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> VerificationTab(
                requests = requests,
                actionState = actionState,
                padding = padding,
                onApprove = { requestId, userId -> viewModel.approveRequest(requestId, userId) },
                onReject = { requestId, userId -> viewModel.rejectRequest(requestId, userId) }
            )
            1 -> UserListTab(
                users = users,
                actionState = actionState,
                padding = padding,
                onBan = { userId -> viewModel.banUser(userId) },
                onUnban = { userId -> viewModel.unbanUser(userId) },
                onGrantAdmin = { userId -> viewModel.grantAdmin(userId) }
            )
            2 -> ReportTab(
                reports = reports,
                actionState = actionState,
                padding = padding,
                onResolve = { reportId, reportedId -> viewModel.resolveReport(reportId, reportedId, false) },
                onResolveAndBan = { reportId, reportedId -> viewModel.resolveReport(reportId, reportedId, true) }
            )
        }
    }
}

@Composable
fun VerificationTab(
    requests: List<VerificationRequest>,
    actionState: AdminActionState,
    padding: PaddingValues,
    onApprove: (String, String) -> Unit,
    onReject: (String, String) -> Unit
) {
    if (requests.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFFF9FAFB)).padding(padding),
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
            modifier = Modifier.fillMaxSize().background(Color(0xFFF9FAFB)).padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("대기 중인 요청", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Gray900)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.background(Purple, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("${requests.size}", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            items(requests) { request ->
                AdminRequestCard(
                    request = request,
                    onApprove = { onApprove(request.requestId, request.userId) },
                    onReject = { onReject(request.requestId, request.userId) },
                    isLoading = actionState is AdminActionState.Loading
                )
            }
        }
    }
}

@Composable
fun UserListTab(
    users: List<UserInfo>,
    actionState: AdminActionState,
    padding: PaddingValues,
    onBan: (String) -> Unit,
    onUnban: (String) -> Unit,
    onGrantAdmin: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredUsers = remember(users, searchQuery) {
        if (searchQuery.isEmpty()) users
        else users.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.email.contains(searchQuery, ignoreCase = true) ||
                    it.department.contains(searchQuery, ignoreCase = true)
        }
    }

    if (users.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFFF9FAFB)).padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text("가입한 유저가 없습니다", fontSize = 16.sp, color = Gray500)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(Color(0xFFF9FAFB)).padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("이름, 이메일, 학과 검색", color = Gray400) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Gray400)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Gray400)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Purple,
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (searchQuery.isEmpty()) "전체 유저 ${users.size}명"
                    else "검색 결과 ${filteredUsers.size}명",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Gray900
                )
            }
            items(filteredUsers) { user ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (user.isBanned) Color(0xFFFEF2F2) else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFFA78BFA), Color(0xFFF472B6)))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (user.profileImages.isNotEmpty()) {
                                AsyncImage(
                                    model = user.profileImages[0],
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    user.name.ifEmpty { "이름 없음" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Gray900
                                )
                                if (user.isBanned) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFFEE2E2), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("차단됨", fontSize = 10.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    if (user.isVerified) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFDCFCE7), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("인증됨", fontSize = 10.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (user.isAdmin) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFEDE9FE), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("관리자", fontSize = 10.sp, color = Purple, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            Text(user.email, fontSize = 12.sp, color = Gray500)
                            if (user.department.isNotEmpty()) {
                                Text(user.department, fontSize = 12.sp, color = Gray400)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // 관리자 권한 버튼 (관리자 아닌 유저만)
                            if (!user.isAdmin) {
                                OutlinedButton(
                                    onClick = { onGrantAdmin(user.userId) },
                                    enabled = actionState !is AdminActionState.Loading,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Purple),
                                    modifier = Modifier.height(36.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("관리자", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // 차단/해제 버튼
                            if (user.isBanned) {
                                OutlinedButton(
                                    onClick = { onUnban(user.userId) },
                                    enabled = actionState !is AdminActionState.Loading,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF16A34A)),
                                    modifier = Modifier.height(36.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("해제", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { onBan(user.userId) },
                                    enabled = actionState !is AdminActionState.Loading,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                    modifier = Modifier.height(36.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("차단", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportTab(
    reports: List<ReportInfo>,
    actionState: AdminActionState,
    padding: PaddingValues,
    onResolve: (String, String) -> Unit,
    onResolveAndBan: (String, String) -> Unit
) {
    if (reports.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFFF9FAFB)).padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎉", fontSize = 56.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("처리할 신고가 없습니다", fontSize = 16.sp, color = Gray500)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(Color(0xFFF9FAFB)).padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("신고 목록", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Gray900)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.background(Color(0xFFEF4444), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("${reports.size}", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            items(reports) { report ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Flag, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("신고 대상: ${report.reportedName.ifEmpty { "알 수 없음" }}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Gray900)
                                Text("신고자: ${report.reporterName.ifEmpty { "알 수 없음" }}", fontSize = 12.sp, color = Gray500)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth().background(Color(0xFFF9FAFB), RoundedCornerShape(10.dp)).padding(12.dp)
                        ) {
                            Text("신고 사유: ${report.reason.ifEmpty { "사유 없음" }}", fontSize = 13.sp, color = Gray700)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onResolve(report.reportId, report.reportedId) },
                                enabled = actionState !is AdminActionState.Loading,
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Gray500)
                            ) {
                                Text("무시", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { onResolveAndBan(report.reportId, report.reportedId) },
                                enabled = actionState !is AdminActionState.Loading,
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                            ) {
                                Text("차단 처리", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
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
                    modifier = Modifier.size(48.dp).background(
                        Brush.linearGradient(listOf(Color(0xFFA78BFA), Color(0xFFF472B6))), CircleShape
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(request.userName.ifEmpty { "이름 없음" }, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Gray900)
                    Text(request.userEmail, fontSize = 13.sp, color = Gray500)
                }
                Box(
                    modifier = Modifier.background(Color(0xFFFFF7ED), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("대기 중", fontSize = 11.sp, color = Color(0xFFB45309), fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF5F3FF)),
                contentAlignment = Alignment.Center
            ) {
                if (request.studentIdImageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = request.studentIdImageUrl,
                        contentDescription = "학생증 사진",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = Gray400, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("사진 없음", fontSize = 12.sp, color = Gray400)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onReject,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("거절", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onApprove,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Purple)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("승인", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}