package com.bugzero.meety.ui.auth

/**
 * 회원가입 단계에서 묻는 밸런스 게임 한 문항.
 *
 * - axis: 매칭 알고리즘에서 사용할 축 이름이자 저장 키 (users/{uid}.balanceProfile.answers 의 key)
 * - leftValue/rightValue: 선택 시 저장되는 값. 왼쪽 -1, 오른쪽 +1 로 통일.
 *
 * 수집된 답변은 users/{uid}.balanceProfile.answers = { axis: -1 | +1 } 형태로 저장되어
 * 추후 피드 팀 추천 매칭 점수의 근거 자료로 쓰인다. (매칭 계산은 피드 담당 영역)
 */
data class BalanceQuestion(
    val axis: String,
    val category: String,
    val emoji: String,
    val question: String,
    val leftLabel: String,
    val rightLabel: String,
    val leftValue: Int = -1,
    val rightValue: Int = 1
)

/**
 * 동아리/소모임 멤버 호환성 판단용 핵심 16문항.
 * 순서는 가벼운 것 → 가치관 순으로 배치해 이탈을 줄였다.
 */
val BALANCE_QUESTIONS: List<BalanceQuestion> = listOf(
    BalanceQuestion("purpose", "동아리 가치관", "🎯", "너에게 동아리란?",
        "스펙·진로 쌓는 곳", "그냥 즐기러 오는 곳"),
    BalanceQuestion("seriousness", "동아리 분위기", "🔥", "선호하는 동아리 분위기는?",
        "진지하게 활동", "유쾌하게 친목"),
    BalanceQuestion("commitment", "참여도", "📅", "시험 기간에는?",
        "시험 기간에도 그대로", "시험 기간엔 잠시 쉼"),
    BalanceQuestion("responsibility", "책임감", "💪", "맡은 일이 버거워지면?",
        "어떻게든 끝까지", "안 되겠다 싶으면 손 뗌"),
    BalanceQuestion("time_punctual", "시간 약속", "⏰", "모임 시간엔?",
        "10분 전 도착", "5분 늦어도 OK"),
    BalanceQuestion("conflict_avoid", "갈등 대처", "🗣️", "무임승차 동기를 보면?",
        "그 자리에서 따끔하게", "그냥 내가 더 함"),
    BalanceQuestion("decision", "의사결정", "🤝", "의견 충돌이 생기면?",
        "다수결로 빠르게", "모두 동의할 때까지"),
    BalanceQuestion("comm_freq", "단톡 스타일", "💬", "단톡방에서 너는?",
        "활발히 떠듦", "공지만 조용히 봄"),
    BalanceQuestion("extraversion", "에너지", "🎉", "사람 많은 모임에 가면?",
        "에너지 충전됨", "끝나고 방전됨"),
    BalanceQuestion("alcohol_pace", "회식·뒤풀이", "🍻", "회식·뒤풀이는?",
        "끝까지 풀로", "1차에서 마무리"),
    BalanceQuestion("leadership", "역할", "👑", "팀 작업을 맡으면?",
        "리더 자원", "서포트 역할"),
    BalanceQuestion("structure", "일하는 방식", "📋", "일은 어떻게?",
        "역할 명확히 분담", "다같이 다 해보기"),
    BalanceQuestion("activity_intensity", "활동 강도", "🧗", "같이 하고 싶은 건?",
        "방탈출·등산처럼 자극적", "카페·식사처럼 편안"),
    BalanceQuestion("indoor_outdoor", "활동 장소", "🌳", "활동 장소는?",
        "실내(보드게임·스터디)", "실외(나들이·운동)"),
    BalanceQuestion("hierarchy", "선후배 관계", "🎓", "선후배 관계는?",
        "위계 있는 게 좋음", "다 친구처럼"),
    BalanceQuestion("group_openness", "멤버 구성", "🚪", "멤버 구성은?",
        "새 멤버 계속 받기", "친한 멤버끼리 안정적")
)
