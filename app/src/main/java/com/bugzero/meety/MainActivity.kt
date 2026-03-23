package com.bugzero.meety

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bugzero.meety.ui.team.MeetingCreateScreen
import com.bugzero.meety.ui.team.MyPageRoute
import com.bugzero.meety.ui.team.MyPageScreen
import com.bugzero.meety.ui.team.MyTeamScreen
import com.bugzero.meety.ui.theme.MeetycomposeTheme
import com.bugzero.meety.ui.team.UserProfileUiState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeetingCreateScreen()
        }
    }
}