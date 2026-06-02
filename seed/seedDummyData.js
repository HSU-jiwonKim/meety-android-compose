/**
 * ════════════════════════════════════════════════════════════════════
 *  Meety 더미 데이터 시드 스크립트 (Firebase Admin SDK)
 * --------------------------------------------------------------------
 *  올리는 컬렉션
 *    teams           : 피드에 표시할 더미 팀 20개
 *    users           : 팀원 프로필 72명 (m-001 ~ m-072) + 테스트 계정 3명
 *    userPreferences : 테스트 계정의 누적 선호도 (태그 매칭 즉시 해금용)
 *
 *  실행
 *    1) cd seed && npm install
 *    2) 서비스 계정 키를 seed/serviceAccountKey.json 로 저장
 *       (Firebase 콘솔 → 프로젝트 설정 → 서비스 계정 → 새 비공개 키 생성)
 *    3) 더미데이터.zip 을 seed/ 폴더에 압축 해제 → seed/더미데이터/ 폴더 생성됨
 *       (unzip 더미데이터.zip -d 더미데이터)
 *    4) 아래 STORAGE_BUCKET 에 Firebase Storage 버킷 이름 입력
 *       (Firebase 콘솔 → Storage → gs://... 형식의 버킷 이름, gs:// 제외)
 *    5) 아래 TEST_ACCOUNT_UIDS 에 콘솔에서 만든 Auth 계정 3개의 UID 입력
 *    6) node seedDummyData.js
 *
 *  ⚠ 실행 시 기존 teams / m-xxx users 를 모두 삭제하고 다시 생성합니다.
 * ════════════════════════════════════════════════════════════════════
 */

const admin = require("firebase-admin");
const path = require("path");
const fs = require("fs");
const serviceAccount = require("./serviceAccountKey.json");

// ════════════════════════════════════════════════════════════════════
//  ★ 여기만 채우세요
// ════════════════════════════════════════════════════════════════════
const STORAGE_BUCKET = "meety-compose.firebasestorage.app"; // e.g. "myapp-12345.appspot.com"

const TEST_ACCOUNT_UIDS = {
  account1: "8UZDoHmR7bQ8XukSz0BMwg22zSA3",   // 코딩마스터즈와 매칭
  account2: "7dGF11pEFfYuqViSjfiJDHaaM7H2",   // 새벽러닝크루와 매칭
  account3: "7dGF11pEFfYuqViSjfiJDHaaM7H2",   // 카페홀릭과 매칭
};
// ════════════════════════════════════════════════════════════════════

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  storageBucket: STORAGE_BUCKET,
});
const db = admin.firestore();
const bucket = admin.storage().bucket();

const now = Date.now();

// 이미지 루트 경로 (더미데이터.zip 을 압축 해제한 폴더)
const IMAGE_ROOT = path.join(__dirname, "더미데이터", "팀 대표 프로필 사진");

// ════════════════════════════════════════════════════════════════════
//  팀 이미지 경로 매핑 (팀 대표사진 + 멤버 폴더 경로)
// ════════════════════════════════════════════════════════════════════
const teamImageMap = {
  "team-001": { dir: "스터디학습(5개)/코딩 스터디(코딩 마스터즈)",     img: "코딩스터디.png" },
  "team-002": { dir: "스터디학습(5개)/토익 스터디(토익900 달성반)",    img: "토익 스터디.png" },
  "team-003": { dir: "스터디학습(5개)/전공 시험 스터디(전공 벼락치기)", img: "전공 시험 스터디.png" },
  "team-004": { dir: "스터디학습(5개)/자격증 스터디(자격증 헌터)",     img: "자격증 스터디.png" },
  "team-005": { dir: "스터디학습(5개)/독서 토론(책벌레 토론방)",       img: "독서토론.png" },
  "team-006": { dir: "운동헬스(4개)/헬스 크루(한성 머슬팩토리)",       img: "헬스 크루.png" },
  "team-007": { dir: "운동헬스(4개)/러닝 크루(새벽 러닝 크루)",        img: "러닝 크루.png" },
  "team-008": { dir: "운동헬스(4개)/배드민턴 동아리(셔틀콕 동아리)",   img: "배드민턴 동아리.png" },
  "team-009": { dir: "운동헬스(4개)/풋살팀(골넣자 풋살팀)",            img: "풋살팀.png" },
  "team-010": { dir: "맛집카페 탐방(3개)/성북구 맛집 탐방(성북 미식회)", img: "성북구 맛집 탐방.png" },
  "team-011": { dir: "맛집카페 탐방(3개)/카페 투어(카페홀릭)",         img: "카페투어.png" },
  "team-012": { dir: "맛집카페 탐방(3개)/야식 번개(야식특공대)",        img: "야식번개.png" },
  "team-013": { dir: "취미문화(3개)/보드게임 모임(주사위 굴려)",        img: "보드게임 모임.png" },
  "team-014": { dir: "취미문화(3개)/영화 감상(무비나잇)",               img: "영화 감상.png" },
  "team-015": { dir: "취미문화(3개)/사진 동아리(찰칵 포토클럽)",       img: "사진동아리.png" },
  "team-016": { dir: "여행아웃도어(2개)/주말 등산(등산왕)",             img: "주말등산.png" },
  "team-017": { dir: "여행아웃도어(2개)/당일치기 여행(당일치기 탐험대)", img: "당일치기 여행.png" },
  "team-018": { dir: "음악공연(2개)/밴드 동아리(한성 사운드웨이브)",    img: "밴드동아리.png" },
  "team-019": { dir: "음악공연(2개)/버스킹 팀(길거리 하모니)",          img: "버스킹 팀.png" },
  "team-020": { dir: "자기계발커리어(1개)/포트폴리오 피드백 모임(포폴 피드백 랩)", img: "포트폴리오 피드백 모임.png" },
};

