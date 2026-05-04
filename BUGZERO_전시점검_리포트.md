# Meety 전시 부스 안정성 점검 리포트

**검토 범위**: app/src/main 전체 (21,426줄, Kotlin 59 파일)
**검토 일자**: 2026-05-02
**검토 목표**: 전시 부스에서 일반인이 직접 사용 — 크래시·UX 막힘·연속 사용 시 데이터 노출 방지

---

## ⚠️ 전시 직전 반드시 수정 (Critical)

### C1. 통화 권한 거절 시 검은 화면 + 종료 불가 → 좀비 통화 잔존
**파일**: `ui/call/CallScreen.kt:66-95`
사용자가 카메라/마이크 권한 다이얼로그에서 "거부" 또는 "한 번만"을 누르면 `permissionLauncher` 콜백이 `if (results.values.all { it })` 분기에서 그대로 빠짐. 화면은 검은 배경 + "연결 중..."만 표시되고 종료 버튼도 안 보이며, 뒤로가기로 빠져도 `endCall()` 미호출 → Firestore의 `calls/{chatId}.status="calling"` 잔존 → 다음 사용자에게 좀비 incoming call 다이얼로그가 뜸.
**수정**: 거절 분기에서 `_callUiState.value = CallUiState.Ended` + `onCallEnded()` 호출, 또는 안내 다이얼로그 후 자동 종료.

### C2. RtcEngine.create 실패 시 무한 "연결 중..."
**파일**: `ui/call/CallViewModel.kt:119-130, 423-441`
`RtcEngine.create()`가 `runCatching`으로 감싸있지만 실패 시 `rtcEngine = null`. 이후 `joinChannel()`이 `engine ?: return`으로 조용히 빠져나가고 `_callUiState`는 계속 `Calling` → 사용자는 강제 종료 외 방법 없음. 부스에서 통화 진입/종료 빠르게 반복할 때 발생.
**수정**: `initEngine()` 실패 시 `_callUiState.value = CallUiState.Ended` 처리, joinChannel 실패도 동일.

### C3. RtcEngine.destroy() 누락 — 누적 메모리 누수 + 카메라 LED 안 꺼짐
**파일**: `ui/call/CallViewModel.kt:449-462`
통화 N회마다 `rtcEngine` 새로 생성되지만 `RtcEngine.destroy()`가 어디서도 호출되지 않음. `onCleared()`도 정상 종료(`Ended` 상태) 분기에서는 `leaveChannel/stopPreview` 안 부름. 부스 30분 사용 후 OOM 또는 카메라 점유 충돌로 native crash. 데모 중 "카메라 LED가 안 꺼져요" 클레임 확정.
**수정**: `onCleared()`에서 무조건 `rtcEngine?.leaveChannel()` + `stopPreview()` + `RtcEngine.destroy()` + `rtcEngine = null` 호출.

### C4. ChatRoom roomName URL 인코딩 누락 → 한글/특수문자 팀명 크래시
**파일**: `navigation/NavGraph.kt:425`
```kotlin
navController.navigate("${Routes.CHAT_ROOM}/$chatId?roomName=$roomName")
```
`roomName`에 `?`, `&`, `#`, `/`, 공백, 한글 등이 들어가면 deep-link 파싱 실패 → `IllegalArgumentException`. 채팅방 이름이 사용자/팀 이름 기반이라 "한성대 / ENFP" 같은 흔한 팀명에서 100% 크래시.
**수정**: `Uri.encode(roomName)` 적용. 같은 패턴이 `MainActivity.kt:198,290,315`에도 있음.

### C5. LazyColumn 빈 메시지 ID로 중복 key 크래시
**파일**: `ui/chat/ChatRoomScreen.kt:261`
`itemsIndexed(items=reversedMessages, key={ _, it -> it.id })` — `ChatModels.kt:19`에서 `id` 기본값 `""`. Firestore 매핑 시 `@DocumentId` 적용 안 되어 있어 빈 id 두 개 이상 생기면 `IllegalArgumentException: Key was already used` → Compose 렌더링 크래시.
**수정**: ChatMessage에 `@DocumentId` 적용 또는 key를 `"${it.id}_${index}"` 식으로 보강.

