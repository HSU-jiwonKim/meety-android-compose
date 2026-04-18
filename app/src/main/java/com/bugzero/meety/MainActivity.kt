package com.bugzero.meety

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.bugzero.meety.navigation.NavGraph
import com.bugzero.meety.navigation.Routes
import com.bugzero.meety.ui.auth.AuthViewModel
import com.bugzero.meety.ui.auth.SplashScreen
import com.bugzero.meety.ui.auth.VerificationCheckState
import com.bugzero.meety.ui.theme.MeetyTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    // 알림에서 받은 Intent를 Compose에 전달하기 위한 StateFlow
    private val pendingIntent = MutableStateFlow<Intent?>(null)


    // 전화 알림 권한 설정화면에서 돌아올 때 재확인용
    private val fullScreenPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    // Android 13+ 알림 권한 요청 런처
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        val systemSplash = installSplashScreen()
        super.onCreate(savedInstanceState)

        var keepSystemSplash = true
        systemSplash.setKeepOnScreenCondition { keepSystemSplash }

        // Android 13(API 33)+ 알림 권한 런타임 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // onCreate로 들어온 알림 Intent 저장 (인증 완료 후 처리)
        val isLaunchedFromCall = intent?.getBooleanExtra("isIncomingCall", false) == true
        if (intent?.hasExtra("chatId") == true) {
            pendingIntent.value = intent
        }

        // 부재중 알림 탭으로 열린 경우 → 해당 사람의 부재중 카운트 초기화
        clearMissedCountIfNeeded(intent)

        // 수신 전화 수락으로 앱이 열리면 통화 알림 즉시 제거 + Firestore 리스너 정리
        if (isLaunchedFromCall) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(CallActionReceiver.NOTIFICATION_ID)
            MyFirebaseMessagingService.removeCallStatusListener()
        }

        setContent {
            MeetyTheme {
                LaunchedEffect(Unit) { keepSystemSplash = false }

                // 수신 전화 수락으로 앱이 열리면 스플래시 건너뛰기
                var showSplash by remember { mutableStateOf(!isLaunchedFromCall) }

                if (showSplash) {
                    SplashScreen(onSplashFinished = { showSplash = false })
                } else {
                    val navController = rememberNavController()
                    val isLoggedIn    = remember { authViewModel.checkAutoLogin() }
                    val verificationState by authViewModel.verificationCheckState.collectAsState()
                    val incoming by pendingIntent.collectAsState()

                    // 인증 네비게이션 완료 여부 (완료 전에 알림 네비게이션 대기)
                    var authNavigationDone by remember { mutableStateOf(false) }

                    // 전화 알림 권한 다이얼로그 표시 여부
                    var showFullScreenPermDialog by remember { mutableStateOf(false) }

                    val startDestination = remember {
                        if (isLoggedIn) Routes.FEED else Routes.ONBOARDING
                    }

                    // Android 14+에서 USE_FULL_SCREEN_INTENT 권한 체크 → 없으면 다이얼로그
                    LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            if (!NotificationManagerCompat.from(this@MainActivity).canUseFullScreenIntent()) {
                                showFullScreenPermDialog = true
                            }
                        }
                    }

                    // 전화 알림 권한 안내 다이얼로그
                    if (showFullScreenPermDialog) {
                        AlertDialog(
                            onDismissRequest = { showFullScreenPermDialog = false },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = Color(0xFFEC4899),
                                    modifier = Modifier.size(32.dp)
                                )
                            },
                            title = { Text("전화 알림 권한 필요") },
                            text = {
                                Text(
                                    "잠금화면에서 수신 전화 알림(초록/빨간 버튼)을 표시하려면 " +
                                            "\"전체화면 알림\" 권한이 필요합니다.\n\n" +
                                            "설정에서 Meety 앱의 권한을 허용해 주세요."
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showFullScreenPermDialog = false
                                        // Android 14+ 전체화면 알림 권한 설정 화면으로 이동
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                            Uri.parse("package:${packageName}")
                                        )
                                        fullScreenPermissionLauncher.launch(intent)
                                    }
                                ) {
                                    Text("설정으로 이동", color = Color(0xFFEC4899))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showFullScreenPermDialog = false }) {
                                    Text("나중에")
                                }
                            }
                        )
                    }

                    LaunchedEffect(isLoggedIn) {
                        if (isLoggedIn) {
                            authViewModel.checkVerificationAndRole()
                            authViewModel.startBanListener {
                                navController.navigate(Routes.ONBOARDING) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    }

                    // 인증 상태에 따라 초기 화면으로 이동 (1회만)
                    LaunchedEffect(verificationState) {
                        if (isLoggedIn && !authNavigationDone) {
                            when (verificationState) {
                                is VerificationCheckState.Admin,
                                is VerificationCheckState.Verified -> {
                                    authNavigationDone = true
                                    val pendingCall = pendingIntent.value
                                    val pIsIncoming = pendingCall?.getBooleanExtra("isIncomingCall", false) == true
                                    val pIsCallBack = pendingCall?.getBooleanExtra("isCallBack", false) == true

                                    if (pIsIncoming || pIsCallBack) {
                                        // 전화 수락 또는 부재중 콜백으로 앱이 열린 경우
                                        pendingIntent.value = null
                                        val chatId   = pendingCall?.getStringExtra("chatId") ?: ""
                                        val callType = pendingCall?.getStringExtra("callType") ?: "voice"
                                        val roomName = pendingCall?.getStringExtra("roomName") ?: "채팅방"
                                        val isIncoming = if (pIsIncoming) "true" else "false"
                                        // FEED → CHAT_ROOM → CALL 순으로 쌓아서 통화 종료 시 ChatRoom 에 남도록
                                        navController.navigate(Routes.FEED) {
                                            popUpTo(startDestination) { inclusive = true }
                                        }
                                        if (chatId.isNotEmpty()) {
                                            navController.navigate("${Routes.CHAT_ROOM}/$chatId?roomName=$roomName")
                                        }
                                        navController.navigate("${Routes.CALL}/$chatId/$callType/$isIncoming")
                                    } else {
                                        navController.navigate(Routes.FEED) {
                                            popUpTo(startDestination) { inclusive = true }
                                        }
                                    }
                                }
                                is VerificationCheckState.NotYet -> {
                                    authNavigationDone = true
                                    navController.navigate(Routes.PENDING_VERIFICATION) {
                                        popUpTo(startDestination) { inclusive = true }
                                    }
                                }
                                else -> {}
                            }
                        }
                    }

                    // 앱이 이미 켜진 상태에서 알림 탭/수락 버튼 → onNewIntent 경유
                    LaunchedEffect(incoming, authNavigationDone) {
                        val intent = incoming ?: return@LaunchedEffect
                        if (!authNavigationDone) return@LaunchedEffect
                        handleNotificationIntent(navController, intent, isLoggedIn)
                    }

                    NavGraph(
                        navController    = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }

    // 앱이 이미 켜있을 때 알림 탭 or 수락 버튼 → 여기서 Intent 수신
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.hasExtra("chatId")) {
            // 수신 전화 수락이면 알림 즉시 제거 + Firestore 리스너 정리
            if (intent.getBooleanExtra("isIncomingCall", false)) {
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(CallActionReceiver.NOTIFICATION_ID)
                MyFirebaseMessagingService.removeCallStatusListener()
            }
            // 부재중 알림 탭 → 카운트 초기화
            clearMissedCountIfNeeded(intent)
            pendingIntent.value = intent
        }
    }

    /**
     * 부재중 알림 탭/다시 전화로 앱이 열린 경우, 해당 사람(chatId)의 부재중 카운트를 초기화.
     * SharedPreferences의 "missed_count_<chatId>" 값을 0으로 리셋.
     */
    private fun clearMissedCountIfNeeded(intent: Intent?) {
        if (intent == null) return
        val shouldClear = intent.getBooleanExtra("clearMissedCount", false)
        if (!shouldClear) return
        val missedChatId = intent.getStringExtra("missedChatId") ?: return
        if (missedChatId.isEmpty()) return

        val prefs = getSharedPreferences("meety_notification_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("missed_count_$missedChatId").apply()
    }

    private fun handleNotificationIntent(
        navController: NavHostController,
        intent: Intent,
        isLoggedIn: Boolean
    ) {
        if (!isLoggedIn) return
        val chatId         = intent.getStringExtra("chatId") ?: return
        val isIncomingCall = intent.getBooleanExtra("isIncomingCall", false)
        val isCallBack     = intent.getBooleanExtra("isCallBack", false)
        val callType       = intent.getStringExtra("callType") ?: "voice"
        val type           = intent.getStringExtra("type")

        // 중복 처리 방지
        pendingIntent.value = null

        val roomName = intent.getStringExtra("roomName") ?: "채팅방"
        when {
            isIncomingCall && chatId.isNotEmpty() -> {
                // 수신 전화 수락 → 해당 채팅방으로 먼저 이동 후, 통화 화면을 그 위에 쌓음
                // 통화 종료 시 popBackStack() 으로 자동으로 채팅방에 남는다.
                val alreadyInRoom = navController.currentDestination?.route
                    ?.startsWith(Routes.CHAT_ROOM) == true &&
                    navController.currentBackStackEntry?.arguments?.getString("chatId") == chatId
                if (!alreadyInRoom) {
                    navController.navigate("${Routes.CHAT_ROOM}/$chatId?roomName=$roomName") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = false }
                        launchSingleTop = true
                    }
                }
                navController.navigate("${Routes.CALL}/$chatId/$callType/true") {
                    launchSingleTop = true
                }
            }
            isCallBack && chatId.isNotEmpty() -> {
                // 부재중 전화 "다시 전화" → 채팅방 거쳐서 통화 화면
                val alreadyInRoom = navController.currentDestination?.route
                    ?.startsWith(Routes.CHAT_ROOM) == true &&
                    navController.currentBackStackEntry?.arguments?.getString("chatId") == chatId
                if (!alreadyInRoom) {
                    navController.navigate("${Routes.CHAT_ROOM}/$chatId?roomName=$roomName") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = false }
                        launchSingleTop = true
                    }
                }
                navController.navigate("${Routes.CALL}/$chatId/$callType/false") {
                    launchSingleTop = true
                }
            }
            type == "chat" && chatId.isNotEmpty() -> {
                navController.navigate("${Routes.CHAT_ROOM}/$chatId?roomName=$roomName") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = false }
                    launchSingleTop = true
                }
            }
        }
    }
}