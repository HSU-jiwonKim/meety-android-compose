package com.bugzero.meety.navigation

<<<<<<< HEAD
import androidx.compose.runtime.Composable
=======
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
>>>>>>> ca178ecdf5d6d6fc1948225597641ad0fce1c061
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
<<<<<<< HEAD
import androidx.navigation.navArgument
import com.bugzero.meety.ui.auth.LoginScreen
import com.bugzero.meety.ui.auth.OnboardingScreen
import com.bugzero.meety.ui.auth.StudentIdUploadScreen
import com.bugzero.meety.ui.feed.FeedScreen
import com.bugzero.meety.ui.feed.MeetingDetailScreen
import com.bugzero.meety.ui.feed.ProfileEditScreen
import com.bugzero.meety.ui.team.MyTeamScreen
import com.bugzero.meety.ui.team.MyPageScreen
import com.bugzero.meety.ui.team.MeetingCreateScreen
import com.bugzero.meety.ui.chat.ChatListScreen
import com.bugzero.meety.ui.chat.ChatRoomScreen
import com.bugzero.meety.ui.chat.ScheduleSyncScreen

object Routes {
    const val LOGIN = "login"
    const val ONBOARDING = "onboarding"
    const val STUDENT_ID_UPLOAD = "student_id_upload"
=======
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
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
import com.bugzero.meety.ui.team.MyPageRoute
import com.bugzero.meety.ui.team.MyTeamScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val SETUP_PROFILE = "setup_profile"
    const val STUDENT_ID_UPLOAD = "student_id_upload"
    const val PENDING_VERIFICATION = "pending_verification"
    const val ADMIN = "admin"
>>>>>>> ca178ecdf5d6d6fc1948225597641ad0fce1c061
    const val FEED = "feed"
    const val MEETING_DETAIL = "meeting_detail"
    const val PROFILE_EDIT = "profile_edit"
    const val MY_TEAM = "my_team"
    const val MY_PAGE = "my_page"
    const val MEETING_CREATE = "meeting_create"
    const val CHAT_LIST = "chat_list"
<<<<<<< HEAD
    // chatId와 roomName을 URL 파라미터로 전달
    const val CHAT_ROOM = "chat_room/{chatId}/{roomName}"
    const val SCHEDULE_SYNC = "schedule_sync"
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {
        composable(Routes.LOGIN) { LoginScreen() }
        composable(Routes.ONBOARDING) { OnboardingScreen() }
        composable(Routes.STUDENT_ID_UPLOAD) { StudentIdUploadScreen() }
        composable(Routes.FEED) { FeedScreen() }
        composable(Routes.MEETING_DETAIL) { MeetingDetailScreen() }
        composable(Routes.PROFILE_EDIT) { ProfileEditScreen() }
        composable(Routes.MY_TEAM) { MyTeamScreen() }
        composable(Routes.MY_PAGE) { MyPageScreen() }
        composable(Routes.MEETING_CREATE) { MeetingCreateScreen() }

        // 채팅 목록 → 채팅방으로 chatId, roomName 전달
        composable(Routes.CHAT_LIST) {
            ChatListScreen(
                onChatClick = { chatId, roomName ->
                    navController.navigate("chat_room/$chatId/$roomName")
                }
            )
        }

        // chatId, roomName을 받아서 채팅방 화면 표시
        composable(
            route = Routes.CHAT_ROOM,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType },
                navArgument("roomName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            ChatRoomScreen(
                chatId = backStackEntry.arguments?.getString("chatId") ?: "",
                roomName = backStackEntry.arguments?.getString("roomName") ?: "채팅방",
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.SCHEDULE_SYNC) { ScheduleSyncScreen() }
=======
    const val CHAT_ROOM = "chat_room"
    const val SCHEDULE_SYNC = "schedule_sync"
}

data class NavItem(
    val route: String,
    val label: String,
    val type: String
)

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Routes.ONBOARDING
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val authViewModel: AuthViewModel = viewModel()
    val isAdmin by authViewModel.isAdmin.collectAsState()
    val verificationCheckState by authViewModel.verificationCheckState.collectAsState()