### C6. FeedRepository 페이지네이션 NPE (`!!`)
**파일**: `data/repository/FeedRepository.kt:77, 138`
```kotlin
query = query.startAfter(lastDocument!!)
```
직전 라인에서 null 체크는 했지만, race condition으로 다른 코루틴이 reset하면 `!!`에서 NPE. 어드민이 데이터 reset 신호 보내거나 새 사용자 로그인 시점에 발생.
**수정**: `lastDocument?.let { query = query.startAfter(it) }` (`lastAllDocument`도 동일).

### C7. UserRepository 로그인 race NPE
**파일**: `data/repository/UserRepository.kt:37`
```kotlin
if (user?.isEmailVerified == true || isTestAccount(user?.email)) {
    db.collection("users").document(user!!.uid).get()
```
콜백 사이 토큰 만료/sign-out으로 `currentUser`가 null이 될 수 있음. 로그인 직후 빠른 화면 전환 시 NPE.
**수정**: `val uid = user?.uid ?: return@addOnSuccessListener` 식으로 분리.

### C8. 빈 chatId로 Firestore 호출 → IllegalArgumentException
**파일**: `MainActivity.kt:189-200`, `MyFirebaseMessagingService.kt:50-66`
손상된 FCM 페이로드(`chatId` 누락)가 들어오면 `chatId ?: ""`로 폴백 → CallViewModel의 `acceptCall("")` → Firestore `calls/""` update → "Document path must not be empty" → 크래시.
**수정**: `chatId.isEmpty()` 시 navigate 건너뛰고 FEED만 유지.

### C9. 비밀번호 확인 미검증
**파일**: `ui/auth/SignUpScreen.kt:119-123`
`confirmPassword`를 받지만 `password == confirmPassword` 검증 없음. 사용자가 다르게 입력해도 그대로 가입 → 다음 로그인 시 비번 모름.
**수정**: signUp 호출 전 `if (password != confirmPassword)` 가드.

### C10. PendingVerification 무한 polling + 거절 감지 실패
**파일**: `ui/auth/Pendingverificationscreen.kt:42-62`
- 10초 무한 루프 polling — 백그라운드 가도 계속 돔 → Firebase 비용 폭증
- `Firestore.get(Source.SERVER)`에 try/catch 없음 → 와이파이 끊기면 거절돼도 영원히 "심사 중" 화면에 갇힘
**수정**: `LaunchedEffect(isRejected) { while(!isRejected) { delay(10000); ... } }` + `addOnFailureListener`.

---

## 🔴 High (UX 마비·데이터 노출)

### H1. 이전 사용자 채팅이 새 사용자에게 노출 (Privacy)
**파일**: `ui/chat/ChatViewModel.kt:268-272`, `data/repository/UserRepository.kt:319-330`
- `observeMessages(chatId)`가 `ListenerRegistration`을 보관/해제하지 않음 → 채팅방 이동 시 이전 listener 살아있어 짧은 순간 다른 방 메시지 노출.
- `startBanListener`도 등록만 하고 stop 함수 없음 → 로그아웃/재로그인 시 listener 누적.
**수정**: 모든 listener를 필드에 보관, 화면 이동/로그아웃 시 명시적 remove.

### H2. AuthViewModel.logout()이 일부 상태만 리셋
**파일**: `ui/auth/AuthViewModel.kt:106-110`
`_authState`, `_isAdmin`만 리셋하고 `_signUpState`, `_emailVerificationState`, `_profileSaveState`, `_uploadState`, `_verificationCheckState`, `_passwordResetState` 모두 그대로 → A 로그아웃 후 B 로그인 시 이전 에러 메시지 그대로 노출.
**수정**: logout()에서 모든 state reset.

### H3. participants ClassCastException
**파일**: `ui/chat/ChatViewModel.kt:343, 431, 618, 1140`, `ui/call/CallViewModel.kt:180`, `data/repository/AgoraCallRepository.kt:66, 118`
`as? List<String>` 패턴은 erasure 때문에 `List<*>`만 검증. Firestore에 잘못된 타입(`null`, `Long` 등)이 섞이면 contains/iteration 시 NPE/CCE.
**수정**: `(snap.get("participants") as? List<*>)?.filterIsInstance<String>() ?: emptyList()`.

### H4. 모임 생성 입력 검증/중복 클릭 방지 없음
**파일**: `ui/team/MeetingCreateScreen.kt:163-178`, `ui/team/TeamViewModel.kt:96-120`
- `teamName.trim()` 빈값 검증 없음 → 빈 폼 그대로 생성
- 길이 제한 없음 → 5000자 입력 가능 → 카드 깨짐
- `isLoading` 상태가 버튼 enabled에 미반영 → 5번 누르면 5개 팀 생성
**수정**: 빈 검증 + `enabled = !isLoading`.

