package com.bugzero.meety.ui.feed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugzero.meety.ui.feed.FeedConstants
import com.bugzero.meety.ui.team.Team
import com.bugzero.meety.ui.theme.Purple
import kotlin.math.roundToInt

/**
 * 틴더 스타일의 스와이프 카드
 *
 * - 좌우로 드래그하면 Like/Pass 처리
 * - 탭하면 상세보기
 */
@Composable
fun SwipeCard(
    team: Team,
    onLike: () -> Unit,
    onPass: () -> Unit,
    onInfo: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val rotation = offsetX / FeedConstants.ROTATION_DIVISOR

    // teamId 해시값으로 카드 배경 색상을 다양하게 결정
    val colorIndex = (team.teamId.hashCode() and Int.MAX_VALUE) % FeedConstants.CardColorPalette.size
    val bgColors = FeedConstants.CardColorPalette[colorIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(offsetX.roundToInt(), 0) }
            .rotate(rotation)
            .shadow(12.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .pointerInput(team.teamId) {
                detectDragGestures(
                    onDragEnd = {
                        when {
                            offsetX > FeedConstants.SWIPE_THRESHOLD -> { onLike(); offsetX = 0f }
                            offsetX < -FeedConstants.SWIPE_THRESHOLD -> { onPass(); offsetX = 0f }
                            else -> { offsetX = 0f }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                    }
                )
            }
            .clickable { onInfo() }
    ) {
        // 배경 그라데이션
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(bgColors)))
        // 하단 어둡게
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.4f to Color.Transparent, 1.0f to Color.Black.copy(alpha = 0.7f))
            )
        )

        // "탭하여 자세히 보기" 안내
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 16.dp)
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .clickable { onInfo() }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("👆", fontSize = 13.sp)
                Spacer(Modifier.width(4.dp))
                Text("탭하여 자세히 보기", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }

        // 팀 정보
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(team.teamName, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(Purple.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "${team.memberIds.size}명",
                        fontSize = 13.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(team.description, fontSize = 16.sp, color = Color.LightGray, maxLines = 2)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                team.tags.take(3).forEach { tag ->
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("#$tag", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
