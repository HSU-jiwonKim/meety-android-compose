# Meety Critical 이슈 재현 테스트 가이드

각 테스트를 시작하기 전 공통 준비:
1. **Android Studio Logcat 열기** → 패키지 필터 `package:mine` 또는 `com.bugzero.meety`
2. 실 기기 또는 에뮬레이터(API 33+ 권장)에서 디버그 빌드
3. 테스트용 Firebase 계정 2개 이상 (사용자 A, B), 어드민 계정 1개
4. **각 테스트 후엔 앱 강제 종료 → 재실행** (이전 상태 영향 제거)

---

## C1. 통화 권한 거절 → 검은 화면에 갇힘

**재현 단계**:
1. 앱 설정 → 앱 정보 → Meety → 권한 → 카메라/마이크 모두 "거부"로 설정 (또는 처음 통화할 때 "한 번만"/"거부" 선택)
2. 앱 진입 → 채팅방 → 영상통화 버튼
3. 권한 다이얼로그에서 **"거부"** 누르기 (또는 "허용 안 함")

**기대 버그**:
- 검은 화면 + "연결 중..." 텍스트만 보임
- 종료(빨간) 버튼 안 보임 → **사용자가 빠져나갈 방법 없음**
- 뒤로가기로 빠지면 Firebase Console → Firestore → `calls/{chatId}` 문서가 `status: "calling"`으로 남아있음

**확인 방법**:
- Logcat 필터: `CallViewModel|CallScreen` — `initEngine` 호출 로그 없음
- Firebase Console에서 `calls` 컬렉션 직접 확인
- 사용자 B로 같은 채팅방에 들어가면 잔존 incoming call 다이얼로그 떠야 함 (좀비 통화)

**난이도**: 쉬움 (1분)

---

## C2. RtcEngine.create 실패 시 무한 "연결 중..."

**재현 단계 (직접 트리거 어려움 — 시뮬레이션 필요)**:
1. 임시로 `CallViewModel.kt:119-130` `initEngine()` 안에 강제 실패 코드 삽입:
   ```kotlin
   // 테스트용: rtcEngine 강제 null
   rtcEngine = null
   _callUiState.value = CallUiState.Calling
   return
   ```
2. 또는 더 현실적: `app/build.gradle.kts`에서 Agora SDK 라인 주석 처리 후 빌드 → 런타임 ClassNotFoundException
3. 실제 부스 환경 시뮬레이션: 비행기 모드 켠 상태에서 통화 시도 (Agora 토큰 발급 실패)

**기대 버그**:
- "연결 중..." 화면이 영원히 유지됨
- 종료 버튼 누르면 동작은 하지만 `_callUiState`가 `Calling`이라 onCallEnded 분기 못 탐
- 강제 종료 외 빠져나갈 수 없음

**확인 방법**:
- Logcat: `RtcEngine` 또는 `runCatching` 관련 에러
- `_callUiState.value` 변화를 디버거로 추적

**난이도**: 보통 (코드 수정 또는 네트워크 차단 필요)

---

## C3. RtcEngine.destroy() 누락 — 메모리 누수 + 카메라 LED

**재현 단계**:
1. 사용자 A로 로그인 → 채팅방 → 영상통화 시작 → 종료 (정상 흐름)
2. **위 과정을 10~20회 반복** (다른 사용자/방 섞어서)
3. Android Studio → **Profiler 열기** → Memory tab
4. 추가로 매 통화 종료 후 기기 카메라 LED(녹색) 꺼지는지 육안 확인

**기대 버그**:
- Memory Profiler에서 `io.agora.rtc.*` 객체 인스턴스가 통화 횟수만큼 누적
- 종료 후에도 카메라 LED가 잠시 켜져있거나 다음 통화 시 "카메라 사용 중" 에러
- 30분 사용 시 OOM 또는 카메라 native crash

**확인 방법**:
- Profiler → Heap dump → `RtcEngine` 검색 → 인스턴스 수가 1을 초과하면 누수
- Logcat: `agora` 또는 `Camera` 키워드 — "Camera in use" 에러
- `adb shell dumpsys media.camera` → CAMERA_DISCONNECTED 누적

**난이도**: 보통 (반복 조작 + Profiler 사용)

---

## C4. ChatRoom roomName URL 인코딩 누락