// 멤버 이름 → userId 매핑 (이름으로 파일 매칭)
const memberNameToId = {
  "박준혁": "m-001", "김서연": "m-002", "이동현": "m-003", "정하은": "m-004",
  "최민수": "m-005", "한소윤": "m-006", "유재민": "m-007", "김예진": "m-008",
  "임성호": "m-009", "오지수": "m-010", "강태윤": "m-011",
  "신동우": "m-012", "윤서아": "m-013", "조현우": "m-014", "배수현": "m-015",
  "문채원": "m-016", "황지훈": "m-017", "노유진": "m-018",
  "장민기": "m-019", "서예린": "m-020", "한승준": "m-021", "김태영": "m-022",
  "이수빈": "m-023", "정우성": "m-024", "양하윤": "m-025", "김재훈": "m-026",
  "박서진": "m-027", "이준서": "m-028", "김나연": "m-029", "최원빈": "m-030",
  "송민혁": "m-031", "이하준": "m-032", "김도윤": "m-033", "정수아": "m-034", "오재현": "m-035",
  "강유나": "m-036", "백승호": "m-037", "임수진": "m-038",
  "전예은": "m-039", "구자현": "m-040", "한지민": "m-041",
  "안재원": "m-042", "홍서윤": "m-043", "류민석": "m-044", "장서연": "m-045",
  "허진우": "m-046", "유하은": "m-047", "남지호": "m-048", "손예림": "m-049",
  "윤서준": "m-050", "김도연": "m-051", "이채린": "m-052",
  "조민서": "m-053", "권도현": "m-054", "정아현": "m-055",
  "이상혁": "m-056", "정은서": "m-057", "박영진": "m-058", "최수현": "m-059",
  "김시우": "m-060", "박지유": "m-061", "오서준": "m-062",
  "권혁진": "m-063", "이다은": "m-064", "장윤호": "m-065", "한서영": "m-066",
  "정하진": "m-067", "민서아": "m-068", "유채원": "m-069",
  "이정민": "m-070", "김하율": "m-071", "최준영": "m-072",
};

