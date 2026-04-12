package com.bugzero.meety

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.bugzero.meety.navigation.NavGraph
import com.bugzero.meety.navigation.Routes
import com.bugzero.meety.ui.auth.AuthViewModel
import com.bugzero.meety.ui.auth.SplashScreen
import com.bugzero.meety.ui.auth.VerificationCheckState
import com.bugzero.meety.ui.theme.MeetyTheme

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // 시스템 스플래시 → 바로 Compose 스플래시로 이어지게 설정
        val systemSplash = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Compose 스플래시가 준비될 때까지 시스템 스플래시를 유지
        var keepSystemSplash = true
        systemSplash.setKeepOnScreenCondition { keepSystemSplash }

        setContent {
            MeetyTheme {
                // 시스템 스플래시 즉시 해제 → Compose 스플래시로 넘어감
                LaunchedEffect(Unit) { keepSystemSplash = false }

                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    SplashScreen(onSplashFinished = { showSplash = false })
                } else {
                    // 기존 코드 그대로
                    val navController = rememberNavController()
                    val isLoggedIn = remember { authViewModel.checkAutoLogin() }
                    val verificationState by authViewModel.verificationCheckState.collectAsState()

                    val startDestination = remember {
                        if (isLoggedIn) Routes.FEED else Routes.ONBOARDING
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

                    LaunchedEffect(verificationState) {
                        if (isLoggedIn) {
                            when (verificationState) {
                                is VerificationCheckState.Admin -> {
                                    navController.navigate(Routes.FEED) {
                                        popUpTo(startDestination) { inclusive = true }
                                    }
                                }
                                is VerificationCheckState.Verified -> {
                                    navController.navigate(Routes.FEED) {
                                        popUpTo(startDestination) { inclusive = true }
                                    }
                                }
                                is VerificationCheckState.NotYet -> {
                                    navController.navigate(Routes.PENDING_VERIFICATION) {
                                        popUpTo(startDestination) { inclusive = true }
                                    }
                                }
                                else -> {}
                            }
                        }
                    }

                    NavGraph(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}