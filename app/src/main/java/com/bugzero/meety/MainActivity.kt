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
            // MeetingCreateScreen() 팀 생성 UI만
            // MyTeamScreen() 매칭 화면 UI만
            /*
                MyPageScreen(
                    uiState = UserProfileUiState(
                        name = "이상혁",
                        age = 24,
                        school = "한성대학교",
                        department = "컴퓨터공학부",
                        height = 175,
                        location = "서울",
                        bio = "안녕하세요. 테스트용 프로필입니다.",
                        interests = listOf("카페투어", "영화감상", "독서"),
                        foodLikes = listOf("파스타", "디저트", "고기"),
                        foodDislikes = listOf("오이", "해산물"),
                        profileImages = emptyList<String>(),
                        schedule = mapOf(
                            "월" to listOf("09:00", "10:00"),
                            "화" to listOf("13:00"),
                            "수" to listOf("11:00"),
                            "목" to emptyList<String>(),
                            "금" to listOf("14:00", "15:00")
                        )
                    )
                )
            */ // 예시용 더미데이터
            // MyPageRoute() 사용자 정보 확인
        }
    }
}