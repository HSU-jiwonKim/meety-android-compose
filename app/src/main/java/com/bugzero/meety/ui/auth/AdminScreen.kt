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
    val actionState by viewModel.actionState.collectAsState()
    val demoUsers by viewModel.demoUsers.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "인증 대기 ${if (requests.isNotEmpty()) "(${requests.size})" else ""}",
        "유저 목록",
        "데모 관리"
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
            2 -> DemoManagementTab(
                demoUsers = demoUsers,
                nonDummyTeams = viewModel.nonDummyTeams.collectAsState().value,
                nonDummyDirectChats = viewModel.nonDummyDirectChats.collectAsState().value,
                pendingDummyLikes = viewModel.pendingDummyLikes.collectAsState().value,
                autoAcceptEnabled = viewModel.autoAcceptEnabled.collectAsState().value,
                onToggleAutoAccept = { viewModel.toggleAutoAccept() },
                actionState = actionState,
                padding = padding,
                onResetUser = { userId -> viewModel.resetUserDemoData(userId) },
                onDeleteTeam = { teamId -> viewModel.deleteTeam(teamId) },
                onDeleteAllNonDummyTeams = { viewModel.deleteAllNonDummyTeams() },
                onAcceptLike = { likeId, fromUserId, toTeamId, toTeamName ->
                    viewModel.acceptLike(likeId, fromUserId, toTeamId, toTeamName)
                }
            )
        }
    }
}

