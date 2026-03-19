package com.bugzero.meety.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.bugzero.meety.ui.admin.AdminScreen
import com.bugzero.meety.ui.auth.AuthViewModel
import com.bugzero.meety.ui.auth.LoginScreen
import com.bugzero.meety.ui.auth.OnboardingScreen
import com.bugzero.meety.ui.auth.PendingVerificationScreen
import com.bugzero.meety.ui.auth.SetupProfileScreen
import com.bugzero.meety.ui.auth.SignUpScreen
import com.bugzero.meety.ui.auth.StudentIdUploadScreen
import com.bugzero.meety.ui.auth.VerificationCheckState
import com.bugzero.meety.ui.chat.ChatListScreen
import com.bugzero.meety.ui.chat.ChatRoomScreen
import com.bugzero.meety.ui.chat.ScheduleSyncScreen
import com.bugzero.meety.ui.feed.FeedScreen
import com.bugzero.meety.ui.feed.MeetingDetailScreen
import com.bugzero.meety.ui.feed.ProfileEditScreen
import com.bugzero.meety.ui.team.MeetingCreateScreen
import com.bugzero.meety.ui.team.MyPageScreen
import com.bugzero.meety.ui.team.MyTeamScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val SETUP_PROFILE = "setup_profile"
    const val STUDENT_ID_UPLOAD = "student_id_upload"
    const val PENDING_VERIFICATION = "pending_verification"
    const val ADMIN = "admin"
    const val FEED = "feed"
    const val MEETING_DETAIL = "meeting_detail"
    const val PROFILE_EDIT = "profile_edit"
    const val MY_TEAM = "my_team"
    const val MY_PAGE = "my_page"
    const val MEETING_CREATE = "meeting_create"
    const val CHAT_LIST = "chat_list"
    const val CHAT_ROOM = "chat_room"
    const val SCHEDULE_SYNC = "schedule_sync"
}

val bottomNavRoutes = listOf(
    Routes.FEED,
    Routes.CHAT_LIST,
    Routes.MEETING_CREATE,
    Routes.MY_PAGE
)

@Composable
fun NavGraph(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = bottomNavRoutes.any { currentRoute == it }

    // NavGraph 전체에서 AuthViewModel 공유
    val authViewModel: AuthViewModel = viewModel()
    val verificationCheckState by authViewModel.verificationCheckState.collectAsState()

    // 로그인 후 역할 분기 처리
    LaunchedEffect(verificationCheckState) {
        when (verificationCheckState) {
            is VerificationCheckState.Admin -> {
                navController.navigate(Routes.ADMIN) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
                authViewModel.resetVerificationCheckState()
            }
            is VerificationCheckState.Verified -> {
                navController.navigate(Routes.FEED) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
                authViewModel.resetVerificationCheckState()
            }
            is VerificationCheckState.NotYet -> {
                navController.navigate(Routes.PENDING_VERIFICATION) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
                authViewModel.resetVerificationCheckState()
            }
            else -> {}
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val items = listOf(
                        Triple(Routes.FEED, Icons.Default.Home, "홈"),
                        Triple(Routes.CHAT_LIST, Icons.Default.Chat, "매칭"),
                        Triple(Routes.MEETING_CREATE, Icons.Default.Group, "팀 생성"),
                        Triple(Routes.MY_PAGE, Icons.Default.Person, "프로필"),
                    )
                    items.forEach { (route, icon, label) ->
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            selected = currentRoute == route,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(Routes.FEED) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.ONBOARDING,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onLoginClick = { navController.navigate(Routes.LOGIN) },
                    onSignUpClick = { navController.navigate(Routes.LOGIN) }
                )
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        // 로그인 성공 → isAdmin / isVerified 체크해서 분기
                        authViewModel.checkVerificationAndRole()
                    },
                    onSignUpClick = { navController.navigate(Routes.SIGNUP) },
                    viewModel = authViewModel
                )
            }
            composable(Routes.SIGNUP) {
                SignUpScreen(
                    onSignUpSuccess = {
                        navController.navigate(Routes.SETUP_PROFILE) {
                            popUpTo(Routes.SIGNUP) { inclusive = true }
                        }
                    },
                    onLoginClick = { navController.navigate(Routes.LOGIN) }
                )
            }
            composable(Routes.SETUP_PROFILE) {
                SetupProfileScreen(
                    onComplete = {
                        navController.navigate(Routes.STUDENT_ID_UPLOAD) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.STUDENT_ID_UPLOAD) {
                StudentIdUploadScreen(
                    onUploadSuccess = {
                        navController.navigate(Routes.PENDING_VERIFICATION) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }

// NavGraph.kt의 PendingVerificationScreen composable 부분만 수정

            composable(Routes.PENDING_VERIFICATION) {
                PendingVerificationScreen(
                    onCheckVerification = {
                        android.util.Log.d("NAV", "🔔 onCheckVerification called from PendingScreen")
                        authViewModel.checkVerificationAndRole()
                    },
                    onLogout = {
                        android.util.Log.d("NAV", "🚪 Logout clicked")
                        authViewModel.logout()
                        navController.navigate(Routes.ONBOARDING) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    authViewModel = authViewModel  // ⭐ 핵심! 같은 ViewModel 인스턴스 전달
                )
            }
            composable(Routes.ADMIN) {
                AdminScreen(
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Routes.ONBOARDING) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.FEED) {
                FeedScreen(
                    onTeamClick = { teamId ->
                        navController.navigate("${Routes.MEETING_DETAIL}/$teamId")
                    }
                )
            }
            composable("${Routes.MEETING_DETAIL}/{teamId}") {
                MeetingDetailScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(Routes.PROFILE_EDIT) {
                ProfileEditScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(Routes.MY_TEAM) {
                MyTeamScreen(
                    onCreateTeamClick = { navController.navigate(Routes.MEETING_CREATE) }
                )
            }
            composable(Routes.MY_PAGE) {
                MyPageScreen(
                    onEditProfileClick = { navController.navigate(Routes.PROFILE_EDIT) },
                    onScheduleClick = { navController.navigate(Routes.SCHEDULE_SYNC) }
                )
            }
            composable(Routes.MEETING_CREATE) {
                MeetingCreateScreen(
                    onBackClick = { navController.popBackStack() },
                    onCreateSuccess = { navController.popBackStack() }
                )
            }
            composable(Routes.CHAT_LIST) {
                ChatListScreen(
                    onChatClick = { chatId ->
                        navController.navigate("${Routes.CHAT_ROOM}/$chatId")
                    }
                )
            }
            composable("${Routes.CHAT_ROOM}/{chatId}") {
                ChatRoomScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(Routes.SCHEDULE_SYNC) {
                ScheduleSyncScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}