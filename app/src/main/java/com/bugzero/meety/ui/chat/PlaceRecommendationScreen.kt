package com.bugzero.meety.ui.chat

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.bugzero.meety.data.repository.PlaceResult

// ─── 대중교통 소요시간 정보 ───────────────────────────────────────────────────
data class TransitUserInfo(
    val userId: String,
    val userName: String,
    val profileImage: String = "",
    val location: String = "",     // 출발지 (예: "서울 강남구")
    val minutes: Int = -1          // 소요 시간(분). -1 = 계산 실패/없음
)

// ─── 컬러 팔레트 ─────────────────────────────────────────────────────────────
private object PlaceColors {
    val primary = Color(0xFF6C5CE7)
    val primaryLight = Color(0xFFA29BFE)
    val primaryDark = Color(0xFF5A4BD1)
    val accent = Color(0xFFFDCB6E)
    val bg = Color(0xFFF8F7FC)
    val card = Color.White
    val text = Color(0xFF1A1A2E)
    val textSecondary = Color(0xFF6B7280)
    val textTertiary = Color(0xFF9CA3AF)
    val border = Color(0xFFF0EDF6)
    val chipBg = Color(0xFFF3F1FA)
    val badge = Color(0xFFFF6B6B)
    val save = Color(0xFFFF6B81)
    val success = Color(0xFF00B894)
}

// ─── 필터 데이터 ──────────────────────────────────────────────────────────────
private data class FilterItem(val label: String, val emoji: String)

private val defaultFilters = listOf(
    FilterItem("찜", "❤️"),
    FilterItem("카페", "☕"),
    FilterItem("음식점", "🍽️"),
    FilterItem("중식", "🥡"),
    FilterItem("한식", "🍚"),
    FilterItem("양식", "🍕"),
    FilterItem("일식", "🍣"),
)