    val bottomNavItems = remember(isAdmin) {
        buildList {
            add(NavItem(Routes.FEED, "홈", "home"))
            add(NavItem(Routes.MY_TEAM, "매칭", "heart"))
            add(NavItem(Routes.MEETING_CREATE, "팀 만들기", "plus"))
            add(NavItem(Routes.CHAT_LIST, "채팅", "chat"))
            add(NavItem(Routes.MY_PAGE, "프로필", "person"))
            if (isAdmin) add(NavItem(Routes.ADMIN, "관리자", "admin"))
        }
    }

    val bottomNavRoutes = listOf(
        Routes.FEED,
        Routes.CHAT_LIST,
        Routes.MEETING_CREATE,
        Routes.MY_TEAM,
        Routes.MY_PAGE,
        Routes.ADMIN
    )
    val showBottomBar = bottomNavRoutes.any { currentRoute == it }

    LaunchedEffect(verificationCheckState) {
        when (verificationCheckState) {
            is VerificationCheckState.Admin -> {
                navController.navigate(Routes.FEED) { popUpTo(startDestination) { inclusive = true } }
                authViewModel.resetVerificationCheckState()
            }
            is VerificationCheckState.Verified -> {
                navController.navigate(Routes.FEED) { popUpTo(startDestination) { inclusive = true } }
                authViewModel.resetVerificationCheckState()
            }
            is VerificationCheckState.NotYet -> {
                navController.navigate(Routes.PENDING_VERIFICATION) { popUpTo(startDestination) { inclusive = true } }
                authViewModel.resetVerificationCheckState()
            }
            else -> {}
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                    bottomNavItems.forEach { item ->
                        val isSelected = when (item.type) {
                            "home"   -> currentRoute == Routes.FEED
                            "chat"   -> currentRoute == Routes.CHAT_LIST
                            "plus"   -> currentRoute == Routes.MEETING_CREATE
                            "heart"  -> currentRoute == Routes.MY_TEAM
                            "person" -> currentRoute == Routes.MY_PAGE
                            "admin"  -> currentRoute == Routes.ADMIN
                            else -> false
                        }
                        val isPlus = item.type == "plus"
                        val isAdminItem = item.type == "admin"

                        NavigationBarItem(
                            icon = {
                                when {
                                    isPlus -> Box(
                                        modifier = Modifier.size(44.dp).background(
                                            if (isSelected) Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899)))
                                            else Brush.linearGradient(listOf(Color(0xFFEDE9FE), Color(0xFFFCE7F3))),
                                            RoundedCornerShape(14.dp)
                                        ), contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = item.label,
                                            tint = if (isSelected) Color.White else Color(0xFF7C3AED),
                                            modifier = Modifier.size(22.dp))
                                    }
                                    isAdminItem -> Box(
                                        modifier = Modifier.size(44.dp).background(
                                            if (isSelected) Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899)))
                                            else Brush.linearGradient(listOf(Color(0xFFF5F3FF), Color(0xFFFDF2F8))),
                                            RoundedCornerShape(14.dp)
                                        ), contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Shield, contentDescription = item.label,
                                            tint = if (isSelected) Color.White else Color(0xFF7C3AED),
                                            modifier = Modifier.size(22.dp))
                                    }
                                    else -> Icon(
                                        when (item.type) {
                                            "home"  -> Icons.Default.Home
                                            "chat"  -> Icons.Default.ChatBubble
                                            "heart" -> Icons.Default.Favorite
                                            else    -> Icons.Default.Person
                                        },
                                        contentDescription = item.label,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            },
                            label = {
                                Text(item.label, fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF7C3AED),
                                selectedTextColor = Color(0xFF7C3AED),
                                unselectedIconColor = Color(0xFF9CA3AF),
                                unselectedTextColor = Color(0xFF9CA3AF),
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)) {

            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onLoginClick = { navController.navigate(Routes.LOGIN) },
                    onSignUpClick = { navController.navigate(Routes.LOGIN) }
                )
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = { authViewModel.checkVerificationAndRole() },
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
            composable(Routes.PENDING_VERIFICATION) {
                PendingVerificationScreen(
                    onCheckVerification = { authViewModel.checkVerificationAndRole() },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Routes.ONBOARDING) { popUpTo(0) { inclusive = true } }
                    },
                    onReupload = {
                        navController.navigate(Routes.STUDENT_ID_UPLOAD) {
                            popUpTo(Routes.PENDING_VERIFICATION) { inclusive = true }
                        }
                    },
                    authViewModel = authViewModel
                )
            }
            composable(Routes.ADMIN) {
                AdminScreen(
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Routes.ONBOARDING) { popUpTo(0) { inclusive = true } }
                    }
                )
            }
            composable(Routes.FEED) {
                FeedScreen(
                    onNavigateToProfile = {
                        navController.navigate(Routes.MY_PAGE) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(
                route = "${Routes.MEETING_DETAIL}/{teamId}",
                arguments = listOf(navArgument("teamId") { type = NavType.StringType })
            ) {
                MeetingDetailScreen(onBackClick = { navController.popBackStack() })
            }
            composable(Routes.PROFILE_EDIT) {
                ProfileEditScreen(onBackClick = { navController.popBackStack() })
            }
            composable(Routes.MY_TEAM) {
                MyTeamScreen(
                    onHomeClick = {
                        navController.navigate(Routes.FEED) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onMatchingClick = {
                        navController.navigate(Routes.MY_TEAM) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onCreateTeamClick = {
                        navController.navigate(Routes.MEETING_CREATE) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onChatClick = {
                        navController.navigate(Routes.CHAT_LIST) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onProfileClick = {
                        navController.navigate(Routes.MY_PAGE) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onCreateNewTeamClick = { navController.navigate(Routes.MEETING_CREATE) }
                )
            }
            composable(Routes.MY_PAGE) {
                MyPageRoute(
                    onHomeClick = {
                        navController.navigate(Routes.FEED) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onMatchingClick = {
                        navController.navigate(Routes.MY_TEAM) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onCreateTeamClick = {
                        navController.navigate(Routes.MEETING_CREATE) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onChatClick = {
                        navController.navigate(Routes.CHAT_LIST) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onProfileClick = {},
                    onEditProfileClick = { navController.navigate(Routes.PROFILE_EDIT) }
                )
            }
            composable(Routes.MEETING_CREATE) {
                MeetingCreateScreen(
                    onHomeClick = {
                        navController.navigate(Routes.FEED) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onMatchingClick = {
                        navController.navigate(Routes.MY_TEAM) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onCreateTeamTabClick = {},
                    onChatClick = {
                        navController.navigate(Routes.CHAT_LIST) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onProfileClick = {
                        navController.navigate(Routes.MY_PAGE) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onCreateTeamClick = { navController.popBackStack() }
                )
            }
            composable(Routes.CHAT_LIST) {
                ChatListScreen(
                    onChatClick = { chatId, roomName ->
                        navController.navigate("${Routes.CHAT_ROOM}/$chatId?roomName=$roomName")
                    }
                )
            }
            composable(
                route = "${Routes.CHAT_ROOM}/{chatId}?roomName={roomName}",
                arguments = listOf(
                    navArgument("chatId")   { type = NavType.StringType; defaultValue = "" },
                    navArgument("roomName") { type = NavType.StringType; defaultValue = "채팅방" }
                )
            ) { backStackEntry ->
                ChatRoomScreen(
                    chatId      = backStackEntry.arguments?.getString("chatId") ?: "",
                    roomName    = backStackEntry.arguments?.getString("roomName") ?: "채팅방",
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(Routes.SCHEDULE_SYNC) {
                ScheduleSyncScreen()
            }
        }
>>>>>>> ca178ecdf5d6d6fc1948225597641ad0fce1c061
    }
}