### H5. 빠른 연속 스와이프 race condition
**파일**: `ui/feed/FeedViewModel.kt:256-296`, `ui/feed/components/SwipeCard.kt:135-160`
fling 애니메이션(300ms) 도중 카드가 다시 swiped 콜백 호출 가능 → currentIndex가 2씩 증가, onLike 이중 호출.
**수정**: `onCardSwiped` 시작에 `processedTeamId` 가드.

### H6. HttpURLConnection disconnect() 누락 — 소켓 누수
**파일**: `data/repository/MeetingPlaceRepository.kt:105-136 외 다수`
`naverGeocode`, `naverReverseGeocode`, `searchNearbyPlaces`, `searchPlacesInRegion`, `findNaverPlaceByAllSearch`, `fetchPhotosFromPlaceHtml`, `fetchVisitorReviewCountFromHtml`, `fetchTransitEndpoint` — 모두 `conn.disconnect()` 누락. 전시에서 수십 회 호출 후 `Too many open files` 크래시.
**수정**: `try { ... } finally { conn.disconnect() }`.

### H7. ListenerRegistration 누수 (chunked loop)
**파일**: `data/repository/FirebaseTeamRepository.kt:325`
`var listenerRegistration` chunked loop 안에서 매번 재할당 → teamIds 11개 이상이면 첫 번째 listener 영영 remove 안 됨.
**수정**: `mutableListOf<ListenerRegistration>()`로 모두 보관 후 함께 remove.

### H8. 통화 중 백그라운드 시 ForegroundService 부재
**파일**: `ui/call/CallViewModel.kt` 전체
사용자가 통화 중 홈 버튼 → 시스템 메모리 압박 시 Activity/ViewModel 죽음 → `endCall` 미호출 → Firestore status calling 잔존 → 좀비 통화.
**수정**: 통화 시작 시 ForegroundService(`type="microphone|camera"`) 시작.

### H9. 메시지 매핑 race condition
**파일**: `data/repository/FirebaseChatRepository.kt:131-280`
`var completed = 0` + `messages.add(...)` 여러 비동기 콜백에서 변경 → ConcurrentModificationException 가능, 카운터 race로 메시지가 영원히 안 뜸.
**수정**: `coroutineScope { docs.map { async { ... } }.awaitAll() }` 패턴.

### H10. CallActionReceiver 비동기 작업 누락
**파일**: `CallActionReceiver.kt:33-38`
`update("status", "ended")` 호출만 하고 결과 대기 안 함. Doze 상태에서 발신자에게 전달 안 되면 발신자 무한 "연결 중...".
**수정**: `goAsync()` + PendingResult 사용.

### H11. acceptReceivedLike / autoAcceptLike 부분 실패 시 inconsistent 상태
**파일**: `data/repository/FirebaseTeamRepository.kt:171-246`, `data/repository/AdminRepository.kt:244-314`
5단계 콜백 체인 — 와이파이 끊김 시 부분 적용 → 팀 멤버는 추가됐는데 like는 pending → 어드민 화면에 중복 표시.
**수정**: `runTransaction`으로 묶기 또는 보상 액션.

---

## 🟡 Medium (UX 개선)

