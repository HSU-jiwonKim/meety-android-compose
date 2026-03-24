package com.bugzero.meety.ui.feed

<<<<<<< HEAD
import androidx.compose.runtime.Composable

@Composable
fun MeetingDetailScreen() {
    // TODO: B 담당 - 팀 상세 정보 · 좋아요/패스 화면 구현
=======
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugzero.meety.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingDetailScreen(
    onBackClick: () -> Unit = {},
    team: MockTeam? = null,
    onLikeClick: () -> Unit = {},
    onPassClick: () -> Unit = {}
) {
    // 데이터가 없을 경우 처리
    if (team == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("데이터를 불러올 수 없습니다.")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(team.name, fontWeight = FontWeight.Bold) }, // 동적 이름
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로", tint = Purple)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onPassClick()
                        onBackClick() // 처리 후 뒤로가기
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("패스", color = Gray500, fontWeight = FontWeight.Bold) }
                Button(
                    onClick = {
                        onLikeClick()
                        onBackClick() // 처리 후 뒤로가기
                    },
                    modifier = Modifier.weight(2f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Purple)
                ) { Text("💜 좋아요!", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(Color(0xFFF9FAFB)).padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(100.dp).clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFFA78BFA), Color(0xFFF472B6)))),
                            contentAlignment = Alignment.Center
                        ) { Text("☕", fontSize = 48.sp) }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(team.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Gray900)
                        Text("${team.department} · ${team.size}", fontSize = 14.sp, color = Gray500)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            team.tags.forEach { tag -> // 태그 동적 렌더링
                                Box(
                                    modifier = Modifier.background(Color(0xFFEDE9FE), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) { Text(tag, fontSize = 12.sp, color = Purple) }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Purple, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("AI 추천 이유", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Purple)
                            Text("${team.name}은 당신의 취향 점수가 높은 팀입니다!", fontSize = 13.sp, color = Gray700)
                        }
                    }
                }
            }
            // 팀원 소개

            item {

                Card(

                    modifier = Modifier.fillMaxWidth(),

                    shape = RoundedCornerShape(16.dp),

                    colors = CardDefaults.cardColors(containerColor = Color.White)

                ) {

                    Column(modifier = Modifier.padding(16.dp)) {

                        Text("팀원 소개", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Gray900)

                        Spacer(modifier = Modifier.height(12.dp))

                        listOf(

                            Triple("😊", "박준호", "23 · ENFJ · 경영학과"),

                            Triple("🧑", "이서윤", "22 · ISFP · 경영학과"),

                            Triple("👩", "최민수", "24 · ENTP · 경영학과"),

                            ).forEach { (emoji, name, info) ->

                            Row(

                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Box(

                                    modifier = Modifier.size(44.dp).clip(CircleShape)

                                        .background(Color(0xFFEDE9FE)),

                                    contentAlignment = Alignment.Center

                                ) { Text(emoji, fontSize = 20.sp) }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {

                                    Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Gray900)

                                    Text(info, fontSize = 12.sp, color = Gray500)

                                }

                            }

                            HorizontalDivider(color = Color(0xFFF3F4F6))

                        }

                    }

                }

            }
        }
    }
>>>>>>> ca178ecdf5d6d6fc1948225597641ad0fce1c061
}