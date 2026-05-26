package com.bugzero.meety.ui.feed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugzero.meety.ui.feed.FeedViewMode
import com.bugzero.meety.ui.theme.Ink
import com.bugzero.meety.ui.theme.Ink3

/**
 * 추천 / 목록 세그먼트 컨트롤 (목업 .seg 스타일)
 * 회색 컨테이너 안에서 선택된 탭이 흰색 알약으로 표시된다.
 */
private val SegTrack = Color(0xFFEAE8F0)

@Composable
fun FeedTabBar(
    currentMode: FeedViewMode,
    onModeChange: (FeedViewMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .background(SegTrack, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        SegItem(
            label = "추천",
            isSelected = currentMode == FeedViewMode.RECOMMEND,
            onClick = { onModeChange(FeedViewMode.RECOMMEND) },
            modifier = Modifier.weight(1f)
        )
        SegItem(
            label = "목록",
            isSelected = currentMode == FeedViewMode.LIST,
            onClick = { onModeChange(FeedViewMode.LIST) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SegItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .then(
                if (isSelected)
                    Modifier
                        .shadow(2.dp, RoundedCornerShape(11.dp))
                        .background(Color.White, RoundedCornerShape(11.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp,
            color = if (isSelected) Ink else Ink3
        )
    }
}