// ════════════════════════════════════════════════════════════════════
//  팀 20개
// ════════════════════════════════════════════════════════════════════
// ⚠ tags 는 팀 만들기 화면의 고정 목록(DefaultTeamTags)에서만 사용한다.
//   활발한 조용한 카페좋아 술좋아 운동좋아 영화매니아 게임러버 음악좋아
//   여행좋아 맛집탐방 예술좋아 독서좋아 춤 노래 요리
const teams = [
  { teamId:"team-001", teamName:"코딩마스터즈", description:"알고리즘·백엔드 스터디",
    tags:["조용한","게임러버","독서좋아"], mbtiTags:["INTP","INTJ","ISTP"],
    memberIds:["m-001","m-002","m-003","m-004"],
    balanceProfile:{meeting_purpose:1.0,intensity:0.75,frequency:-0.75,cost:-1.0,vibe:-0.75,planning:1.0} },
  { teamId:"team-002", teamName:"토익900 달성반", description:"목표 900+ 토익 스터디",
    tags:["조용한","독서좋아"], mbtiTags:["ISTJ","ESTJ","ISFJ"],
    memberIds:["m-005","m-006","m-007","m-008"],
    balanceProfile:{meeting_purpose:0.75,intensity:0.5,frequency:-0.25,cost:-0.5,vibe:0.25,planning:0.75} },
  { teamId:"team-003", teamName:"전공벼락치기", description:"기말고사 전공 스터디",
    tags:["조용한","독서좋아"], mbtiTags:["ISTJ","INTJ","ENFJ"],
    memberIds:["m-009","m-010","m-011"],
    balanceProfile:{meeting_purpose:0.67,intensity:1.0,frequency:-0.67,cost:-0.67,vibe:0.0,planning:0.67} },
  { teamId:"team-004", teamName:"자격증 헌터", description:"정처기·ADsP 같이 준비",
    tags:["조용한","독서좋아","게임러버"], mbtiTags:["ISTJ","ESTJ","ENTJ"],
    memberIds:["m-012","m-013","m-014","m-015"],
    balanceProfile:{meeting_purpose:0.5,intensity:0.5,frequency:-0.25,cost:-0.5,vibe:-0.25,planning:0.5} },
  { teamId:"team-005", teamName:"책벌레 토론방", description:"격주 독서 토론 모임",
    tags:["조용한","독서좋아","예술좋아"], mbtiTags:["INFJ","INFP","ENFP"],
    memberIds:["m-016","m-017","m-018"],
    balanceProfile:{meeting_purpose:-0.33,intensity:-0.33,frequency:0.33,cost:-0.33,vibe:-0.67,planning:0.33} },
  { teamId:"team-006", teamName:"한성 머슬팩토리", description:"같이 운동하고 식단 공유",
    tags:["운동좋아","활발한"], mbtiTags:["ESTP","ISTP","ENTJ"],
    memberIds:["m-019","m-020","m-021","m-022"],
    balanceProfile:{meeting_purpose:0.25,intensity:1.0,frequency:-0.75,cost:-0.5,vibe:0.5,planning:0.25} },
  { teamId:"team-007", teamName:"새벽러닝크루", description:"매주 토요일 새벽 러닝",
    tags:["운동좋아","활발한","여행좋아"], mbtiTags:["ENFP","ESFP","ESTP"],
    memberIds:["m-023","m-024","m-025","m-026"],
    balanceProfile:{meeting_purpose:-0.5,intensity:0.75,frequency:-0.75,cost:-0.75,vibe:1.0,planning:0.75} },
  { teamId:"team-008", teamName:"셔틀콕 동아리", description:"배드민턴 초보~중급",
    tags:["운동좋아","활발한"], mbtiTags:["ESFP","ENFP","ISFP"],
    memberIds:["m-027","m-028","m-029","m-030"],
    balanceProfile:{meeting_purpose:-0.5,intensity:0.0,frequency:-0.25,cost:-0.5,vibe:0.5,planning:-0.25} },
  { teamId:"team-009", teamName:"골넣자 풋살팀", description:"주말 풋살 경기",
    tags:["운동좋아","활발한","술좋아"], mbtiTags:["ESTP","ENTP","ISTP"],
    memberIds:["m-031","m-032","m-033","m-034","m-035"],
    balanceProfile:{meeting_purpose:-0.2,intensity:0.8,frequency:-0.4,cost:-0.4,vibe:0.8,planning:0.2} },
  { teamId:"team-010", teamName:"성북 미식회", description:"성북구 숨은 맛집 탐방",
    tags:["맛집탐방","활발한","요리"], mbtiTags:["ESFP","ENFP","ESFJ"],
    memberIds:["m-036","m-037","m-038"],
    balanceProfile:{meeting_purpose:-1.0,intensity:-0.33,frequency:-0.33,cost:0.33,vibe:0.67,planning:-0.33} },
  { teamId:"team-011", teamName:"카페홀릭", description:"서울 감성카페 투어",
    tags:["카페좋아","조용한","예술좋아"], mbtiTags:["INFP","ISFP","ENFP"],
    memberIds:["m-039","m-040","m-041"],
    balanceProfile:{meeting_purpose:-1.0,intensity:-0.67,frequency:0.67,cost:0.67,vibe:-0.67,planning:-0.67} },
  { teamId:"team-012", teamName:"야식특공대", description:"금요일 밤 야식 번개",
    tags:["술좋아","활발한","맛집탐방"], mbtiTags:["ESTP","ESFP","ENTP"],
    memberIds:["m-042","m-043","m-044","m-045"],
    balanceProfile:{meeting_purpose:-1.0,intensity:-0.25,frequency:-0.25,cost:0.5,vibe:0.75,planning:-0.75} },
  { teamId:"team-013", teamName:"주사위 굴려", description:"매주 보드게임 모임",
    tags:["게임러버","활발한"], mbtiTags:["ENTP","ENFP","INTP"],
    memberIds:["m-046","m-047","m-048","m-049"],
    balanceProfile:{meeting_purpose:-0.5,intensity:-0.25,frequency:-0.25,cost:-0.5,vibe:0.5,planning:-0.5} },
  { teamId:"team-014", teamName:"무비나잇", description:"매월 영화 감상 & 리뷰",
    tags:["영화매니아","조용한","독서좋아"], mbtiTags:["INFP","INFJ","ISFP"],
    memberIds:["m-050","m-051","m-052"],
    balanceProfile:{meeting_purpose:-0.67,intensity:-0.33,frequency:0.33,cost:0.0,vibe:-0.33,planning:0.33} },
  { teamId:"team-015", teamName:"찰칵 포토클럽", description:"사진 촬영 & 보정 스터디",
    tags:["예술좋아","조용한","여행좋아"], mbtiTags:["ISFP","INFP","INTP"],
    memberIds:["m-053","m-054","m-055"],
    balanceProfile:{meeting_purpose:-0.33,intensity:-0.33,frequency:-0.33,cost:-0.33,vibe:-0.33,planning:0.67} },
  { teamId:"team-016", teamName:"등산왕", description:"주말 근교 등산",
    tags:["운동좋아","여행좋아"], mbtiTags:["ISFJ","ISTJ","ESFJ"],
    memberIds:["m-056","m-057","m-058","m-059"],
    balanceProfile:{meeting_purpose:-0.5,intensity:0.25,frequency:-0.5,cost:-0.5,vibe:-0.25,planning:0.5} },
  { teamId:"team-017", teamName:"당일치기 탐험대", description:"서울 근교 당일 여행",
    tags:["여행좋아","활발한","맛집탐방"], mbtiTags:["ENFP","ESFP","ENTP"],
    memberIds:["m-060","m-061","m-062"],
    balanceProfile:{meeting_purpose:-1.0,intensity:0.33,frequency:-0.33,cost:0.33,vibe:0.67,planning:-0.67} },
  { teamId:"team-018", teamName:"한성 사운드웨이브", description:"밴드 합주 & 공연 준비",
    tags:["음악좋아","활발한","예술좋아"], mbtiTags:["ENFP","ISFP","ENTP"],
    memberIds:["m-063","m-064","m-065","m-066"],
    balanceProfile:{meeting_purpose:-0.25,intensity:0.75,frequency:-0.5,cost:-0.5,vibe:0.75,planning:0.5} },
  { teamId:"team-019", teamName:"길거리 하모니", description:"홍대 버스킹 팀",
    tags:["음악좋아","노래","활발한"], mbtiTags:["ENFP","ESFP","INFP"],
    memberIds:["m-067","m-068","m-069"],
    balanceProfile:{meeting_purpose:-1.0,intensity:0.33,frequency:-0.67,cost:0.0,vibe:1.0,planning:-0.67} },
  { teamId:"team-020", teamName:"포폴 피드백 랩", description:"포트폴리오 상호 리뷰",
    tags:["조용한","독서좋아"], mbtiTags:["ENTJ","INTJ","ENTP"],
    memberIds:["m-070","m-071","m-072"],
    balanceProfile:{meeting_purpose:1.0,intensity:0.33,frequency:0.33,cost:-0.67,vibe:-0.33,planning:0.67} },
];