| # | 위치 | 이슈 |
|---|---|---|
| M1 | `ui/auth/LoginScreen.kt:84` | email trim 안 함 → 자동완성 끝 공백으로 `@hansung.ac.kr` 검증 실패 |
| M2 | `LoginScreen/SignUpScreen` 전체 | `imePadding()` 누락 → 키보드가 비번 필드/버튼 가림 |
| M3 | `ui/auth/SignUpScreen.kt:33-39`, `SetupProfileScreen.kt:57-68` | `remember`만 사용(rememberSaveable 아님) → 회전 시 입력 전체 손실 |
| M4 | `ui/auth/Pendingverificationscreen.kt:24` | 시스템 뒤로가기 처리 없음 → 부스 사용자가 뒤로 누르면 앱 종료 |
| M5 | `ui/auth/SetupProfileScreen.kt:236-250` | 나이/키 필드 KeyboardType 미지정 → 한글 입력 후 toInt() NFE |
| M6 | `ui/auth/SetupProfileScreen.kt:80-85` | `GetMultipleContents` deprecated, 이미지 압축/다운샘플링 없음 → OOM |
| M7 | `ui/auth/StudentIdUploadScreen.kt:43-47` | URI Persistable 권한 미획득 → 회전 후 SecurityException |
| M8 | `ui/team/MyPageViewModel.kt:88-100` | unchecked `as? List<String>` → mixed-type 배열 시 후속 ClassCastException |
| M9 | 전체 await() Firebase 호출 | timeout 없음 → 전시 와이파이 불안정 시 ViewModel 코루틴 무한 매달림. `withTimeoutOrNull(15_000)` 필요 |
| M10 | `ui/chat/ChatViewModel.kt:274-282` | 메시지 길이 제한 없음 → 1MB 초과 시 silent fail |
| M11 | `ui/feed/FeedViewModel.kt:88-95` | `while(true)` 무한 새로고침 — 화면 이탈 후에도 5분마다 호출. `repeatOnLifecycle` 권장 |
| M12 | `ui/call/CallScreen.kt:158-162, 310-314` | SurfaceView가 recompose마다 setupRemoteVideo 재호출 → 깜빡임/native crash 위험 |
| M13 | 모든 코루틴 `catch (e: Exception)` | `CancellationException` 잡아먹어 cancel 전파 실패. `if (e is CancellationException) throw e` 추가 |
| M14 | `MyFirebaseMessagingService.kt:50-66` | chatId 빈 문자열 incoming_call 페이로드 → FCM 서비스 크래시 |

---

## 🟢 Low (개선 권장)

- `data/repository/MeetingPlaceRepository.kt:50-55` — 네이버 API 시크릿 키 하드코딩 (보안)
- `data/repository/AdminRepository.kt:194` — `processingLikes`가 `mutableSetOf` (스레드 안전성 명시 권장)
- `ui/feed/ProfileEditScreen.kt:67` — `remember { FeedRepository() }` 화면 회전마다 cold load
- `ui/auth/AdminViewModel.kt:82-87` — `init`에서 fetch 동시 실행, 어드민 재로그인 시 데이터 stale
- `data/repository/FirebaseChatRepository.kt:63-67` — sender 이름 조회 직렬 await (그룹 채팅 N명이면 N번 왕복)

---

## ✅ False Positive 정정

- ❌ "AndroidManifest application name 불일치" — `Meety/Application.kt`의 패키지 선언이 `com.bugzero.meety`이고 클래스가 `MeetyApplication`이므로 매니페스트 `.MeetyApplication`과 일치. 파일 경로는 무관 (Kotlin은 file path != package OK).
- ❌ "MyFirebaseMessagingService 위치 어색" — 파일이 `java/` 직하에 있지만 패키지 선언이 `com.bugzero.meety`라 매니페스트와 일치. 동작상 문제 없음 (단, IDE에서 경로 정리하면 깔끔).

---

## 🎯 전시 직전 우선순위 체크리스트

**오늘 안에 반드시:**
- [ ] C4 — `Uri.encode(roomName)` 4곳 (NavGraph.kt:425, MainActivity.kt:198/290/315)
- [ ] C5 — ChatMessage `@DocumentId` 적용
- [ ] C8 — chatId 빈 문자열 가드 (MainActivity, FCM)
- [ ] C9 — 비번 확인 검증
- [ ] C6, C7 — `!!` 제거 (FeedRepository 2곳, UserRepository 1곳)
- [ ] H4 — 모임 생성 빈값 검증 + 중복 클릭 방지
- [ ] H2 — logout()에서 전체 state reset

**가능하면:**
- [ ] C1, C2, C3 — 통화 권한/엔진/destroy
- [ ] C10 — Pending verification polling 수정
- [ ] H1 — listener 정리
- [ ] H6 — HttpURLConnection disconnect()
- [ ] M9 — Firebase await에 timeout 추가

**전시 운영 팁:**
1. 시연용 계정을 미리 5~10개 만들어두고, 손님이 바뀔 때마다 강제 종료 → 재실행 (메모리 누수/listener 누수 회피)
2. 통화 기능은 데모 자제 (Agora 관련 이슈가 가장 많음)
3. 와이파이 장애 발생 시 즉시 앱 재실행
4. 어드민 계정으로 정기적으로 좀비 calls/likes 정리
