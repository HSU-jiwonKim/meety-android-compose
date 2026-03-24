package com.bugzero.meety.ui.team

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

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
    onEditProfileClick: () -> Unit = {}
) {
    val screenState by viewModel.screenState.collectAsState()

    when {
        // 🔵 로딩 중
        screenState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // 🔴 에러
        screenState.errorMessage != null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = screenState.errorMessage ?: "오류가 발생했습니다.")
            }
        }

        // 🟢 성공 (데이터 있음)
        screenState.uiState != null -> {
            MyPageScreen(
                uiState = screenState.uiState!!,

                // 하단바
                onHomeClick = onHomeClick,
                onMatchingClick = onMatchingClick,
                onCreateTeamClick = onCreateTeamClick,
                onChatClick = onChatClick,
                onProfileClick = onProfileClick,

                // 상단바
                onSearchClick = onSearchClick,
                onNotificationClick = onNotificationClick,

                // 버튼
                onEditProfileClick = onEditProfileClick
            )
        }
    }
}