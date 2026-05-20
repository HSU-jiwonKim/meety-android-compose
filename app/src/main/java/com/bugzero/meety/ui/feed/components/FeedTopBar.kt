package com.bugzero.meety.ui.feed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugzero.meety.ui.feed.FeedConstants

/**
 * 피드 화면 상단의 앱바
 *
 * Meety 로고 + 검색 / 알림 아이콘
 */
@Composable
fun FeedTopBar(
    onSearchClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    hasUnreadNotification: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── 로고 ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(FeedConstants.GradientPurplePink, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "Meety",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.drawWithCache {
                    val brush = FeedConstants.GradientPurplePink
                    onDrawWithContent {
                        drawContent()
                        drawRect(brush, blendMode = BlendMode.SrcAtop)
                    }
                }
            )
        }

        // ── 아이콘 버튼들 ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "검색", tint = FeedConstants.IconGray)
            }
            Box {
                IconButton(onClick = onNotificationClick) {
                    Icon(Icons.Default.Notifications, contentDescription = "알림", tint = FeedConstants.IconGray)
                }
                // 미읽음 알림이 있을 때만 빨간 점 표시
                if (hasUnreadNotification) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(FeedConstants.AccentPink, CircleShape)
                            .align(Alignment.TopEnd)
                            .offset(x = (-10).dp, y = 10.dp)
                    )
                }
            }
        }
    }
}
