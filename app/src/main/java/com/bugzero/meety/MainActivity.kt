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

                // ⭐ 자동 로그인 체크
                val isLoggedIn = remember { authViewModel.checkAutoLogin() }
                val verificationState by authViewModel.verificationCheckState.collectAsState()

                // ⭐ 로그인 상태라면 역할 확인
                LaunchedEffect(isLoggedIn) {
                    if (isLoggedIn) {
                        android.util.Log.d("MAIN", "🔐 User is logged in, checking role...")
                        authViewModel.checkVerificationAndRole()
                    } else {
                        android.util.Log.d("MAIN", "👤 User not logged in, staying at ONBOARDING")
                    }
                }

                // ⭐ 역할 확인 후 적절한 화면으로 이동
                LaunchedEffect(verificationState) {
                    if (isLoggedIn) {
                        when (verificationState) {
                            is VerificationCheckState.Admin -> {
                                android.util.Log.d("MAIN", "👑 Admin detected → navigating to ADMIN")
                                navController.navigate(Routes.ADMIN) {
                                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                                }
                            }
                            is VerificationCheckState.Verified -> {
                                android.util.Log.d("MAIN", "✅ Verified user → navigating to FEED")
                                navController.navigate(Routes.FEED) {
                                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                                }
                            }
                            is VerificationCheckState.NotYet -> {
                                android.util.Log.d("MAIN", "⏳ Not verified yet → navigating to PENDING_VERIFICATION")
                                navController.navigate(Routes.PENDING_VERIFICATION) {
                                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                                }
                            }
                            is VerificationCheckState.Loading -> {
                                android.util.Log.d("MAIN", "⏳ Loading verification status...")
                            }
                            is VerificationCheckState.Error -> {
                                android.util.Log.e("MAIN", "❌ Error checking verification: ${(verificationState as VerificationCheckState.Error).message}")
                            }
                            else -> {
                                android.util.Log.d("MAIN", "🤷 Idle state")
                            }
                        }
                    }
                }

                NavGraph(navController = navController)
            }
        }
    }
}