const M = (userId,name,age,department,mbti,bio,height,location,interests,foodLikes)=>(
  {userId,name,age,department,mbti,bio,height,location,interests,foodLikes,profileImages:[],teamIds:[]});

const members = [
  // 코딩마스터즈 (계정1 이준호와 매칭 — 관심사/음식 다수 겹치도록 설계)
  M("m-001","박준혁",23,"컴퓨터공학부","INTP","백엔드 개발에 관심 많은 3학년입니다",178,"서울 성북구",["게임","독서","음악감상"],["일식","면요리"]),
  M("m-002","김서연",22,"컴퓨터공학부","INTJ","프론트엔드 개발자 지망생이에요",164,"서울 강북구",["독서","그림","영화감상"],["양식","샐러드"]),
  M("m-003","이동현",24,"컴퓨터공학부","ISTP","시스템 프로그래밍 좋아합니다",175,"서울 노원구",["게임","운동","드라이브"],["고기","한식"]),
  M("m-004","정하은",22,"AI응용학과","INTP","머신러닝 공부 중! 같이 해요",160,"서울 성북구",["게임","카페투어","독서"],["분식","디저트"]),
  // 토익900 달성반
  M("m-005","최민수",23,"영어영문학트랙","ISTJ","현재 850점, 900 넘기고 싶어요",180,"서울 성북구",["영화감상","여행","운동"],["고기","햄버거"]),
  M("m-006","한소윤",22,"경영트랙","ESTJ","취업 준비 중, 토익 필수!",163,"서울 동대문구",["독서","운동","요리"],["샐러드","카페"]),
  M("m-007","유재민",24,"무역학트랙","ISTJ","무역 회사 취업 목표입니다",176,"서울 중랑구",["운동","게임","독서"],["치킨","피자"]),
  M("m-008","김예진",21,"경영트랙","ISFJ","꾸준히 공부하는 스타일이에요",158,"서울 성북구",["독서","영화감상","음악감상"],["일식","한식"]),
  // 전공벼락치기
  M("m-009","임성호",23,"전자트랙","ISTJ","회로이론 같이 공부해요",174,"서울 성북구",["게임","독서","음악감상"],["일식","면요리"]),
  M("m-010","오지수",22,"전자트랙","INTJ","전공 A+ 목표! 같이 달려요",166,"서울 강북구",["독서","음악감상","영화감상"],["분식","양식"]),
  M("m-011","강태윤",23,"전자트랙","ENFJ","네트워크 전공, 같이 시험 준비",181,"서울 도봉구",["운동","독서","게임"],["고기","면요리"]),
  // 자격증 헌터
  M("m-012","신동우",24,"컴퓨터공학부","ISTJ","정처기 실기 준비 중",177,"서울 성북구",["게임","운동","독서"],["고기","술"]),
  M("m-013","윤서아",23,"빅데이터트랙","ESTJ","ADsP 따고 빅데이터 분야 취업!",162,"서울 종로구",["독서","카페투어","요리"],["카페","디저트"]),
  M("m-014","조현우",25,"융합보안학과","ENTJ","정보보안기사 도전 중입니다",179,"서울 성북구",["게임","등산","독서"],["고기","한식"]),
  M("m-015","배수현",22,"컴퓨터공학부","ISTJ","리눅스마스터 같이 준비해요",165,"서울 동대문구",["게임","요리","독서"],["양식","면요리"]),
  // 책벌레 토론방
  M("m-016","문채원",22,"한국어문학트랙","INFJ","인문학 책 좋아해요, 같이 읽어요",167,"서울 성북구",["독서","산책","요리"],["한식","카페"]),
  M("m-017","황지훈",23,"역사문화트랙","INFP","철학서적 토론 좋아합니다",173,"서울 종로구",["독서","영화감상","카페투어"],["햄버거","술"]),
  M("m-018","노유진",21,"미디어디자인트랙","ENFP","소설·에세이 다 좋아해요!",161,"서울 성북구",["독서","카페투어","그림"],["디저트","카페"]),
  // 한성 머슬팩토리
  M("m-019","장민기",24,"스포츠미디어트랙","ESTP","벌크업 중! 같이 운동하실 분",183,"서울 성북구",["운동","요리","게임"],["고기","한식"]),
  M("m-020","서예린",22,"스포츠미디어트랙","ISTP","필라테스·웨이트 둘 다 해요",168,"서울 강북구",["운동","요리","산책"],["샐러드","해산물"]),
  M("m-021","한승준",23,"경영트랙","ENTJ","운동 루틴 공유합시다",180,"서울 동대문구",["운동","게임","여행"],["고기","한식"]),
  M("m-022","김태영",25,"인테리어디자인트랙","ESTP","3분할 루틴으로 훈련 중",185,"서울 성북구",["운동","캠핑","요리"],["고기","한식"]),
  // 새벽러닝크루 (계정2 박미래와 매칭 — 운동/여행/요리/사진 겹치도록 설계)
  M("m-023","이수빈",22,"뷰티디자인매니지먼트트랙","ENFP","러닝으로 하루 시작해요!",165,"서울 성북구",["운동","요리","여행"],["샐러드","디저트"]),
  M("m-024","정우성",24,"산업공학트랙","ESFP","마라톤 완주가 목표입니다",178,"서울 노원구",["운동","등산","사진"],["한식","샐러드"]),
  M("m-025","양하윤",21,"상담심리트랙","ENFP","같이 뛰면 덜 힘들어요~",162,"서울 성북구",["운동","산책","카페투어"],["샐러드","햄버거"]),
  M("m-026","김재훈",23,"경제트랙","ESTP","주 3회 한강 러닝 합니다",176,"서울 중구",["운동","드라이브","음악감상"],["한식","고기"]),
  // 셔틀콕 동아리
  M("m-027","박서진",22,"경영트랙","ESFP","배드민턴 초보인데 재밌어요!",170,"서울 성북구",["운동","쇼핑","게임"],["치킨","분식"]),
  M("m-028","이준서",23,"기계설계트랙","ENFP","동호회 급 실력 목표!",179,"서울 동대문구",["운동","게임","맛집탐방"],["고기","한식"]),
  M("m-029","김나연",21,"커뮤니케이션디자인트랙","ISFP","운동 부족해서 시작했어요",163,"서울 성북구",["운동","그림","영화감상"],["양식","피자"]),
  M("m-030","최원빈",24,"전자트랙","ENFP","중급 수준, 같이 실력 올려요",182,"서울 강북구",["운동","게임","음악감상"],["햄버거","치킨"]),
  // 골넣자 풋살팀
  M("m-031","송민혁",24,"스포츠미디어트랙","ESTP","풋살 경력 3년, 포지션 공격수",180,"서울 성북구",["운동","게임","술"],["고기","술"]),
  M("m-032","이하준",23,"경영트랙","ENTP","수비 담당! 주말마다 뜁니다",177,"서울 종로구",["운동","게임","여행"],["치킨","피자"]),
  M("m-033","김도윤",22,"인테리어디자인트랙","ISTP","미드필더 선호합니다",175,"서울 동대문구",["운동","게임","그림"],["일식","면요리"]),
  M("m-034","정수아",22,"스포츠미디어트랙","ESTP","여자 풋살러! 같이해요",167,"서울 성북구",["운동","음악감상","춤"],["분식","한식"]),
  M("m-035","오재현",25,"사회트랙","ENTP","골키퍼 자청합니다 ㅋㅋ",184,"서울 노원구",["운동","맛집탐방","게임"],["고기","술"]),
  // 성북 미식회
  M("m-036","강유나",22,"식품영양학트랙","ESFP","성북구 맛집 다 알아요!",164,"서울 성북구",["맛집탐방","요리","쇼핑"],["한식","일식"]),
  M("m-037","백승호",23,"호텔외식경영학과","ENFP","숨은 맛집 발굴이 취미",176,"서울 성북구",["맛집탐방","요리","사진"],["중식","양식"]),
  M("m-038","임수진",21,"미디어디자인트랙","ESFJ","먹방 블로그 운영 중이에요",160,"서울 강북구",["맛집탐방","카페투어","영화감상"],["디저트","카페"]),
  // 카페홀릭 (계정3 김소율과 매칭 — 카페투어/독서/사진/그림 + 카페/디저트 겹치도록 설계)
  M("m-039","전예은",21,"커뮤니케이션디자인트랙","INFP","감성카페 찾아다니는 게 취미",162,"서울 성북구",["카페투어","그림","사진"],["카페","디저트"]),
  M("m-040","구자현",23,"인테리어디자인트랙","ISFP","인테리어 좋은 카페 좋아해요",178,"서울 마포구",["카페투어","그림","드라이브"],["카페","디저트"]),
  M("m-041","한지민",22,"한국어문학트랙","ENFP","카페에서 책 읽는 거 최고!",165,"서울 성북구",["카페투어","독서","산책"],["카페","디저트"]),
  // 야식특공대
  M("m-042","안재원",24,"경영트랙","ESTP","금요일 밤엔 치맥이죠",181,"서울 성북구",["술","게임","맛집탐방"],["치킨","술"]),
  M("m-043","홍서윤",22,"미디어디자인트랙","ESFP","포장마차 감성 좋아해요~",163,"서울 동대문구",["술","노래","맛집탐방"],["분식","찜/탕"]),
  M("m-044","류민석",23,"신소재화학트랙","ENTP","새벽 라면 같이 먹을 사람?",177,"서울 성북구",["술","게임","음악감상"],["면요리","치킨"]),
  M("m-045","장서연",21,"상담심리트랙","ESFP","야식은 곧 힐링이다",159,"서울 종로구",["술","영화감상","맛집탐방"],["피자","치킨"]),
  // 주사위 굴려
  M("m-046","허진우",23,"빅데이터트랙","ENTP","카탄 좋아하는 전략 게이머",175,"서울 성북구",["게임","독서","음악감상"],["피자","카페"]),
  M("m-047","유하은",22,"상담심리트랙","ENFP","마피아 게임 고수에요!",164,"서울 강북구",["게임","영화감상","독서"],["분식","디저트"]),
  M("m-048","남지호",24,"경제트랙","INTP","복잡한 게임일수록 재밌어요",179,"서울 동대문구",["게임","독서","여행"],["일식","면요리"]),
  M("m-049","손예림",21,"커뮤니케이션디자인트랙","ENFP","귀여운 게임 좋아해요~",158,"서울 성북구",["게임","그림","카페투어"],["디저트","카페"]),
  // 무비나잇
  M("m-050","윤서준",23,"문학문화콘텐츠학과","INFP","매달 영화 2편 감상 & 리뷰",176,"서울 성북구",["영화감상","독서","음악감상"],["분식","양식"]),
  M("m-051","김도연",22,"미디어디자인트랙","INFJ","OTT 정주행이 일상이에요",163,"서울 마포구",["영화감상","카페투어","독서"],["술","카페"]),
  M("m-052","이채린",21,"한국어문학트랙","ISFP","예술영화 좋아하는 문과생",160,"서울 성북구",["영화감상","독서","전시회"],["카페","디저트"]),
  // 찰칵 포토클럽
  M("m-053","조민서",22,"커뮤니케이션디자인트랙","ISFP","필름카메라 감성 좋아해요",165,"서울 성북구",["사진","전시회","카페투어"],["카페","디저트"]),
  M("m-054","권도현",24,"제품서비스디자인트랙","INFP","풍경 사진 전문입니다",180,"서울 종로구",["사진","여행","음악감상"],["카페","양식"]),
  M("m-055","정아현",21,"미디어디자인트랙","INTP","라이트룸 보정 공부 중!",161,"서울 성북구",["사진","영화감상","맛집탐방"],["양식","술"]),
  // 등산왕
  M("m-056","이상혁",25,"기계설계트랙","ISTJ","북한산 정기 등반합니다",179,"서울 성북구",["등산","캠핑","요리"],["분식","술"]),
  M("m-057","정은서",22,"뷰티디자인매니지먼트트랙","ISFJ","자연 속에서 힐링해요",163,"서울 강북구",["등산","산책","독서"],["한식","카페"]),
  M("m-058","박영진",24,"행정트랙","ISTJ","도봉산, 수락산 자주 갑니다",175,"서울 도봉구",["등산","여행","사진"],["한식","찜/탕"]),
  M("m-059","최수현",23,"기계자동화트랙","ESFJ","등산 후 막걸리가 최고!",168,"서울 성북구",["등산","산책","요리"],["술","해산물"]),
  // 당일치기 탐험대
  M("m-060","김시우",23,"부동산트랙","ENFP","매주 새로운 곳 탐험!",177,"서울 성북구",["여행","사진","맛집탐방"],["한식","분식"]),
  M("m-061","박지유",22,"영어영문학트랙","ESFP","예쁜 곳 찾아다니는 게 좋아요",164,"서울 마포구",["여행","쇼핑","카페투어"],["양식","디저트"]),
  M("m-062","오서준",24,"사회트랙","ENTP","기획력 있는 여행 리더!",181,"서울 동대문구",["여행","독서","맛집탐방"],["해산물","한식"]),
  // 한성 사운드웨이브
  M("m-063","권혁진",24,"예술학부","ENFP","기타 담당, 밴드 보컬 겸업",178,"서울 성북구",["음악감상","노래","술"],["술","고기"]),
  M("m-064","이다은",22,"예술학부","ISFP","키보드 치면서 노래해요",162,"서울 마포구",["음악감상","노래","독서"],["술","카페"]),
  M("m-065","장윤호",23,"컴퓨터공학부","ENTP","드럼 독학 3년차입니다",180,"서울 성북구",["음악감상","게임","운동"],["치킨","술"]),
  M("m-066","한서영",21,"미디어디자인트랙","ENFP","베이스 초보, 열정은 만렙!",165,"서울 종로구",["음악감상","춤","쇼핑"],["분식","양식"]),
  // 길거리 하모니
  M("m-067","정하진",23,"예술학부","ENFP","홍대 버스킹 매주 나갑니다",175,"서울 마포구",["음악감상","노래","술"],["술","치킨"]),
  M("m-068","민서아",22,"예술학부","ESFP","보컬 담당! 같이 불러요",164,"서울 성북구",["노래","춤","쇼핑"],["디저트","카페"]),
  M("m-069","유채원",21,"한국어문학트랙","INFP","어쿠스틱 감성 좋아해요",160,"서울 성북구",["음악감상","독서","카페투어"],["카페","한식"]),
  // 포폴 피드백 랩
  M("m-070","이정민",25,"컴퓨터공학부","ENTJ","백엔드 포트폴리오 피드백 환영",179,"서울 성북구",["게임","독서","운동"],["카페","양식"]),
  M("m-071","김하율",24,"커뮤니케이션디자인트랙","INTJ","UI/UX 디자이너 지망생이에요",166,"서울 마포구",["그림","전시회","카페투어"],["양식","디저트"]),
  M("m-072","최준영",24,"경영트랙","ENTP","기획·마케팅 포폴 같이 리뷰해요",176,"서울 성북구",["독서","여행","술"],["고기","양식"]),
];

