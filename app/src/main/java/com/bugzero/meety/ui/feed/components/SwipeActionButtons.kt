package com.bugzero.meety.ui.feed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bugzero.meety.ui.feed.FeedConstants
import com.bugzero.meety.ui.theme.Gray500

/**
 * 스와이프 카드 하단의 되돌리기 / 패스 / 좋아요 버튼 묶음
 */
@Composable
fun SwipeActionButtons(
    onUndo: () -> Unit,
    onPass: () -> Unit,
    onLike: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 되돌리기
        Box(
            modifier = Modifier
                .size(52.dp)
                .shadow(4.dp, CircleShape)
                .background(Color.White, CircleShape)
                .clickable(onClick = onUndo),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Undo, "되돌리기", tint = Gray500, modifier = Modifier.size(26.dp))
        }

        Spacer(Modifier.width(20.dp))

        // 패스 (X)
        Box(
            modifier = Modifier
                .size(64.dp)
                .shadow(6.dp, CircleShape)
                .background(Color.White, CircleShape)
                .border(2.dp, FeedConstants.PassRed.copy(alpha = 0.4f), CircleShape)
                .clickable(onClick = onPass),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Close, "패스", tint = FeedConstants.PassRed, modifier = Modifier.size(32.dp))
        }

        Spacer(Modifier.width(20.dp))

        // 좋아요 (하트)
        Box(
            modifier = Modifier
                .size(64.dp)
                .shadow(6.dp, CircleShape)
                .background(FeedConstants.GradientPurplePink, CircleShape)
                .border(2.dp, FeedConstants.PassRed.copy(alpha = 0.4f), CircleShape)
                .clickable(onClick = onLike),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Favorite, "좋아요", tint = Color.White, modifier = Modifier.size(32.dp))
        }
    }
}
