package com.bugzero.meety.ui.auth

/**
 * 회원가입 단계 밸런스 게임에서 보여지는 한 문항의 표시 데이터.
 *
 * - axis: 문항 식별 키 (저장/매칭 로직에서 사용 — 로직은 별도 담당자가 구현)
 * - question: 화면 중앙에 큰 글씨로 보여지는 질문 텍스트
 * - optionA / optionB: 위/아래 두 카드에 표시되는 보기
 *
 * 본 파일은 화면 표시에 필요한 데이터만 정의하고, 답변 저장/매칭 로직은 포함하지 않는다.
 */
data class BalanceOption(
    val emoji: String,
    val label: String,
    val subtitle: String,
    val tag: String
)

data class BalanceQuestion(
    val axis: String,
    val question: String,
    val optionA: BalanceOption,
    val optionB: BalanceOption
)

/**
 * 가입 온보딩에서 묻는 6문항.
 * meety-balancegame-mockup.html 의 QUESTIONS 배열을 그대로 옮긴 것.
 */
val BALANCE_QUESTIONS: List<BalanceQuestion> = listOf(
    BalanceQuestion(
        axis = "meeting_purpose",
        question = "모임은 어떤 쪽이 더 좋아요?",
        optionA = BalanceOption("🍻", "친목 위주", "수다 떨고 친해지는 게 좋아", "친목형"),
        optionB = BalanceOption("🎯", "목표 위주", "같이 뭔가 이뤄내는 게 좋아", "목표형")
    ),
    BalanceQuestion(
        axis = "intensity",
        question = "활동 강도는 어느 정도?",
        optionA = BalanceOption("🌿", "가볍게", "부담 없이 즐겨요", "가볍게"),
        optionB = BalanceOption("🔥", "몰입해서", "할 거면 제대로", "몰입형")
    ),
    BalanceQuestion(
        axis = "frequency",
        question = "얼마나 자주 만날까요?",
        optionA = BalanceOption("📅", "자주", "주 1회 이상", "자주 만남"),
        optionB = BalanceOption("🌙", "가끔", "월 1~2번 정도", "가끔 만남")
    ),
    BalanceQuestion(
        axis = "cost",
        question = "비용은 어떻게 하는 게 편해요?",
        optionA = BalanceOption("🧾", "깔끔하게 더치", "1/n이 편해", "더치페이"),
        optionB = BalanceOption("💸", "분위기 따라", "가끔 쏘기도 해", "유연한 비용")
    ),
    BalanceQuestion(
        axis = "vibe",
        question = "모임 분위기는?",
        optionA = BalanceOption("☕", "차분하게", "조용한 게 좋아", "차분한"),
        optionB = BalanceOption("🎉", "활발하게", "왁자지껄 좋아", "활발한")
    ),
    BalanceQuestion(
        axis = "planning",
        question = "약속 잡는 스타일은?",
        optionA = BalanceOption("⚡", "즉흥적으로", "갑자기 번개도 OK", "즉흥형"),
        optionB = BalanceOption("🗓️", "미리 계획", "날짜 정해두는 게 좋아", "계획형")
    )
)