const testAccounts = [
  {
    key:"account1", name:"이준호", target:"코딩마스터즈(team-001)",
    profile:{ name:"이준호", age:22, department:"컴퓨터공학부", mbti:"INTP", location:"서울 성북구",
      interests:["게임","독서","영화감상","음악감상"], foodLikes:["일식","양식","면요리"], height:177 },
    balanceAnswers:{meeting_purpose:1,intensity:1,frequency:-1,cost:-1,vibe:-1,planning:1},
    topTags:["조용한","게임러버","독서좋아"], topMbti:["INTP","INTJ"], actionCount:12,
  },
  {
    key:"account2", name:"박미래", target:"새벽러닝크루(team-007)",
    profile:{ name:"박미래", age:21, department:"스포츠미디어트랙", mbti:"ENFP", location:"서울 강북구",
      interests:["운동","여행","요리","사진"], foodLikes:["샐러드","한식"], height:165 },
    balanceAnswers:{meeting_purpose:-1,intensity:1,frequency:-1,cost:-1,vibe:1,planning:1},
    topTags:["운동좋아","활발한","여행좋아"], topMbti:["ENFP","ESFP"], actionCount:15,
  },
  {
    key:"account3", name:"김소율", target:"카페홀릭(team-011)",
    profile:{ name:"김소율", age:22, department:"미디어디자인트랙", mbti:"INFP", location:"서울 성북구",
      interests:["카페투어","독서","사진","그림"], foodLikes:["카페","디저트"], height:163 },
    balanceAnswers:{meeting_purpose:-1,intensity:-1,frequency:1,cost:1,vibe:-1,planning:-1},
    topTags:["카페좋아","조용한","예술좋아"], topMbti:["INFP","ISFP"], actionCount:11,
  },
];

