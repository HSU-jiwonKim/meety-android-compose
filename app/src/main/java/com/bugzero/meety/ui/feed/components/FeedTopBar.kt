package com.bugzero.meety.ui.feed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugzero.meety.ui.feed.FeedConstants
import com.bugzero.meety.ui.theme.Brand2
import com.bugzero.meety.ui.theme.Ink
import com.bugzero.meety.ui.theme.Line

/**
 * 피드 화면 상단 앱바 (목업 .appbar)
 * 그라데이션 로고 + Meety 워드마크 / 라운드 알림 버튼(미읽음 핑크 점)
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
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── 로고 + 워드마크 ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            // clip → drawWithCache: 그라데이션 rect 먼저, 그 위에 흰 아이콘
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .drawWithCache {
                        val brush = FeedConstants.GradientPurplePink
                        onDrawWithContent {
                            drawRect(brush)
                            drawContent()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(Modifier.width(9.dp))
            // TextStyle(brush=...) 방식 — BlendMode 방식보다 모든 기기에서 안정적
            Text(
                text = "Meety",
                style = TextStyle(
                    brush = FeedConstants.GradientPurplePink,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.4).sp
                )
            )
        }

        // ── 알림 버튼 ──
        Box(
            modifier = Modifier
                .size(40.dp)
                .shadow(2.dp, RoundedCornerShape(13.dp))
                .background(Color.White, RoundedCornerShape(13.dp))
                .border(1.dp, Line, RoundedCornerShape(13.dp))
                .clickable(onClick = onNotificationClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Notifications,
                contentDescription = "알림",
                tint = Ink,
                modifier = Modifier.size(20.dp)
            )
            if (hasUnreadNotification) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-9).dp, y = 8.dp)
                        .size(8.dp)
                        .background(Brand2, CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                )
            }
        }
    }
}
