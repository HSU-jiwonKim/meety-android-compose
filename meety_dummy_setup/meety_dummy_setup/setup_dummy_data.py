#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
╔══════════════════════════════════════════════════════════════════════╗
║   Meety Firebase 더미데이터 세팅 스크립트                               ║
║   meety-compose 프로젝트 / 피드 매칭 시연용                              ║
╠══════════════════════════════════════════════════════════════════════╣
║  실행 전 필수:                                                          ║
║    1) pip install firebase-admin                                       ║
║    2) Firebase 콘솔 → 프로젝트 설정 → 서비스 계정                        ║
║       → 새 비공개 키 생성 → 다운로드한 JSON을 이 스크립트와 같은 폴더에    ║
║       'serviceAccountKey.json' 으로 저장                               ║
║                                                                       ║
║  실행:  python setup_dummy_data.py                                    ║
╚══════════════════════════════════════════════════════════════════════╝
"""

import os, sys, time, traceback

try:
    import firebase_admin
    from firebase_admin import auth, credentials, firestore
except ImportError:
    print("❌  firebase-admin 패키지가 없습니다.")
    print("    pip install firebase-admin  실행 후 다시 시도하세요.\n")
    sys.exit(1)

KEY_FILE   = os.path.join(os.path.dirname(os.path.abspath(__file__)), "serviceAccountKey.json")
PROJECT_ID = "meety-compose"
NOW_MS     = int(time.time() * 1000)

# ═══════════════════════════════════════════════════════════════════
#  테스트 계정 (Auth + Firestore users + userPreferences)
# ═══════════════════════════════════════════════════════════════════
TEST_ACCOUNTS = [
    # ─────────────────────────────────────────────────────────────────
    #  계정1 — 이준호 (INTP, 컴공, 성북구) → 코딩마스터즈 (team-001) 최고 매칭
    #
    #  [수정] 타겟 team-001 은 liked/passed 어디에도 없어야 피드에 노출됨
    #  [수정] liked(5) + passed(5) = 10 → actionCount=10 → 태그 카드 해금
    #
    #  초기 정렬 점수 검증 (sortByPreference):
    #    team-001: 코딩(8)+알고리즘(6)+개발(5) + INTP(10)+INTJ(8)+ISTP(4) = 41점
    #    2위 팀 최대: ~4점  → team-001 압도적 1위
    # ─────────────────────────────────────────────────────────────────
    {
        "uid":"test-user-001","email":"test1@hansung.ac.kr","password":"meety1234!",
        "name":"이준호","age":22,"department":"컴퓨터공학부","mbti":"INTP",
        "location":"서울 성북구","height":176,
        "bio":"알고리즘 좋아하는 컴공 3학년이에요. 같이 코딩 스터디 해요!",
        "interests":["코딩","알고리즘","게임","독서"],"foodLikes":["라멘","파스타"],
        "balanceAnswers":{"meeting_purpose":1,"intensity":1,"frequency":-1,"cost":-1,"vibe":-1,"planning":1},
        # 과거 스터디 팀 5개 좋아요 → 코딩/알고리즘 태그 점수 축적
        "tagScores":{"코딩":8,"알고리즘":6,"개발":5,"목표":4,"스터디":3},
        "mbtiScores":{"INTP":10,"INTJ":8,"ISTP":4},
        # ★ team-001 은 여기 없음 → 피드에 노출됨
        "likedTeamIds":["team-002","team-003","team-004","team-005","team-020"],
        "passedTeamIds":["team-012","team-013","team-017","team-019","team-008"],
        # liked(5) + passed(5) = 10 → "자주 누른 태그" 카드 해금
    },
    # ─────────────────────────────────────────────────────────────────
    #  계정2 — 박미래 (ENFP, 스포츠, 강북구) → 새벽러닝크루 (team-007) 최고 매칭
    # ─────────────────────────────────────────────────────────────────
    {
        "uid":"test-user-002","email":"test2@hansung.ac.kr","password":"meety1234!",
        "name":"박미래","age":21,"department":"스포츠미디어트랙","mbti":"ENFP",
        "location":"서울 강북구","height":163,
        "bio":"마라톤 준비 중인 체육 전공생이에요. 같이 뛰어요!",
        "interests":["러닝","마라톤","요가","사진"],"foodLikes":["과일","오트밀"],
        "balanceAnswers":{"meeting_purpose":-1,"intensity":1,"frequency":-1,"cost":-1,"vibe":1,"planning":1},
        "tagScores":{"러닝":10,"아침운동":7,"조깅":6,"운동":5,"건강":4},
        "mbtiScores":{"ENFP":12,"ESFP":6,"ESTP":4},
        # ★ team-007 은 여기 없음
        "likedTeamIds":["team-008","team-006","team-009","team-016","team-010"],
        "passedTeamIds":["team-001","team-003","team-004","team-005","team-014"],
        # liked(5) + passed(5) = 10 → 해금
    },
    # ─────────────────────────────────────────────────────────────────
    #  계정3 — 김소율 (INFP, 디자인, 성북구) → 카페홀릭 (team-011) 최고 매칭
    # ─────────────────────────────────────────────────────────────────
    {
        "uid":"test-user-003","email":"test3@hansung.ac.kr","password":"meety1234!",
        "name":"김소율","age":22,"department":"미디어디자인트랙","mbti":"INFP",
        "location":"서울 성북구","height":161,
        "bio":"카페 투어랑 사진 찍는 거 좋아해요. 감성 모임 같이 해요!",
        "interests":["카페","독서","사진","글쓰기"],"foodLikes":["라떼","브런치"],
        "balanceAnswers":{"meeting_purpose":-1,"intensity":-1,"frequency":1,"cost":1,"vibe":-1,"planning":-1},
        "tagScores":{"카페":9,"디저트":7,"감성":6,"브런치":5,"차분한":4},
        "mbtiScores":{"INFP":10,"ISFP":8,"ENFP":3},
        # ★ team-011 은 여기 없음
        "likedTeamIds":["team-010","team-014","team-015","team-005","team-017"],
        "passedTeamIds":["team-001","team-006","team-009","team-002","team-004"],
        # liked(5) + passed(5) = 10 → 해금
    },
    # ─────────────────────────────────────────────────────────────────
    #  계정4 — 서재원 (ENFP, 예술, 노원구) → 한성 사운드웨이브 (team-018) 최고 매칭
    # ─────────────────────────────────────────────────────────────────
    {
        "uid":"test-user-004","email":"test4@hansung.ac.kr","password":"meety1234!",
        "name":"서재원","age":23,"department":"예술학부","mbti":"ENFP",
        "location":"서울 노원구","height":178,
        "bio":"기타 독학 2년차 밴드 지망생이에요. 같이 합주해요!",
        "interests":["기타","밴드","작곡","공연"],"foodLikes":["맥주","치킨"],
        "balanceAnswers":{"meeting_purpose":-1,"intensity":1,"frequency":-1,"cost":-1,"vibe":1,"planning":-1},
        "tagScores":{"기타":12,"밴드":10,"음악":8,"공연":6,"작곡":5},
        "mbtiScores":{"ENFP":14,"ISFP":6,"ENTP":4},
        # ★ team-018 은 여기 없음
        "likedTeamIds":["team-019","team-010","team-013","team-012","team-017"],
        "passedTeamIds":["team-002","team-005","team-003","team-004","team-001"],
        # liked(5) + passed(5) = 10 → 해금
    },
]


# ═══════════════════════════════════════════════════════════════════
#  팀 데이터 (20개)
# ═══════════════════════════════════════════════════════════════════
TEAMS = [
    {"teamId":"team-001","teamName":"코딩마스터즈","description":"알고리즘·백엔드 스터디",
     "tags":["코딩","알고리즘","백엔드","개발"],"mbtiTags":["INTP","INTJ","ISTP"],
     "memberIds":["m-001","m-002","m-003","m-004"],"leaderId":"m-001","status":"active",
     "profileImages":[],"teamProfileImage":"",
     "balanceProfile":{"meeting_purpose":1.0,"intensity":0.75,"frequency":-0.5,"cost":-1.0,"vibe":-0.5,"planning":1.0},
     "createdAt":NOW_MS-10*86400000},
    {"teamId":"team-002","teamName":"토익900 달성반","description":"목표 900+ 토익 스터디",
     "tags":["토익","영어","자격증","어학"],"mbtiTags":["ISTJ","ESTJ","ISFJ"],
     "memberIds":["m-005","m-006","m-007","m-008"],"leaderId":"m-005","status":"active",
     "profileImages":[],"teamProfileImage":"",
     "balanceProfile":{"meeting_purpose":1.0,"intensity":0.5,"frequency":-0.5,"cost":-1.0,"vibe":-1.0,"planning":1.0},
     "createdAt":NOW_MS-9*86400000},
    {"teamId":"team-003","teamName":"전공벼락치기","description":"기말고사 전공 스터디",
     "tags":["전공","시험","학점","공부"],"mbtiTags":["ISTJ","INTJ","ENFJ"],
     "memberIds":["m-009","m-010","m-011"],"leaderId":"m-009","status":"active",
     "profileImages":[],"teamProfileImage":"",
     "balanceProfile":{"meeting_purpose":1.0,"intensity":1.0,"frequency":-1.0,"cost":-1.0,"vibe":-0.33,"planning":1.0},
     "createdAt":NOW_MS-8*86400000},
    {"teamId":"team-004","teamName":"자격증 헌터","description":"정처기·ADsP 같이 준비",
     "tags":["자격증","정처기","ADsP","취업"],"mbtiTags":["ISTJ","ESTJ","ENTJ"],
     "memberIds":["m-012","m-013","m-014","m-015"],"leaderId":"m-012","status":"active",
     "profileImages":[],"teamProfileImage":"",
     "balanceProfile":{"meeting_purpose":1.0,"intensity":0.75,"frequency":-0.5,"cost":-0.75,"vibe":-0.25,"planning":1.0},
     "createdAt":NOW_MS-8*86400000},
    {"teamId":"team-005","teamName":"책벌레 토론방","description":"격주 독서 토론 모임",
     "tags":["독서","토론","인문학","교양"],"mbtiTags":["INFJ","INFP","ENFP"],
     "memberIds":["m-016","m-017","m-018"],"leaderId":"m-016","status":"active",
     "profileImages":[],"teamProfileImage":"",
     "balanceProfile":{"meeting_purpose":-0.33,"intensity":-0.67,"frequency":0.33,"cost":-0.33,"vibe":-1.0,"planning":0.33},
     "createdAt":NOW_MS-7*86400000},
    {"teamId":"team-006","teamName":"한성 머슬팩토리","description":"같이 운동하고 식단 공유",
     "tags":["헬스","운동","다이어트","벌크업"],"mbtiTags":["ESTP","ISTP","ENTJ"],
     "memberIds":["m-019","m-020","m-021","m-022"],"leaderId":"m-019","status":"active",
     "profileImages":[],"teamProfileImage":"",
     "balanceProfile":{"meeting_purpose":0.5,"intensity":1.0,"frequency":-0.75,"cost":-0.5,"vibe":0.75,"planning":0.25},
     "createdAt":NOW_MS-7*86400000},
    {"teamId":"team-007","teamName":"새벽러닝크루","description":"매주 토요일 새벽 러닝",
     "tags":["러닝","조깅","아침운동","건강"],"mbtiTags":["ENFP","ESFP","ESTP"],
     "memberIds":["m-023","m-024","m-025","m-026"],"leaderId":"m-023","status":"active",
     "profileImages":[],"teamProfileImage":"",
     "balanceProfile":{"meeting_purpose":-0.5,"intensity":0.75,"frequency":-1.0,"cost":-1.0,"vibe":1.0,"planning":0.5},
     "createdAt":NOW_MS-6*86400000},
    {"teamId":"team-008","teamName":"셔틀콕 동아리","description":"배드민턴 초보~중급",
     "tags":["배드민턴","라켓","운동","체육관"],"mbtiTags":["ESFP","ENFP","ISFP"],
     "memberIds":["m-027","m-028","m-029","m-030"],"leaderId":"m-027","status":"active",
     "profileImages":[],"teamProfileImage":"",
     "balanceProfile":{"meeting_purpose":-0.75,"intensity":-0.25,"frequency":-0.5,"cost":-0.75,"vibe":0.75,"planning":-0.25},
     "createdAt":NOW_MS-6*86400000},
    {"teamId":"team-009","teamName":"골넣자 풋살팀","description":"주말 풋살 경기",
     "tags":["풋살","축구","스포츠","팀플레이"],"mbtiTags":["ESTP","ENTP","ISTP"],
     "memberIds":["m-031","m-032","m-033","m-034","m-035"],"leaderId":"m-031","status":"active",
     "profileImages":[],"teamProfileImage":"",
     "balanceProfile":{"meeting_purpose":-0.2,"intensity":0.8,"frequency":-0.6,"cost":-0.6,"vibe":1.0,"planning":0.2},
     "createdAt":NOW_MS-5*86400000},
    {"teamId":"team-010","teamName":"성북 미식회","description":"성북구 숨은 맛집 탐방",
     "tags":["맛집","성북구","먹방","푸드"],"mbtiTags":["ESFP","ENFP","ESFJ"],
     "memberIds":["m-036","m-037","m-038"],"leaderId":"m-036","status":"active",
     "profileImages":[],"teamProfileImage":"",
     "balanceProfile":{"meeting_purpose":-1.0,"intensity":-0.67,"frequency":-0.33,"cost":0.33,"vibe":0.67,"planning":-0.33},
     "createdAt":NOW_MS-5*86400000},
    {"teamId":"team-011","teamName":"카페홀릭","description":"서울 감성카페 투어",
     "tags":["카페","디저트","감성","브런치"],"mbtiTags":["INFP","ISFP","ENFP"],
     "memberIds":["m-039","m-040","m-041"],"leaderId":"m-039","status":"active",
     "profileImages":[],"teamProfileImage":"",
     "balanceProfile":{"meeting_purpose":-1.0,"intensity":-1.0,"frequency":0.33,"cost":0.33,"vibe":-0.67,"planning":-0.33},
     "createdAt":NOW_MS-4*86400000},
    {"teamId":"team-012","teamName":"야식특공대","description":"금요일 밤 야식 번개",
     "tags":["야식","치킨","맥주","번개"],"mbtiTags":["ESTP","ESFP","ENTP"],
     "memberIds":["m-042","m-043","m-044","m-045"],"leaderId":"m-042","status":"active",
     "profileImages":[],"teamProfileImage":"",
     "balanceProfile":{"meeting_purpose":-1.0,"intensity":-0.5,"frequency":-0.5,"cost":0.5,"vibe":1.0,"planning":-1.0},
     "createdAt":NOW_MS-4*86400000},
    {"teamId":"team-013","teamName":"주사위 굴려","description":"매주 보드게임 모임",
     "tags":["보드게임","카탄","마피아","취미"],"mbtiTags":["ENTP","ENFP","INTP"],
     "memberIds":["m-046","m-047","m-048","m-049"],"leaderId":"m-046","status":"active",
     "profileImages":[],"teamProfileImage":"",
     "balanceProfile":{"meeting_purpose":-0.5,"intensity":-0.25,"frequency":-0.25,"cost":-0.5,"vibe":0.5,"planning":-0.5},
     "createdAt":NOW_MS-3*86400000},
    {"teamId":"team-014","teamName":"무비나잇","description":"매월 영화 감상 & 리뷰",
     "tags":["영화","감상","리뷰","OTT"],"mbtiTags":["INFP","INFJ","ISFP"],
     "memberIds":["m-050","m-051","m-052"],"leaderId":"m-050","status":"active",
     "profileImages":[],"teamProfileImage":"",
     "balanceProfile":{"meeting_purpose":-0.67,"intensity":-0.67,"frequency":0.33,"cost":0.0,"vibe":-0.67,"planning":0.33},
     "createdAt":NOW_MS-3*86400000},
    {"teamId":"team-015","teamName":"찰칵 포토클럽","description":"사진 촬영 & 보정 스터디",
     "tags":["사진","카메라","보정","출사"],"mbtiTags":["ISFP","INFP","INTP"],
     "memberIds":["m-053","m-054","m-055"],"leaderId":"m-053","status":"active",
     "profileImages":[],"teamProfileImage":"",
     "balanceProfile":{"meeting_purpose":-0.33,"intensity":-0.33,"frequency":-0.33,"cost":-0.33,"vibe":-0.33,"planning":0.67},
     "createdAt":NOW_MS-2*86400000},
    {"teamId":"team-016","teamName":"등산왕","description":"주말 근교 등산",
     "tags":["등산","산","자연","하이킹"],"mbtiTags":["ISFJ","ISTJ","ESFJ"],
     "memberIds":["m-056","m-057","m-058","m-059"],"leaderId":"m-056","status":"active",
     "profileImages":[],"teamProfileImage":"",
     "balanceProfile":{"meeting_purpose":-0.5,"intensity":0.25,"frequency":-0.5,"cost":-0.5,"vibe":-0.25,"planning":0.75},
     "createdAt":NOW_MS-2*86400000},
    {"teamId":"team-017","teamName":"당일치기 탐험대","description":"서울 근교 당일 여행",
     "tags":["여행","당일치기","근교","탐험"],"mbtiTags":["ENFP","ESFP","ENTP"],
     "memberIds":["m-060","m-061","m-062"],"leaderId":"m-060","status":"active",
     "profileImages":[],"teamProfileImage":"",
     "balanceProfile":{"meeting_purpose":-1.0,"intensity":0.33,"frequency":-0.33,"cost":0.33,"vibe":0.67,"planning":-0.67},
     "createdAt":NOW_MS-86400000},
    {"teamId":"team-018","teamName":"한성 사운드웨이브","description":"밴드 합주 & 공연 준비",
     "tags":["밴드","기타","드럼","음악"],"mbtiTags":["ENFP","ISFP","ENTP"],
     "memberIds":["m-063","m-064","m-065","m-066"],"leaderId":"m-063","status":"active",
     "profileImages":[],"teamProfileImage":"",
     "balanceProfile":{"meeting_purpose":-0.25,"intensity":1.0,"frequency":-0.5,"cost":-0.5,"vibe":0.75,"planning":0.5},
     "createdAt":NOW_MS-86400000},
    {"teamId":"team-019","teamName":"길거리 하모니","description":"홍대 버스킹 팀",
     "tags":["버스킹","노래","어쿠스틱","공연"],"mbtiTags":["ENFP","ESFP","INFP"],
     "memberIds":["m-067","m-068","m-069"],"leaderId":"m-067","status":"active",
     "profileImages":[],"teamProfileImage":"",
     "balanceProfile":{"meeting_purpose":-1.0,"intensity":0.33,"frequency":-0.67,"cost":0.0,"vibe":1.0,"planning":-0.67},
     "createdAt":NOW_MS-43200000},
    {"teamId":"team-020","teamName":"포폴 피드백 랩","description":"포트폴리오 상호 리뷰",
     "tags":["포트폴리오","취업","이력서","피드백"],"mbtiTags":["ENTJ","INTJ","ENTP"],
     "memberIds":["m-070","m-071","m-072"],"leaderId":"m-070","status":"active",
     "profileImages":[],"teamProfileImage":"",
     "balanceProfile":{"meeting_purpose":1.0,"intensity":0.33,"frequency":0.33,"cost":-0.67,"vibe":-0.33,"planning":0.67},
     "createdAt":NOW_MS-3600000},
]

# ═══════════════════════════════════════════════════════════════════
#  팀원 프로필 (72명)
# ═══════════════════════════════════════════════════════════════════
MEMBERS = [
    # 코딩마스터즈
    {"userId":"m-001","name":"박준혁","age":23,"department":"컴퓨터공학부","mbti":"INTP","bio":"백엔드 개발에 관심 많은 3학년입니다","height":178,"location":"서울 성북구","interests":["코딩","알고리즘","게임"],"foodLikes":["라멘","초밥"],"teamIds":["team-001"]},
    {"userId":"m-002","name":"김서연","age":22,"department":"컴퓨터공학부","mbti":"INTJ","bio":"프론트엔드 개발자 지망생이에요","height":164,"location":"서울 강북구","interests":["웹개발","UI디자인","독서"],"foodLikes":["파스타","샐러드"],"teamIds":["team-001"]},
    {"userId":"m-003","name":"이동현","age":24,"department":"컴퓨터공학부","mbti":"ISTP","bio":"시스템 프로그래밍 좋아합니다","height":175,"location":"서울 노원구","interests":["리눅스","서버","자전거"],"foodLikes":["삼겹살","비빔밥"],"teamIds":["team-001"]},
    {"userId":"m-004","name":"정하은","age":22,"department":"AI응용학과","mbti":"INTP","bio":"머신러닝 공부 중! 같이 해요","height":160,"location":"서울 성북구","interests":["AI","데이터분석","카페투어"],"foodLikes":["떡볶이","마카롱"],"teamIds":["team-001"]},
    # 토익900 달성반
    {"userId":"m-005","name":"최민수","age":23,"department":"영어영문학트랙","mbti":"ISTJ","bio":"현재 850점, 900 넘기고 싶어요","height":180,"location":"서울 성북구","interests":["영어","미드","해외여행"],"foodLikes":["스테이크","버거"],"teamIds":["team-002"]},
    {"userId":"m-006","name":"한소윤","age":22,"department":"경영트랙","mbti":"ESTJ","bio":"취업 준비 중, 토익 필수!","height":163,"location":"서울 동대문구","interests":["토익","자격증","요가"],"foodLikes":["샐러드","아사이볼"],"teamIds":["team-002"]},
    {"userId":"m-007","name":"유재민","age":24,"department":"무역학트랙","mbti":"ISTJ","bio":"무역 회사 취업 목표입니다","height":176,"location":"서울 중랑구","interests":["경제","뉴스","농구"],"foodLikes":["치킨","피자"],"teamIds":["team-002"]},
    {"userId":"m-008","name":"김예진","age":21,"department":"경영트랙","mbti":"ISFJ","bio":"꾸준히 공부하는 스타일이에요","height":158,"location":"서울 성북구","interests":["어학","일본어","드라마"],"foodLikes":["일식","카레"],"teamIds":["team-002"]},
    # 전공벼락치기
    {"userId":"m-009","name":"임성호","age":23,"department":"전자트랙","mbti":"ISTJ","bio":"회로이론 같이 공부해요","height":174,"location":"서울 성북구","interests":["전자공학","납땜","게임"],"foodLikes":["돈까스","우동"],"teamIds":["team-003"]},
    {"userId":"m-010","name":"오지수","age":22,"department":"전자트랙","mbti":"INTJ","bio":"전공 A+ 목표! 같이 달려요","height":166,"location":"서울 강북구","interests":["공부","피아노","넷플릭스"],"foodLikes":["떡볶이","타코"],"teamIds":["team-003"]},
    {"userId":"m-011","name":"강태윤","age":23,"department":"전자트랙","mbti":"ENFJ","bio":"네트워크 전공, 같이 시험 준비","height":181,"location":"서울 도봉구","interests":["네트워크","보안","축구"],"foodLikes":["삼겹살","냉면"],"teamIds":["team-003"]},
    # 자격증 헌터
    {"userId":"m-012","name":"신동우","age":24,"department":"컴퓨터공학부","mbti":"ISTJ","bio":"정처기 실기 준비 중","height":177,"location":"서울 성북구","interests":["자격증","코딩","헬스"],"foodLikes":["곱창","소주"],"teamIds":["team-004"]},
    {"userId":"m-013","name":"윤서아","age":23,"department":"빅데이터트랙","mbti":"ESTJ","bio":"ADsP 따고 빅데이터 분야 취업!","height":162,"location":"서울 종로구","interests":["데이터","통계","카페"],"foodLikes":["브런치","크로와상"],"teamIds":["team-004"]},
    {"userId":"m-014","name":"조현우","age":25,"department":"융합보안학과","mbti":"ENTJ","bio":"정보보안기사 도전 중입니다","height":179,"location":"서울 성북구","interests":["보안","해킹","등산"],"foodLikes":["불고기","된장찌개"],"teamIds":["team-004"]},
    {"userId":"m-015","name":"배수현","age":22,"department":"컴퓨터공학부","mbti":"ISTJ","bio":"리눅스마스터 같이 준비해요","height":165,"location":"서울 동대문구","interests":["리눅스","서버","요리"],"foodLikes":["파스타","리조또"],"teamIds":["team-004"]},
    # 책벌레 토론방
    {"userId":"m-016","name":"노유진","age":21,"department":"한국어문학트랙","mbti":"INFJ","bio":"인문학 책 좋아해요. 함께 읽고 토론해요","height":161,"location":"서울 성북구","interests":["독서","글쓰기","전시회"],"foodLikes":["차","베이글"],"teamIds":["team-005"]},
    {"userId":"m-017","name":"문채원","age":22,"department":"사회트랙","mbti":"INFP","bio":"사회학 관점에서 책 읽는 걸 좋아해요","height":167,"location":"서울 성북구","interests":["독서","철학","카페"],"foodLikes":["디저트","허브티"],"teamIds":["team-005"]},
    {"userId":"m-018","name":"오태민","age":23,"department":"철학트랙","mbti":"ENFP","bio":"철학적 토론 환영합니다!","height":175,"location":"서울 동대문구","interests":["철학","토론","음악"],"foodLikes":["커피","샌드위치"],"teamIds":["team-005"]},
    # 한성 머슬팩토리
    {"userId":"m-019","name":"장민재","age":23,"department":"스포츠미디어트랙","mbti":"ESTP","bio":"헬스 3년차, 벌크업 중이에요","height":182,"location":"서울 성북구","interests":["헬스","보충제","자전거"],"foodLikes":["닭가슴살","고구마"],"teamIds":["team-006"]},
    {"userId":"m-020","name":"이나현","age":22,"department":"뷰티디자인매니지먼트트랙","mbti":"ISTP","bio":"다이어트 목적으로 시작했어요","height":165,"location":"서울 강북구","interests":["필라테스","요가","운동"],"foodLikes":["샐러드","두부"],"teamIds":["team-006"]},
    {"userId":"m-021","name":"박경호","age":24,"department":"기계설계트랙","mbti":"ENTJ","bio":"파워리프팅 도전 중입니다!","height":179,"location":"서울 노원구","interests":["역도","크로스핏","수영"],"foodLikes":["프로틴쉐이크","스테이크"],"teamIds":["team-006"]},
    {"userId":"m-022","name":"송지아","age":21,"department":"스포츠미디어트랙","mbti":"ESFP","bio":"운동 입문자예요, 같이 배워요!","height":160,"location":"서울 성북구","interests":["운동","유튜브","댄스"],"foodLikes":["과일","요거트"],"teamIds":["team-006"]},
    # 새벽러닝크루
    {"userId":"m-023","name":"이수빈","age":22,"department":"스포츠미디어트랙","mbti":"ENFP","bio":"매주 5km 달리는 러닝 유발자!","height":168,"location":"서울 성북구","interests":["러닝","마라톤","요가"],"foodLikes":["과일","스무디"],"teamIds":["team-007"]},
    {"userId":"m-024","name":"정우성","age":23,"department":"체육트랙","mbti":"ESTP","bio":"풀코스 마라톤 목표!","height":177,"location":"서울 노원구","interests":["마라톤","자전거","트레일"],"foodLikes":["바나나","에너지바"],"teamIds":["team-007"]},
    {"userId":"m-025","name":"양하윤","age":21,"department":"스포츠미디어트랙","mbti":"ESFP","bio":"아침 러닝으로 하루 시작해요","height":163,"location":"서울 성북구","interests":["러닝","사진","음악"],"foodLikes":["오트밀","그래놀라"],"teamIds":["team-007"]},
    {"userId":"m-026","name":"김재훈","age":24,"department":"체육트랙","mbti":"ENTP","bio":"달리면서 음악 듣는 게 행복이에요","height":175,"location":"서울 중구","interests":["러닝","음악감상","여행"],"foodLikes":["단백질바","커피"],"teamIds":["team-007"]},
    # 셔틀콕 동아리
    {"userId":"m-027","name":"백승호","age":23,"department":"체육트랙","mbti":"ESFP","bio":"배드민턴 동아리 창설 멤버!","height":176,"location":"서울 성북구","interests":["배드민턴","탁구","농구"],"foodLikes":["삼겹살","맥주"],"teamIds":["team-008"]},
    {"userId":"m-028","name":"강유나","age":22,"department":"뷰티디자인매니지먼트트랙","mbti":"ENFP","bio":"처음 배우는 분도 환영해요!","height":164,"location":"서울 성북구","interests":["배드민턴","댄스","여행"],"foodLikes":["과일","요거트"],"teamIds":["team-008"]},
    {"userId":"m-029","name":"임수진","age":21,"department":"스포츠미디어트랙","mbti":"ISFP","bio":"라켓 스포츠 좋아해요","height":160,"location":"서울 강북구","interests":["배드민턴","테니스","수영"],"foodLikes":["샐러드","파니니"],"teamIds":["team-008"]},
    {"userId":"m-030","name":"안재원","age":24,"department":"기계설계트랙","mbti":"ESTP","bio":"배드민턴으로 체력 관리 중!","height":181,"location":"서울 도봉구","interests":["배드민턴","헬스","축구"],"foodLikes":["치킨","맥주"],"teamIds":["team-008"]},
    # 골넣자 풋살팀
    {"userId":"m-031","name":"손준혁","age":23,"department":"체육트랙","mbti":"ESTP","bio":"주말 풋살 빠질 수 없죠!","height":178,"location":"서울 성북구","interests":["축구","풋살","스포츠뉴스"],"foodLikes":["치킨","피자"],"teamIds":["team-009"]},
    {"userId":"m-032","name":"이현준","age":22,"department":"스포츠미디어트랙","mbti":"ENTP","bio":"포지션은 미드필더입니다","height":175,"location":"서울 성북구","interests":["풋살","게임","영화"],"foodLikes":["햄버거","콜라"],"teamIds":["team-009"]},
    {"userId":"m-033","name":"박도윤","age":24,"department":"체육트랙","mbti":"ISTP","bio":"수비 잘 보는 수비수예요","height":180,"location":"서울 노원구","interests":["축구","헬스","자전거"],"foodLikes":["삼겹살","냉면"],"teamIds":["team-009"]},
    {"userId":"m-034","name":"류민석","age":23,"department":"체육트랙","mbti":"ESTP","bio":"공격수, 골 넣는 게 제일 좋아요","height":177,"location":"서울 성북구","interests":["축구","마라톤","음악"],"foodLikes":["국밥","소주"],"teamIds":["team-009"]},
    {"userId":"m-035","name":"장서연","age":21,"department":"스포츠미디어트랙","mbti":"ESFP","bio":"첫 여성 멤버로 합류했어요!","height":159,"location":"서울 성북구","interests":["풋살","달리기","요가"],"foodLikes":["떡볶이","순대"],"teamIds":["team-009"]},
    # 성북 미식회
    {"userId":"m-036","name":"홍서윤","age":22,"department":"식품영양학트랙","mbti":"ESFP","bio":"먹는 게 제일 행복해요","height":163,"location":"서울 성북구","interests":["맛집탐방","요리","인스타"],"foodLikes":["한식","디저트"],"teamIds":["team-010"]},
    {"userId":"m-037","name":"김태양","age":23,"department":"식품영양학트랙","mbti":"ENFP","bio":"성북구 맛집은 다 알고 있어요","height":175,"location":"서울 성북구","interests":["맛집","요리","유튜브"],"foodLikes":["이탈리안","멕시칸"],"teamIds":["team-010"]},
    {"userId":"m-038","name":"윤채은","age":21,"department":"식품영양학트랙","mbti":"ESFJ","bio":"리뷰 블로그 운영 중이에요","height":160,"location":"서울 성북구","interests":["맛집리뷰","사진","여행"],"foodLikes":["일식","카페"],"teamIds":["team-010"]},
    # 카페홀릭
    {"userId":"m-039","name":"전예은","age":21,"department":"커뮤니케이션디자인트랙","mbti":"INFP","bio":"감성 카페 덕후입니다","height":162,"location":"서울 성북구","interests":["카페","독서","사진"],"foodLikes":["라떼","마카롱"],"teamIds":["team-011"]},
    {"userId":"m-040","name":"구자현","age":23,"department":"제품서비스디자인트랙","mbti":"ISFP","bio":"필름 카메라로 카페 찍는 게 취미","height":178,"location":"서울 마포구","interests":["카페","사진","여행"],"foodLikes":["아메리카노","브런치"],"teamIds":["team-011"]},
    {"userId":"m-041","name":"한지민","age":22,"department":"시각디자인트랙","mbti":"ENFP","bio":"새로운 카페 찾는 게 일상이에요","height":165,"location":"서울 성북구","interests":["카페투어","그림","전시"],"foodLikes":["플랫화이트","케이크"],"teamIds":["team-011"]},
    # 야식특공대
    {"userId":"m-042","name":"최진우","age":23,"department":"식품영양학트랙","mbti":"ESTP","bio":"야식은 금요일 밤의 로망이죠","height":180,"location":"서울 성북구","interests":["야식","먹방","게임"],"foodLikes":["치킨","맥주"],"teamIds":["team-012"]},
    {"userId":"m-043","name":"박지수","age":22,"department":"식품영양학트랙","mbti":"ESFP","bio":"맛있는 거 먹으면 행복해요!","height":161,"location":"서울 성북구","interests":["먹방","요리","여행"],"foodLikes":["족발","보쌈"],"teamIds":["team-012"]},
    {"userId":"m-044","name":"이원석","age":24,"department":"경영트랙","mbti":"ENTP","bio":"야식 메뉴 선정 담당자!","height":177,"location":"서울 강북구","interests":["음식","게임","영화"],"foodLikes":["피자","파스타"],"teamIds":["team-012"]},
    {"userId":"m-045","name":"김민아","age":21,"department":"경영트랙","mbti":"ESFP","bio":"번개 모임 전문가입니다","height":158,"location":"서울 성북구","interests":["모임","SNS","음악"],"foodLikes":["떡볶이","라볶이"],"teamIds":["team-012"]},
    # 주사위 굴려
    {"userId":"m-046","name":"허진우","age":23,"department":"빅데이터트랙","mbti":"ENTP","bio":"카탄 좋아하는 전략 게이머","height":175,"location":"서울 성북구","interests":["보드게임","전략게임","수학"],"foodLikes":["피자","콜라"],"teamIds":["team-013"]},
    {"userId":"m-047","name":"유하은","age":22,"department":"상담심리트랙","mbti":"ENFP","bio":"마피아 게임 고수에요!","height":164,"location":"서울 강북구","interests":["보드게임","심리학","영화"],"foodLikes":["과자","떡볶이"],"teamIds":["team-013"]},
    {"userId":"m-048","name":"남지호","age":24,"department":"경제트랙","mbti":"INTP","bio":"복잡한 게임일수록 재밌어요","height":179,"location":"서울 동대문구","interests":["보드게임","퍼즐","투자"],"foodLikes":["초밥","라멘"],"teamIds":["team-013"]},
    {"userId":"m-049","name":"손예림","age":21,"department":"커뮤니케이션디자인트랙","mbti":"ENFP","bio":"귀여운 게임 좋아해요~","height":158,"location":"서울 성북구","interests":["보드게임","일러스트","카페"],"foodLikes":["마카롱","밀크티"],"teamIds":["team-013"]},
    # 무비나잇
    {"userId":"m-050","name":"윤서준","age":23,"department":"문학문화콘텐츠학과","mbti":"INFP","bio":"매달 영화 2편 감상 & 리뷰","height":176,"location":"서울 성북구","interests":["영화","감독론","글쓰기"],"foodLikes":["팝콘","나초"],"teamIds":["team-014"]},
    {"userId":"m-051","name":"김도연","age":22,"department":"미디어디자인트랙","mbti":"INFJ","bio":"OTT 정주행이 일상이에요","height":163,"location":"서울 마포구","interests":["넷플릭스","드라마","카페"],"foodLikes":["와인","치즈"],"teamIds":["team-014"]},
    {"userId":"m-052","name":"이채린","age":21,"department":"한국어문학트랙","mbti":"ISFP","bio":"예술영화 좋아하는 문과생","height":160,"location":"서울 성북구","interests":["영화","소설","전시회"],"foodLikes":["브런치","케이크"],"teamIds":["team-014"]},
    # 찰칵 포토클럽
    {"userId":"m-053","name":"조민서","age":22,"department":"커뮤니케이션디자인트랙","mbti":"ISFP","bio":"필름카메라 감성 좋아해요","height":165,"location":"서울 성북구","interests":["사진","필름","전시"],"foodLikes":["브런치","디저트"],"teamIds":["team-015"]},
    {"userId":"m-054","name":"권도현","age":24,"department":"제품서비스디자인트랙","mbti":"INFP","bio":"풍경 사진 전문입니다","height":180,"location":"서울 종로구","interests":["사진","여행","편집"],"foodLikes":["커피","빵"],"teamIds":["team-015"]},
    {"userId":"m-055","name":"정아현","age":21,"department":"미디어디자인트랙","mbti":"INTP","bio":"라이트룸 보정 공부 중!","height":161,"location":"서울 성북구","interests":["사진보정","유튜브","맛집"],"foodLikes":["파스타","와인"],"teamIds":["team-015"]},
    # 등산왕
    {"userId":"m-056","name":"이상혁","age":25,"department":"기계설계트랙","mbti":"ISTJ","bio":"북한산 정기 등반합니다","height":179,"location":"서울 성북구","interests":["등산","캠핑","요리"],"foodLikes":["김밥","막걸리"],"teamIds":["team-016"]},
    {"userId":"m-057","name":"정은서","age":22,"department":"뷰티디자인매니지먼트트랙","mbti":"ISFJ","bio":"자연 속에서 힐링해요","height":163,"location":"서울 강북구","interests":["등산","플로깅","독서"],"foodLikes":["한식","차"],"teamIds":["team-016"]},
    {"userId":"m-058","name":"박영진","age":24,"department":"행정트랙","mbti":"ISTJ","bio":"도봉산 수락산 자주 갑니다","height":175,"location":"서울 도봉구","interests":["등산","트레킹","사진"],"foodLikes":["전","파전"],"teamIds":["team-016"]},
    {"userId":"m-059","name":"최수현","age":23,"department":"기계자동화트랙","mbti":"ESFJ","bio":"등산 후 막걸리가 최고!","height":168,"location":"서울 성북구","interests":["등산","환경","봉사"],"foodLikes":["막걸리","해물파전"],"teamIds":["team-016"]},
    # 당일치기 탐험대
    {"userId":"m-060","name":"김시우","age":23,"department":"부동산트랙","mbti":"ENFP","bio":"매주 새로운 곳 탐험!","height":177,"location":"서울 성북구","interests":["여행","사진","맛집"],"foodLikes":["로컬음식","길거리음식"],"teamIds":["team-017"]},
    {"userId":"m-061","name":"박지유","age":22,"department":"영어영문학트랙","mbti":"ESFP","bio":"예쁜 곳 찾아다니는 게 좋아요","height":164,"location":"서울 마포구","interests":["여행","인스타","카페"],"foodLikes":["브런치","디저트"],"teamIds":["team-017"]},
    {"userId":"m-062","name":"오서준","age":24,"department":"사회트랙","mbti":"ENTP","bio":"기획력 있는 여행 리더!","height":181,"location":"서울 동대문구","interests":["여행","기획","블로그"],"foodLikes":["현지음식","해산물"],"teamIds":["team-017"]},
    # 한성 사운드웨이브
    {"userId":"m-063","name":"권혁진","age":24,"department":"예술학부","mbti":"ENFP","bio":"기타 담당, 밴드 보컬 겸업","height":178,"location":"서울 성북구","interests":["기타","작곡","공연"],"foodLikes":["맥주","안주"],"teamIds":["team-018"]},
    {"userId":"m-064","name":"이다은","age":22,"department":"예술학부","mbti":"ISFP","bio":"키보드 치면서 노래해요","height":162,"location":"서울 마포구","interests":["피아노","노래","작사"],"foodLikes":["와인","치즈"],"teamIds":["team-018"]},
    {"userId":"m-065","name":"장윤호","age":23,"department":"컴퓨터공학부","mbti":"ENTP","bio":"드럼 독학 3년차입니다","height":180,"location":"서울 성북구","interests":["드럼","밴드","코딩"],"foodLikes":["치킨","맥주"],"teamIds":["team-018"]},
    {"userId":"m-066","name":"한서영","age":21,"department":"미디어디자인트랙","mbti":"ENFP","bio":"베이스 초보, 열정은 만렙!","height":165,"location":"서울 종로구","interests":["베이스","음악감상","댄스"],"foodLikes":["떡볶이","타코"],"teamIds":["team-018"]},
    # 길거리 하모니
    {"userId":"m-067","name":"정하진","age":23,"department":"예술학부","mbti":"ENFP","bio":"홍대 버스킹 매주 나갑니다","height":175,"location":"서울 마포구","interests":["버스킹","기타","작곡"],"foodLikes":["맥주","치킨"],"teamIds":["team-019"]},
    {"userId":"m-068","name":"민서아","age":22,"department":"예술학부","mbti":"ESFP","bio":"보컬 담당! 같이 불러요","height":164,"location":"서울 성북구","interests":["노래","댄스","패션"],"foodLikes":["디저트","과일"],"teamIds":["team-019"]},
    {"userId":"m-069","name":"유채원","age":21,"department":"한국어문학트랙","mbti":"INFP","bio":"어쿠스틱 감성 좋아해요","height":160,"location":"서울 성북구","interests":["어쿠스틱","시","카페"],"foodLikes":["차","베이글"],"teamIds":["team-019"]},
    # 포폴 피드백 랩
    {"userId":"m-070","name":"이정민","age":25,"department":"컴퓨터공학부","mbti":"ENTJ","bio":"백엔드 포트폴리오 피드백 환영","height":179,"location":"서울 성북구","interests":["포트폴리오","이력서","개발"],"foodLikes":["커피","샌드위치"],"teamIds":["team-020"]},
    {"userId":"m-071","name":"김하율","age":24,"department":"커뮤니케이션디자인트랙","mbti":"INTJ","bio":"UI/UX 디자이너 지망생이에요","height":166,"location":"서울 마포구","interests":["디자인","피그마","전시"],"foodLikes":["브런치","스무디"],"teamIds":["team-020"]},
    {"userId":"m-072","name":"최준영","age":24,"department":"경영트랙","mbti":"ENTP","bio":"기획마케팅 포폴 같이 리뷰해요","height":176,"location":"서울 성북구","interests":["기획","마케팅","프레젠테이션"],"foodLikes":["스테이크","와인"],"teamIds":["team-020"]},
]


# ═══════════════════════════════════════════════════════════════════
#  헬퍼
# ═══════════════════════════════════════════════════════════════════
def p(msg):  print(f"\n{'─'*56}\n  {msg}\n{'─'*56}")
def ok(msg): print(f"  [OK]   {msg}")
def sk(msg): print(f"  [SKIP] {msg}")
def er(msg): print(f"  [ERR]  {msg}")

def init_firebase():
    if not os.path.exists(KEY_FILE):
        print(f"\n[ERR] '{KEY_FILE}' 없음")
        print("  Firebase 콘솔 → 프로젝트 설정 → 서비스 계정 → 새 비공개 키 생성")
        print("  다운로드한 파일을 'serviceAccountKey.json' 으로 이름 바꿔서 같은 폴더에 저장\n")
        sys.exit(1)
    cred = credentials.Certificate(KEY_FILE)
    if not firebase_admin._apps:
        firebase_admin.initialize_app(cred)
    db = firestore.client()
    ok(f"Firebase 연결 완료 (project: {PROJECT_ID})")
    return db

def setup_auth():
    p("1단계: Firebase Auth — 테스트 계정 4개")
    for acc in TEST_ACCOUNTS:
        uid, email, pw, name = acc["uid"], acc["email"], acc["password"], acc["name"]
        try:
            auth.get_user(uid)
            auth.update_user(uid, email=email, password=pw, display_name=name, disabled=False)
            sk(f"[{uid}] {name} ({email}) 업데이트")
        except auth.UserNotFoundError:
            auth.create_user(uid=uid, email=email, password=pw, display_name=name)
            ok(f"[{uid}] {name} ({email}) 생성")
        except Exception as e:
            er(f"[{uid}] {e}")

def setup_members(db):
    p("2단계: Firestore users — 더미 팀원 72명")
    col = db.collection("users")
    for m in MEMBERS:
        col.document(m["userId"]).set({
            "userId":m["userId"],"name":m["name"],"age":m["age"],
            "department":m["department"],"mbti":m["mbti"],"bio":m["bio"],
            "height":m["height"],"location":m["location"],
            "interests":m["interests"],"foodLikes":m["foodLikes"],
            "foodDislikes":[],"profileImages":[],"profileImageUrl":"",
            "teamIds":m.get("teamIds",[]),"isVerified":True,"isDummy":True,
            "createdAt":NOW_MS,"updatedAt":NOW_MS,
        }, merge=True)
    ok(f"팀원 {len(MEMBERS)}명 업서트 완료")

def setup_test_profiles(db):
    p("3단계: Firestore users — 테스트 계정 4개")
    col = db.collection("users")
    for acc in TEST_ACCOUNTS:
        col.document(acc["uid"]).set({
            "userId":acc["uid"],"name":acc["name"],"age":acc["age"],
            "department":acc["department"],"mbti":acc["mbti"],"bio":acc["bio"],
            "height":acc["height"],"location":acc["location"],
            "interests":acc["interests"],"foodLikes":acc["foodLikes"],
            "foodDislikes":[],"profileImages":[],"profileImageUrl":"",
            "email":acc["email"],"teamIds":[],
            "isVerified":True,"isDummy":True,
            "balanceAnswers":acc["balanceAnswers"],
            "balanceProfile":{"answers":acc["balanceAnswers"],"completedAt":NOW_MS},
            "createdAt":NOW_MS,"updatedAt":NOW_MS,
        }, merge=True)
        ok(f"[{acc['uid']}] {acc['name']} 업서트")

def setup_teams(db):
    p("4단계: Firestore teams — 20개 팀")
    col = db.collection("teams")
    for t in TEAMS:
        col.document(t["teamId"]).set({**t,"updatedAt":NOW_MS,"isDummy":True}, merge=True)
    ok(f"팀 {len(TEAMS)}개 업서트 완료")

def setup_preferences(db):
    p("5단계: Firestore userPreferences — 테스트 계정 선호도")
    col = db.collection("userPreferences")
    for acc in TEST_ACCOUNTS:
        col.document(acc["uid"]).set({
            "userId":acc["uid"],
            "tagScores":acc["tagScores"],
            "mbtiScores":acc["mbtiScores"],
            "likedTeamIds":acc["likedTeamIds"],
            "passedTeamIds":acc["passedTeamIds"],
            "actionCount": len(acc.get("likedTeamIds", [])) + len(acc.get("passedTeamIds", [])),
            "updatedAt":NOW_MS,
        }, merge=True)
        action_count = len(acc.get("likedTeamIds", [])) + len(acc.get("passedTeamIds", []))
        ok(f"[{acc['uid']}] {acc['name']} — 태그 {len(acc['tagScores'])}개, actionCount={action_count}")

def main():
    print("\n" + "="*56)
    print("  Meety Firebase 더미데이터 세팅")
    print("  project: " + PROJECT_ID)
    print("="*56)
    try:
        db = init_firebase()
        setup_auth()
        setup_members(db)
        setup_test_profiles(db)
        setup_teams(db)
        setup_preferences(db)
    except Exception:
        er("예기치 못한 오류:")
        traceback.print_exc()
        sys.exit(1)

    print("\n" + "="*56)
    print("  완료! 아래 계정으로 앱 로그인 후 피드를 확인하세요.")
    print("="*56)
    print("""
  계정1  이준호   test1@hansung.ac.kr  / meety1234!
         -> 코딩마스터즈 (team-001) 최고 매칭
         가치관 6/6 일치, 관심사 겹침, 같은 성북구

  계정2  박미래   test2@hansung.ac.kr  / meety1234!
         -> 새벽러닝크루 (team-007) 최고 매칭
         러닝 태그 10점 누적, 강북<->성북 근거리

  계정3  김소율   test3@hansung.ac.kr  / meety1234!
         -> 카페홀릭 (team-011) 최고 매칭
         가치관 6/6 완전 일치, 모두 성북구

  계정4  서재원   test4@hansung.ac.kr  / meety1234!
         -> 한성 사운드웨이브 (team-018) 최고 매칭
         기타/밴드/음악 관심사 완전 겹침, 몰입형 일치

  [주의] "자주 누른 태그" 카드: actionCount>=10 -> 모두 해금 상태
""")

if __name__ == "__main__":
    main()