// ════════════════════════════════════════════════════════════════════
//  이미지 업로드 헬퍼
// ════════════════════════════════════════════════════════════════════

/**
 * 로컬 파일을 Firebase Storage에 업로드하고 공개 다운로드 URL 반환
 */
async function uploadImage(localPath, destination) {
  if (!fs.existsSync(localPath)) {
    console.warn(`  ⚠ 이미지 없음, 건너뜀: ${localPath}`);
    return "";
  }
  await bucket.upload(localPath, {
    destination,
    metadata: { contentType: "image/png" },
    public: true,
  });
  const encodedDest = destination.split("/").map(encodeURIComponent).join("/");
  return `https://storage.googleapis.com/${bucket.name}/${encodedDest}`;
}

// ════════════════════════════════════════════════════════════════════
//  기존 데이터 삭제
// ════════════════════════════════════════════════════════════════════
async function deleteExistingData() {
  console.log("🗑  기존 teams 삭제 중...");
  const teamsSnap = await db.collection("teams").get();
  if (teamsSnap.size > 0) {
    const deleteBatch = db.batch();
    teamsSnap.docs.forEach((doc) => deleteBatch.delete(doc.ref));
    await deleteBatch.commit();
  }
  console.log(`   → teams ${teamsSnap.size}개 삭제 완료`);

  console.log("🗑  기존 m-xxx users 삭제 중...");
  const usersSnap = await db.collection("users").get();
  const memberDocs = usersSnap.docs.filter((doc) => doc.id.startsWith("m-"));
  if (memberDocs.length > 0) {
    const userBatch = db.batch();
    memberDocs.forEach((doc) => userBatch.delete(doc.ref));
    await userBatch.commit();
  }
  console.log(`   → users(m-xxx) ${memberDocs.length}개 삭제 완료`);
}

