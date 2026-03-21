package com.bugzero.meety.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
    const val FEED = "feed"
    const val MEETING_DETAIL = "meeting_detail"
    const val PROFILE_EDIT = "profile_edit"
    const val MY_TEAM = "my_team"
    const val MY_PAGE = "my_page"
    const val MEETING_CREATE = "meeting_create"
    const val CHAT_LIST = "chat_list"
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
    }
}