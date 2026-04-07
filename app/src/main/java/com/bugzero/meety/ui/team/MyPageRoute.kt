package com.bugzero.meety.ui.team

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun MyPageRoute(
    viewModel: MyPageViewModel = viewModel(),

    // 🔹 네비게이션 콜백
    onHomeClick: () -> Unit = {},
    onMatchingClick: () -> Unit = {},
    onCreateTeamClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},

    // 🔹 상단바
    onSearchClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},

    // 🔹 기능 버튼
    onEditProfileClick: () -> Unit = {},
    onScheduleClick: () -> Unit = {},

    // 🔹 비로그인 시 처리
    onRequireLogin: () -> Unit = {}
) {
    val screenState by viewModel.screenState.collectAsState()
    val currentUser = FirebaseAuth.getInstance().currentUser

    LaunchedEffect(currentUser?.uid) {
        if (currentUser == null) {
            viewModel.clearListener()
            onRequireLogin()
        } else {
            viewModel.loadMyProfile()
        }
    }

    if (currentUser == null) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    when {
        screenState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        screenState.errorMessage != null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = screenState.errorMessage ?: "오류가 발생했습니다.")
            }
        }

        screenState.uiState != null -> {
            MyPageScreen(
                uiState = screenState.uiState!!,
                viewModel = viewModel,
                onHomeClick = onHomeClick,
                onMatchingClick = onMatchingClick,
                onCreateTeamClick = onCreateTeamClick,
                onChatClick = onChatClick,
                onProfileClick = onProfileClick,
                onSearchClick = onSearchClick,
                onNotificationClick = onNotificationClick,
                onEditProfileClick = onEditProfileClick,
                onScheduleClick = onScheduleClick
            )
        }

        else -> {
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}