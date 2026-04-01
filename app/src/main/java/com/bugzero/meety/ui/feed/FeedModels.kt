package com.bugzero.meety.ui.feed

/**
 * 피드에서 좋아요/패스 행동을 Firebase에 저장하는 모델
 *
 * Firestore 컬렉션: "likes"
 *
 * team 패키지 담당자가 매칭탭(보낸 관심)을 구현할 때 이 컬렉션을 읽으면 된다.
 */
data class Like(
    val likeId: String = "",
    val fromUserId: String = "",       // 좋아요를 누른 사람 (현재 로그인 유저)
    val fromTeamId: String = "",       // 내 팀 ID (팀이 없으면 빈 문자열)
    val toTeamId: String = "",         // 좋아요 받은 팀 ID
    val toTeamName: String = "",       // 표시용 팀 이름
    val toTeamTags: List<String> = emptyList(),
    val toTeamMbtiTags: List<String> = emptyList(),
    val createdAt: Long = 0L,
    val status: String = "pending"     // 좋아요 상태 (pending/accepted/rejected)
)

/**
 * 사용자가 어떤 성향의 팀을 선호/비선호하는지 누적 점수를 저장하는 모델
 *
 * Firestore 컬렉션: "userPreferences"
 *
 * 앱을 껐다 켜도 AI 추천 성향이 유지된다.
 * 나중에 Gemini AI가 이 점수를 참고해 최적의 팀을 추천할 수 있다.
 *
 * 점수 부여 기준:
 *   - 좋아요한 팀의 태그/MBTI: 점수 +
 *   - 패스한 팀의 태그/MBTI: 점수 -
 */
data class UserPreference(
    val userId: String = "",
    val tagScores: Map<String, Int> = emptyMap(),     // e.g. {"활발한": 5, "조용한": -2}
    val mbtiScores: Map<String, Int> = emptyMap(),    // e.g. {"ENFP": 4, "INTJ": -1}
    val likedTeamIds: List<String> = emptyList(),     // 이미 좋아요한 팀 → 피드에서 제외
    val passedTeamIds: List<String> = emptyList(),    // 이미 패스한 팀 → 피드에서 제외
    val updatedAt: Long = 0L
)