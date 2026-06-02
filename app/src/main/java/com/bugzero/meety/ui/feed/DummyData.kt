package com.bugzero.meety.ui.feed

import com.bugzero.meety.ui.team.Team

/**
 * ══════════════════════════════════════════════════════════════════
 * 매칭 근거(MatchReasonSheet) 시연 / 테스트용 더미 데이터
 *
 * 사용 방법
 * ──────────────────────────────────────────────────────────────────
 * 1. MatchReasonSheetPreview.kt 의 @Preview 로 Android Studio 에서 바로 확인
 * 2. 실기기 디버그 빌드에서 FeedScreen 에 DummyData.injectTo(viewModel) 호출
 *
 * 테스트 계정 × 타겟 팀 대응표
 * ──────────────────────────────────────────────────────────────────
 *  계정1  이준호 (컴공 / 성북구) → 코딩마스터즈   (관심사·가치관 최고 일치)
 *  계정2  박미래 (스포츠 / 강북구) → 새벽러닝크루 (거리·활동강도 최고 일치)
 *  계정3  김소율 (디자인 / 성북구) → 카페홀릭     (취향·분위기 최고 일치)
 *  계정4  서재원 (예술 / 노원구)  → 한성사운드웨이브 (음악·몰입형 최고 일치)
 * ══════════════════════════════════════════════════════════════════
 */
object DummyData {

    // ─────────────────────────────────────────────────────────────
    //  데이터 클래스 (테스트 계정 래퍼)
    // ─────────────────────────────────────────────────────────────

    /**
     * 테스트 계정 묶음.
     * [targetTeamId] 팀에 대한 MatchReasonSheet 를 바로 띄울 수 있도록
     * 거리 결과와 fitScore 를 미리 계산해 포함시킴.
     */
    data class TestAccount(
        val profile: CurrentUserProfile,
        /** actionCount >= 10 → "자주 누른 태그" 카드 해금 */
        val userTopTags: List<String>,
        val actionCount: Int,
        /** 이 계정과 가장 잘 맞는 팀 ID */
        val targetTeamId: String,
        /** 타겟 팀 멤버까지의 대중교통 거리 결과 (미리 계산) */
        val targetDistanceResults: List<MemberDistanceResult>,
        /** 미리 계산한 종합 적합도 점수 (카드 배지와 동일하게 맞춰야 함) */
        val fitScore: Int
    )

    // ═════════════════════════════════════════════════════════════
    //  팀 목록 (20개)  ·  meety_dummy_data.xlsx teams 시트 기준
    // ═════════════════════════════════════════════════════════════

    // ── 스터디 (5) ────────────────────────────────────────────────

    val teamCodingMasters = Team(
        teamId      = "team-001",
        teamName    = "코딩마스터즈",
        description = "알고리즘·백엔드 스터디",
        tags        = listOf("코딩", "알고리즘", "백엔드", "개발"),
        mbtiTags    = listOf("INTP", "INTJ", "ISTP"),
        memberIds   = listOf("m-001", "m-002", "m-003", "m-004"),
        status      = "active",
        balanceProfile = mapOf(
            "meeting_purpose" to  1.0f,   // 목표형 만장일치
            "intensity"       to  0.75f,  // 몰입형 다수
            "frequency"       to -0.5f,   // 자주 만남 다수
            "cost"            to -1.0f,   // 더치페이 만장일치
            "vibe"            to -0.5f,   // 차분 다수
            "planning"        to  1.0f    // 계획형 만장일치
        )
    )

    val teamToeic900 = Team(
        teamId      = "team-002",
        teamName    = "토익900 달성반",
        description = "목표 900+ 토익 스터디",
        tags        = listOf("토익", "영어", "자격증", "어학"),
        mbtiTags    = listOf("ISTJ", "ESTJ", "ISFJ"),
        memberIds   = listOf("m-005", "m-006", "m-007", "m-008"),
        status      = "active",
        balanceProfile = mapOf(
            "meeting_purpose" to  1.0f,
            "intensity"       to  0.5f,
            "frequency"       to -0.5f,
            "cost"            to -1.0f,
            "vibe"            to -1.0f,
            "planning"        to  1.0f
        )
    )

    val teamMajorCrunch = Team(
        teamId      = "team-003",
        teamName    = "전공벼락치기",
        description = "기말고사 전공 스터디",
        tags        = listOf("전공", "시험", "학점", "공부"),
        mbtiTags    = listOf("ISTJ", "INTJ", "ENFJ"),
        memberIds   = listOf("m-009", "m-010", "m-011"),
        status      = "active",
        balanceProfile = mapOf(
            "meeting_purpose" to  1.0f,
            "intensity"       to  1.0f,
            "frequency"       to -1.0f,
            "cost"            to -1.0f,
            "vibe"            to -0.33f,
            "planning"        to  1.0f
        )
    )

    val teamCertHunter = Team(
        teamId      = "team-004",
        teamName    = "자격증 헌터",
        description = "정처기·ADsP 같이 준비",
        tags        = listOf("자격증", "정처기", "ADsP", "취업"),
        mbtiTags    = listOf("ISTJ", "ESTJ", "ENTJ"),
        memberIds   = listOf("m-012", "m-013", "m-014", "m-015"),
        status      = "active",
        balanceProfile = mapOf(
            "meeting_purpose" to  1.0f,
            "intensity"       to  0.75f,
            "frequency"       to -0.5f,
            "cost"            to -0.75f,
            "vibe"            to -0.25f,
            "planning"        to  1.0f
        )
    )

    val teamBookworm = Team(
        teamId      = "team-005",
        teamName    = "책벌레 토론방",
        description = "격주 독서 토론 모임",
        tags        = listOf("독서", "토론", "인문학", "교양"),
        mbtiTags    = listOf("INFJ", "INFP", "ENFP"),
        memberIds   = listOf("m-016", "m-017", "m-018"),
        status      = "active",
        balanceProfile = mapOf(
            "meeting_purpose" to -0.33f,
            "intensity"       to -0.67f,
            "frequency"       to  0.33f,
            "cost"            to -0.33f,
            "vibe"            to -1.0f,
            "planning"        to  0.33f
        )
    )