// ════════════════════════════════════════════════════════════════════
//  이미지 업로드 (팀 대표사진 + 팀원 프로필)
// ════════════════════════════════════════════════════════════════════
async function uploadAllImages() {
  // 더미데이터 폴더가 없으면 이미지 없이 진행
  if (!fs.existsSync(IMAGE_ROOT)) {
    console.warn(`⚠ 이미지 폴더 없음 (${IMAGE_ROOT}), 사진 없이 진행합니다.`);
    return { teamImageUrls: {}, memberImageUrls: {} };
  }

  const teamImageUrls = {};
  const memberImageUrls = {};

  for (const [teamId, info] of Object.entries(teamImageMap)) {
    // 팀 대표사진
    const teamImgLocal = path.join(IMAGE_ROOT, info.dir, info.img);
    console.log(`📸 팀 대표사진 업로드: ${teamId}`);
    teamImageUrls[teamId] = await uploadImage(
      teamImgLocal,
      `teams/${teamId}/profile.png`
    );

    // 멤버 사진: 멤버 폴더 안의 파일들 스캔
    const memberDir = path.join(IMAGE_ROOT, info.dir, "멤버");
    if (fs.existsSync(memberDir)) {
      const files = fs.readdirSync(memberDir).filter((f) => f.endsWith(".png"));
      for (const file of files) {
        // 파일명에서 이름 추출: "박준혁 (남, 23세, 178cm).png" → "박준혁"
        const nameMatch = file.match(/^([가-힣]+)\s/);
        if (!nameMatch) continue;
        const name = nameMatch[1];
        const userId = memberNameToId[name];
        if (!userId) {
          console.warn(`  ⚠ 매핑 없음: ${name}`);
          continue;
        }
        console.log(`👤 멤버 프로필 업로드: ${userId} (${name})`);
        memberImageUrls[userId] = await uploadImage(
          path.join(memberDir, file),
          `users/${userId}/profile.png`
        );
      }
    }
  }

  return { teamImageUrls, memberImageUrls };
}

