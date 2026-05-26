package com.bugzero.meety.ui.feed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugzero.meety.ui.feed.FeedConstants
import com.bugzero.meety.ui.theme.Brand1
import com.bugzero.meety.ui.theme.Ink3
import com.bugzero.meety.ui.theme.PassGray

/**
 * 스와이프 카드 하단 액션 버튼 (목업 .swipe-actions)
 * 되돌리기(sm) · 패스(lg) · 좋아요(lg) · 자세히(sm) + 안내 힌트
 */
@Composable
fun SwipeActionButtons(
    onUndo: () -> Unit,
    onPass: () -> Unit,
    onLike: () -> Unit,
    onInfo: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 되돌리기
            ActionButton(size = 52.dp, bg = Color.White, onClick = onUndo) {
                Icon(Icons.Default.Undo, "되돌리기", tint = Color(0xFFF5A623), modifier = Modifier.size(22.dp))
            }
            // 패스 (X)
            ActionButton(size = 66.dp, bg = Color.White, onClick = onPass) {
                Icon(Icons.Default.Close, "패스", tint = PassGray, modifier = Modifier.size(28.dp))
            }
            // 좋아요 (그라데이션 하트)
            ActionButton(size = 66.dp, gradient = true, onClick = onLike) {
                Icon(Icons.Default.Favorite, "좋아요", tint = Color.White, modifier = Modifier.size(28.dp))
            }
            // 자세히
            ActionButton(size = 52.dp, bg = Color.White, onClick = onInfo) {
                Icon(Icons.Default.Info, "자세히", tint = Brand1, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "← 패스 · 좋아요 → · 카드를 탭하면 상세보기",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Ink3
        )
    }
}

@Composable
private fun ActionButton(
    size: Dp,
    onClick: () -> Unit,
    bg: Color = Color.White,
    gradient: Boolean = false,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(6.dp, CircleShape)
            .then(
                if (gradient) Modifier.background(FeedConstants.GradientPurplePink, CircleShape)
                else Modifier.background(bg, CircleShape)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}
