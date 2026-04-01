package com.bugzero.meety.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bugzero.meety.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleSyncScreen(
    onBackClick: () -> Unit = {},
    viewModel: ScheduleViewModel = viewModel()
) {
    val days = listOf("월", "화", "수", "목", "금")
    val times = listOf(
        "09:00", "09:30",
        "10:00", "10:30",
        "11:00", "11:30",
        "12:00", "12:30",
        "13:00", "13:30",
        "14:00", "14:30",
        "15:00", "15:30",
        "16:00", "16:30",
        "17:00", "17:30",
        "18:00"
    )

    val selectedCells by viewModel.selectedCells.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) onBackClick()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("시간표 설정", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로", tint = Purple)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9FAFB))
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("수업 시간을 선택해주세요", fontSize = 14.sp, color = Gray500)
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
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
                    times.forEachIndexed { timeIdx, timeStr ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 정시만 시간 표시, 30분은 빈칸
                            Text(
                                text = if (timeStr.endsWith(":00")) timeStr else "",
                                fontSize = 9.sp,
                                color = Gray400,
                                modifier = Modifier.width(52.dp)
                            )
                            days.forEachIndexed { dayIdx, _ ->
                                val cell = Pair(timeIdx, dayIdx)
                                val isSelected = cell in selectedCells
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(24.dp)
                                        .padding(1.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (isSelected)
                                                Brush.linearGradient(listOf(Purple, Color(0xFFF472B6)))
                                            else
                                                Brush.linearGradient(listOf(Gray100, Gray100))
                                        )
                                        .clickable { viewModel.toggleCell(cell) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 범례
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Brush.linearGradient(listOf(Purple, Color(0xFFF472B6))))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("수업", fontSize = 12.sp, color = Gray500)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Gray100)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("공강", fontSize = 12.sp, color = Gray500)
                }
                Text(
                    "선택: ${selectedCells.size}칸",
                    fontSize = 12.sp,
                    color = Purple,
                    fontWeight = FontWeight.Medium
                )
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, fontSize = 12.sp, color = Color.Red)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.saveSchedule() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Purple),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("저장하기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}