    // ── 운동 (4) ──────────────────────────────────────────────────

    val teamMuscleFab = Team(
        teamId      = "team-006",
        teamName    = "한성 머슬팩토리",
        description = "같이 운동하고 식단 공유",
        tags        = listOf("헬스", "운동", "다이어트", "벌크업"),
        mbtiTags    = listOf("ESTP", "ISTP", "ENTJ"),
        memberIds   = listOf("m-019", "m-020", "m-021", "m-022"),
        status      = "active",
        balanceProfile = mapOf(
            "meeting_purpose" to  0.5f,
            "intensity"       to  1.0f,
            "frequency"       to -0.75f,
            "cost"            to -0.5f,
            "vibe"            to  0.75f,
            "planning"        to  0.25f
        )
    )

    val teamRunningCrew = Team(
        teamId      = "team-007",
        teamName    = "새벽러닝크루",
        description = "매주 토요일 새벽 러닝",
        tags        = listOf("러닝", "조깅", "아침운동", "건강"),
        mbtiTags    = listOf("ENFP", "ESFP", "ESTP"),
        memberIds   = listOf("m-023", "m-024", "m-025", "m-026"),
        status      = "active",
        balanceProfile = mapOf(
            "meeting_purpose" to -0.5f,
            "intensity"       to  0.75f,
            "frequency"       to -1.0f,
            "cost"            to -1.0f,
            "vibe"            to  1.0f,
            "planning"        to  0.5f
        )
    )

    val teamBadminton = Team(
        teamId      = "team-008",
        teamName    = "셔틀콕 동아리",
        description = "배드민턴 초보~중급",
        tags        = listOf("배드민턴", "라켓", "운동", "체육관"),
        mbtiTags    = listOf("ESFP", "ENFP", "ISFP"),
        memberIds   = listOf("m-027", "m-028", "m-029", "m-030"),
        status      = "active",
        balanceProfile = mapOf(
            "meeting_purpose" to -0.75f,
            "intensity"       to -0.25f,
            "frequency"       to -0.5f,
            "cost"            to -0.75f,
            "vibe"            to  0.75f,
            "planning"        to -0.25f
        )
    )

    val teamFutsal = Team(
        teamId      = "team-009",
        teamName    = "골넣자 풋살팀",
        description = "주말 풋살 경기",
        tags        = listOf("풋살", "축구", "스포츠", "팀플레이"),
        mbtiTags    = listOf("ESTP", "ENTP", "ISTP"),
        memberIds   = listOf("m-031", "m-032", "m-033", "m-034", "m-035"),
        status      = "active",
        balanceProfile = mapOf(
            "meeting_purpose" to -0.2f,
            "intensity"       to  0.8f,
            "frequency"       to -0.6f,
            "cost"            to -0.6f,
            "vibe"            to  1.0f,
            "planning"        to  0.2f
        )
    )

    // ── 맛집 (3) ──────────────────────────────────────────────────

    val teamFoodClub = Team(
        teamId      = "team-010",
        teamName    = "성북 미식회",
        description = "성북구 숨은 맛집 탐방",
        tags        = listOf("맛집", "성북구", "먹방", "푸드"),
        mbtiTags    = listOf("ESFP", "ENFP", "ESFJ"),
        memberIds   = listOf("m-036", "m-037", "m-038"),
        status      = "active",
        balanceProfile = mapOf(
            "meeting_purpose" to -1.0f,
            "intensity"       to -0.67f,
            "frequency"       to -0.33f,
            "cost"            to  0.33f,
            "vibe"            to  0.67f,
            "planning"        to -0.33f
        )
    )

    val teamCafeHolic = Team(
        teamId      = "team-011",
        teamName    = "카페홀릭",
        description = "서울 감성카페 투어",
        tags        = listOf("카페", "디저트", "감성", "브런치"),
        mbtiTags    = listOf("INFP", "ISFP", "ENFP"),
        memberIds   = listOf("m-039", "m-040", "m-041"),
        status      = "active",
        balanceProfile = mapOf(
            "meeting_purpose" to -1.0f,   // 친목형 만장일치
            "intensity"       to -1.0f,   // 가볍게 만장일치
            "frequency"       to  0.33f,  // 가끔 만남 다수
            "cost"            to  0.33f,  // 유연한 비용 다수
            "vibe"            to -0.67f,  // 차분한 다수
            "planning"        to -0.33f   // 즉흥형 약간
        )
    )

    val teamLateNight = Team(
        teamId      = "team-012",
        teamName    = "야식특공대",
        description = "금요일 밤 야식 번개",
        tags        = listOf("야식", "치킨", "맥주", "번개"),
        mbtiTags    = listOf("ESTP", "ESFP", "ENTP"),
        memberIds   = listOf("m-042", "m-043", "m-044", "m-045"),
        status      = "active",
        balanceProfile = mapOf(
            "meeting_purpose" to -1.0f,
            "intensity"       to -0.5f,
            "frequency"       to -0.5f,
            "cost"            to  0.5f,
            "vibe"            to  1.0f,
            "planning"        to -1.0f
        )
    )

    // ── 취미 (3) ──────────────────────────────────────────────────

    val teamBoardGame = Team(
        teamId      = "team-013",
        teamName    = "주사위 굴려",
        description = "매주 보드게임 모임",
        tags        = listOf("보드게임", "카탄", "마피아", "취미"),
        mbtiTags    = listOf("ENTP", "ENFP", "INTP"),
        memberIds   = listOf("m-046", "m-047", "m-048", "m-049"),
        status      = "active",
        balanceProfile = mapOf(
            "meeting_purpose" to -0.5f,
            "intensity"       to -0.25f,
            "frequency"       to -0.25f,
            "cost"            to -0.5f,
            "vibe"            to  0.5f,
            "planning"        to -0.5f
        )
    )

    val teamMovieNight = Team(
        teamId      = "team-014",
        teamName    = "무비나잇",
        description = "매월 영화 감상 & 리뷰",
        tags        = listOf("영화", "감상", "리뷰", "OTT"),
        mbtiTags    = listOf("INFP", "INFJ", "ISFP"),
        memberIds   = listOf("m-050", "m-051", "m-052"),
        status      = "active",
        balanceProfile = mapOf(
            "meeting_purpose" to -0.67f,
            "intensity"       to -0.67f,
            "frequency"       to  0.33f,
            "cost"            to  0.0f,
            "vibe"            to -0.67f,
            "planning"        to  0.33f
        )
    )