// ─── 카테고리 → 이모지 매핑 ──────────────────────────────────────────────────
private fun categoryEmoji(category: String): String {
    return when {
        category.contains("카페") || category.contains("커피") -> "☕"
        category.contains("디저트") || category.contains("베이커리") || category.contains("빵") -> "🍰"
        category.contains("음식점") || category.contains("맛집") || category.contains("식당") -> "🍽️"
        category.contains("브런치") -> "🥯"
        category.contains("차") || category.contains("티") -> "🍵"
        category.contains("술") || category.contains("바") || category.contains("주점") -> "🍷"
        category.contains("고기") || category.contains("삼겹") -> "🥩"
        category.contains("치킨") -> "🍗"
        category.contains("피자") || category.contains("양식") -> "🍕"
        category.contains("일식") || category.contains("초밥") -> "🍣"
        category.contains("중식") || category.contains("중국") -> "🥡"
        category.contains("한식") -> "🍚"
        else -> "📍"
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 메인 장소 추천 화면 (BottomSheet / FullScreen Dialog 용)
// ═══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceRecommendationScreen(
    isLoading: Boolean,
    places: List<PlaceResult>,
    error: String?,
    areaName: String = "",
    participantCount: Int = 0,
    transitAverages: Map<String, Int> = emptyMap(),          // placeKey → 평균 분
    transitBreakdowns: Map<String, List<TransitUserInfo>> = emptyMap(), // placeKey → 사용자별 시간
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
    onFilterChanged: (List<String>) -> Unit = {},
    onSharePlace: (PlaceResult) -> Unit = {},
    // ── 재추천 개선용 파라미터 ──────────────────────────────────────────────
    notice: String? = null,                               // "이미 본 3곳 제외 · 반경 1.5km로 넓힘"
    onDismissNotice: () -> Unit = {},
    savedPlaces: List<PlaceResult> = emptyList(),
    savedPlaceKeys: Set<String> = emptySet(),
    onToggleSave: (PlaceResult) -> Unit = {},
    initialRadiusMeters: Int = 800,
    showConditionSheet: Boolean = false,
    onOpenConditionSheet: () -> Unit = {},
    onCloseConditionSheet: () -> Unit = {},
    onApplyConditions: (radius: Int, keywords: List<String>, includeShown: Boolean) -> Unit = { _, _, _ -> },
    // ── 지역 직접 검색 (다른 지역으로 검색) — 파라미터 유지 (하위 호환용)
    searchRegion: String? = null,
    onSelectRegion: (String) -> Unit = {},
    onReturnToMidpoint: () -> Unit = {},
    // ── 지역 기준 대중교통 시간 (가게별 X, 지역 단위로 한 번만 계산) ─────────
    regionAvgTransitMin: Int? = null,
    regionTransitBreakdown: List<TransitUserInfo> = emptyList()
) {
    val activeFilters = remember { mutableStateListOf<String>() }
    val context = LocalContext.current
    var showRegionPicker by remember { mutableStateOf(false) }
    val isRegionMode = !searchRegion.isNullOrBlank()

    // 필터 단일 선택 토글 — 한 번에 하나만 선택
    fun toggleFilter(label: String) {
        if (activeFilters.contains(label)) {
            activeFilters.clear()
        } else {
            activeFilters.clear()
            activeFilters.add(label)
        }
        onFilterChanged(activeFilters.toList())
    }

    // 지역 선택 시트
    if (showRegionPicker) {
        RegionPickerSheet(
            isRegionMode = isRegionMode,
            onDismiss = { showRegionPicker = false },
            onSelect = { region ->
                showRegionPicker = false
                onSelectRegion(region)
            },
            onReturnToMidpoint = {
                showRegionPicker = false
                onReturnToMidpoint()
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PlaceColors.bg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ─── 헤더 ──────────────────────────────────────────────────────
            PlaceHeader(
                areaName = if (isRegionMode) searchRegion.orEmpty() else areaName,
                participantCount = participantCount,
                isRegionMode = isRegionMode,
                onClose = onDismiss,
                onAreaNameClick = { showRegionPicker = true },
                regionAvgTransitMin = regionAvgTransitMin,
                regionTransitBreakdown = regionTransitBreakdown
            )

            // ─── 필터 칩 ────────────────────────────────────────────────────
            FilterChipRow(
                filters = defaultFilters,
                activeFilters = activeFilters,
                onToggle = ::toggleFilter
            )

            // ─── 변경 사유 안내 배너 (2단계) ─────────────────────────────────
            // "이미 본 3곳 제외 · 반경 1.5km로 넓힘" 같은 1회성 안내. 3초 뒤 자동 소거.
            RefreshNoticeBanner(
                text = notice,
                onDismiss = onDismissNotice
            )

            // ─── 스크롤 콘텐츠 ──────────────────────────────────────────────
            val isSavedMode = activeFilters.contains("찜")
            Box(modifier = Modifier.weight(1f)) {
                when {
                    // ── 찜 모드 ──────────────────────────────────────────────
                    isSavedMode -> {
                        if (savedPlaces.isEmpty()) {
                            EmptySavedContent()
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 20.dp, end = 20.dp,
                                    top = 16.dp, bottom = 40.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                item {
                                    Text(
                                        text = "❤️ 찜한 장소 ${savedPlaces.size}곳",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PlaceColors.text,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                itemsIndexed(savedPlaces) { index, place ->
                                    PlaceCard(
                                        rank = index + 1,
                                        place = place,
                                        isTop = false,
                                        avgTransitMin = null,
                                        transitBreakdown = emptyList(),
                                        isSaved = true,
                                        onToggleSave = { onToggleSave(place) },
                                        onShare = { onSharePlace(place) },
                                        onNavigate = {
                                            val uri = if (place.lat != 0.0 && place.lng != 0.0)
                                                Uri.parse("nmap://route/public?dlat=${place.lat}&dlng=${place.lng}&dname=${Uri.encode(place.name)}&appname=com.bugzero.meety")
                                            else Uri.parse("nmap://search?query=${Uri.encode(place.name)}&appname=com.bugzero.meety")
                                            val intent = Intent(Intent.ACTION_VIEW, uri).apply { addCategory(Intent.CATEGORY_BROWSABLE) }
                                            try { context.startActivity(intent) } catch (e: Exception) {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://map.naver.com/v5/search/${Uri.encode(place.name)}")))
                                            }
                                        },
                                        onDetail = {
                                            val nmapUri = if (place.placeId.isNotBlank())
                                                Uri.parse("nmap://place?id=${place.placeId}&appname=com.bugzero.meety")
                                            else Uri.parse("nmap://search?query=${Uri.encode(place.name)}&appname=com.bugzero.meety")
                                            val fallbackUri = if (place.placeId.isNotBlank())
                                                Uri.parse("https://m.place.naver.com/place/${place.placeId}/home")
                                            else Uri.parse("https://map.naver.com/v5/search/${Uri.encode(place.name)}")
                                            try { context.startActivity(Intent(Intent.ACTION_VIEW, nmapUri).apply { addCategory(Intent.CATEGORY_BROWSABLE) }) }
                                            catch (e: Exception) { context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUri)) }
                                        },
                                        onCall = {
                                            if (place.phone.isNotBlank()) {
                                                try { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${place.phone}"))) } catch (_: Exception) {}
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // ── 일반 추천 모드 ────────────────────────────────────────
                    isLoading -> LoadingContent()

                    error != null && places.isEmpty() -> ErrorContent(
                        error = error,
                        onRetry = onRefresh
                    )

                    places.isNotEmpty() -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 20.dp, end = 20.dp,
                                top = 16.dp, bottom = 100.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // 섹션 타이틀
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "추천 장소",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PlaceColors.text
                                    )
                                    Text(
                                        text = "중간 지점 기준",
                                        fontSize = 12.sp,
                                        color = PlaceColors.textTertiary
                                    )
                                }
                            }

                            // 장소 카드들
                            itemsIndexed(places) { index, place ->
                                val isSaved = savedPlaceKeys.contains("${place.name}|${place.address}")
                                PlaceCard(
                                    rank = index + 1,
                                    place = place,
                                    isTop = index == 0,
                                    avgTransitMin = regionAvgTransitMin,
                                    transitBreakdown = regionTransitBreakdown,
                                    isSaved = isSaved,
                                    onToggleSave = { onToggleSave(place) },
                                    onShare = { onSharePlace(place) },
                                    onNavigate = {
                                        // 네이버 지도 앱으로 대중교통 길찾기
                                        val uri = if (place.lat != 0.0 && place.lng != 0.0) {
                                            Uri.parse("nmap://route/public?dlat=${place.lat}&dlng=${place.lng}&dname=${Uri.encode(place.name)}&appname=com.bugzero.meety")
                                        } else {
                                            Uri.parse("nmap://search?query=${Uri.encode(place.name)}&appname=com.bugzero.meety")
                                        }
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        intent.addCategory(Intent.CATEGORY_BROWSABLE)
                                        // 네이버 지도 앱이 없으면 브라우저 대중교통 경로로 열기
                                        val fallback = Intent(
                                            Intent.ACTION_VIEW,
                                            if (place.lat != 0.0 && place.lng != 0.0) {
                                                Uri.parse(
                                                    "https://map.naver.com/v5/directions/-/" +
                                                        "${place.lng},${place.lat},${Uri.encode(place.name)},,PLACE_POI" +
                                                        "/-/transit"
                                                )
                                            } else {
                                                Uri.parse("https://map.naver.com/v5/search/${Uri.encode(place.name)}")
                                            }
                                        )
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            context.startActivity(fallback)
                                        }
                                    },
                                    onDetail = {
                                        // 네이버 지도 앱에서 해당 업체 상세화면 바로 열기
                                        // placeId가 있으면 nmap://place?id=... 로 상세 페이지 직접 진입,
                                        // 없으면 검색 fallback.
                                        val nmapUri = if (place.placeId.isNotBlank()) {
                                            Uri.parse("nmap://place?id=${place.placeId}&appname=com.bugzero.meety")
                                        } else {
                                            Uri.parse("nmap://search?query=${Uri.encode(place.name)}&appname=com.bugzero.meety")
                                        }
                                        val intent = Intent(Intent.ACTION_VIEW, nmapUri)
                                        intent.addCategory(Intent.CATEGORY_BROWSABLE)
                                        // 네이버 지도 앱이 없으면 웹 m.place.naver.com 상세 페이지로 fallback
                                        val fallbackUri = if (place.placeId.isNotBlank()) {
                                            Uri.parse("https://m.place.naver.com/place/${place.placeId}/home")
                                        } else {
                                            Uri.parse("https://map.naver.com/v5/search/${Uri.encode(place.name)}")
                                        }
                                        val fallback = Intent(Intent.ACTION_VIEW, fallbackUri)
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            context.startActivity(fallback)
                                        }
                                    },
                                    onCall = {
                                        if (place.phone.isNotBlank()) {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${place.phone}"))
                                            try { context.startActivity(intent) } catch (_: Exception) {}
                                        }
                                    }
                                )
                            }
                        }
                    }

                    else -> EmptyContent()
                }
            }
        }

    }
}

// ─── 헤더 ─────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceHeader(
    areaName: String,
    participantCount: Int,
    onClose: () -> Unit,
    isRegionMode: Boolean = false,
    onAreaNameClick: () -> Unit = {},
    regionAvgTransitMin: Int? = null,
    regionTransitBreakdown: List<TransitUserInfo> = emptyList()
) {
    Surface(
        color = PlaceColors.card,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "만나기 좋은 장소",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PlaceColors.text,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(Modifier.height(6.dp))
                    // 지역명 — 탭하면 지역 변경 시트 오픈
                    Surface(
                        onClick = onAreaNameClick,
                        shape = RoundedCornerShape(8.dp),
                        color = if (isRegionMode)
                            PlaceColors.primary.copy(alpha = 0.08f)
                        else
                            PlaceColors.chipBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = PlaceColors.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = buildString {
                                    if (areaName.isNotBlank()) append(areaName)
                                    else append("중간 지점")
                                    if (participantCount > 0) append(" · ${participantCount}명")
                                },
                                fontSize = 13.sp,
                                color = if (isRegionMode) PlaceColors.primary else PlaceColors.textSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                Icons.Default.KeyboardArrowRight,
                                contentDescription = "지역 변경",
                                tint = if (isRegionMode) PlaceColors.primary else PlaceColors.textTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // 닫기 버튼
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PlaceColors.chipBg)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = PlaceColors.textSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// ─── 필터 칩 ──────────────────────────────────────────────────────────────────
@Composable
private fun FilterChipRow(
    filters: List<FilterItem>,
    activeFilters: List<String>,
    onToggle: (String) -> Unit
) {
    Surface(
        color = PlaceColors.card,
        shadowElevation = 1.dp
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            items(filters) { filter ->
                val isActive = activeFilters.contains(filter.label)
                FilterChipItem(
                    label = filter.label,
                    emoji = filter.emoji,
                    isActive = isActive,
                    onClick = { onToggle(filter.label) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipItem(
    label: String,
    emoji: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        if (isActive) PlaceColors.primary else PlaceColors.card,
        label = "chipBg"
    )
    val textColor by animateColorAsState(
        if (isActive) Color.White else PlaceColors.text,
        label = "chipText"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = bgColor,
        border = BorderStroke(
            width = if (isActive) 2.dp else 1.5.dp,
            color = if (isActive) PlaceColors.primary else PlaceColors.border
        ),
        shadowElevation = if (isActive) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(emoji, fontSize = 13.sp)
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                color = textColor
            )
        }
    }
}

// ─── 장소 카드 ────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceCard(
    rank: Int,
    place: PlaceResult,
    isTop: Boolean,
    avgTransitMin: Int? = null,
    transitBreakdown: List<TransitUserInfo> = emptyList(),
    isSaved: Boolean = false,
    onToggleSave: () -> Unit = {},
    onShare: () -> Unit,
    onNavigate: () -> Unit,
    onDetail: () -> Unit,
    onCall: () -> Unit
) {
    var showTransitDetail by remember { mutableStateOf(false) }
    if (showTransitDetail) {
        TransitDetailDialog(
            placeName = place.name,
            avgMinutes = avgTransitMin,
            breakdown = transitBreakdown,
            onDismiss = { showTransitDetail = false }
        )
    }
    val emoji = categoryEmoji(place.category)
    val context = LocalContext.current
    val hasImage = place.imageUrl.isNotBlank()
    val hasImages = place.imageUrls.isNotEmpty()

    // 전체화면 뷰어 상태: Pair(사진 리스트, 초기 인덱스)
    var fullscreenState by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }

    // 전체화면 뷰어 표시
    fullscreenState?.let { (photos, startIndex) ->
        FullscreenPhotoViewer(
            photos = photos,
            initialIndex = startIndex,
            onDismiss = { fullscreenState = null }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (isTop) 20.dp else 16.dp),
        colors = CardDefaults.cardColors(containerColor = PlaceColors.card),
        border = BorderStroke(
            if (isTop) 2.dp else 1.dp,
            if (isTop) PlaceColors.primaryLight else PlaceColors.border
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isTop) 8.dp else 4.dp
        )
    ) {
      Box(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 상위 1위 그라데이션 바
            if (isTop) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(PlaceColors.primary, PlaceColors.accent)
                            )
                        )
                )
            }

            Column(
                modifier = Modifier.padding(
                    if (isTop) PaddingValues(18.dp, 16.dp) else PaddingValues(16.dp, 14.dp)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    // ── 대표 이미지 (실제 사진 or 이모지 폴백) ──
                    Box(
                        modifier = Modifier
                            .size(if (isTop) 76.dp else 62.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isTop) Brush.linearGradient(
                                    listOf(
                                        PlaceColors.primaryLight.copy(alpha = 0.15f),
                                        PlaceColors.accent.copy(alpha = 0.15f)
                                    )
                                ) else Brush.linearGradient(
                                    listOf(PlaceColors.chipBg, PlaceColors.chipBg)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasImage) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(place.imageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = place.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        fullscreenState = Pair(place.imageUrls.ifEmpty { listOf(place.imageUrl) }, 0)
                                    }
                            )
                        } else {
                            Text(emoji, fontSize = if (isTop) 30.sp else 24.sp)
                        }

                        // 순위 뱃지 (이미지 위에 겹쳐서)
                        val rankColor = when (rank) {
                            1 -> Color(0xFFFFD700)
                            2 -> Color(0xFFC0C0C0)
                            3 -> Color(0xFFCD7F32)
                            else -> PlaceColors.textTertiary
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = (-2).dp, y = (-2).dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(rankColor)
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$rank",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    // ── 정보 영역 ──
                    Column(modifier = Modifier.weight(1f)) {
                        // 배지 + 카테고리
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isTop) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(PlaceColors.badge, Color(0xFFFF8E53))
                                            )
                                        )
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "🔥 추천 1위",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        letterSpacing = 0.3.sp
                                    )
                                }
                            }
                            Text(
                                text = place.category,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = PlaceColors.textTertiary
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        // 이름
                        Text(
                            text = place.name,
                            fontSize = if (isTop) 17.sp else 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlaceColors.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = (if (isTop) 22 else 20).sp
                        )

                        Spacer(Modifier.height(4.dp))

                        // ── 방문자 리뷰 수 ──
                        if (place.reviewCount > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = PlaceColors.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "방문자 리뷰 ${formatReviewCount(place.reviewCount)}개",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PlaceColors.primary
                                )
                            }
                            Spacer(Modifier.height(3.dp))
                        }

                        // 주소 (간략 표시: 시/도 생략, 구/군 + 도로명까지만)
                        val shortAddr = shortenAddress(place.address)
                        if (shortAddr.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = PlaceColors.textTertiary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = shortAddr,
                                    fontSize = 12.sp,
                                    color = PlaceColors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // 전화번호
                        if (place.phone.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = PlaceColors.textTertiary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = place.phone,
                                    fontSize = 11.sp,
                                    color = PlaceColors.textTertiary
                                )
                            }
                        }
                    }
                }

                // ── 📸 사진 갤러리 (네이버 지도 공식 사진, 클릭 시 전체화면) ──
                if (hasImages) {
                    Spacer(Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = place.imageUrls.take(6),
                            key = { _, url -> url }
                        ) { index, imgUrl ->
                            Box(
                                modifier = Modifier
                                    .size(width = 130.dp, height = 96.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PlaceColors.chipBg)
                                    .clickable {
                                        fullscreenState = Pair(place.imageUrls, index)
                                    }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imgUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "${place.name} 사진",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                // 여러 장일 때 확대 힌트 아이콘
                                if (place.imageUrls.size > 1 && index == 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.55f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${place.imageUrls.size}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 액션 버튼들
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 길찾기 버튼
                    Button(
                        onClick = onNavigate,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PlaceColors.primary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            Icons.Default.Navigation,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "길찾기",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // 상세보기 버튼
                    OutlinedButton(
                        onClick = onDetail,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, PlaceColors.border),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PlaceColors.text
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = PlaceColors.primary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "상세보기",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // 전화 버튼 (번호 있을 때만)
                    if (place.phone.isNotBlank()) {
                        OutlinedButton(
                            onClick = onCall,
                            modifier = Modifier
                                .size(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, PlaceColors.border),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = "전화",
                                modifier = Modifier.size(16.dp),
                                tint = PlaceColors.success
                            )
                        }
                    }
                }

                // ── 대중교통 평균 시간 말풍선 ──
                if (avgTransitMin != null && avgTransitMin >= 0) {
                    Spacer(Modifier.height(10.dp))
                    TransitBubble(
                        avgMinutes = avgTransitMin,
                        onClick = { showTransitDetail = true }
                    )
                }
            }
        }

        // ── 우측 상단 액션 (찜 + 공유) ──
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 찜(하트) — 토글. 찜한 장소는 재추천해도 제외되지 않음
            Surface(
                onClick = onToggleSave,
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = if (isSaved) PlaceColors.save.copy(alpha = 0.12f)
                        else PlaceColors.card,
                border = BorderStroke(
                    1.dp,
                    if (isSaved) PlaceColors.save.copy(alpha = 0.5f)
                    else PlaceColors.border
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isSaved) "찜 해제" else "찜하기",
                        modifier = Modifier.size(16.dp),
                        tint = if (isSaved) PlaceColors.save else PlaceColors.textSecondary
                    )
                }
            }

            // 공유(Send) 버튼
            Surface(
                onClick = onShare,
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = PlaceColors.primary.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, PlaceColors.primary.copy(alpha = 0.25f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "채팅방에 공유",
                        modifier = Modifier.size(16.dp),
                        tint = PlaceColors.primary
                    )
                }
            }
        }
      }
    }
}

