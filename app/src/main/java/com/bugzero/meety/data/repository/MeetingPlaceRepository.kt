package com.bugzero.meety.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

// ─── 데이터 모델 ──────────────────────────────────────────────────────────────

data class LatLng(val lat: Double, val lng: Double)

data class PlaceResult(
    val name: String,
    val address: String,
    val category: String,
    val phone: String = "",
    val url: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val imageUrl: String = "",          // 대표 이미지 URL
    val imageUrls: List<String> = emptyList(),  // 업체 관련 사진들 (음식/내부 등)
    val rating: Double = 0.0,           // 미사용 (네이버 지도는 별점 미제공)
    val reviewCount: Int = 0,           // 방문자 리뷰 수 (네이버 지도 공식 API)
    val placeId: String = ""            // 네이버 지도 place ID (상세보기 딥링크에 사용)
)

/**
 * 장소 추천 레포지토리 (네이버 API 기반)
 *
 * 위치 정규화 흐름:
 *   사용자 입력 (예: "강남구") → [네이버 Geocoding] → 정확한 좌표
 *                              → 실패 시 "서울" prefix 보완 후 재시도
 *
 * 장소 검색 흐름:
 *   중간 좌표 → [네이버 Reverse Geocode] → 지역명
 *            → [네이버 Local Search] "{지역명} 카페" → Top 5 장소
 */
class MeetingPlaceRepository {

    companion object {
        // ── 네이버 클라우드 플랫폼 (NCP) — 지오코딩/역지오코딩용 ──────────────
        const val NCP_CLIENT_ID = "k56cdclroc"
        const val NCP_CLIENT_SECRET = "zdmyA6OKQBHglsm8vbqCTGjYbXHuM3JvkaNEu5R6"

        // ── 네이버 Developers — 장소 검색용 ──────────────────────────────────
        const val NAVER_SEARCH_CLIENT_ID = "ynJinwWrPxnz5wxrsISp"
        const val NAVER_SEARCH_CLIENT_SECRET = "JkUMcAEDnx"

        // ── API Endpoints ─────────────────────────────────────────────────────
        private const val NAVER_GEO_BASE = "https://maps.apigw.ntruss.com/map-geocode/v2/geocode"
        private const val NAVER_REVERSE_GEO_BASE =
            "https://maps.apigw.ntruss.com/map-reversegeocode/v2/gc"
        private const val NAVER_SEARCH_BASE =
            "https://openapi.naver.com/v1/search/local.json"
        private const val TAG = "MeetingPlaceRepo"

        // ── Circuit breaker 설정 ──────────────────────────────────────────────
        //   외부 엔드포인트가 연속으로 실패하면 일정 시간 호출 스킵 — 타임아웃 대기로
        //   UI 가 50초 넘게 멈추는 것을 막는다. 2분이 지나면 카운터 리셋.
        private const val CB_FAIL_THRESHOLD = 3
        private const val CB_BLACKOUT_MS = 120_000L // 2분
    }

    // ─── Circuit breakers (엔드포인트별 연속 실패 카운트 + 차단 해제 시각) ──
    private val cbAllSearchFails = AtomicInteger(0)
    private val cbAllSearchBlockedUntil = AtomicLong(0L)
    private val cbPlaceIdHtmlFails = AtomicInteger(0)
    private val cbPlaceIdHtmlBlockedUntil = AtomicLong(0L)
    private val cbPhotoHtmlFails = AtomicInteger(0)
    private val cbPhotoHtmlBlockedUntil = AtomicLong(0L)
    private val cbTransitFails = AtomicInteger(0)
    private val cbTransitBlockedUntil = AtomicLong(0L)

    private fun cbIsBlocked(until: AtomicLong): Boolean =
        System.currentTimeMillis() < until.get()

    private fun cbRecordFailure(fails: AtomicInteger, until: AtomicLong, label: String) {
        val n = fails.incrementAndGet()
        if (n >= CB_FAIL_THRESHOLD) {
            val blockUntil = System.currentTimeMillis() + CB_BLACKOUT_MS
            until.set(blockUntil)
            fails.set(0)
            Log.w(TAG, "[CB] $label 차단 개시 — ${CB_BLACKOUT_MS / 1000}초 동안 호출 스킵")
        }
    }

    private fun cbRecordSuccess(fails: AtomicInteger, until: AtomicLong) {
        fails.set(0)
        until.set(0L)
    }

    // ─── 네이버 Geocoding (주소 → 좌표) ──────────────────────────────────────

    /**
     * 네이버 Geocoding API로 주소 → 좌표 변환
     */
    private suspend fun naverGeocode(address: String): LatLng? = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(address, "UTF-8")
            val url = URL("$NAVER_GEO_BASE?query=$encoded")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.apply {
                requestMethod = "GET"
                setRequestProperty("x-ncp-apigw-api-key-id", NCP_CLIENT_ID)
                setRequestProperty("x-ncp-apigw-api-key", NCP_CLIENT_SECRET)
                connectTimeout = 5000
                readTimeout = 5000
            }
            val responseCode = conn.responseCode
            if (responseCode != 200) {
                Log.w(TAG, "naverGeocode HTTP $responseCode for '$address'")
                return@withContext null
            }
            val body = conn.inputStream.bufferedReader().readText()
            val root = JSONObject(body)
            val addresses = root.getJSONArray("addresses")
            if (addresses.length() == 0) return@withContext null

