package com.bugzero.meety

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
<<<<<<< HEAD
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.bugzero.meety.ui.theme.MeetycomposeTheme
import com.bugzero.meety.ui.chat.ChatListScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            MeetycomposeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        // 샘플 Greeting 대신 예원님의 진짜 화면을 넣었습니다.
                        Box(modifier = Modifier.padding(innerPadding)) {
                            ChatListScreen()
                        }
                    }
                }
            }
        }
    }
}

=======
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.bugzero.meety.navigation.NavGraph
import com.bugzero.meety.navigation.Routes
import com.bugzero.meety.ui.auth.AuthViewModel
import com.bugzero.meety.ui.auth.VerificationCheckState
import com.bugzero.meety.ui.theme.MeetyTheme

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MeetyTheme {
                val navController = rememberNavController()
                val isLoggedIn = remember { authViewModel.checkAutoLogin() }
                val verificationState by authViewModel.verificationCheckState.collectAsState()

                val startDestination = remember {
                    if (isLoggedIn) Routes.FEED else Routes.ONBOARDING
                }

                // 로그인 상태면 역할 확인 + 실시간 차단 감지 시작
                LaunchedEffect(isLoggedIn) {
                    if (isLoggedIn) {
                        authViewModel.checkVerificationAndRole()
                        authViewModel.startBanListener {
                            // 차단되면 즉시 온보딩으로 이동
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
>>>>>>> ca178ecdf5d6d6fc1948225597641ad0fce1c061
