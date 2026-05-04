package com.bugzero.meety.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bugzero.meety.ui.theme.* // 기존 테마 색상(Purple 등) 사용을 위해 임포트

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScheduleScreen(
    participantIds: List<String>, // 채팅방 참여자 UID 목록
    onBackClick: () -> Unit = {},
    viewModel: ScheduleViewModel = viewModel()
) {
    // 합쳐진 시간표 데이터 관찰
    val mergedBusyTimes by viewModel.mergedBusyTimes.collectAsState()

    // 화면 진입 시 팀원들의 스케줄 데이터를 합치는 함수 호출
    LaunchedEffect(participantIds) {
        if (participantIds.isNotEmpty()) {
            viewModel.fetchTeamSchedules(participantIds)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("우리들의 공강 시간", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기", tint = Purple)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF9FAFB)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 상단 안내 멘트
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = "팀 공강 추천", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "보라색으로 표시된 시간은 모두가 만날 수 있는 공강 시간이에요!",
                        fontSize = 13.sp,
                        color = Gray500
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 합쳐진 시간표 그리기
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                MergedTimetableGrid(mergedBusyTimes = mergedBusyTimes)
            }
        }
    }
}

@Composable
private fun MergedTimetableGrid(mergedBusyTimes: Map<String, List<String>>) {
    val days = listOf("월", "화", "수", "목", "금")
    val times = listOf(
        "09:00", "09:30", "10:00", "10:30", "11:00", "11:30",
        "12:00", "12:30", "13:00", "13:30", "14:00", "14:30",
        "15:00", "15:30", "16:00", "16:30", "17:00", "17:30", "18:00"
    )

    Column(
        modifier = Modifier
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 요일 헤더
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(52.dp))
            days.forEach { day ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(day, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Gray700)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // 시간별 행
        times.forEach { timeStr ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 시간 표시
                Text(
                    text = if (timeStr.endsWith(":00")) timeStr else "",
                    fontSize = 9.sp,
                    color = Gray400,
                    modifier = Modifier.width(52.dp),
                    textAlign = TextAlign.Center
                )

                // 각 요일별 칸 색칠하기
                days.forEach { day ->
                    val busyTimesByDay = mergedBusyTimes[day] ?: emptyList()
                    val isBusy = busyTimesByDay.contains(timeStr)

                    // 핵심 로직: 누군가 수업이 있으면(Busy) 회색, 모두 비어있으면(공강) 보라색!
                    val cellColor = if (isBusy) Gray100 else Purple

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(24.dp)
                            .padding(1.dp)
                            .background(cellColor, RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}