            val first = addresses.getJSONObject(0)
            LatLng(
                lat = first.getString("y").toDouble(),
                lng = first.getString("x").toDouble()
            )
        } catch (e: Exception) {
            Log.e(TAG, "naverGeocode 실패 ('$address'): ${e.message}")
            null
        }
    }

    // ─── 네이버 Reverse Geocoding (좌표 → 지역명) ────────────────────────────

    /**
     * 좌표를 지역명으로 변환 (예: 37.5, 127.0 → "서울특별시 강남구")
     */
    private suspend fun naverReverseGeocode(lat: Double, lng: Double): String? =
        withContext(Dispatchers.IO) {
            try {
                val url = URL(
                    "$NAVER_REVERSE_GEO_BASE?coords=$lng,$lat&output=json&orders=admcode"
                )
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.apply {
                    requestMethod = "GET"
                    setRequestProperty("x-ncp-apigw-api-key-id", NCP_CLIENT_ID)
                    setRequestProperty("x-ncp-apigw-api-key", NCP_CLIENT_SECRET)
                    connectTimeout = 5000
                    readTimeout = 5000
                }
                if (conn.responseCode != 200) {
                    Log.w(TAG, "naverReverseGeocode HTTP ${conn.responseCode}")
                    return@withContext null
                }
                val body = conn.inputStream.bufferedReader().readText()
                val root = JSONObject(body)
                val results = root.getJSONArray("results")
                if (results.length() == 0) return@withContext null

                val region = results.getJSONObject(0).getJSONObject("region")
                val area1 = region.getJSONObject("area1").getString("name") // 시/도
                val area2 = region.getJSONObject("area2").getString("name") // 구/군/시

                "$area1 $area2".trim().also {
                    Log.d(TAG, "역지오코딩 성공: ($lat, $lng) → '$it'")
                }
            } catch (e: Exception) {
                Log.e(TAG, "naverReverseGeocode 실패: ${e.message}")
                null
            }
        }

    /**
     * 중간 지점 좌표를 "서울 용산구" 처럼 searchPlacesInRegion 이 기대하는
     * 시/도 + 시/군/구 포맷으로 정규화해서 돌려준다.
     * 네이버는 "서울특별시 용산구" 형태로 주므로 접미사(특별시/광역시/…)를 제거.
     */
    suspend fun resolveRegionName(lat: Double, lng: Double): String? {
        val raw = naverReverseGeocode(lat, lng) ?: return null
        val parts = raw.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (parts.isEmpty()) return null
        val province = normalizeProvincePrefix(parts[0])
        val district = parts.getOrNull(1).orEmpty()
        return if (district.isBlank()) province else "$province $district"
    }

    /** "서울특별시" → "서울", "부산광역시" → "부산", "경기도" → "경기" 등. */
    private fun normalizeProvincePrefix(area1: String): String {
        val suffixes = listOf("특별자치도", "특별자치시", "특별시", "광역시", "도")
        for (s in suffixes) {
            if (area1.endsWith(s) && area1.length > s.length) return area1.dropLast(s.length)
        }
        return area1
    }

    // ─── 지오코딩 메인 ────────────────────────────────────────────────────────

    /**
     * 위치 문자열 → 좌표 변환
     *
     * 시도 순서:
     *   1. 네이버 Geocoding (원본)
     *   2. 네이버 Geocoding ("서울 OO구" prefix 보완)
     */
    suspend fun geocodeAddress(rawLocation: String): LatLng? = withContext(Dispatchers.IO) {
        Log.d(TAG, "지오코딩 시작: '$rawLocation'")

        // ── 1: 네이버 직접 시도 ───────────────────────────────────────────────
        naverGeocode(rawLocation)?.let {
            Log.d(TAG, "네이버 지오코딩 성공: '$rawLocation'")
            return@withContext it
        }

        // ── 2: "서울" prefix 자동 보완 ─────────────────────────────────────────
        val withSeoulPrefix = if (!rawLocation.startsWith("서울") && rawLocation.endsWith("구")) {
            "서울 $rawLocation"
        } else null

        withSeoulPrefix?.let { prefixed ->
            naverGeocode(prefixed)?.let {
                Log.d(TAG, "서울 prefix 네이버 지오코딩 성공: '$prefixed'")
                return@withContext it
            }
        }

        Log.w(TAG, "모든 지오코딩 시도 실패: '$rawLocation'")
        null
    }

    // ─── 네이버 장소 검색 (주변 장소 추천용) ─────────────────────────────────

    /**
     * 중간 지점 좌표 주변의 장소를 네이버 Local Search로 검색.
     *
     * 1. 좌표 → 역지오코딩으로 지역명 추출 (예: "서울특별시 강남구")
     * 2. "{지역명} {키워드}" 로 네이버 검색
     */
    suspend fun searchNearbyPlaces(
        lat: Double,
        lng: Double,
        keywords: List<String> = listOf("카페", "음식점", "맛집"),
        radiusMeters: Int = 3000,
        excludeKeys: Set<String> = emptySet(),
        limit: Int = 5
    ): List<PlaceResult> = withContext(Dispatchers.IO) {
        // 1. 역지오코딩으로 지역명 추출
        val areaName = naverReverseGeocode(lat, lng)
        if (areaName.isNullOrBlank()) {
            Log.w(TAG, "역지오코딩 실패 → 장소 검색 불가")
            return@withContext emptyList()
        }

        val results = mutableListOf<PlaceResult>()
        val seenNames = mutableSetOf<String>()

        // 네이버 Local Search는 display 최대 5, start 1..1000.
        // 429 (rate limit) 는 키 전체에 걸리므로, 한 번 받으면 이후 호출은 의미 없다.
        // → 가벼운 1회차 (start=1) 만 전 키워드에 돌리고, 아직 모자라면 2회차 추가.
        val pageStartRounds = listOf(listOf(1), listOf(6, 11))
        var rateLimited = false

        // 2. 키워드별로 네이버 Local Search
        rounds@ for (pageStarts in pageStartRounds) {
            if (results.size >= limit || rateLimited) break
            outer@ for (keyword in keywords) {
                if (results.size >= limit || rateLimited) break
                for (pageStart in pageStarts) {
                    if (results.size >= limit || rateLimited) break
                    try {
                        val query = "$areaName $keyword"
                        val encoded = URLEncoder.encode(query, "UTF-8")
                        val url = URL("$NAVER_SEARCH_BASE?query=$encoded&display=5&start=$pageStart&sort=random")
                        val conn = url.openConnection() as java.net.HttpURLConnection
                        conn.apply {
                            requestMethod = "GET"
                            setRequestProperty("X-Naver-Client-Id", NAVER_SEARCH_CLIENT_ID)
                            setRequestProperty("X-Naver-Client-Secret", NAVER_SEARCH_CLIENT_SECRET)
                            connectTimeout = 3000
                            readTimeout = 3000
                        }

                        if (conn.responseCode != 200) {
                            val errorBody = try { conn.errorStream?.bufferedReader()?.readText() } catch (_: Exception) { null }
                            Log.w(TAG, "naverLocalSearch HTTP ${conn.responseCode} for '$query' start=$pageStart | error: $errorBody")
                            // 429: 키 전체가 잠겼으니 즉시 전부 중단
                            if (conn.responseCode == 429) { rateLimited = true; break }
                            // 400: start 범위 밖 — 해당 키워드만 스킵
                            if (conn.responseCode == 400) break
                            continue
                        }

                        val body = conn.inputStream.bufferedReader().readText()
                        val items = JSONObject(body).getJSONArray("items")
                        if (items.length() == 0) break  // 더 이상 페이지 없음

                        for (i in 0 until items.length()) {
                            if (results.size >= limit) break
                            val item = items.getJSONObject(i)

                            val name = item.getString("title")
                                .replace("<b>", "")
                                .replace("</b>", "")
                                .trim()
                            if (seenNames.contains(name)) continue

                            val addr = item.optString("roadAddress", "")
                                .ifEmpty { item.optString("address", "") }
                            val category = item.optString("category", keyword)
                                .split(">").lastOrNull()?.trim() ?: keyword

                            // 외부에서 건네준 제외 집합 체크 (key = "name|address")
                            val extKey = "$name|$addr"
                            if (excludeKeys.contains(extKey)) continue

                            // 네이버 좌표 변환
                            val mapx = item.optString("mapx", "0")
                            val mapy = item.optString("mapy", "0")
                            var placeLat = 0.0
                            var placeLng = 0.0
                            try {
                                val mx = mapx.toLong()
                                val my = mapy.toLong()
                                if (mx > 1000000) {
                                    placeLng = mx / 10000000.0
                                    placeLat = my / 10000000.0
                                }
                            } catch (_: Exception) { }

                            // 반경 필터 — 좌표가 있을 때만 거리 계산
                            if (radiusMeters > 0 && placeLat != 0.0 && placeLng != 0.0) {
                                val dist = haversineMeters(lat, lng, placeLat, placeLng)
                                if (dist > radiusMeters) continue
                            }

                            seenNames.add(name)
                            results.add(
                                PlaceResult(
                                    name = name,
                                    address = addr,
                                    category = category,
                                    phone = item.optString("telephone", ""),
                                    url = item.optString("link", ""),
                                    lat = placeLat,
                                    lng = placeLng
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "searchNearbyPlaces '$keyword' start=$pageStart 실패: ${e.message}")
                    }
                }
            }
        }
        if (rateLimited) Log.w(TAG, "naverLocalSearch rate-limited — 이번 세션에서 더 이상 호출 안 함")
        Log.d(TAG, "searchNearbyPlaces 결과 ${results.size}개 (반경=${radiusMeters}m, 제외=${excludeKeys.size})")
        results.take(limit)
    }

    /**
     * 특정 지역명(시/도 + 시/군/구) 기준으로 "인기 장소" 를 최대한 많이 가져온다.
     *
     * searchNearbyPlaces 와 달리:
     *  - 반경 필터를 사용하지 않는다 (중간 지점이 없는 시나리오).
     *  - sort 파라미터는 붙이지 않는다 — 네이버 Local Search 의 기본 정렬(정확도순)을 그대로 사용.
     *    `sort=comparison` 은 SE04 오류가 나고, `sort=random` 은 말 그대로 랜덤이어서 둘 다 부적합.
     *  - display=5 × start=1,6,11,…96 → 키워드 1개당 최대 100개 까지 조회 후 합집합.
     *  - keywords 가 비어있으면 기본으로 "카페" 한 개만 돌리지만, 여러 개 주면 모두 병합.
     *  - 결과는 시/도 + 시/군/구 양쪽 모두 매칭되는 주소만 남긴다 (예: "서울 중구" 로 검색해도
     *    부산 중구 결과가 섞이지 않도록).
     *
     * @param regionName  "서울 용산구" 처럼 시/도 + 구/군 을 공백으로 이은 문자열
     * @param keywords    필터 키워드 (예: ["카페"])
     * @param limit       반환 최대 개수 (기본 100 — 카카오맵의 "인기 100순위" 스타일)
     */
    suspend fun searchPlacesInRegion(
        regionName: String,
        keywords: List<String>,
        limit: Int = 100
    ): List<PlaceResult> = withContext(Dispatchers.IO) {
        val region = regionName.trim()
        if (region.isBlank()) return@withContext emptyList()

        val effectiveKeywords = keywords.ifEmpty { listOf("카페") }
        val results = mutableListOf<PlaceResult>()
        val seenNames = mutableSetOf<String>()

        // ── 지역 매칭 도우미: 시/도 + 시/군/구 양쪽 모두 주소에 포함되어야 통과 ──
        val regionTokens = region.split(Regex("\\s+")).filter { it.isNotBlank() }
        val provinceAliases = provinceAliasesFor(regionTokens.getOrNull(0).orEmpty())
        val district = regionTokens.getOrNull(1).orEmpty()

        // Naver Local Search: display max=5, start 범위 1..1000.
        // display=5 × 20페이지 = 최대 100건/키워드.
        val pageStarts = (1..96 step 5).toList()  // 1,6,11,…,96 → 20페이지
        var rateLimited = false

        for (keyword in effectiveKeywords) {
            if (results.size >= limit || rateLimited) break
            for (pageStart in pageStarts) {
                if (results.size >= limit || rateLimited) break
                try {
                    val query = "$region $keyword"
                    val encoded = URLEncoder.encode(query, "UTF-8")
                    // sort 은 붙이지 않음 — 네이버 기본(정확도순) 사용
                    val url = URL("$NAVER_SEARCH_BASE?query=$encoded&display=5&start=$pageStart")
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.apply {
                        requestMethod = "GET"
                        setRequestProperty("X-Naver-Client-Id", NAVER_SEARCH_CLIENT_ID)
                        setRequestProperty("X-Naver-Client-Secret", NAVER_SEARCH_CLIENT_SECRET)
                        connectTimeout = 3000
                        readTimeout = 3000
                    }
                    if (conn.responseCode != 200) {
                        val errorBody = try { conn.errorStream?.bufferedReader()?.readText() } catch (_: Exception) { null }
                        Log.w(TAG, "searchPlacesInRegion HTTP ${conn.responseCode} for '$query' start=$pageStart | $errorBody")
                        if (conn.responseCode == 429) { rateLimited = true; break }
                        if (conn.responseCode == 400) break // start 범위 밖 — 키워드 페이지네이션 끝
                        continue
                    }
                    val body = conn.inputStream.bufferedReader().readText()
                    val items = JSONObject(body).getJSONArray("items")
                    if (items.length() == 0) break

                    for (i in 0 until items.length()) {
                        if (results.size >= limit) break
                        val item = items.getJSONObject(i)
                        val name = item.getString("title")
                            .replace("<b>", "")
                            .replace("</b>", "")
                            .trim()
                        if (seenNames.contains(name)) continue

                        val addr = item.optString("roadAddress", "")
                            .ifEmpty { item.optString("address", "") }

                        // ── 지역 매칭 체크: 시/도 alias 중 하나 + 시/군/구 모두 포함 ──
                        val provinceMatch = provinceAliases.any { addr.contains(it) }
                        val districtMatch = district.isBlank() || addr.contains(district)
                        if (!provinceMatch || !districtMatch) continue

                        // ── 카테고리 엄격 필터: 키워드와 실제 업체 카테고리가 맞는지 체크 ──
                        val fullCategory = item.optString("category", "")
                        if (!categoryMatchesKeyword(fullCategory, keyword)) continue

                        val category = fullCategory.ifBlank { keyword }
                            .split(">").lastOrNull()?.trim() ?: keyword

                        val mapx = item.optString("mapx", "0")
                        val mapy = item.optString("mapy", "0")
                        var placeLat = 0.0
                        var placeLng = 0.0
                        try {
                            val mx = mapx.toLong()
                            val my = mapy.toLong()
                            if (mx > 1000000) {
                                placeLng = mx / 10000000.0
                                placeLat = my / 10000000.0
                            }
                        } catch (_: Exception) { }

                        seenNames.add(name)
                        results.add(
                            PlaceResult(
                                name = name,
                                address = addr,
                                category = category,
                                phone = item.optString("telephone", ""),
                                url = item.optString("link", ""),
                                lat = placeLat,
                                lng = placeLng
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "searchPlacesInRegion '$keyword' start=$pageStart 실패: ${e.message}")
                }
            }
        }
        if (rateLimited) Log.w(TAG, "searchPlacesInRegion rate-limited")
        Log.d(TAG, "searchPlacesInRegion('$region', $effectiveKeywords) → ${results.size}개")
        results.take(limit)
    }

    /**
     * "서울" → ["서울", "서울특별시"] 와 같은 시/도 prefix alias 리스트.
     * 네이버 주소는 "서울특별시 ..." 로 오기도 하고 "서울 ..." 로 오기도 해서 양쪽 다 허용한다.
     */
    private fun provinceAliasesFor(prefix: String): List<String> = when (prefix) {
        "서울" -> listOf("서울", "서울특별시")
        "부산" -> listOf("부산", "부산광역시")
        "대구" -> listOf("대구", "대구광역시")
        "인천" -> listOf("인천", "인천광역시")
        "광주" -> listOf("광주", "광주광역시")
        "대전" -> listOf("대전", "대전광역시")
        "울산" -> listOf("울산", "울산광역시")
        "세종" -> listOf("세종", "세종특별자치시")
        "경기" -> listOf("경기", "경기도")
        "강원" -> listOf("강원", "강원도", "강원특별자치도")
        "충북" -> listOf("충북", "충청북도")
        "충남" -> listOf("충남", "충청남도")
        "전북" -> listOf("전북", "전라북도", "전북특별자치도")
        "전남" -> listOf("전남", "전라남도")
        "경북" -> listOf("경북", "경상북도")
        "경남" -> listOf("경남", "경상남도")
        "제주" -> listOf("제주", "제주도", "제주특별자치도")
        else -> if (prefix.isBlank()) emptyList() else listOf(prefix)
    }

    /**
     * 사용자가 선택한 키워드(예: "카페", "음식점", "맛집")와 네이버가 돌려준
     * 실제 업체 카테고리 경로("음식점>한식>육류,고기요리" 등) 가 논리적으로
     * 맞는지 엄격하게 판단.
     *
     * 네이버 카테고리 트리는 최상위가 "음식점" 이라 카페/디저트도 "음식점>카페,디저트>..." 로 잡히기 때문에
     * 단순 substring 체크를 하면 "음식점" 선택 시 카페도 섞여 들어온다. 이를 방지한다.
     */
    private fun categoryMatchesKeyword(categoryPath: String, keyword: String): Boolean {
        val cat = categoryPath.trim()
        if (cat.isEmpty()) return true  // 카테고리 정보가 없으면 차단하지 않음
        return when (keyword.trim()) {
            "카페" -> cat.contains("카페") || cat.contains("디저트") || cat.contains("베이커리")
            "음식점", "맛집" ->
                cat.contains("음식점") &&
                    !cat.contains("카페") &&
                    !cat.contains("디저트") &&
                    !cat.contains("베이커리") &&
                    !cat.contains("중식") &&
                    !cat.contains("한식") &&
                    !cat.contains("양식") &&
                    !cat.contains("일식")
            "중식" ->
                cat.contains("중식") ||
                    cat.contains("중국") ||
                    cat.contains("딤섬") ||
                    cat.contains("마라")
            "한식" ->
                cat.contains("한식") ||
                    cat.contains("한국") ||
                    cat.contains("고기") ||
                    cat.contains("삼겹") ||
                    cat.contains("갈비") ||
                    cat.contains("국밥") ||
                    cat.contains("분식")
            "양식" ->
                cat.contains("양식") ||
                    cat.contains("이탈리아") ||
                    cat.contains("피자") ||
                    cat.contains("파스타") ||
                    cat.contains("스테이크") ||
                    cat.contains("버거") ||
                    cat.contains("샌드위치")
            "일식" ->
                cat.contains("일식") ||
                    cat.contains("초밥") ||
                    cat.contains("스시") ||
                    cat.contains("라멘") ||
                    cat.contains("우동") ||
                    cat.contains("일본")
            else -> cat.contains(keyword.trim())
        }
    }

    /** 두 좌표 사이의 대원 거리(m). 반경 내 필터에 사용. */
    private fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6371000.0 // 지구 반지름(m)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = kotlin.math.sin(dLat / 2).let { it * it } +
            kotlin.math.cos(Math.toRadians(lat1)) *
            kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLng / 2).let { it * it }
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return R * c
    }

    // ─── 네이버 지도 공식 정보 조회 ──────────────────────────────────────────────

    /**
     * 네이버 지도 URL에서 place ID 추출.
     * 지원 형식:
     *   https://map.naver.com/v5/entry/place/12345
     *   https://m.place.naver.com/place/12345
     *   https://m.place.naver.com/restaurant/12345
     *   https://pcmap.place.naver.com/place/12345
     */
    private fun extractPlaceId(url: String): String? {
        if (url.isBlank()) return null
        // /place/숫자 또는 entry/place/숫자 패턴
        val match = Regex("""(?:entry/place|/place|/restaurant|/cafe|/beauty)/(\d{5,})""").find(url)
        return match?.groupValues?.get(1)
    }

    /**
     * 네이버 지도 내부 AllSearch JSON API로 업체 정보를 한 번에 조회한다.
     * 반환값에는 placeId + 썸네일 URL이 함께 들어있어, HTML 스크래핑이 실패해도
     * 최소 대표 이미지는 확보할 수 있다.
     *
     * 응답 구조 (관심 부분만):
     *   {
     *     "result": {
     *       "place": {
     *         "list": [
     *           { "id": "1234567", "thumUrls": ["...phinf.pstatic.net..."], ... }
     *         ]
     *       }
     *     }
     *   }
     */
    private data class NaverPlaceHit(val placeId: String, val photoUrls: List<String>)

    private suspend fun findNaverPlaceByAllSearch(query: String): NaverPlaceHit? =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext null
            if (cbIsBlocked(cbAllSearchBlockedUntil)) {
                Log.d(TAG, "[CB] allSearch skipped (blocked): '$query'")
                return@withContext null
            }
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val url = URL("https://map.naver.com/p/api/search/allSearch?query=$encoded&type=place&page=1&displayCount=5")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-S901B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    setRequestProperty("Accept", "application/json, text/plain, */*")
                    setRequestProperty("Accept-Language", "ko-KR,ko;q=0.9")
                    setRequestProperty("Referer", "https://map.naver.com/")
                    connectTimeout = 1500
                    readTimeout = 2500
                }
                if (conn.responseCode != 200) {
                    Log.w(TAG, "allSearch HTTP ${conn.responseCode}: query='$query'")
                    cbRecordFailure(cbAllSearchFails, cbAllSearchBlockedUntil, "allSearch")
                    return@withContext null
                }
                val body = conn.inputStream.bufferedReader().readText()
                val root = JSONObject(body)
                val list = root.optJSONObject("result")
                    ?.optJSONObject("place")
                    ?.optJSONArray("list")
                if (list == null || list.length() == 0) {
                    Log.d(TAG, "allSearch 결과 없음: '$query'")
                    cbRecordSuccess(cbAllSearchFails, cbAllSearchBlockedUntil)
                    return@withContext null
                }
                val first = list.getJSONObject(0)
                val id = first.optString("id", "")
                val photoUrls = mutableListOf<String>()
                // thumUrls: ["...", "..."]
                first.optJSONArray("thumUrls")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val u = arr.optString(i, "")
                        if (u.isNotBlank() && u.contains("pstatic.net", ignoreCase = true)) {
                            photoUrls += upgradeToHighQuality(u)
                        }
                    }
                }
                // 일부 응답은 thumUrl (단수) 로 오기도 함
                first.optString("thumUrl", "").takeIf { it.isNotBlank() && it.contains("pstatic.net", true) }
                    ?.let { photoUrls += upgradeToHighQuality(it) }
                Log.d(TAG, "allSearch 성공: '$query' → id='$id', 사진 ${photoUrls.size}장")
                cbRecordSuccess(cbAllSearchFails, cbAllSearchBlockedUntil)
                if (id.isBlank() && photoUrls.isEmpty()) null
                else NaverPlaceHit(placeId = id, photoUrls = photoUrls.distinct())
            } catch (e: Exception) {
                Log.w(TAG, "allSearch 예외 '$query': ${e.message}")
                cbRecordFailure(cbAllSearchFails, cbAllSearchBlockedUntil, "allSearch")
                null
            }
        }

    /**
     * 네이버 지도 모바일 검색에서 업체명(+ 주소/카테고리)으로 Naver place ID를 조회한다.
     * Local Search API는 place ID를 제공하지 않으므로 상세 페이지 조회/딥링크를 위해 별도 lookup.
     *
     * placeId 확보율을 올리기 위해 여러 질의 변형을 구체적 → 일반적 순으로 시도한다.
     * 이는 이름만으로는 동명(영화/드라마/일반명사) 충돌이 잦아 엉뚱한 장소에 매칭되거나
     * 아예 장소 페이지로 리다이렉트되지 않는 경우를 줄이기 위함이다.
     */
    private suspend fun findNaverPlaceIdBySearch(
        name: String,
        address: String = "",
        lat: Double = 0.0,
        lng: Double = 0.0,
        category: String = ""
    ): String? = withContext(Dispatchers.IO) {
        if (name.isBlank()) return@withContext null
        if (cbIsBlocked(cbPlaceIdHtmlBlockedUntil)) {
            Log.d(TAG, "[CB] placeId HTML 검색 skipped (blocked): '$name'")
            return@withContext null
        }

        val addrTokens = address.split(" ").filter { it.isNotBlank() }
        val region2 = addrTokens.take(2).joinToString(" ").trim()       // "서울특별시 용산구"

        // 쿼리는 1개만 — 가장 공소한 실패 원인이 매칭 실패가 아니라 네트워크 타임아웃이라
        // 여러 쿼리를 돌려봤자 전부 같은 식으로 타임아웃 남. 구체적 쿼리 1개로 제한.
        val singleQuery = if (region2.isNotBlank()) "$region2 $name" else name

        val idPatterns = listOf(
            Regex("""/(?:place|restaurant|cafe|beauty|hairshop)/(\d{5,})"""),
            Regex(""""id"\s*:\s*"(\d{5,})""""),
            Regex(""""placeId"\s*:\s*"?(\d{5,})"?"""),
            Regex(""""entry"\s*:\s*"(\d{5,})"""")
        )

        val encoded = URLEncoder.encode(singleQuery, "UTF-8")
        val urls = listOf(
            "https://m.map.naver.com/search2/search.naver?query=$encoded",
            "https://map.naver.com/p/search/$encoded"
        )
        var anyFailed = false
        for (urlStr in urls) {
            try {
                val conn = URL(urlStr).openConnection() as java.net.HttpURLConnection
                conn.apply {
                    requestMethod = "GET"
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-S901B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    setRequestProperty("Accept-Language", "ko-KR,ko;q=0.9")
                    setRequestProperty("Referer", "https://map.naver.com/")
                    connectTimeout = 1500
                    readTimeout = 2500
                }
                // 리다이렉트된 최종 URL에서 먼저 추출 시도
                val finalUrl = conn.url?.toString().orEmpty()
                extractPlaceId(finalUrl)?.let {
                    Log.d(TAG, "place ID (redirect URL) 발견: '$singleQuery' → $it")
                    cbRecordSuccess(cbPlaceIdHtmlFails, cbPlaceIdHtmlBlockedUntil)
                    return@withContext it
                }
                if (conn.responseCode == 200) {
                    val html = conn.inputStream.bufferedReader().readText()
                    for (pattern in idPatterns) {
                        val id = pattern.find(html)?.groupValues?.get(1)
                        if (!id.isNullOrBlank()) {
                            Log.d(TAG, "place ID 검색 성공: '$singleQuery' → $id (pattern=$pattern)")
                            cbRecordSuccess(cbPlaceIdHtmlFails, cbPlaceIdHtmlBlockedUntil)
                            return@withContext id
                        }
                    }
                    Log.w(TAG, "place ID 검색 패턴 불일치: '$singleQuery' url=$urlStr")
                } else {
                    anyFailed = true
                }
            } catch (e: Exception) {
                anyFailed = true
                Log.w(TAG, "place ID 검색 실패 ($urlStr): ${e.message}")
            }
        }
        if (anyFailed) cbRecordFailure(cbPlaceIdHtmlFails, cbPlaceIdHtmlBlockedUntil, "placeId HTML 검색")
        null
    }

    /** 네이버 pstatic 이미지 URL을 고화질로 업그레이드 (type=w80 → type=w1024_suc) */
    private fun upgradeToHighQuality(url: String): String =
        if (url.contains("pstatic.net") && url.contains("type=w"))
            url.replace(Regex("type=w\\d+[^&]*"), "type=w1024_suc")
        else url

    private data class NaverPlaceDetails(
        val photos: List<String> = emptyList(),
        val visitorReviewCount: Int = 0
    )

    /**
     * 네이버 지도 장소 페이지 HTML에서 방문자 리뷰 수를 추출한다.
     * 페이지 소스에 JSON 데이터가 임베딩되어 있어 정규식으로 파싱 가능.
     */
    private suspend fun fetchVisitorReviewCountFromHtml(placeId: String): Int =
        withContext(Dispatchers.IO) {
            if (cbIsBlocked(cbPhotoHtmlBlockedUntil)) {
                Log.d(TAG, "[CB] visitorReview HTML skipped (blocked): placeId=$placeId")
                return@withContext 0
            }
            // 시도할 URL 목록 (둘 다 시도)
            val urls = listOf(
                "https://m.place.naver.com/place/$placeId/home",
                "https://pcmap.place.naver.com/place/$placeId/home"
            )
            // 방문자 리뷰 수 전용 패턴 (블로그 리뷰는 명시적으로 매칭하지 않음)
            val patterns = listOf(
                Regex(""""visitorReviewCount"\s*:\s*(\d+)"""),
                Regex(""""visitor_review_count"\s*:\s*(\d+)"""),
                Regex(""""visitorReviewsTotal(?:Count)?"\s*:\s*(\d+)"""),
                Regex(""""visitorReview"\s*:\s*\{[^}]{0,300}"total(?:Count)?"\s*:\s*(\d+)"""),
                Regex(""""visitorReviews"\s*:\s*\{[^}]{0,300}"total(?:Count)?"\s*:\s*(\d+)"""),
                // "방문자 리뷰 1,234" 같은 렌더링된 텍스트 fallback (블로그 리뷰 제외)
                Regex("""방문자\s*리뷰[^\d]{0,30}([\d,]+)""")
            )
            for (urlStr in urls) {
                try {
                    val conn = URL(urlStr).openConnection() as java.net.HttpURLConnection
                    conn.apply {
                        requestMethod = "GET"
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-S901B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                        setRequestProperty("Accept-Language", "ko-KR,ko;q=0.9")
                        setRequestProperty("Referer", "https://map.naver.com/")
                        connectTimeout = 1500
                        readTimeout = 2500
                    }
                    if (conn.responseCode == 200) {
                        val html = conn.inputStream.bufferedReader().readText()
                        for (pattern in patterns) {
                            val raw = pattern.find(html)?.groupValues?.get(1) ?: continue
                            // 쉼표 구분된 숫자("1,234") 대응
                            val count = raw.replace(",", "").toIntOrNull() ?: 0
                            if (count > 0) {
                                Log.d(TAG, "HTML 방문자 리뷰 성공: placeId=$placeId url=$urlStr count=$count (raw=$raw)")
                                return@withContext count
                            }
                        }
                        Log.w(TAG, "HTML 방문자 리뷰 패턴 불일치: placeId=$placeId url=$urlStr")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "HTML 방문자 리뷰 실패 ($urlStr): ${e.message}")
                }
            }
            0
        }

    /**
     * 네이버 지도 내부 API로 공식 사진 + 방문자 리뷰 수를 함께 조회한다.
     * 사진/리뷰 수 모두 요약 API → HTML 파싱 순으로 fallback.
     * HTML fallback도 해당 placeId의 장소 페이지만 파싱하므로 다른 업체 사진이 섞이지 않는다.
     * placeId가 비어있으면 빈 결과 반환.
     */
    private suspend fun fetchNaverPlaceDetailsById(
        placeId: String,
        maxPhotos: Int = 5
    ): NaverPlaceDetails = withContext(Dispatchers.IO) {
        if (placeId.isBlank()) return@withContext NaverPlaceDetails()
        Log.d(TAG, "fetchNaverPlaceDetailsById: placeId=$placeId")

        val photoUrls = mutableListOf<String>()
        var visitorReviewCount = 0

        // ── 1. 사진: 해당 placeId의 네이버 플레이스 페이지 HTML에서 직접 추출 ─────
        //    (이전엔 map.naver.com/v5/api/sites/summary/{id} 요약 API를 먼저 시도했으나,
        //    현재 이 엔드포인트는 React SPA HTML 셸만 리턴하고 JSON을 안 준다. 제거함.)
        //    m.place.naver.com/...는 해당 업체의 정식 페이지라 긁는 사진은 항상 해당 업체 것.
        val htmlPhotos = fetchPhotosFromPlaceHtml(placeId, maxPhotos)
        htmlPhotos.forEach { if (it !in photoUrls) photoUrls.add(it) }

        // ── 2. 방문자 리뷰 수: HTML 파싱 ────────────────────────────────────────
        visitorReviewCount = fetchVisitorReviewCountFromHtml(placeId)

        NaverPlaceDetails(photos = photoUrls.take(maxPhotos), visitorReviewCount = visitorReviewCount)
    }

    /**
     * 네이버 플레이스 장소 페이지 HTML에서 사진 URL을 직접 추출한다.
     * URL이 placeId로 스코프되어 있으므로 항상 해당 업체의 사진만 나온다.
     *
     * 네이버의 SSR 페이로드는 JSON 안에 URL을 `"https:\/\/ldb-phinf.pstatic.net\/..."`
     * 형태로 이스케이프해서 심어두므로, 정규식을 돌리기 전에 `\/` → `/` 로 un-escape 한다.
     * /photo 엔드포인트가 사진 배열을 제일 풍부하게 담고 있어 가장 먼저 시도한다.
     */
    private suspend fun fetchPhotosFromPlaceHtml(
        placeId: String,
        maxPhotos: Int = 5
    ): List<String> = withContext(Dispatchers.IO) {
        if (placeId.isBlank()) return@withContext emptyList()
        if (cbIsBlocked(cbPhotoHtmlBlockedUntil)) {
            Log.d(TAG, "[CB] photo HTML skipped (blocked): placeId=$placeId")
            return@withContext emptyList()
        }

        val urls = listOf(
            // /photo 탭이 SSR 페이로드에 사진 배열을 가장 많이 담고 있음 — 최우선
            "https://m.place.naver.com/restaurant/$placeId/photo",
            "https://m.place.naver.com/place/$placeId/photo",
            "https://pcmap.place.naver.com/restaurant/$placeId/photo",
            "https://pcmap.place.naver.com/place/$placeId/photo",
            // home 탭도 대표 이미지는 들어있음 — fallback
            "https://m.place.naver.com/restaurant/$placeId/home",
            "https://m.place.naver.com/place/$placeId/home",
            "https://pcmap.place.naver.com/place/$placeId/home"
        )
        // og:image(대표 이미지)를 최우선으로 잡는다 — pstatic.net 도메인이면 모두 허용
        val ogPattern = Regex("""<meta\s+property=["']og:image["']\s+content=["']([^"']+)["']""")
        // 네이버 플레이스 사진 CDN 전용 — 다른 도메인의 이미지는 무시
        //   ldb-phinf.pstatic.net, phinf.pstatic.net, dthumb-phinf.pstatic.net 등
        // (HTML은 이미 unescape 되어있는 상태로 이 regex를 돌린다 — 아래 참조)
        val imagePattern = Regex("""https?://[\w-]*phinf[\w-]*\.pstatic\.net/[^"'\s<>\\]+""")

        val collected = linkedSetOf<String>()
        var anyFailed = false
        for (urlStr in urls) {
            if (collected.size >= maxPhotos) break
            try {
                val conn = URL(urlStr).openConnection() as java.net.HttpURLConnection
                conn.apply {
                    requestMethod = "GET"
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-S901B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    setRequestProperty("Accept-Language", "ko-KR,ko;q=0.9")
                    setRequestProperty("Referer", "https://map.naver.com/")
                    connectTimeout = 1500
                    readTimeout = 2500
                }
                if (conn.responseCode != 200) {
                    Log.w(TAG, "HTML 사진 추출 HTTP ${conn.responseCode}: placeId=$placeId url=$urlStr")
                    anyFailed = true
                    continue
                }
                // ★ JSON-escape 해제: "https:\/\/..." → "https://..."
                //    네이버 SSR HTML은 URL을 JSON 문자열로 직렬화해 삽입하기 때문에
                //    이 단계를 거치지 않으면 phinf.pstatic.net URL이 정규식에 안 걸린다.
                val html = conn.inputStream.bufferedReader().readText()
                    .replace("\\/", "/")
                    .replace("\\u002F", "/", ignoreCase = true)

                // 1) og:image — 대표 이미지 최우선 (pstatic.net 도메인이면 모두 허용)
                ogPattern.find(html)?.groupValues?.get(1)?.let { og ->
                    if (og.contains("pstatic.net", ignoreCase = true)) {
                        collected.add(upgradeToHighQuality(og))
                    }
                }
                // 2) 본문 내 phinf.pstatic.net 이미지들
                for (m in imagePattern.findAll(html)) {
                    if (collected.size >= maxPhotos) break
                    val raw = m.value
                    // 프로필 아바타 / 마이플레이스 아이콘류 제외
                    if (raw.contains("MY_PLACE_PROFILE", ignoreCase = true)) continue
                    if (raw.contains("profile", ignoreCase = true)) continue
                    collected.add(upgradeToHighQuality(raw))
                }
                if (collected.isNotEmpty()) {
                    Log.d(TAG, "HTML 사진 추출 성공: placeId=$placeId url=$urlStr count=${collected.size}")
                    break
                } else {
                    Log.w(TAG, "HTML 사진 추출 매칭 없음: placeId=$placeId url=$urlStr (html ${html.length}자)")
                }
            } catch (e: Exception) {
                anyFailed = true
                Log.w(TAG, "HTML 사진 추출 실패 ($urlStr): ${e.message}")
            }
        }
        if (collected.isNotEmpty()) {
            cbRecordSuccess(cbPhotoHtmlFails, cbPhotoHtmlBlockedUntil)
        } else if (anyFailed) {
            cbRecordFailure(cbPhotoHtmlFails, cbPhotoHtmlBlockedUntil, "photo HTML")
        }
        collected.take(maxPhotos).toList()
    }

    // ─── 장소 결과에 사진·방문자 리뷰 수 보강 ───────────────────────────────────

    /**
     * 네이버 지도 공식 API로 사진 + 방문자 리뷰 수를 보강한다.
     * 공식 API로 placeId → 사진을 가져오지 못하면 사진은 비워둔다 (UI에서 카테고리 이모지로 대체).
     * 이전엔 이미지 검색 API로 fallback했으나, 이름만으로 검색하다 보니 동명의 영화/드라마
     * 포스터 등 업체와 무관한 사진이 표시되는 문제가 있어 제거됨.
     */
    suspend fun enrichPlacesWithDetails(
        places: List<PlaceResult>
    ): List<PlaceResult> = coroutineScope {
        // 각 place 보강에 하드 타임아웃 4.5초 — 타임아웃 나면 원본 반환.
        // 모든 place 는 병렬이므로 전체 소요도 ~4.5초 상한.
        val perPlaceTimeoutMs = 4500L

        places.map { place ->
            async(Dispatchers.IO) {
                val t0 = System.currentTimeMillis()
                val enriched = withTimeoutOrNull(perPlaceTimeoutMs) {
                    try {
                        // ── 1) placeId 확보 경로 ───────────────────────────────
                        //   a. place.url 에 map.naver.com/entry/place/12345 형태 포함 시 추출
                        //   b. AllSearch 내부 JSON API — placeId + 썸네일 한 번에
                        //   c. m.map.naver.com 검색 HTML 스크래핑
                        val addrTokens = place.address.split(" ").filter { it.isNotBlank() }
                        val region2 = addrTokens.take(2).joinToString(" ").trim()
                        // 쿼리 1개 — "지역2 + 이름". AllSearch 는 대부분 400 이 뜨므로
                        // 3 variants 돌려봤자 기다림만 길어짐.
                        val singleAllSearchQuery =
                            if (region2.isNotBlank()) "$region2 ${place.name}" else place.name

                        var placeId = extractPlaceId(place.url) ?: ""
                        val quickPhotos = mutableListOf<String>()

                        // AllSearch 로 1차 시도 — placeId 와 썸네일을 함께 받아온다
                        if (placeId.isBlank() || quickPhotos.isEmpty()) {
                            val hit = findNaverPlaceByAllSearch(singleAllSearchQuery)
                            if (hit != null) {
                                if (placeId.isBlank() && hit.placeId.isNotBlank()) placeId = hit.placeId
                                if (hit.photoUrls.isNotEmpty()) quickPhotos.addAll(hit.photoUrls)
                            }
                        }

                        // 여전히 placeId 가 비어있으면 HTML 검색 fallback (circuit breaker 존중)
                        if (placeId.isBlank()) {
                            placeId = findNaverPlaceIdBySearch(
                                name = place.name,
                                address = place.address,
                                lat = place.lat,
                                lng = place.lng,
                                category = place.category
                            ) ?: ""
                        }
                        Log.d(TAG, "enrichPlace '${place.name}' → placeId='$placeId' (allSearch 썸네일 ${quickPhotos.size}장, ${System.currentTimeMillis() - t0}ms)")

                        // ── 2) 사진 / 방문자 리뷰 수 확보 ──────────────────────
                        //   - quickPhotos (AllSearch 썸네일) 을 기본으로 깔고
                        //   - placeId 있으면 플레이스 페이지 HTML 에서 고해상도 사진 덧붙이기
                        val photos = linkedSetOf<String>().apply { addAll(quickPhotos) }
                        var reviewCount = 0
                        if (placeId.isNotBlank()) {
                            val details = fetchNaverPlaceDetailsById(placeId)
                            details.photos.forEach { photos.add(it) }
                            reviewCount = details.visitorReviewCount
                        }

                        if (photos.isEmpty()) {
                            val reason = when {
                                placeId.isBlank() -> "placeId 확보 실패 (이름='${place.name}', 주소='${place.address}')"
                                else -> "placeId=$placeId 에서 사진 추출 실패 (AllSearch / HTML 모두 빈 결과)"
                            }
                            Log.w(TAG, "사진 없음 → 이모지 표시: $reason")
                        }

                        val photoList = photos.toList()
                        place.copy(
                            imageUrl = photoList.firstOrNull() ?: "",
                            imageUrls = photoList,
                            reviewCount = reviewCount,
                            placeId = placeId
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "enrichPlace 실패 ('${place.name}'): ${e.message}")
                        place
                    }
                }
                if (enriched == null) {
                    Log.w(TAG, "enrichPlace 타임아웃 (${perPlaceTimeoutMs}ms): '${place.name}' → 원본 그대로 반환")
                    place
                } else enriched
            }
        }.awaitAll()
    }

    // ─── 유틸 ─────────────────────────────────────────────────────────────────

    fun calculateCentroid(points: List<LatLng>): LatLng {
        if (points.isEmpty()) return LatLng(37.5665, 126.9780) // 서울시청 기본값
        return LatLng(
            lat = points.map { it.lat }.average(),
            lng = points.map { it.lng }.average()
        )
    }

    fun isApiKeyConfigured(): Boolean =
        NCP_CLIENT_ID.isNotBlank() && NAVER_SEARCH_CLIENT_ID.isNotBlank()

    // ═══════════════════════════════════════════════════════════════════════════
    // 대중교통 소요시간 계산
    // ═══════════════════════════════════════════════════════════════════════════
    //
    // 네이버 지도의 대중교통 길찾기 내부 API(`map.naver.com`)를 사용해
    // 출발지 → 도착지 대중교통 소요시간(분)을 조회한다.
    //
    // 동일 네트워크 응답 스키마가 일반 사용자의 지도 페이지에서도 사용되며
    // `context[0].paths[0].durationSummary.durationMin` 필드에 총 소요 분이 들어있다.
    //
    // 공식 API가 아니므로 응답 스키마가 달라지거나 차단될 경우 단순 거리 기반
    // 휴리스틱(평균 대중교통 속도 22 km/h 가정, 기본 대기 10분 추가)으로 대체한다.
    // ───────────────────────────────────────────────────────────────────────────

    /**
     * 대중교통 소요시간을 분 단위로 반환. 실패 시 휴리스틱 결과, 그마저도 실패면 null.
     */
    suspend fun fetchTransitDurationMinutes(
        startLat: Double,
        startLng: Double,
        goalLat: Double,
        goalLng: Double
    ): Int? = withContext(Dispatchers.IO) {
        if (startLat == 0.0 || startLng == 0.0 || goalLat == 0.0 || goalLng == 0.0) {
            return@withContext null
        }

        // 1) 네이버 지도 내부 대중교통 API 시도 (circuit breaker 존중)
        if (!cbIsBlocked(cbTransitBlockedUntil)) {
            runCatching { queryNaverTransitApi(startLat, startLng, goalLat, goalLng) }
                .getOrNull()
                ?.let { return@withContext it }
        }

        // 2) 실패 시 거리 기반 휴리스틱
        estimateTransitMinutesByDistance(startLat, startLng, goalLat, goalLng)
    }

    /**
     * 네이버 지도 웹의 대중교통 경로 API를 호출해 최단 소요시간을 분 단위로 반환.
     * 실패 시 null.
     */
    private fun queryNaverTransitApi(
        sLat: Double, sLng: Double, gLat: Double, gLng: Double
    ): Int? {
        val endpoints = listOf(
            // 네이버 지도 웹 (m.map.naver.com / map.naver.com) 내부 API. 쿼리 포맷은
            // 시기에 따라 조금씩 달라지는 편이라 여러 경로를 시도한다.
            "https://map.naver.com/p/api/directions/public?start=$sLng,$sLat,&goal=$gLng,$gLat,&crs=EPSG%3A4326&mode=TIME&lang=ko&includeDetailOperation=true",
            "https://map.naver.com/v5/api/directions/public?start=$sLng,$sLat&goal=$gLng,$gLat&crs=EPSG%3A4326&mode=TIME&lang=ko",
            "https://pubtrans-mobile.naver.com/api/v1/routes/public/transport?start=$sLng,$sLat&goal=$gLng,$gLat&lang=ko&osType=AOS"
        )
        for (url in endpoints) {
            val min = fetchTransitEndpoint(url) ?: continue
            return min
        }
        return null
    }

    private fun fetchTransitEndpoint(urlStr: String): Int? {
        if (cbIsBlocked(cbTransitBlockedUntil)) {
            Log.d(TAG, "[CB] transit skipped (blocked): $urlStr")
            return null
        }
        return try {
            val url = URL(urlStr)
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.apply {
                requestMethod = "GET"
                instanceFollowRedirects = true
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/120 Mobile Safari/537.36"
                )
                setRequestProperty("Accept", "application/json, text/plain, */*")
                setRequestProperty("Referer", "https://map.naver.com/")
                setRequestProperty("Accept-Language", "ko-KR,ko;q=0.9")
                connectTimeout = 1500
                readTimeout = 2500
            }
            if (conn.responseCode != 200) {
                Log.w(TAG, "transitApi HTTP ${conn.responseCode} @ $urlStr")
                cbRecordFailure(cbTransitFails, cbTransitBlockedUntil, "transitApi")
                return null
            }
            val body = conn.inputStream.bufferedReader().readText()
            // 응답 JSON 내부 어디든 durationMin / totalTime / duration 필드가 있을 수 있으므로
            // 여러 키에 대한 패턴 스캔을 돌린다 (분 단위 우선).
            val minPatterns = listOf(
                Regex("\"durationMin\"\\s*:\\s*(\\d+)"),
                Regex("\"totalTime\"\\s*:\\s*(\\d+)"),
                Regex("\"totalMin\"\\s*:\\s*(\\d+)"),
                Regex("\"totalTimeMin\"\\s*:\\s*(\\d+)")
            )
            for (p in minPatterns) {
                val m = p.find(body)
                if (m != null) {
                    val v = m.groupValues[1].toIntOrNull() ?: continue
                    if (v in 1..360) {
                        cbRecordSuccess(cbTransitFails, cbTransitBlockedUntil)
                        return v
                    }
                }
            }
            // 초 단위 필드 (duration, totalSec)
            val secPatterns = listOf(
                Regex("\"duration\"\\s*:\\s*(\\d+)"),
                Regex("\"totalSec\"\\s*:\\s*(\\d+)"),
                Regex("\"totalTimeSec\"\\s*:\\s*(\\d+)")
            )
            for (p in secPatterns) {
                val m = p.find(body)
                if (m != null) {
                    val sec = m.groupValues[1].toIntOrNull() ?: continue
                    val min = sec / 60
                    if (min in 1..360) {
                        cbRecordSuccess(cbTransitFails, cbTransitBlockedUntil)
                        return min
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "transitApi 실패 @ $urlStr: ${e.message}")
            cbRecordFailure(cbTransitFails, cbTransitBlockedUntil, "transitApi")
            null
        }
    }

    /**
     * 대중교통 API 미가용 시 사용되는 거리 기반 휴리스틱.
     *  - 서울 기준 평균 대중교통 이동속도 ~22 km/h
     *  - 환승·대기 여유 10분 가산, 2 km 이하 단거리는 도보 기준 +5분
     */
    private fun estimateTransitMinutesByDistance(
        sLat: Double, sLng: Double, gLat: Double, gLng: Double
    ): Int? {
        val km = haversineKm(sLat, sLng, gLat, gLng)
        if (km.isNaN() || km < 0) return null
        val speedKmh = if (km < 2.0) 5.0 /* 도보 */ else 22.0 /* 대중교통 */
        val rawMin = (km / speedKmh) * 60.0
        val buffer = if (km < 2.0) 0.0 else 10.0
        val total = (rawMin + buffer).toInt().coerceAtLeast(1)
        return total
    }

    private fun haversineKm(
        lat1: Double, lng1: Double, lat2: Double, lng2: Double
    ): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}