    val teamPhotoClub = Team(
        teamId      = "team-015",
        teamName    = "찰칵 포토클럽",
        description = "사진 촬영 & 보정 스터디",
        tags        = listOf("사진", "카메라", "보정", "출사"),
        mbtiTags    = listOf("ISFP", "INFP", "INTP"),
        memberIds   = listOf("m-053", "m-054", "m-055"),
        status      = "active",
        balanceProfile = mapOf(
            "meeting_purpose" to -0.33f,
            "intensity"       to -0.33f,
            "frequency"       to -0.33f,
            "cost"            to -0.33f,
            "vibe"            to -0.33f,
            "planning"        to  0.67f
        )
    )

    // ── 여행 (2) ──────────────────────────────────────────────────

    val teamHiking = Team(
        teamId      = "team-016",
        teamName    = "등산왕",
        description = "주말 근교 등산",
        tags        = listOf("등산", "산", "자연", "하이킹"),
        mbtiTags    = listOf("ISFJ", "ISTJ", "ESFJ"),
        memberIds   = listOf("m-056", "m-057", "m-058", "m-059"),
        status      = "active",
        balanceProfile = mapOf(
            "meeting_purpose" to -0.5f,
            "intensity"       to  0.25f,
            "frequency"       to -0.5f,
            "cost"            to -0.5f,
            "vibe"            to -0.25f,
            "planning"        to  0.75f
        )
    )

    val teamDayTrip = Team(
        teamId      = "team-017",
        teamName    = "당일치기 탐험대",
        description = "서울 근교 당일 여행",
        tags        = listOf("여행", "당일치기", "근교", "탐험"),
        mbtiTags    = listOf("ENFP", "ESFP", "ENTP"),
        memberIds   = listOf("m-060", "m-061", "m-062"),
        status      = "active",
        balanceProfile = mapOf(
            "meeting_purpose" to -1.0f,
            "intensity"       to  0.33f,
            "frequency"       to -0.33f,
            "cost"            to  0.33f,
            "vibe"            to  0.67f,
            "planning"        to -0.67f
        )
    )

    // ── 음악 (2) ──────────────────────────────────────────────────

    val teamSoundWave = Team(
        teamId      = "team-018",
        teamName    = "한성 사운드웨이브",
        description = "밴드 합주 & 공연 준비",
        tags        = listOf("밴드", "기타", "드럼", "음악"),
        mbtiTags    = listOf("ENFP", "ISFP", "ENTP"),
        memberIds   = listOf("m-063", "m-064", "m-065", "m-066"),
        status      = "active",
        balanceProfile = mapOf(
            "meeting_purpose" to -0.25f,  // 친목형 약간
            "intensity"       to  1.0f,   // 몰입형 만장일치
            "frequency"       to -0.5f,   // 자주 만남 다수
            "cost"            to -0.5f,   // 더치페이 다수
            "vibe"            to  0.75f,  // 활발 다수
            "planning"        to  0.5f    // 계획형 다수
        )
    )

    val teamBusking = Team(
        teamId      = "team-019",
        teamName    = "길거리 하모니",
        description = "홍대 버스킹 팀",
        tags        = listOf("버스킹", "노래", "어쿠스틱", "공연"),
        mbtiTags    = listOf("ENFP", "ESFP", "INFP"),
        memberIds   = listOf("m-067", "m-068", "m-069"),
        status      = "active",
        balanceProfile = mapOf(
            "meeting_purpose" to -1.0f,
            "intensity"       to  0.33f,
            "frequency"       to -0.67f,
            "cost"            to  0.0f,
            "vibe"            to  1.0f,
            "planning"        to -0.67f
        )
    )

    // ── 자기계발 (1) ──────────────────────────────────────────────

    val teamPortfolioLab = Team(
        teamId      = "team-020",
        teamName    = "포폴 피드백 랩",
        description = "포트폴리오 상호 리뷰",
        tags        = listOf("포트폴리오", "취업", "이력서", "피드백"),
        mbtiTags    = listOf("ENTJ", "INTJ", "ENTP"),
        memberIds   = listOf("m-070", "m-071", "m-072"),
        status      = "active",
        balanceProfile = mapOf(
            "meeting_purpose" to  1.0f,
            "intensity"       to  0.33f,
            "frequency"       to  0.33f,
            "cost"            to -0.67f,
            "vibe"            to -0.33f,
            "planning"        to  0.67f
        )
    )

    /** 전체 팀 목록 (피드에 주입할 때 사용) */
    val allTeams: List<Team> = listOf(
        teamCodingMasters, teamToeic900, teamMajorCrunch, teamCertHunter, teamBookworm,
        teamMuscleFab, teamRunningCrew, teamBadminton, teamFutsal,
        teamFoodClub, teamCafeHolic, teamLateNight,
        teamBoardGame, teamMovieNight, teamPhotoClub,
        teamHiking, teamDayTrip,
        teamSoundWave, teamBusking,
        teamPortfolioLab
    )

    // ═════════════════════════════════════════════════════════════
    //  팀원 프로필  ·  meety_dummy_data.xlsx members 시트 기준
    // ═════════════════════════════════════════════════════════════

    // ── 코딩마스터즈 ──────────────────────────────────────────────
    val membersCodingMasters: List<MemberProfile> = listOf(
        MemberProfile("m-001", "박준혁", 23, "컴퓨터공학부", "INTP", "백엔드 개발에 관심 많은 3학년입니다", 178, "서울 성북구",
            interests = listOf("코딩", "알고리즘", "게임"),
            foodLikes = listOf("라멘", "초밥")),
        MemberProfile("m-002", "김서연", 22, "컴퓨터공학부", "INTJ", "프론트엔드 개발자 지망생이에요", 164, "서울 강북구",
            interests = listOf("웹개발", "UI디자인", "독서"),
            foodLikes = listOf("파스타", "샐러드")),
        MemberProfile("m-003", "이동현", 24, "컴퓨터공학부", "ISTP", "시스템 프로그래밍 좋아합니다", 175, "서울 노원구",
            interests = listOf("리눅스", "서버", "자전거"),
            foodLikes = listOf("삼겹살", "비빔밥")),
        MemberProfile("m-004", "정하은", 22, "AI응용학과", "INTP", "머신러닝 공부 중! 같이 해요", 160, "서울 성북구",
            interests = listOf("AI", "데이터분석", "카페투어"),
            foodLikes = listOf("떡볶이", "마카롱"))
    )

