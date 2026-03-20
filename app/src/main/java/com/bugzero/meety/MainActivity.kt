package com.bugzero.meety

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

                // 시작 화면 결정 (로그인 상태면 온보딩 스킵)
                val startDestination = remember {
                    if (isLoggedIn) Routes.FEED else Routes.ONBOARDING
                }

                LaunchedEffect(isLoggedIn) {
                    if (isLoggedIn) {
                        authViewModel.checkVerificationAndRole()
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