package com.bugzero.meety.ui.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.bugzero.meety.ui.feed.components.MatchReasonSheet
import com.bugzero.meety.ui.theme.MeetyTheme

/**
 * ══════════════════════════════════════════════════════════════════
 * 매칭 근거(MatchReasonSheet) @Preview 모음
 *
 * Android Studio → Split 뷰에서 이 파일을 열면 바로 확인 가능.
 *
 * 미리보기 목록
 * ──────────────────────────────────────────────────────────────────
 *  1. [계정1] 이준호  → 코딩마스터즈   (관심사·가치관 최고 일치, 해금)
 *  2. [계정2] 박미래  → 새벽러닝크루   (거리·활동강도 최고 일치, 해금)
 *  3. [계정3] 김소율  → 카페홀릭       (취향·분위기 최고 일치, 해금)
 *  4. [계정4] 서재원  → 사운드웨이브   (음악·몰입형 일치, 해금)
 *  5. [계정1 잠금]    → 코딩마스터즈   (actionCount=3, 마지막 카드 잠금 상태)
 * ══════════════════════════════════════════════════════════════════
 */

// ─────────────────────────────────────────────────────────────────
// 공통 래퍼 — DummyData.TestAccount 를 받아 MatchReasonSheet 를 렌더링
// ─────────────────────────────────────────────────────────────────
@Composable
private fun DummyMatchReasonSheet(
    account: DummyData.TestAccount,
    /** actionCount 를 오버라이드하고 싶을 때 (잠금 테스트 등) */
    actionCountOverride: Int = account.actionCount
) {
    val team    = DummyData.allTeams.first { it.teamId == account.targetTeamId }
    val members = DummyData.membersByTeamId[account.targetTeamId] ?: emptyList()

    MeetyTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            MatchReasonSheet(
                team               = team,
                userTopTags        = account.userTopTags,
                actionCount        = actionCountOverride,
                currentUserProfile = account.profile,
                memberProfiles     = members,
                distanceResults    = account.targetDistanceResults,
                fitScore           = account.fitScore,
                onClose            = {},
                onApply            = {}
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════
//  Preview 1  ·  계정1 이준호 → 코딩마스터즈
//  관심사 3/3 일치(코딩·알고리즘·개발), 가치관 6/6 일치, 거리 88점
// ═════════════════════════════════════════════════════════════════
@Preview(
    name      = "[계정1] 이준호 → 코딩마스터즈 (적합도 83점, 해금)",
    showBackground = true,
    heightDp  = 860,
    widthDp   = 400
)
@Composable
fun Preview_Account1_CodingMasters() {
    DummyMatchReasonSheet(account = DummyData.testAccount1)
}

// ═════════════════════════════════════════════════════════════════
//  Preview 2  ·  계정2 박미래 → 새벽러닝크루
//  관심사 3/3 일치(러닝·아침운동·조깅), 가치관 6/6 일치, 거리 83점
// ═════════════════════════════════════════════════════════════════
@Preview(
    name      = "[계정2] 박미래 → 새벽러닝크루 (적합도 79점, 해금)",
    showBackground = true,
    heightDp  = 860,
    widthDp   = 400
)
@Composable
fun Preview_Account2_RunningCrew() {
    DummyMatchReasonSheet(account = DummyData.testAccount2)
}

// ═════════════════════════════════════════════════════════════════
//  Preview 3  ·  계정3 김소율 → 카페홀릭
//  관심사 3/3 일치(카페·디저트·감성), 가치관 6/6 일치, 거리 84점
// ═════════════════════════════════════════════════════════════════
@Preview(
    name      = "[계정3] 김소율 → 카페홀릭 (적합도 81점, 해금)",
    showBackground = true,
    heightDp  = 860,
    widthDp   = 400
)
@Composable
fun Preview_Account3_CafeHolic() {
    DummyMatchReasonSheet(account = DummyData.testAccount3)
}

// ═════════════════════════════════════════════════════════════════
//  Preview 4  ·  계정4 서재원 → 한성 사운드웨이브
//  관심사 3/3 일치(기타·밴드·음악), 가치관 5/6 일치, 거리 70점
// ═════════════════════════════════════════════════════════════════
@Preview(
    name      = "[계정4] 서재원 → 사운드웨이브 (적합도 77점, 해금)",
    showBackground = true,
    heightDp  = 860,
    widthDp   = 400
)
@Composable
fun Preview_Account4_SoundWave() {
    DummyMatchReasonSheet(account = DummyData.testAccount4)
}

// ═════════════════════════════════════════════════════════════════
//  Preview 5  ·  잠금 상태 테스트 (actionCount = 3)
//  마지막 "자주 누른 태그" 카드가 잠금 UI로 표시됨
// ═════════════════════════════════════════════════════════════════
@Preview(
    name      = "[계정1 잠금] 이준호 → 코딩마스터즈 (actionCount=3, 마지막 카드 잠금)",
    showBackground = true,
    heightDp  = 860,
    widthDp   = 400
)
@Composable
fun Preview_Account1_Locked() {
    DummyMatchReasonSheet(
        account             = DummyData.testAccount1,
        actionCountOverride = 3
    )
}

// ─────────────────────────────────────────────────────────────────
//  실기기 디버그 주입 (FeedViewModel 없이 FeedScreen 에서 사용)
// ─────────────────────────────────────────────────────────────────
/**
 * 디버그 빌드에서 FeedScreen 에 더미 데이터를 주입하는 헬퍼.
 *
 * 사용 예시 (FeedScreen.kt 최상단 LaunchedEffect):
 * ```kotlin
 * if (BuildConfig.DEBUG) {
 *     LaunchedEffect(Unit) {
 *         DummyData.injectTo(viewModel)
 *     }
 * }
 * ```
 */
fun DummyData.injectTo(viewModel: FeedViewModel) {
    // 테스트 계정1 기준으로 주입
    val account = testAccount1
    val team    = allTeams.first { it.teamId == account.targetTeamId }
    val members = membersByTeamId[account.targetTeamId] ?: emptyList()

    viewModel.injectDummyState(
        teams           = allTeams,
        currentUser     = account.profile,
        userTopTags     = account.userTopTags,
        actionCount     = account.actionCount,
        memberCache     = membersByTeamId,
        distanceCache   = mapOf(team.teamId to account.targetDistanceResults),
        fitScoreCache   = mapOf(team.teamId to account.fitScore)
    )
}