// ═══════════════════════════════════════
// 데모 관리 탭
// ═══════════════════════════════════════
@Composable
fun DemoManagementTab(
    demoUsers: List<UserInfo>,
    nonDummyTeams: List<TeamInfo>,
    nonDummyDirectChats: List<DirectChatInfo>,
    pendingDummyLikes: List<PendingLikeInfo>,
    autoAcceptEnabled: Boolean,
    onToggleAutoAccept: () -> Unit,
    actionState: AdminActionState,
    padding: PaddingValues,
    onResetUser: (String) -> Unit,
    onDeleteTeam: (String) -> Unit,
    onDeleteAllNonDummyTeams: () -> Unit,
    onAcceptLike: (likeId: String, fromUserId: String, toTeamId: String, toTeamName: String) -> Unit
) {
    var showResetUserDialog by remember { mutableStateOf<UserInfo?>(null) }
    var showDeleteTeamDialog by remember { mutableStateOf<TeamInfo?>(null) }
    var showDeleteAllTeamsDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── 더미팀 좋아요 수락 ──
        item {
            Text(
                "더미팀 좋아요 수락",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF111827)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "더미팀으로 들어온 수락 대기 중인 좋아요입니다",
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )
        }

        // ── 더미팀 좋아요 자동 수락 토글 ──
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "더미팀 좋아요 자동 수락",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF111827)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "켜두면 더미팀에 들어온 좋아요만 자동으로 수락돼요. 실제 사용자 팀은 영향 없어요.",
                            fontSize = 11.5.sp,
                            color = Color(0xFF6B7280),
                            lineHeight = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = autoAcceptEnabled,
                        onCheckedChange = { onToggleAutoAccept() }
                    )
                }
            }
        }

        if (pendingDummyLikes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("수락 대기 중인 좋아요가 없습니다", color = Color(0xFF6B7280), fontSize = 13.sp)
                }
            }
        } else {
            items(pendingDummyLikes) { like ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 프로필 아바타
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFFA78BFA), Color(0xFFF472B6))),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (like.fromUserProfileImage.isNotBlank()) {
                                AsyncImage(
                                    model = like.fromUserProfileImage,
                                    contentDescription = null,
                                    modifier = Modifier.size(44.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                like.fromUserName.ifEmpty { "이름 없음" },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color(0xFF111827)
                            )
                            Text(
                                like.fromUserEmail,
                                fontSize = 11.sp,
                                color = Color(0xFF6B7280)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = Purple,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    like.toTeamName.ifEmpty { "팀" },
                                    fontSize = 11.sp,
                                    color = Purple,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Button(
                            onClick = {
                                onAcceptLike(
                                    like.likeId,
                                    like.fromUserId,
                                    like.toTeamId,
                                    like.toTeamName
                                )
                            },
                            enabled = actionState !is AdminActionState.Loading,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Purple)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("수락", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ── 시연 계정 목록 ──
        item {
            Text(
                "시연 계정 개별 초기화",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF111827)
            )
        }

        if (demoUsers.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("시연 계정이 없습니다", color = Color(0xFF6B7280))
                }
            }
        } else {
            items(demoUsers) { user ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 프로필 아바타
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFFA78BFA), Color(0xFFF472B6))),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (user.profileImages.isNotEmpty() && user.profileImages[0].isNotBlank()) {
                                AsyncImage(
                                    model = user.profileImages[0],
                                    contentDescription = null,
                                    modifier = Modifier.size(44.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                user.name.ifEmpty { "이름 없음" },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color(0xFF111827)
                            )
                            Text(
                                user.email,
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                        OutlinedButton(
                            onClick = { showResetUserDialog = user },
                            enabled = actionState !is AdminActionState.Loading,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                        ) {
                            Icon(
                                Icons.Default.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("초기화", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ── 사용자 팀·채팅방 삭제 ──
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "사용자 팀·채팅방 삭제",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF111827)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "더미 데이터를 제외한 사용자가 만든 팀과 채팅방을 삭제합니다",
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // ── 개수 + 전체 삭제 카드 ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (nonDummyTeams.isEmpty() && nonDummyDirectChats.isEmpty()) Color(0xFFF9FAFB) else Color(0xFFFFF1F2)
                ),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (nonDummyTeams.isEmpty() && nonDummyDirectChats.isEmpty()) Color(0xFFE5E7EB) else Color(0xFFEF4444),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.GroupOff,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "사용자 생성 팀·채팅방",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color(0xFF111827)
                        )
                        val totalCount = nonDummyTeams.size + nonDummyDirectChats.size
                        Text(
                            "팀 ${nonDummyTeams.size}개  ·  개인/그룹채팅 ${nonDummyDirectChats.size}개",
                            fontSize = 12.sp,
                            color = if (totalCount == 0) Color(0xFF6B7280) else Color(0xFFEF4444)
                        )
                        Text(
                            "총 ${totalCount}개",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (totalCount == 0) Color(0xFF6B7280) else Color(0xFFEF4444)
                        )
                    }
                    Button(
                        onClick = { showDeleteAllTeamsDialog = true },
                        enabled = (nonDummyTeams.isNotEmpty() || nonDummyDirectChats.isNotEmpty()) && actionState !is AdminActionState.Loading,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444),
                            disabledContainerColor = Color(0xFFE5E7EB)
                        )
                    ) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "전체 삭제",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (nonDummyTeams.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("삭제할 수 있는 팀이 없습니다", color = Color(0xFF6B7280), fontSize = 13.sp)
                }
            }
        } else {
            items(nonDummyTeams) { team ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFFA78BFA))),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Group,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                team.teamName.ifEmpty { "이름 없음" },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color(0xFF111827)
                            )
                            Text(
                                "${team.memberCount}명",
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                        OutlinedButton(
                            onClick = { showDeleteTeamDialog = team },
                            enabled = actionState !is AdminActionState.Loading,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("삭제", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 하단 여백
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    // ── 팀 삭제 확인 다이얼로그 ──
    showDeleteTeamDialog?.let { team ->
        AlertDialog(
            onDismissRequest = { showDeleteTeamDialog = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
            title = { Text("팀 삭제", fontWeight = FontWeight.Bold) },
            text = { Text("'${team.teamName}' 팀을 삭제합니다.\n채팅, 좋아요 기록도 함께 삭제되며 되돌릴 수 없습니다.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteTeamDialog = null
                        onDeleteTeam(team.teamId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteTeamDialog = null }) {
                    Text("취소")
                }
            }
        )
    }

    // ── 사용자 팀·채팅방 전체 삭제 확인 다이얼로그 ──
    if (showDeleteAllTeamsDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllTeamsDialog = false },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFEF4444)) },
            title = { Text("전체 팀·채팅방 삭제", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "사용자가 만든 팀 ${nonDummyTeams.size}개,\n" +
                    "개인/그룹 채팅방 ${nonDummyDirectChats.size}개를 모두 삭제합니다.\n\n" +
                    "더미 데이터는 절대 삭제되지 않으며,\n이 작업은 되돌릴 수 없습니다."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAllTeamsDialog = false
                        onDeleteAllNonDummyTeams()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("전체 삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllTeamsDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    // ── 개별 유저 초기화 확인 다이얼로그 ──
    showResetUserDialog?.let { user ->
        AlertDialog(
            onDismissRequest = { showResetUserDialog = null },
            icon = { Icon(Icons.Default.PersonRemove, contentDescription = null, tint = Color(0xFFEF4444)) },
            title = { Text("${user.name} 초기화", fontWeight = FontWeight.Bold) },
            text = { Text("${user.email}의 좋아요, 패스, 채팅 기록을 모두 삭제합니다.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetUserDialog = null
                        onResetUser(user.userId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("초기화")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetUserDialog = null }) {
                    Text("취소")
                }
            }
        )
    }
}

// ═══════════════════════════════════════
// 기존 탭들 (인증 대기, 유저 목록)
// ═══════════════════════════════════════

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
    val filteredUsers = if (searchQuery.isBlank()) users
    else users.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.email.contains(searchQuery, ignoreCase = true) ||
                it.department.contains(searchQuery, ignoreCase = true)
    }

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
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Gray400) },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Purple,
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true
            )
        }
        item {
            Text(
                "전체 ${filteredUsers.size}명",
                fontSize = 13.sp,
                color = Gray500,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
        items(filteredUsers) { user ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).background(
                            Brush.linearGradient(listOf(Color(0xFFA78BFA), Color(0xFFF472B6))), CircleShape
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (user.profileImages.isNotEmpty() && user.profileImages[0].isNotBlank()) {
                            AsyncImage(
                                model = user.profileImages[0],
                                contentDescription = null,
                                modifier = Modifier.size(44.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(user.name.ifEmpty { "이름 없음" }, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Gray900)
                            if (user.isAdmin) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(modifier = Modifier.background(Purple, RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                                    Text("관리자", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (user.isBanned) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(modifier = Modifier.background(Color(0xFFEF4444), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                                    Text("차단됨", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text(user.email, fontSize = 12.sp, color = Gray500)
                        if (user.department.isNotEmpty()) {
                            Text(user.department, fontSize = 11.sp, color = Gray400)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        if (!user.isAdmin) {
                            if (user.isBanned) {
                                TextButton(onClick = { onUnban(user.userId) }, enabled = actionState !is AdminActionState.Loading) {
                                    Text("차단해제", fontSize = 11.sp, color = Color(0xFF22C55E))
                                }
                            } else {
                                TextButton(onClick = { onBan(user.userId) }, enabled = actionState !is AdminActionState.Loading) {
                                    Text("차단", fontSize = 11.sp, color = Color(0xFFEF4444))
                                }
                            }
                            TextButton(onClick = { onGrantAdmin(user.userId) }, enabled = actionState !is AdminActionState.Loading) {
                                Text("관리자", fontSize = 11.sp, color = Purple)
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