**재현 단계 (가장 빠름)**:
1. 어드민 계정 또는 Firebase Console로 `teams` 컬렉션에 팀 하나 직접 생성하고 `name` 필드를 다음 중 하나로:
   - `한성대 / ENFP` (슬래시)
   - `팀 #1` (#)
   - `Q&A 모임` (&)
   - `2026/3월 캡스톤` (슬래시)
2. 사용자 A로 해당 팀과 매칭 → 채팅방 진입 시도

**기대 버그 (확률 높음)**:
- **`/`**: navigate가 `chat_room/abc123/한성대 / ENFP?roomName=...` 식으로 깨져서 routing 실패 → IllegalArgumentException 또는 잘못된 화면
- **`#`**: 프래그먼트로 인식되어 roomName이 잘림 → 빈 roomName으로 화면 진입
- **`&`**: query param 파싱 깨짐 → 누락 파라미터로 크래시

**확인 방법**:
- Logcat: `IllegalArgumentException`, `NavController` 또는 `Navigation`
- Stack trace에서 `ChatRoomScreen` 또는 `NavGraph.kt:425` 근처 라인 확인
- 채팅방 상단 타이틀이 잘려있거나 "채팅방"으로 폴백되면 라이트 버그

**난이도**: 매우 쉬움 (5분)

---

## C5. LazyColumn 빈 메시지 ID 중복 key 크래시

**재현 단계**:
1. 어드민 계정으로 로그인 → 매칭 처리 시 시스템 메시지를 두 번 이상 트리거
   - 또는 Firebase Console → `chats/{chatId}/messages` 컬렉션에 직접 문서 추가, `id` 필드를 빈 문자열 `""`로 (또는 필드 자체를 빼기) — 두 개 이상 만들기
2. 해당 채팅방 진입

**기대 버그**:
- 채팅방 진입 즉시 크래시
- Logcat 에러: `IllegalArgumentException: Key "" was already used. If you are using LazyColumn/LazyRow please make sure that the items have a unique key.`

**확인 방법**:
- Logcat: `Key "" was already used` 또는 `Key was already used`
- 시스템 메시지가 한 개일 때는 정상이지만, 두 번째가 들어오는 순간 크래시

**난이도**: 쉬움 (Firebase Console 직접 편집)

---

## C6. FeedRepository `lastDocument!!` NPE

**재현 단계**:
1. 사용자 A 로그인 → 피드 화면 → 카드를 끝까지 스와이프 (loadMore 트리거)
2. 동시에 다른 단말 또는 어드민 화면에서 `resetAllDemoData` 또는 새 팀 대량 생성 (race condition 유도)
3. **더 쉬운 재현**: 임시 코드 삽입 — `FeedRepository.kt`에서 `lastDocument`를 매번 null로 리셋하는 두 번째 코루틴 추가

**기대 버그**:
- `kotlin.KotlinNullPointerException` 크래시
- Stack trace에서 `FeedRepository.kt:77` 또는 `:138` 가리킴

**확인 방법**:
- Logcat: `FATAL EXCEPTION` + `KotlinNullPointerException` + `FeedRepository`
- 일상 사용 중에도 종종 발생할 수 있어 부스에서 위험

**대안 검증** (실제 race 어려우면):
- `lastDocument`를 임의로 reset 후 즉시 loadMore 호출하는 테스트 함수 추가
- 또는 페이지 끝까지 스와이프 → 빠르게 새로고침 반복

**난이도**: 어려움 (race condition이라 100% 재현 어려움 — 코드 검토로 확신)

---

## C7. UserRepository 로그인 race NPE

**재현 단계**:
1. 안 좋은 와이파이 환경 또는 네트워크 throttling (Android Studio → Emulator → Extended Controls → Cellular → "Edge"/"GSM")
2. 로그인 화면에서 이메일/비번 입력 후 "로그인" 클릭
3. 로딩 중 즉시 앱을 백그라운드로 → 다시 포그라운드 (또는 빠르게 다른 버튼 연타)

**기대 버그**:
- `currentUser`가 sign-out 직전 null이 되어 `user!!.uid` NPE
- 크래시 또는 ANR

**확인 방법**:
- Logcat: `FATAL EXCEPTION` + `UserRepository.kt:37`
- 로그인 직후 → 로그아웃 버튼 빠르게 누르기로도 트리거 가능

**난이도**: 보통 (네트워크 throttling 필요)

---

## C8. 빈 chatId로 Firestore 호출 → IllegalArgumentException

**재현 단계 (FCM 페이로드 위조)**:
1. Firebase Console → Cloud Messaging → "Send your first message" 또는 `fcm.googleapis.com/v1/projects/.../messages:send`
2. 페이로드를 다음과 같이 (chatId 누락):
   ```json
   {
     "message": {
       "token": "<디바이스 FCM 토큰>",
       "data": {
         "type": "incoming_call",
         "callType": "voice",
         "roomName": "테스트"
       }
     }
   }
   ```
3. 디바이스의 FCM 토큰은 `MyFirebaseMessagingService.onNewToken` 로그 또는 Firestore `users/{uid}.fcmToken`에서 확인
4. 알림 도착 후 통화 수락 버튼 또는 알림 탭

**기대 버그**:
- `IllegalArgumentException: Document path must not be empty`
- `MyFirebaseMessagingService` 프로세스 또는 MainActivity 크래시

**확인 방법**:
- Logcat: `Document path must not be empty` 또는 `IllegalArgumentException` + `Firestore`
- 또는 그냥 `MainActivity.kt:189-200` 디버거 브레이크포인트 → `chatId.value`가 `""`인 채로 navigate 호출되는지 확인

**더 쉬운 검증**: 코드만 보고 검증 — `chatId ?: ""` → 빈 문자열이 그대로 navigate에 들어가는 게 명백.

**난이도**: 보통 (FCM 페이로드 발송 필요)

---

## C9. 비밀번호 확인 검증 누락

**재현 단계 (가장 쉬움)**:
1. 앱 → 회원가입 화면 진입
2. Step1에서 이메일/이름 입력 → 다음
3. 비밀번호: `aaaaaa1!`
4. 비밀번호 확인: `bbbbbb2!` (전혀 다른 값)
5. 가입 진행

**기대 버그**:
- **에러 없이 가입 통과** → Firebase Auth는 첫 번째 비밀번호로 계정 생성
- 사용자는 자기가 입력했다고 생각하는 비밀번호 모름
- 다음 로그인 시 "비밀번호 틀림" 에러로 막힘

**확인 방법**:
- 크래시는 안 나지만 명백한 기능 결함
- Firebase Console → Authentication → 가입된 계정 확인 후 두 비밀번호로 로그인 테스트

**난이도**: 매우 쉬움 (30초)

---

## C10. PendingVerification 무한 polling + 거절 감지 실패

**시나리오 A — 무한 polling 확인**:
1. 학생증 미인증 상태 계정으로 로그인 → PendingVerification 화면 진입
2. **앱을 백그라운드로 보내고** 30분 방치
3. Logcat 또는 Firebase Console → Firestore Usage 탭 모니터링

**기대**: 백그라운드 상태에서도 10초마다 Firestore `users/{uid}.get()` 호출이 계속 찍혀야 함 → Firebase 비용 + 배터리 소모

**시나리오 B — 거절 감지 실패**:
1. 학생증 미인증 계정으로 로그인 → PendingVerification 화면 진입 (대기 중)
2. **기기를 비행기 모드로** (와이파이/데이터 차단)
3. 다른 단말 또는 어드민 페이지에서 해당 계정을 **거절** 처리 (`isRejected: true`)
4. 비행기 모드 해제하지 않고 5분 대기
5. 비행기 모드 해제 → Firestore 동기화

**기대 버그**:
- 비행기 모드 동안 `Firestore.get(Source.SERVER)` 실패 — try/catch가 없어서 silent fail
- `addOnFailureListener` 없으니 retry 로직도 안 돔
- 거절됐는데 `isRejected = false`로 남아 영원히 "심사 중" 화면

**확인 방법**:
- Logcat: 비행기 모드 동안 `Firestore`, `FirebaseFirestoreException` 에러 로그 (잡히지 않은 채로)
- 어드민에서 거절했는데 PendingVerification 화면이 풀리지 않음

**난이도**: 보통 (관리 권한 + 네트워크 제어 필요)

---

## 🎯 빠른 부스 직전 스모크 테스트 (15분)

가장 흔히 터지는 것만 빠르게:

| 순서 | 테스트 | 시간 |
|---|---|---|
| 1 | C9 (비번 확인) — 가입 시 다른 비번 입력 | 30초 |
| 2 | C4 (URL 인코딩) — 팀명에 `/` 또는 `#` 넣기 | 2분 |
| 3 | C5 (빈 ID key) — Firestore에 빈 id 메시지 2개 추가 | 3분 |
| 4 | C1 (통화 권한 거절) — 권한 거부 후 통화 시도 | 1분 |
| 5 | C3 (메모리 누수) — 통화 10번 반복 + 카메라 LED 확인 | 5분 |
| 6 | C10 (무한 polling) — 비행기 모드에서 거절 처리 후 복구 | 3분 |

**Logcat 필수 필터** (저장해두고 테스트마다 적용):
```
package:mine (level:ERROR | tag:CallViewModel | tag:ChatViewModel | tag:FeedRepository | tag:Firebase)
```

**테스트 중 크래시 발생 시**:
1. Logcat에서 `FATAL EXCEPTION` 키워드로 stack trace 캡처
2. 크래시 후 앱 자동 재시작되는지 확인 (재시작 안 되면 ANR/native)
3. Firebase Crashlytics 연동돼있으면 콘솔에서도 확인 가능 (현재 미연동 — 권장)

---

## 📌 부스 운영 비상 가이드 (테스트 결과 모두 재현되면)

**5분 안에 못 고치는 이슈에 대한 우회**:
- C1, C2, C3 (통화 관련) → **통화 기능 자체를 데모에서 제외**, "현재 베타 중" 안내
- C4 (URL 인코딩) → **시연용 팀 이름은 영문/숫자만 사용** ("Team A", "Project X" 등)
- C5 (시스템 메시지 중복 ID) → **시연 전 Firebase Console에서 모든 시스템 메시지 정리**
- C8 (빈 chatId FCM) → **시연 중 외부 FCM 발송 차단** (자체 발송만 사용)
- C10 (PendingVerification) → **시연 계정은 모두 인증 완료 상태로 미리 준비**

**손님 1명마다 표준 프로토콜**:
1. 앱 강제 종료 (최근앱 → 스와이프)
2. 시연 계정으로 자동 로그인
3. 손님 시연 (5분 이내)
4. 로그아웃
5. 손님 교체