// ════════════════════════════════════════════════════════════════════
//  업로드 실행
// ════════════════════════════════════════════════════════════════════
async function seed() {
  // 1) 기존 데이터 삭제
  await deleteExistingData();

  // 2) 이미지 업로드
  console.log("\n📦 이미지 업로드 시작...");
  const { teamImageUrls, memberImageUrls } = await uploadAllImages();
  console.log("✅ 이미지 업로드 완료\n");

  let batch = db.batch();
  let ops = 0;
  const commitIfNeeded = async () => {
    if (++ops >= 400) { await batch.commit(); batch = db.batch(); ops = 0; }
  };

  // 3) teams
  console.log("📝 teams 업로드 중...");
  for (let i = 0; i < teams.length; i++) {
    const t = teams[i];
    const ref = db.collection("teams").doc(t.teamId);
    // 멤버 프로필 이미지 URL 목록 수집
    const memberProfileImages = t.memberIds
      .map((id) => memberImageUrls[id] || "")
      .filter((url) => url !== "");
    batch.set(ref, {
      ...t,
      leaderId: t.memberIds[0],
      profileImages: memberProfileImages,
      teamProfileImage: teamImageUrls[t.teamId] || "",
      isDummy: true,
      status: "active",
      createdAt: now - i * 60000,
    });
    await commitIfNeeded();
  }

  // 4) member users
  console.log("📝 users(멤버) 업로드 중...");
  for (const m of members) {
    const ref = db.collection("users").doc(m.userId);
    const imageUrl = memberImageUrls[m.userId] || "";
    batch.set(ref, {
      ...m,
      profileImages: imageUrl ? [imageUrl] : [],
      isDummy: true,
    });
    await commitIfNeeded();
  }

  // 5) 테스트 계정
  console.log("📝 테스트 계정 업로드 중...");
  for (const acc of testAccounts) {
    const uid = TEST_ACCOUNT_UIDS[acc.key];
    if (!uid || uid.startsWith("PASTE_")) {
      console.warn(`⚠ ${acc.key}(${acc.name}) UID 미입력 → 건너뜀`);
      continue;
    }

    const userRef = db.collection("users").doc(uid);
    batch.set(userRef, {
      ...acc.profile,
      profileImages: [],
      teamIds: [],
      balanceProfile: { answers: acc.balanceAnswers },
    }, { merge: true });

    const tagScores = {};
    acc.topTags.forEach((tag, idx) => { tagScores[tag] = acc.actionCount - idx; });
    const mbtiScores = {};
    acc.topMbti.forEach((mb, idx) => { mbtiScores[mb] = (acc.actionCount - idx) * 2; });

    const likedCount = Math.ceil(acc.actionCount / 2);
    const passedCount = acc.actionCount - likedCount;
    const likedTeamIds = Array.from({length:likedCount}, (_,i)=>`seed-liked-${i+1}`);
    const passedTeamIds = Array.from({length:passedCount}, (_,i)=>`seed-passed-${i+1}`);

    const prefRef = db.collection("userPreferences").doc(uid);
    batch.set(prefRef, {
      userId: uid, tagScores, mbtiScores, likedTeamIds, passedTeamIds, updatedAt: now,
    });
    await commitIfNeeded();
  }

  await batch.commit();
  console.log(`\n✅ 시드 완료 — 팀 ${teams.length}개, 팀원 ${members.length}명, 테스트 계정 ${testAccounts.length}개`);
  console.log("   팀 대표사진 및 멤버 프로필 사진이 Storage에 업로드되었습니다.");
}

seed().catch((e) => { console.error("시드 실패:", e); process.exit(1); });
