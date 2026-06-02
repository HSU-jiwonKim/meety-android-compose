╔══════════════════════════════════════════════════════════════════╗
║      Meety Firebase 더미데이터 세팅 도구                           ║
║      피드 매칭 시연 최적화 버전                                      ║
╚══════════════════════════════════════════════════════════════════╝

■ 이 도구가 하는 일
──────────────────────────────────────────────────────────────────
  • Firebase Authentication 에 테스트 계정 4개 생성/갱신
  • Firestore users 컬렉션에 더미 팀원 72명 + 테스트 계정 4개 저장
  • Firestore teams 컬렉션에 더미 팀 20개 저장
  • Firestore userPreferences 에 테스트 계정 선호도/스와이프 이력 저장
    (actionCount ≥ 10 → "자주 누른 태그" 매칭 근거 카드 자동 해금)

■ 사전 준비 (필수)
──────────────────────────────────────────────────────────────────
  1. Python 3.8 이상 설치
     https://www.python.org/downloads/

  2. Firebase 서비스 계정 키 발급
     a) Firebase 콘솔 https://console.firebase.google.com 접속
     b) meety-compose 프로젝트 선택
     c) 왼쪽 메뉴: 프로젝트 설정(톱니바퀴) → 서비스 계정 탭
     d) [새 비공개 키 생성] 버튼 클릭 → JSON 파일 다운로드
     e) 다운로드한 파일을 이 폴더에
        "serviceAccountKey.json" 으로 이름 바꿔서 저장

  ※ serviceAccountKey.json 은 절대 외부 공유 금지!
     .gitignore 에 추가되어 있는지 확인하세요.

■ 실행 방법
──────────────────────────────────────────────────────────────────
  Windows:
    run_windows.bat 더블클릭
    (또는 CMD에서: python setup_dummy_data.py)

  Mac / Linux:
    터미널에서:  bash run_mac_linux.sh
    (또는:       python3 setup_dummy_data.py)

  pip 직접 설치가 필요한 경우:
    pip install firebase-admin

■ 생성되는 테스트 계정
──────────────────────────────────────────────────────────────────
  계정1  이준호   test1@hansung.ac.kr  /  meety1234!
         → 코딩마스터즈 (team-001) 와 최고 매칭
           가치관 6/6 일치, 관심사(코딩·알고리즘·개발) 겹침, 같은 성북구

  계정2  박미래   test2@hansung.ac.kr  /  meety1234!
         → 새벽러닝크루 (team-007) 와 최고 매칭
           러닝 태그 누적 10점, 활발·몰입형 일치, 강북↔성북 근거리

  계정3  김소율   test3@hansung.ac.kr  /  meety1234!
         → 카페홀릭 (team-011) 와 최고 매칭
           가치관 6/6 완전 일치, 관심사(카페·감성) 겹침, 모두 성북구

  계정4  서재원   test4@hansung.ac.kr  /  meety1234!
         → 한성 사운드웨이브 (team-018) 와 최고 매칭
           기타·밴드·음악 관심사 완전 겹침, 몰입형·활발 일치

■ 매칭 시연 포인트
──────────────────────────────────────────────────────────────────
  각 계정으로 로그인 후:
  1. 피드 탭 → 추천 카드에서 최적 팀이 상위 노출됨
  2. 카드의 적합도 배지(%) 확인
  3. 카드 탭 → MatchReasonSheet 에서 근거 3가지 확인
     • 관심사 겹침 (예: "코딩 외 2개 일치")
     • 가치관 일치 (예: "6문항 중 6개 일치")
     • 거리 점수 (예: "평균 88점 / 성북구 기준")
  4. actionCount ≥ 10 → "자주 누른 태그" 카드도 해금 상태

■ 폴더 구성
──────────────────────────────────────────────────────────────────
  meety_dummy_setup/
  ├── setup_dummy_data.py    ← 메인 스크립트
  ├── serviceAccountKey.json ← 직접 발급해서 여기 저장 (Git 제외)
  ├── requirements.txt       ← pip install -r requirements.txt
  ├── run_windows.bat        ← Windows 실행기
  ├── run_mac_linux.sh       ← Mac/Linux 실행기
  └── README.txt             ← 이 파일

■ 주의사항
──────────────────────────────────────────────────────────────────
  • 스크립트는 set(merge=True) 로 동작하므로 여러 번 실행해도 안전합니다.
  • 팀 프로필 이미지는 Firebase Storage에 별도 업로드 후
    Firestore teams/{teamId}.teamProfileImage 필드를 직접 수정하세요.
  • isDummy: true 필드가 기록되므로 어드민 화면에서 더미 데이터 구분 가능합니다.