    // ── 토익900 달성반 ────────────────────────────────────────────
    val membersToeic900: List<MemberProfile> = listOf(
        MemberProfile("m-005", "최민수", 23, "영어영문학트랙", "ISTJ", "현재 850점, 900 넘기고 싶어요", 180, "서울 성북구",
            interests = listOf("영어", "미드", "해외여행"),
            foodLikes = listOf("스테이크", "버거")),
        MemberProfile("m-006", "한소윤", 22, "경영트랙", "ESTJ", "취업 준비 중, 토익 필수!", 163, "서울 동대문구",
            interests = listOf("토익", "자격증", "요가"),
            foodLikes = listOf("샐러드", "아사이볼")),
        MemberProfile("m-007", "유재민", 24, "무역학트랙", "ISTJ", "무역 회사 취업 목표입니다", 176, "서울 중랑구",
            interests = listOf("경제", "뉴스", "농구"),
            foodLikes = listOf("치킨", "피자")),
        MemberProfile("m-008", "김예진", 21, "경영트랙", "ISFJ", "꾸준히 공부하는 스타일이에요", 158, "서울 성북구",
            interests = listOf("어학", "일본어", "드라마"),
            foodLikes = listOf("일식", "카레"))
    )

    // ── 전공벼락치기 ──────────────────────────────────────────────
    val membersMajorCrunch: List<MemberProfile> = listOf(
        MemberProfile("m-009", "임성호", 23, "전자트랙", "ISTJ", "회로이론 같이 공부해요", 174, "서울 성북구",
            interests = listOf("전자공학", "납땜", "게임"),
            foodLikes = listOf("돈까스", "우동")),
        MemberProfile("m-010", "오지수", 22, "전자트랙", "INTJ", "전공 A+ 목표! 같이 달려요", 166, "서울 강북구",
            interests = listOf("공부", "피아노", "넷플릭스"),
            foodLikes = listOf("떡볶이", "타코")),
        MemberProfile("m-011", "강태윤", 23, "전자트랙", "ENFJ", "네트워크 전공, 같이 시험 준비", 181, "서울 도봉구",
            interests = listOf("네트워크", "보안", "축구"),
            foodLikes = listOf("삼겹살", "냉면"))
    )

    // ── 자격증 헌터 ───────────────────────────────────────────────
    val membersCertHunter: List<MemberProfile> = listOf(
        MemberProfile("m-012", "신동우", 24, "컴퓨터공학부", "ISTJ", "정처기 실기 준비 중", 177, "서울 성북구",
            interests = listOf("자격증", "코딩", "헬스"),
            foodLikes = listOf("곱창", "소주")),
        MemberProfile("m-013", "윤서아", 23, "빅데이터트랙", "ESTJ", "ADsP 따고 빅데이터 분야 취업!", 162, "서울 종로구",
            interests = listOf("데이터", "통계", "카페"),
            foodLikes = listOf("브런치", "크로와상")),
        MemberProfile("m-014", "조현우", 25, "융합보안학과", "ENTJ", "정보보안기사 도전 중입니다", 179, "서울 성북구",
            interests = listOf("보안", "해킹", "등산"),
            foodLikes = listOf("불고기", "된장찌개")),
        MemberProfile("m-015", "배수현", 22, "컴퓨터공학부", "ISTJ", "리눅스마스터 같이 준비해요", 165, "서울 동대문구",
            interests = listOf("리눅스", "서버", "요리"),
            foodLikes = listOf("파스타", "리조또"))
    )

    // ── 책벌레 토론방 ─────────────────────────────────────────────
    val membersBookworm: List<MemberProfile> = listOf(
        MemberProfile("m-016", "문채원", 22, "한국어문학트랙", "INFJ", "인문학 책 좋아해요, 같이 읽어요", 167, "서울 성북구",
            interests = listOf("독서", "글쓰기", "산책"),
            foodLikes = listOf("한식", "차")),
        MemberProfile("m-017", "황지훈", 23, "역사문화트랙", "INFP", "철학서적 토론 좋아합니다", 173, "서울 종로구",
            interests = listOf("철학", "영화", "커피"),
            foodLikes = listOf("수제버거", "크래프트맥주")),
        MemberProfile("m-018", "노유진", 21, "미디어디자인트랙", "ENFP", "소설·에세이 다 좋아해요!", 161, "서울 성북구",
            interests = listOf("소설", "글쓰기", "카페투어"),
            foodLikes = listOf("케이크", "마카롱"))
    )

    // ── 한성 머슬팩토리 ───────────────────────────────────────────
    val membersMuscleFab: List<MemberProfile> = listOf(
        MemberProfile("m-019", "장민기", 24, "스포츠미디어트랙", "ESTP", "벌크업 중! 같이 운동하실 분", 183, "서울 성북구",
            interests = listOf("헬스", "식단관리", "복싱"),
            foodLikes = listOf("닭가슴살", "고구마")),
        MemberProfile("m-020", "서예린", 22, "스포츠미디어트랙", "ISTP", "필라테스·웨이트 둘 다 해요", 168, "서울 강북구",
            interests = listOf("필라테스", "웨이트", "요리"),
            foodLikes = listOf("샐러드", "연어")),
        MemberProfile("m-021", "한승준", 23, "경영트랙", "ENTJ", "운동 루틴 공유합시다", 180, "서울 동대문구",
            interests = listOf("헬스", "농구", "투자"),
            foodLikes = listOf("스테이크", "프로틴")),
        MemberProfile("m-022", "김태영", 25, "인테리어디자인트랙", "ESTP", "3분할 루틴으로 훈련 중", 185, "서울 성북구",
            interests = listOf("헬스", "수영", "캠핑"),
            foodLikes = listOf("소고기", "계란"))
    )

