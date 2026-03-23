package com.bugzero.meety.ui.team

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class TeamBottomTab {
    HOME, MATCHING, CREATE_TEAM, CHAT, PROFILE
}

@Composable
fun TeamCommonTopBar(
    onSearchClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {
    val gradientBrush = Brush.linearGradient(listOf(Color(0xFFB44FD3), Color(0xFFEC4899)))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 피드와 동일한 로고 + 그라데이션 텍스트
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(gradientBrush, RoundedCornerShape(12.dp)),
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
                    val brush = Brush.linearGradient(
                        listOf(Color(0xFFB44FD3), Color(0xFFEC4899))
                    )
                    onDrawWithContent {
                        drawContent()
                        drawRect(brush, blendMode = BlendMode.SrcAtop)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        IconButton(onClick = onSearchClick) {
            Icon(Icons.Default.Search, contentDescription = "검색", tint = Color(0xFF4B4B4B))
        }

        Box {
            IconButton(onClick = onNotificationClick) {
                Icon(Icons.Default.Notifications, contentDescription = "알림", tint = Color(0xFF4B4B4B))
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color(0xFFEC4899), CircleShape)
                    .align(Alignment.TopEnd)
                    .offset(x = (-10).dp, y = 10.dp)
            )
        }
    }
}