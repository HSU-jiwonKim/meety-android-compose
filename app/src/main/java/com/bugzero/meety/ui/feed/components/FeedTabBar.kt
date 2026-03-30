package com.bugzero.meety.ui.feed.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugzero.meety.ui.feed.FeedConstants
import com.bugzero.meety.ui.feed.FeedViewMode

/**
 * 추천 / 전체 목록 탭 전환 바
 */
@Composable
fun FeedTabBar(
    currentMode: FeedViewMode,
    onModeChange: (FeedViewMode) -> Unit
) {
    val isRecommend = currentMode == FeedViewMode.RECOMMEND
    val isList = currentMode == FeedViewMode.LIST

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .border(BorderStroke(1.5.dp, FeedConstants.PurpleBorder), RoundedCornerShape(25.dp))
                .background(Color.White, RoundedCornerShape(25.dp))
                .padding(4.dp)
        ) {
            Row {
                TabItem(
                    label = "추천",
                    icon = Icons.Default.AutoAwesome,
                    isSelected = isRecommend,
                    onClick = { onModeChange(FeedViewMode.RECOMMEND) }
                )
                TabItem(
                    label = "전체 목록",
                    icon = Icons.Default.FormatListBulleted,
                    isSelected = isList,
                    onClick = { onModeChange(FeedViewMode.LIST) }
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val activeColor = if (isSelected) Color.White else FeedConstants.GradientStart

    Box(
        modifier = Modifier
            .height(36.dp)
            .then(
                if (isSelected) Modifier.background(FeedConstants.GradientPurplePink, RoundedCornerShape(20.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = activeColor
            )
            Text(
                label,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = activeColor
            )
        }
    }
}