    // ── 새벽러닝크루 ──────────────────────────────────────────────
    val membersRunningCrew: List<MemberProfile> = listOf(
        MemberProfile("m-023", "이수빈", 22, "뷰티디자인매니지먼트트랙", "ENFP", "러닝으로 하루 시작해요!", 165, "서울 성북구",
            interests = listOf("러닝", "요가", "여행"),
            foodLikes = listOf("과일", "그래놀라")),
        MemberProfile("m-024", "정우성", 24, "산업공학트랙", "ESFP", "마라톤 완주가 목표입니다", 178, "서울 노원구",
            interests = listOf("마라톤", "등산", "사진"),
            foodLikes = listOf("바나나", "오트밀")),
        MemberProfile("m-025", "양하윤", 21, "상담심리트랙", "ENFP", "같이 뛰면 덜 힘들어요~", 162, "서울 성북구",
            interests = listOf("러닝", "심리학", "카페"),
            foodLikes = listOf("스무디", "샌드위치")),
        MemberProfile("m-026", "김재훈", 23, "경제트랙", "ESTP", "주 3회 한강 러닝 합니다", 176, "서울 중구",
            interests = listOf("러닝", "자전거", "음악"),
            foodLikes = listOf("파워에이드", "에너지바"))
    )

    // ── 셔틀콕 동아리 ─────────────────────────────────────────────
    val membersBadminton: List<MemberProfile> = listOf(
        MemberProfile("m-027", "박서진", 22, "경영트랙", "ESFP", "배드민턴 초보인데 재밌어요!", 170, "서울 성북구",
            interests = listOf("배드민턴", "테니스", "쇼핑"),
            foodLikes = listOf("치킨", "떡볶이")),
        MemberProfile("m-028", "이준서", 23, "기계설계트랙", "ENFP", "동호회 급 실력 목표!", 179, "서울 동대문구",
            interests = listOf("배드민턴", "게임", "맛집"),
            foodLikes = listOf("족발", "보쌈")),
        MemberProfile("m-029", "김나연", 21, "커뮤니케이션디자인트랙", "ISFP", "운동 부족해서 시작했어요", 163, "서울 성북구",
            interests = listOf("배드민턴", "그림", "넷플릭스"),
            foodLikes = listOf("파스타", "피자")),
        MemberProfile("m-030", "최원빈", 24, "전자트랙", "ENFP", "중급 수준, 같이 실력 올려요", 182, "서울 강북구",
            interests = listOf("배드민턴", "탁구", "코딩"),
            foodLikes = listOf("햄버거", "감자튀김"))
    )

    // ── 골넣자 풋살팀 ─────────────────────────────────────────────
    val membersFutsal: List<MemberProfile> = listOf(
        MemberProfile("m-031", "송민혁", 24, "스포츠미디어트랙", "ESTP", "풋살 경력 3년, 포지션 공격수", 180, "서울 성북구",
            interests = listOf("풋살", "축구", "피파"),
            foodLikes = listOf("삼겹살", "맥주")),
        MemberProfile("m-032", "이하준", 23, "경영트랙", "ENTP", "수비 담당! 주말마다 뜁니다", 177, "서울 종로구",
            interests = listOf("풋살", "농구", "투자"),
            foodLikes = listOf("치킨", "피자")),
        MemberProfile("m-033", "김도윤", 22, "인테리어디자인트랙", "ISTP", "미드필더 선호합니다", 175, "서울 동대문구",
            interests = listOf("풋살", "건축", "게임"),
            foodLikes = listOf("라멘", "돈까스")),
        MemberProfile("m-034", "정수아", 22, "스포츠미디어트랙", "ESTP", "여자 풋살러! 같이해요", 167, "서울 성북구",
            interests = listOf("풋살", "태권도", "음악"),
            foodLikes = listOf("떡볶이", "순두부")),
        MemberProfile("m-035", "오재현", 25, "사회트랙", "ENTP", "골키퍼 자청합니다 ㅋㅋ", 184, "서울 노원구",
            interests = listOf("풋살", "야구관람", "맛집"),
            foodLikes = listOf("곱창", "소주"))
    )

    // ── 성북 미식회 ───────────────────────────────────────────────
    val membersFoodClub: List<MemberProfile> = listOf(
        MemberProfile("m-036", "강유나", 22, "식품영양학트랙", "ESFP", "성북구 맛집 다 알아요!", 164, "서울 성북구",
            interests = listOf("맛집", "요리", "인스타"),
            foodLikes = listOf("한식", "일식")),
        MemberProfile("m-037", "백승호", 23, "호텔외식경영학과", "ENFP", "숨은 맛집 발굴이 취미", 176, "서울 성북구",
            interests = listOf("맛집탐방", "요리", "사진"),
            foodLikes = listOf("중식", "양식")),
        MemberProfile("m-038", "임수진", 21, "미디어디자인트랙", "ESFJ", "먹방 블로그 운영 중이에요", 160, "서울 강북구",
            interests = listOf("먹방", "블로그", "카페"),
            foodLikes = listOf("디저트", "브런치"))
    )

    // ── 카페홀릭 ──────────────────────────────────────────────────
    val membersCafeHolic: List<MemberProfile> = listOf(
        MemberProfile("m-039", "전예은", 21, "커뮤니케이션디자인트랙", "INFP", "감성카페 찾아다니는 게 취미", 162, "서울 성북구",
            interests = listOf("카페", "디자인", "사진"),
            foodLikes = listOf("라떼", "크루아상")),
        MemberProfile("m-040", "구자현", 23, "인테리어디자인트랙", "ISFP", "인테리어 좋은 카페 좋아해요", 178, "서울 마포구",
            interests = listOf("카페", "건축", "스케치"),
            foodLikes = listOf("에스프레소", "티라미수")),
        MemberProfile("m-041", "한지민", 22, "한국어문학트랙", "ENFP", "카페에서 책 읽는 거 최고!", 165, "서울 성북구",
            interests = listOf("카페", "독서", "글쓰기"),
            foodLikes = listOf("말차라떼", "베이글"))
    )