// ─── 대중교통 평균 시간 말풍선 ────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransitBubble(
    avgMinutes: Int,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = PlaceColors.primary.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, PlaceColors.primary.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🚌", fontSize = 16.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "대중교통으로 평균 ${formatMinutes(avgMinutes)} 걸려요",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = PlaceColors.text,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = "자세히",
                tint = PlaceColors.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ─── 사용자별 소요시간 상세 다이얼로그 ────────────────────────────────────────
@Composable
private fun TransitDetailDialog(
    placeName: String,
    avgMinutes: Int?,
    breakdown: List<TransitUserInfo>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = PlaceColors.card,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🚌", fontSize = 22.sp)
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "대중교통 소요시간",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlaceColors.text
                        )
                        Text(
                            text = placeName,
                            fontSize = 12.sp,
                            color = PlaceColors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "닫기",
                            tint = PlaceColors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (avgMinutes != null && avgMinutes >= 0) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PlaceColors.primary.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "참여자 평균 ${formatMinutes(avgMinutes)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PlaceColors.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                if (breakdown.isEmpty()) {
                    Text(
                        text = "소요시간 정보를 불러올 수 없어요.",
                        fontSize = 13.sp,
                        color = PlaceColors.textSecondary
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        breakdown.forEach { info ->
                            TransitUserRow(info)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransitUserRow(info: TransitUserInfo) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 프로필 이미지 or 이니셜
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(PlaceColors.chipBg),
            contentAlignment = Alignment.Center
        ) {
            if (info.profileImage.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(info.profileImage)
                        .crossfade(true)
                        .build(),
                    contentDescription = info.userName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = info.userName.take(1).ifBlank { "?" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlaceColors.primary
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = info.userName.ifBlank { "참여자" },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = PlaceColors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (info.location.isNotBlank()) {
                Text(
                    text = info.location,
                    fontSize = 11.sp,
                    color = PlaceColors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = if (info.minutes >= 0) formatMinutes(info.minutes) else "정보 없음",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (info.minutes >= 0) PlaceColors.primary else PlaceColors.textTertiary
        )
    }
}

// ─── 분을 "X시간 Y분" 또는 "Y분" 으로 포매팅 ─────────────────────────────────
private fun formatMinutes(total: Int): String {
    if (total < 0) return "정보 없음"
    if (total < 60) return "${total}분"
    val h = total / 60
    val m = total % 60
    return if (m == 0) "${h}시간" else "${h}시간 ${m}분"
}

// ─── 장소 고유 키 (평균/브레이크다운 Map 조회용) ───────────────────────────────
internal fun placeTransitKey(place: PlaceResult): String =
    if (place.placeId.isNotBlank()) "id:${place.placeId}"
    else "ll:${place.lat},${place.lng}:${place.name}"

// ─── 리뷰 수 포매팅 ──────────────────────────────────────────────────────────
private fun formatReviewCount(count: Int): String = when {
    count >= 10000 -> "${count / 10000}.${(count % 10000) / 1000}만"
    count >= 1000  -> "${count / 1000},${String.format("%03d", count % 1000)}"
    else           -> "$count"
}

// ─── 주소 축약 ────────────────────────────────────────────────────────────────
// "서울 영등포구 당산로35길 3 1층" → "영등포구 당산로35길"
// 시/도는 생략하고, 구/군 + 도로명(또는 동) 까지만 보여줘 카드가 짤리지 않게 한다.
private val AddressSidoPrefixes = setOf(
    "서울", "서울특별시",
    "부산", "부산광역시",
    "대구", "대구광역시",
    "인천", "인천광역시",
    "광주", "광주광역시",
    "대전", "대전광역시",
    "울산", "울산광역시",
    "세종", "세종특별자치시",
    "경기", "경기도",
    "강원", "강원도", "강원특별자치도",
    "충북", "충청북도",
    "충남", "충청남도",
    "전북", "전라북도", "전북특별자치도",
    "전남", "전라남도",
    "경북", "경상북도",
    "경남", "경상남도",
    "제주", "제주도", "제주특별자치도"
)

private fun shortenAddress(addr: String): String {
    val trimmed = addr.trim()
    if (trimmed.isEmpty()) return ""
    val parts = trimmed.split(Regex("\\s+"))
    if (parts.isEmpty()) return trimmed

    // 1) 시/도 prefix 제거
    val start = if (parts[0] in AddressSidoPrefixes) 1 else 0
    // 2) 구/군 + 도로명(동) 까지만 — 최대 2 토큰
    val end = (start + 2).coerceAtMost(parts.size)
    val slice = parts.subList(start, end)
    return if (slice.isEmpty()) trimmed else slice.joinToString(" ")
}

// ─── 전체화면 사진 뷰어 ──────────────────────────────────────────────────────
@Composable
private fun FullscreenPhotoViewer(
    photos: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(initialPage = initialIndex.coerceIn(0, (photos.size - 1).coerceAtLeast(0))) {
        photos.size
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // ── 사진 슬라이더 ──
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(photos[page])
                            .crossfade(true)
                            .size(1920, 1920)   // 전체화면도 원본 대신 최대 1920px로 제한
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // ── 닫기 버튼 (우상단) ──
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "닫기",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // ── 사진 카운터 + 페이지 인디케이터 ──
            if (photos.size > 1) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 페이지 카운터 (예: 2 / 5)
                    Text(
                        text = "${pagerState.currentPage + 1} / ${photos.size}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f)
                    )

                    // 점 인디케이터
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        photos.indices.forEach { index ->
                            val isCurrent = index == pagerState.currentPage
                            Box(
                                modifier = Modifier
                                    .size(if (isCurrent) 8.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCurrent) Color.White
                                        else Color.White.copy(alpha = 0.45f)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── 로딩 상태 ────────────────────────────────────────────────────────────────
@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 로딩 애니메이션
            val infiniteTransition = rememberInfiniteTransition(label = "loading")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse"
            )

            Text(
                "📍",
                fontSize = (40 * scale).sp
            )
            Spacer(Modifier.height(20.dp))
            CircularProgressIndicator(
                color = PlaceColors.primary,
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "참여자 위치 분석 중...",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = PlaceColors.text
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "중간 지점을 계산하고 있어요",
                fontSize = 13.sp,
                color = PlaceColors.textTertiary
            )
        }
    }
}

// ─── 에러 상태 ────────────────────────────────────────────────────────────────
@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("😅", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = error,
                fontSize = 14.sp,
                color = PlaceColors.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PlaceColors.primary
                )
            ) {
                Text(
                    "다시 시도",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ─── 빈 상태 ──────────────────────────────────────────────────────────────────
@Composable
private fun EmptyContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("🗺️", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "장소 추천 버튼을 눌러\n주변 만남 장소를 찾아보세요!",
                fontSize = 14.sp,
                color = PlaceColors.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

// ─── 찜 빈 상태 ───────────────────────────────────────────────────────────────
@Composable
private fun EmptySavedContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("🤍", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "아직 찜한 장소가 없어요\n마음에 드는 곳에 ❤️를 눌러보세요",
                fontSize = 14.sp,
                color = PlaceColors.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

// ─── 변경 사유 안내 배너 (2단계) ──────────────────────────────────────────────
@Composable
private fun RefreshNoticeBanner(
    text: String?,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = !text.isNullOrBlank(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        // 3초 뒤 자동 소거
        LaunchedEffect(text) {
            if (!text.isNullOrBlank()) {
                kotlinx.coroutines.delay(3500)
                onDismiss()
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PlaceColors.primary.copy(alpha = 0.08f))
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ℹ️", fontSize = 13.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = text.orEmpty(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = PlaceColors.primary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─── 찜한 장소 가로 스크롤 (3단계) ────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedPlacesRow(
    savedPlaces: List<PlaceResult>,
    onRemove: (PlaceResult) -> Unit,
    onShare: (PlaceResult) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(
            items = savedPlaces,
            key = { "${it.name}|${it.address}" }
        ) { place ->
            Surface(
                modifier = Modifier
                    .width(180.dp)
                    .height(76.dp),
                shape = RoundedCornerShape(14.dp),
                color = PlaceColors.card,
                border = BorderStroke(1.dp, PlaceColors.save.copy(alpha = 0.3f)),
                shadowElevation = 2.dp,
                onClick = { onShare(place) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 썸네일 or 이모지
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PlaceColors.chipBg),
                        contentAlignment = Alignment.Center
                    ) {
                        if (place.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = place.imageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(categoryEmoji(place.category), fontSize = 22.sp)
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = place.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlaceColors.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = place.category,
                            fontSize = 11.sp,
                            color = PlaceColors.textTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // 찜 해제
                    Surface(
                        onClick = { onRemove(place) },
                        modifier = Modifier.size(26.dp),
                        shape = CircleShape,
                        color = Color.Transparent
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = "찜 해제",
                                tint = PlaceColors.save,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── 조건 바꾸기 BottomSheet (4단계) ─────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ConditionSheet(
    initialRadiusMeters: Int,
    initialKeywords: List<String>,
    onDismiss: () -> Unit,
    onApply: (radius: Int, keywords: List<String>, includeShown: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var radius by remember { mutableStateOf(initialRadiusMeters.toFloat()) }
    val selected = remember { mutableStateListOf<String>().apply { addAll(initialKeywords) } }
    var includeShown by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PlaceColors.card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = "어떤 장소를 보여드릴까요?",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = PlaceColors.text
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "반경과 분위기를 바꿔서 다시 추천받을 수 있어요",
                fontSize = 12.sp,
                color = PlaceColors.textTertiary
            )

            Spacer(Modifier.height(22.dp))

            // ── 반경 슬라이더 ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("📍 반경", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PlaceColors.text)
                Text(
                    text = formatRadius(radius.toInt()),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlaceColors.primary
                )
            }
            Slider(
                value = radius,
                onValueChange = { radius = it },
                valueRange = 500f..5000f,
                steps = 8,     // 500, 1000, 1500, 2000, 2500, 3000, 3500, 4000, 4500, 5000
                colors = SliderDefaults.colors(
                    thumbColor = PlaceColors.primary,
                    activeTrackColor = PlaceColors.primary,
                    inactiveTrackColor = PlaceColors.border
                )
            )

            Spacer(Modifier.height(14.dp))

            // ── 분위기 / 카테고리 태그 ──────────────────────────────────────
            Text(
                "🎨 분위기 · 카테고리",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = PlaceColors.text
            )
            Spacer(Modifier.height(8.dp))
            val sheetTags = listOf(
                "카페" to "☕",
                "음식점" to "🍽️",
                "중식" to "🥡",
                "한식" to "🍚",
                "양식" to "🍕",
                "일식" to "🍣",
                "데이트" to "💞",
                "회식" to "🍻",
                "가족" to "👨\u200D👩\u200D👧"
            )
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                sheetTags.forEach { (label, emoji) ->
                    val active = selected.contains(label)
                    Surface(
                        onClick = {
                            if (active) selected.clear()
                            else { selected.clear(); selected.add(label) }
                        },
                        shape = RoundedCornerShape(50),
                        color = if (active) PlaceColors.primary else PlaceColors.card,
                        border = BorderStroke(
                            1.5.dp,
                            if (active) PlaceColors.primary else PlaceColors.border
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(emoji, fontSize = 12.sp)
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (active) Color.White else PlaceColors.text
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── 이미 본 곳 포함 스위치 ──────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "이미 본 곳 다시 포함",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PlaceColors.text
                    )
                    Text(
                        text = "끄면 눌렀던 장소는 제외하고 새로 찾아드려요",
                        fontSize = 11.sp,
                        color = PlaceColors.textTertiary
                    )
                }
                Switch(
                    checked = includeShown,
                    onCheckedChange = { includeShown = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PlaceColors.primary
                    )
                )
            }

            Spacer(Modifier.height(22.dp))

            // ── 적용 버튼 ───────────────────────────────────────────────────
            Button(
                onClick = {
                    onApply(radius.toInt(), selected.toList(), includeShown)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PlaceColors.primary
                )
            ) {
                Text(
                    "이 조건으로 다시 추천받기",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun formatRadius(m: Int): String =
    if (m >= 1000) "${"%.1f".format(m / 1000.0).trimEnd('0').trimEnd('.')}km"
    else "${m}m"

// ═══════════════════════════════════════════════════════════════════════════════
// 지역 선택 BottomSheet — 전국 주요 시/도 → 시/군/구 계층 선택
// ═══════════════════════════════════════════════════════════════════════════════
private data class RegionProvince(val name: String, val districts: List<String>)

// 전국 시/도 + 시/군/구 (주요 지역 커버)
private val AllRegions: List<RegionProvince> = listOf(
    RegionProvince("서울", listOf(
        "강남구", "강동구", "강북구", "강서구", "관악구", "광진구", "구로구", "금천구",
        "노원구", "도봉구", "동대문구", "동작구", "마포구", "서대문구", "서초구",
        "성동구", "성북구", "송파구", "양천구", "영등포구", "용산구", "은평구",
        "종로구", "중구", "중랑구"
    )),
    RegionProvince("부산", listOf(
        "강서구", "금정구", "남구", "동구", "동래구", "부산진구", "북구", "사상구",
        "사하구", "서구", "수영구", "연제구", "영도구", "중구", "해운대구", "기장군"
    )),
    RegionProvince("대구", listOf(
        "남구", "달서구", "동구", "북구", "서구", "수성구", "중구", "달성군", "군위군"
    )),
    RegionProvince("인천", listOf(
        "강화군", "계양구", "남동구", "동구", "미추홀구", "부평구", "서구", "연수구",
        "옹진군", "중구"
    )),
    RegionProvince("광주", listOf("광산구", "남구", "동구", "북구", "서구")),
    RegionProvince("대전", listOf("대덕구", "동구", "서구", "유성구", "중구")),
    RegionProvince("울산", listOf("남구", "동구", "북구", "중구", "울주군")),
    RegionProvince("세종", listOf("세종시")),
    RegionProvince("경기", listOf(
        "수원시", "성남시", "고양시", "용인시", "부천시", "안산시", "안양시", "남양주시",
        "화성시", "평택시", "의정부시", "시흥시", "파주시", "김포시", "광명시", "광주시",
        "군포시", "하남시", "오산시", "이천시", "안성시", "의왕시", "양주시", "구리시",
        "포천시", "동두천시", "과천시", "여주시"
    )),
    RegionProvince("강원", listOf(
        "춘천시", "원주시", "강릉시", "동해시", "태백시", "속초시", "삼척시",
        "홍천군", "횡성군", "영월군", "평창군", "정선군", "철원군", "화천군",
        "양구군", "인제군", "고성군", "양양군"
    )),
    RegionProvince("충북", listOf(
        "청주시", "충주시", "제천시", "보은군", "옥천군", "영동군", "증평군",
        "진천군", "괴산군", "음성군", "단양군"
    )),
    RegionProvince("충남", listOf(
        "천안시", "공주시", "보령시", "아산시", "서산시", "논산시", "계룡시", "당진시",
        "금산군", "부여군", "서천군", "청양군", "홍성군", "예산군", "태안군"
    )),
    RegionProvince("전북", listOf(
        "전주시", "군산시", "익산시", "정읍시", "남원시", "김제시",
        "완주군", "진안군", "무주군", "장수군", "임실군", "순창군", "고창군", "부안군"
    )),
    RegionProvince("전남", listOf(
        "목포시", "여수시", "순천시", "나주시", "광양시",
        "담양군", "곡성군", "구례군", "고흥군", "보성군", "화순군", "장흥군", "강진군",
        "해남군", "영암군", "무안군", "함평군", "영광군", "장성군", "완도군", "진도군", "신안군"
    )),
    RegionProvince("경북", listOf(
        "포항시", "경주시", "김천시", "안동시", "구미시", "영주시", "영천시", "상주시",
        "문경시", "경산시",
        "의성군", "청송군", "영양군", "영덕군", "청도군", "고령군", "성주군", "칠곡군",
        "예천군", "봉화군", "울진군", "울릉군"
    )),
    RegionProvince("경남", listOf(
        "창원시", "진주시", "통영시", "사천시", "김해시", "밀양시", "거제시", "양산시",
        "의령군", "함안군", "창녕군", "고성군", "남해군", "하동군", "산청군", "함양군",
        "거창군", "합천군"
    )),
    RegionProvince("제주", listOf("제주시", "서귀포시"))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegionPickerSheet(
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    isRegionMode: Boolean = false,
    onReturnToMidpoint: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedProvince by remember { mutableStateOf<RegionProvince?>(null) }
    var query by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PlaceColors.card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 16.dp)
        ) {
            // 상단 타이틀 + 뒤로가기
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectedProvince != null) {
                    Surface(
                        onClick = { selectedProvince = null; query = "" },
                        modifier = Modifier.size(32.dp),
                        shape = CircleShape,
                        color = PlaceColors.chipBg
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.KeyboardArrowRight,
                                contentDescription = "뒤로",
                                tint = PlaceColors.textSecondary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayerRotate(180f)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (selectedProvince == null) "어느 지역을 볼까요?"
                        else "${selectedProvince?.name} 의 시/군/구",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlaceColors.text
                    )
                    Text(
                        text = if (selectedProvince == null) "시/도를 선택한 뒤 시/군/구를 골라보세요"
                        else "원하는 지역을 누르면 인기 장소 순위가 보여요",
                        fontSize = 12.sp,
                        color = PlaceColors.textTertiary
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // 간단한 필터 (시/군/구 목록 단계에서만 사용)
            if (selectedProvince != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PlaceColors.chipBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔍", fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        BasicTextFieldSingle(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = "예: 용산, 강남"
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // 리스트 영역
            val lazyListModifier = Modifier
                .fillMaxWidth()
                .weight(1f)

            if (selectedProvince == null) {
                // 시/도 리스트
                LazyColumn(
                    modifier = lazyListModifier,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 다른 지역으로 바꾼 상태면 "중간 지점으로 돌아가기" 항목 상단 노출
                    if (isRegionMode) {
                        item {
                            Surface(
                                onClick = onReturnToMidpoint,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = PlaceColors.primary.copy(alpha = 0.08f),
                                border = BorderStroke(1.5.dp, PlaceColors.primary.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("📍", fontSize = 16.sp)
                                    Text(
                                        text = "중간 지점으로 돌아가기",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PlaceColors.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                    items(AllRegions) { province ->
                        Surface(
                            onClick = { selectedProvince = province },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = PlaceColors.card,
                            border = BorderStroke(1.dp, PlaceColors.border)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = province.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PlaceColors.text,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${province.districts.size}개",
                                    fontSize = 12.sp,
                                    color = PlaceColors.textTertiary
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = PlaceColors.textTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // 시/군/구 리스트 (query 필터)
                val filtered = remember(query, selectedProvince) {
                    val provinces = selectedProvince?.districts.orEmpty()
                    if (query.isBlank()) provinces
                    else provinces.filter { it.contains(query.trim()) }
                }
                LazyColumn(
                    modifier = lazyListModifier,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered) { district ->
                        val regionLabel = "${selectedProvince?.name} $district"
                        Surface(
                            onClick = { onSelect(regionLabel) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = PlaceColors.card,
                            border = BorderStroke(1.dp, PlaceColors.border)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = PlaceColors.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = district,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PlaceColors.text,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = PlaceColors.textTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    if (filtered.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "일치하는 지역이 없어요",
                                    fontSize = 13.sp,
                                    color = PlaceColors.textTertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 단순 한 줄 TextField — BasicTextField 래핑 (ModalBottomSheet 내 경량 입력용) */
@Composable
private fun BasicTextFieldSingle(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = PlaceColors.text,
            fontSize = 14.sp
        ),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 14.sp,
                        color = PlaceColors.textTertiary
                    )
                }
                inner()
            }
        }
    )
}

/** 간단한 회전 modifier (뒤로가기 아이콘 용). */
private fun Modifier.graphicsLayerRotate(degrees: Float): Modifier =
    this.then(Modifier.graphicsLayer { rotationZ = degrees })
