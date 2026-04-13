package com.bugzero.meety

import android.Manifest
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
        if (intent?.hasExtra("chatId") == true) {
            pendingIntent.value = intent
        }

        setContent {
            MeetyTheme {
                LaunchedEffect(Unit) { keepSystemSplash = false }

                var showSplash by remember { mutableStateOf(true) }

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
                                is VerificationCheckState.Admin -> {
                                    authNavigationDone = true
                                    navController.navigate(Routes.FEED) {
                                        popUpTo(startDestination) { inclusive = true }
                                    }
                                }
                                is VerificationCheckState.Verified -> {
                                    authNavigationDone = true
                                    navController.navigate(Routes.FEED) {
                                        popUpTo(startDestination) { inclusive = true }
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

                    // 알림 Intent 처리:
                    // - 앱 꺼진 상태에서 탭: authNavigationDone이 true 될 때까지 대기
                    // - 앱 켜진 상태에서 탭(onNewIntent): 이미 true이므로 즉시 처리
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
            pendingIntent.value = intent
        }
    }

    private fun handleNotificationIntent(
        navController: NavHostController,
        intent: Intent,
        isLoggedIn: Boolean
    ) {
        if (!isLoggedIn) return
        val chatId         = intent.getStringExtra("chatId") ?: return
        val isIncomingCall = intent.getBooleanExtra("isIncomingCall", false)
        val callType       = intent.getStringExtra("callType") ?: "voice"
        val type           = intent.getStringExtra("type")

        // 중복 처리 방지
        pendingIntent.value = null

        when {
            isIncomingCall && chatId.isNotEmpty() -> {
                navController.navigate("${Routes.CALL}/$chatId/$callType/true") {
                    popUpTo(Routes.FEED) { inclusive = false }
                }
            }
            type == "chat" && chatId.isNotEmpty() -> {
                navController.navigate("${Routes.CHAT_ROOM}/$chatId") {
                    popUpTo(Routes.FEED) { inclusive = false }
                }
            }
        }
    }
}