    // ── 야식특공대 ────────────────────────────────────────────────
    val membersLateNight: List<MemberProfile> = listOf(
        MemberProfile("m-042", "안재원", 24, "경영트랙", "ESTP", "금요일 밤엔 치맥이죠", 181, "서울 성북구",
            interests = listOf("야식", "치맥", "당구"),
            foodLikes = listOf("치킨", "맥주")),
        MemberProfile("m-043", "홍서윤", 22, "미디어디자인트랙", "ESFP", "포장마차 감성 좋아해요~", 163, "서울 동대문구",
            interests = listOf("야식", "포장마차", "노래방"),
            foodLikes = listOf("떡볶이", "오뎅")),
        MemberProfile("m-044", "류민석", 23, "신소재화학트랙", "ENTP", "새벽 라면 같이 먹을 사람?", 177, "서울 성북구",
            interests = listOf("야식", "게임", "맥주"),
            foodLikes = listOf("라면", "교촌치킨")),
        MemberProfile("m-045", "장서연", 21, "상담심리트랙", "ESFP", "야식은 곧 힐링이다", 159, "서울 종로구",
            interests = listOf("야식", "심리학", "유튜브"),
            foodLikes = listOf("피자", "닭발"))
    )

    // ── 주사위 굴려 ───────────────────────────────────────────────
    val membersBoardGame: List<MemberProfile> = listOf(
        MemberProfile("m-046", "허진우", 23, "빅데이터트랙", "ENTP", "카탄 좋아하는 전략 게이머", 175, "서울 성북구",
            interests = listOf("보드게임", "전략게임", "수학"),
            foodLikes = listOf("피자", "콜라")),
        MemberProfile("m-047", "유하은", 22, "상담심리트랙", "ENFP", "마피아 게임 고수에요!", 164, "서울 강북구",
            interests = listOf("보드게임", "심리학", "영화"),
            foodLikes = listOf("과자", "떡볶이")),
        MemberProfile("m-048", "남지호", 24, "경제트랙", "INTP", "복잡한 게임일수록 재밌어요", 179, "서울 동대문구",
            interests = listOf("보드게임", "퍼즐", "투자"),
            foodLikes = listOf("초밥", "라멘")),
        MemberProfile("m-049", "손예림", 21, "커뮤니케이션디자인트랙", "ENFP", "귀여운 게임 좋아해요~", 158, "서울 성북구",
            interests = listOf("보드게임", "일러스트", "카페"),
            foodLikes = listOf("마카롱", "밀크티"))
    )

    // ── 무비나잇 ──────────────────────────────────────────────────
    val membersMovieNight: List<MemberProfile> = listOf(
        MemberProfile("m-050", "윤서준", 23, "문학문화콘텐츠학과", "INFP", "매달 영화 2편 감상 & 리뷰", 176, "서울 성북구",
            interests = listOf("영화", "감독론", "글쓰기"),
            foodLikes = listOf("팝콘", "나초")),
        MemberProfile("m-051", "김도연", 22, "미디어디자인트랙", "INFJ", "OTT 정주행이 일상이에요", 163, "서울 마포구",
            interests = listOf("넷플릭스", "드라마", "카페"),
            foodLikes = listOf("와인", "치즈")),
        MemberProfile("m-052", "이채린", 21, "한국어문학트랙", "ISFP", "예술영화 좋아하는 문과생", 160, "서울 성북구",
            interests = listOf("영화", "소설", "전시회"),
            foodLikes = listOf("브런치", "케이크"))
    )

    // ── 찰칵 포토클럽 ─────────────────────────────────────────────
    val membersPhotoClub: List<MemberProfile> = listOf(
        MemberProfile("m-053", "조민서", 22, "커뮤니케이션디자인트랙", "ISFP", "필름카메라 감성 좋아해요", 165, "서울 성북구",
            interests = listOf("사진", "필름", "전시"),
            foodLikes = listOf("브런치", "디저트")),
        MemberProfile("m-054", "권도현", 24, "제품서비스디자인트랙", "INFP", "풍경 사진 전문입니다", 180, "서울 종로구",
            interests = listOf("사진", "여행", "편집"),
            foodLikes = listOf("커피", "빵")),
        MemberProfile("m-055", "정아현", 21, "미디어디자인트랙", "INTP", "라이트룸 보정 공부 중!", 161, "서울 성북구",
            interests = listOf("사진보정", "유튜브", "맛집"),
            foodLikes = listOf("파스타", "와인"))
    )

    // ── 등산왕 ────────────────────────────────────────────────────
    val membersHiking: List<MemberProfile> = listOf(
        MemberProfile("m-056", "이상혁", 25, "기계설계트랙", "ISTJ", "북한산 정기 등반합니다", 179, "서울 성북구",
            interests = listOf("등산", "캠핑", "요리"),
            foodLikes = listOf("김밥", "막걸리")),
        MemberProfile("m-057", "정은서", 22, "뷰티디자인매니지먼트트랙", "ISFJ", "자연 속에서 힐링해요", 163, "서울 강북구",
            interests = listOf("등산", "플로깅", "독서"),
            foodLikes = listOf("한식", "차")),
        MemberProfile("m-058", "박영진", 24, "행정트랙", "ISTJ", "도봉산, 수락산 자주 갑니다", 175, "서울 도봉구",
            interests = listOf("등산", "트레킹", "사진"),
            foodLikes = listOf("전", "파전")),
        MemberProfile("m-059", "최수현", 23, "기계자동화트랙", "ESFJ", "등산 후 막걸리가 최고!", 168, "서울 성북구",
            interests = listOf("등산", "환경", "봉사"),
            foodLikes = listOf("막걸리", "해물파전"))
    )

    // ── 당일치기 탐험대 ───────────────────────────────────────────
    val membersDayTrip: List<MemberProfile> = listOf(
        MemberProfile("m-060", "김시우", 23, "부동산트랙", "ENFP", "매주 새로운 곳 탐험!", 177, "서울 성북구",
            interests = listOf("여행", "사진", "맛집"),
            foodLikes = listOf("로컬음식", "길거리음식")),
        MemberProfile("m-061", "박지유", 22, "영어영문학트랙", "ESFP", "예쁜 곳 찾아다니는 게 좋아요", 164, "서울 마포구",
            interests = listOf("여행", "인스타", "카페"),
            foodLikes = listOf("브런치", "디저트")),
        MemberProfile("m-062", "오서준", 24, "사회트랙", "ENTP", "기획력 있는 여행 리더!", 181, "서울 동대문구",
            interests = listOf("여행", "기획", "블로그"),
            foodLikes = listOf("현지음식", "해산물"))
    )

    // ── 한성 사운드웨이브 ─────────────────────────────────────────
    val membersSoundWave: List<MemberProfile> = listOf(
        MemberProfile("m-063", "권혁진", 24, "예술학부", "ENFP", "기타 담당, 밴드 보컬 겸업", 178, "서울 성북구",
            interests = listOf("기타", "작곡", "공연"),
            foodLikes = listOf("맥주", "안주")),
        MemberProfile("m-064", "이다은", 22, "예술학부", "ISFP", "키보드 치면서 노래해요", 162, "서울 마포구",
            interests = listOf("피아노", "노래", "작사"),
            foodLikes = listOf("와인", "치즈")),
        MemberProfile("m-065", "장윤호", 23, "컴퓨터공학부", "ENTP", "드럼 독학 3년차입니다", 180, "서울 성북구",
            interests = listOf("드럼", "밴드", "코딩"),
            foodLikes = listOf("치킨", "맥주")),
        MemberProfile("m-066", "한서영", 21, "미디어디자인트랙", "ENFP", "베이스 초보, 열정은 만렙!", 165, "서울 종로구",
            interests = listOf("베이스", "음악감상", "댄스"),
            foodLikes = listOf("떡볶이", "타코"))
    )

    // ── 길거리 하모니 ─────────────────────────────────────────────
    val membersBusking: List<MemberProfile> = listOf(
        MemberProfile("m-067", "정하진", 23, "예술학부", "ENFP", "홍대 버스킹 매주 나갑니다", 175, "서울 마포구",
            interests = listOf("버스킹", "기타", "작곡"),
            foodLikes = listOf("맥주", "치킨")),
        MemberProfile("m-068", "민서아", 22, "예술학부", "ESFP", "보컬 담당! 같이 불러요", 164, "서울 성북구",
            interests = listOf("노래", "댄스", "패션"),
            foodLikes = listOf("디저트", "과일")),
        MemberProfile("m-069", "유채원", 21, "한국어문학트랙", "INFP", "어쿠스틱 감성 좋아해요", 160, "서울 성북구",
            interests = listOf("어쿠스틱", "시", "카페"),
            foodLikes = listOf("차", "베이글"))
    )

    // ── 포폴 피드백 랩 ────────────────────────────────────────────
    val membersPortfolioLab: List<MemberProfile> = listOf(
        MemberProfile("m-070", "이정민", 25, "컴퓨터공학부", "ENTJ", "백엔드 포트폴리오 피드백 환영", 179, "서울 성북구",
            interests = listOf("포트폴리오", "이력서", "개발"),
            foodLikes = listOf("커피", "샌드위치")),
        MemberProfile("m-071", "김하율", 24, "커뮤니케이션디자인트랙", "INTJ", "UI/UX 디자이너 지망생이에요", 166, "서울 마포구",
            interests = listOf("디자인", "피그마", "전시"),
            foodLikes = listOf("브런치", "스무디")),
        MemberProfile("m-072", "최준영", 24, "경영트랙", "ENTP", "기획·마케팅 포폴 같이 리뷰해요", 176, "서울 성북구",
            interests = listOf("기획", "마케팅", "프레젠테이션"),
            foodLikes = listOf("스테이크", "와인"))
    )

    /** teamId → 팀원 목록 맵 (prefetchCardData 없이 Preview 에서 바로 사용) */
    val membersByTeamId: Map<String, List<MemberProfile>> = mapOf(
        "team-001" to membersCodingMasters,
        "team-002" to membersToeic900,
        "team-003" to membersMajorCrunch,
        "team-004" to membersCertHunter,
        "team-005" to membersBookworm,
        "team-006" to membersMuscleFab,
        "team-007" to membersRunningCrew,
        "team-008" to membersBadminton,
        "team-009" to membersFutsal,
        "team-010" to membersFoodClub,
        "team-011" to membersCafeHolic,
        "team-012" to membersLateNight,
        "team-013" to membersBoardGame,
        "team-014" to membersMovieNight,
        "team-015" to membersPhotoClub,
        "team-016" to membersHiking,
        "team-017" to membersDayTrip,
        "team-018" to membersSoundWave,
        "team-019" to membersBusking,
        "team-020" to membersPortfolioLab
    )

    // ═════════════════════════════════════════════════════════════
    //  테스트 계정 4개
    //  ─ balanceAnswers: -1 = optionA 선택, +1 = optionB 선택
    //  ─ userTopTags: 10회 이상 스와이프 누적 상위 태그 (해금 상태)
    // ═════════════════════════════════════════════════════════════

    /**
     * 계정1 — 이준호 (컴공 / 성북구) → [코딩마스터즈] 와 최고 매칭
     *
     * 관심사·태그  3/3 겹침: 코딩✓ 알고리즘✓ 개발✓
     * 가치관       6/6 일치: 목표형·몰입형·자주·더치·차분·계획
     * 거리         평균 ≈ 88점 (성북구 ↔ 성북구~노원구)
     */
    val testAccount1 = TestAccount(
        profile = CurrentUserProfile(
            userId     = "test-user-001",
            age        = 22,
            department = "컴퓨터공학부",
            mbti       = "INTP",
            location   = "서울 성북구",
            interests  = listOf("코딩", "알고리즘", "게임", "독서"),
            foodLikes  = listOf("라멘", "파스타"),
            balanceAnswers = mapOf(
                "meeting_purpose" to  1,  // 목표형
                "intensity"       to  1,  // 몰입형
                "frequency"       to -1,  // 자주 만남
                "cost"            to -1,  // 더치페이
                "vibe"            to -1,  // 차분하게
                "planning"        to  1   // 계획형
            )
        ),
        userTopTags   = listOf("코딩", "알고리즘", "개발"),
        actionCount   = 12,
        targetTeamId  = "team-001",
        targetDistanceResults = listOf(
            MemberDistanceResult("m-001", "박준혁", "서울 성북구",  10,  1.2, 92),
            MemberDistanceResult("m-002", "김서연", "서울 강북구",  15,  3.8, 88),
            MemberDistanceResult("m-003", "이동현", "서울 노원구",  28,  8.5, 78),
            MemberDistanceResult("m-004", "정하은", "서울 성북구",   8,  0.9, 94)
        ),
        fitScore = 83
    )

    /**
     * 계정2 — 박미래 (스포츠 / 강북구) → [새벽러닝크루] 와 최고 매칭
     *
     * 관심사·태그  3/3 겹침: 러닝✓ 아침운동✓ 조깅✓
     * 가치관       6/6 일치: 친목·몰입형·자주·더치·활발·계획
     * 거리         평균 ≈ 83점 (강북구 ↔ 성북구~중구)
     */
    val testAccount2 = TestAccount(
        profile = CurrentUserProfile(
            userId     = "test-user-002",
            age        = 21,
            department = "스포츠미디어트랙",
            mbti       = "ENFP",
            location   = "서울 강북구",
            interests  = listOf("러닝", "마라톤", "요가", "사진"),
            foodLikes  = listOf("과일", "오트밀"),
            balanceAnswers = mapOf(
                "meeting_purpose" to -1,  // 친목형
                "intensity"       to  1,  // 몰입형
                "frequency"       to -1,  // 자주 만남
                "cost"            to -1,  // 더치페이
                "vibe"            to  1,  // 활발하게
                "planning"        to  1   // 계획형
            )
        ),
        userTopTags   = listOf("러닝", "아침운동", "조깅"),
        actionCount   = 15,
        targetTeamId  = "team-007",
        targetDistanceResults = listOf(
            MemberDistanceResult("m-023", "이수빈", "서울 성북구",  15,  3.8, 88),
            MemberDistanceResult("m-024", "정우성", "서울 노원구",  20,  6.2, 84),
            MemberDistanceResult("m-025", "양하윤", "서울 성북구",  14,  3.5, 89),
            MemberDistanceResult("m-026", "김재훈", "서울 중구",    35, 10.1, 72)
        ),
        fitScore = 79
    )

    /**
     * 계정3 — 김소율 (디자인 / 성북구) → [카페홀릭] 와 최고 매칭
     *
     * 관심사·태그  3/3 겹침: 카페✓ 디저트✓ 감성✓
     * 가치관       6/6 일치: 친목·가볍게·가끔·유연·차분·즉흥
     * 거리         평균 ≈ 84점 (성북구 ↔ 성북구~마포구)
     */
    val testAccount3 = TestAccount(
        profile = CurrentUserProfile(
            userId     = "test-user-003",
            age        = 22,
            department = "미디어디자인트랙",
            mbti       = "INFP",
            location   = "서울 성북구",
            interests  = listOf("카페", "독서", "사진", "글쓰기"),
            foodLikes  = listOf("라떼", "브런치"),
            balanceAnswers = mapOf(
                "meeting_purpose" to -1,  // 친목형
                "intensity"       to -1,  // 가볍게
                "frequency"       to  1,  // 가끔 만남
                "cost"            to  1,  // 유연한 비용
                "vibe"            to -1,  // 차분하게
                "planning"        to -1   // 즉흥형
            )
        ),
        userTopTags   = listOf("카페", "디저트", "감성"),
        actionCount   = 11,
        targetTeamId  = "team-011",
        targetDistanceResults = listOf(
            MemberDistanceResult("m-039", "전예은", "서울 성북구",   9,  1.1, 93),
            MemberDistanceResult("m-040", "구자현", "서울 마포구",  42, 13.5, 66),
            MemberDistanceResult("m-041", "한지민", "서울 성북구",   7,  0.8, 94)
        ),
        fitScore = 81
    )

    /**
     * 계정4 — 서재원 (예술 / 노원구) → [한성 사운드웨이브] 와 최고 매칭
     *
     * 관심사·태그  3/3 겹침: 기타✓ 밴드✓ 음악✓
     * 가치관       5/6 일치: 친목·몰입형·자주·더치·활발 (planning 불일치)
     * 거리         평균 ≈ 70점 (노원구 ↔ 성북구~마포구)
     */
    val testAccount4 = TestAccount(
        profile = CurrentUserProfile(
            userId     = "test-user-004",
            age        = 23,
            department = "예술학부",
            mbti       = "ENFP",
            location   = "서울 노원구",
            interests  = listOf("기타", "밴드", "작곡", "공연"),
            foodLikes  = listOf("맥주", "치킨"),
            balanceAnswers = mapOf(
                "meeting_purpose" to -1,  // 친목형
                "intensity"       to  1,  // 몰입형
                "frequency"       to -1,  // 자주 만남
                "cost"            to -1,  // 더치페이
                "vibe"            to  1,  // 활발하게
                "planning"        to -1   // 즉흥형 (팀은 계획형 → 불일치 1개)
            )
        ),
        userTopTags   = listOf("기타", "밴드", "음악"),
        actionCount   = 18,
        targetTeamId  = "team-018",
        targetDistanceResults = listOf(
            MemberDistanceResult("m-063", "권혁진", "서울 성북구",  28,  7.2, 78),
            MemberDistanceResult("m-064", "이다은", "서울 마포구",  55, 18.3, 56),
            MemberDistanceResult("m-065", "장윤호", "서울 성북구",  25,  6.8, 80),
            MemberDistanceResult("m-066", "한서영", "서울 종로구",  45, 14.1, 64)
        ),
        fitScore = 77
    )

    /** 계정 잠금 상태 테스트 — actionCount=3 이라 "자주 누른 태그" 카드가 잠겨있음 */
    val testAccount1Locked = testAccount1.copy(
        actionCount = 3
    )

    /** 모든 테스트 계정 리스트 */
    val allTestAccounts = listOf(testAccount1, testAccount2, testAccount3, testAccount4)
}
