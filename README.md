<div align="center">

<br/>

<pre>
███╗   ███╗███████╗███████╗████████╗██╗   ██╗
████╗ ████║██╔════╝██╔════╝╚══██╔══╝╚██╗ ██╔╝
██╔████╔██║█████╗  █████╗     ██║    ╚████╔╝ 
██║╚██╔╝██║██╔══╝  ██╔══╝     ██║     ╚██╔╝  
██║ ╚═╝ ██║███████╗███████╗   ██║      ██║   
╚═╝     ╚═╝╚══════╝╚══════╝   ╚═╝      ╚═╝   
</pre>

### 🤝 한성대학교 대학생 팀 모임 매칭 서비스

[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)](https://firebase.google.com)
[![Gemini](https://img.shields.io/badge/Gemini_AI-4285F4?style=for-the-badge&logo=google&logoColor=white)](https://ai.google.dev)

<br/>

> **같은 관심사, 같은 목표를 가진 팀원을 찾고 있나요?**  
> Meety가 AI로 딱 맞는 팀을 추천해드릴게요 🎯

<br/>

---

</div>

<br/>

## ✨ 주요 기능

<br/>

| 기능 | 설명 | 상태 |
|------|------|------|
| 🪪 **학생 인증** | 한성대 이메일 인증 + 학생증 업로드로 신뢰할 수 있는 매칭 환경 | ✅ 완료 |
| 🔐 **로그인/회원가입** | Firebase Auth 기반 이메일 인증, 자동 로그인, 비밀번호 찾기 | ✅ 완료 |
| 👤 **프로필 설정** | MBTI · 관심사 · 음식 취향 · 프로필 사진 업로드 | ✅ 완료 |
| 🛡 **관리자 시스템** | 인증 승인/거절 · 유저 목록 · 차단 · 신고 처리 · 권한 부여 | ✅ 완료 |
| 🤖 **AI 팀 추천** | Gemini AI가 MBTI · 관심사 기반으로 나에게 맞는 팀 추천 | 🔨 개발 중 |
| 💜 **스와이프 매칭** | 좋아요 / 패스로 간편하게 팀 매칭 | 🔨 개발 중 |
| 💬 **실시간 채팅** | 매칭된 팀과 Firebase 기반 실시간 채팅 | 🔨 개발 중 |
| 📅 **시간표 공유** | 공강 시간 기반 모임 일정 조율 | 🔨 개발 중 |

<br/>

---

<br/>

## 📱 앱 화면

<br/>

<div align="center">

| 온보딩 | 로그인 | 회원가입 | 프로필 설정 |
|--------|--------|---------|------------|
| 슬라이드 애니메이션 | Firebase Auth | 3단계 이메일 인증 | 사진 업로드 |

| 학생증 인증 | 인증 대기 | 관리자 | 피드 |
|------------|---------|-------|------|
| Storage 업로드 | 자동 폴링 | 승인/거절/차단 | AI 추천 |

</div>

<br/>

---

<br/>

## 🛠 기술 스택

<br/>

**📱 Client**
- Language: Kotlin
- UI: Jetpack Compose
- Navigation: Navigation Compose
- Image: Coil
- Async: StateFlow / ViewModel

**🔥 Backend (Firebase)**
- Auth: Firebase Authentication (이메일 인증)
- Database: Cloud Firestore
- Storage: Firebase Storage (프로필/학생증 사진)
- Push: Firebase Cloud Messaging (개발 중)

**🤖 AI**
- Recommend: Google Gemini API (개발 중)

**🔧 Tools**
- Version: Git / GitHub
- Build: Gradle Kotlin DSL

<br/>

---

<br/>

## 🗂 프로젝트 구조

<br/>

    meety-android-compose/
    ├── app/src/main/java/com/bugzero/meety/
    │   ├── MainActivity.kt
    │   ├── navigation/
    │   │   └── NavGraph.kt
    │   └── ui/
    │       ├── auth/          # 로그인 · 온보딩 · 학생증 인증 · 관리자
    │       │   ├── AuthViewModel.kt
    │       │   ├── AdminViewModel.kt
    │       │   ├── LoginScreen.kt
    │       │   ├── SignUpScreen.kt
    │       │   ├── OnboardingScreen.kt
    │       │   ├── SetupProfileScreen.kt
    │       │   ├── StudentIdUploadScreen.kt
    │       │   ├── PendingVerificationScreen.kt
    │       │   └── AdminScreen.kt
    │       ├── feed/          # 피드 · AI 추천 · 팀 상세 (개발 중)
    │       ├── team/          # 내 모임 · 모임 생성 · 마이페이지 (개발 중)
    │       ├── chat/          # 채팅 목록 · 채팅방 · 시간표 (개발 중)
    │       └── theme/         # Color · Theme · Type
    └── res/
        ├── values/            # colors · strings · themes
        └── drawable/          # 아이콘 · 런처 이미지

<br/>

---

<br/>

## 🗃 Firebase 데이터 구조

<br/>

    users/{userId}
    ├── name, email, isVerified, isAdmin, isBanned
    ├── profileImages: List<String>   # Storage URL
    ├── studentIdImageUrl: String
    ├── age, department, mbti, bio, height, location
    ├── interests, foodLikes, foodDislikes: List<String>
    └── createdAt: Timestamp

    adminQueue/{requestId}
    ├── userId, userName, userEmail
    ├── studentIdImageUrl: String
    ├── status: pending / approved / rejected
    └── createdAt: Timestamp

    teams/{teamId}           # B 담당
    meetings/{meetingId}     # C 담당
    chats/{chatId}           # D 담당
    reports/{reportId}       # 신고 처리

<br/>

---

<br/>

## 🌿 브랜치 전략

<br/>

    main              최종 발표용 (직접 push 금지)
     └── dev          통합 테스트 브랜치
          ├── feature/auth-screen-A    팀장 · 로그인 · 인증 · 관리자 ✅
          ├── feature/feed-B           피드 · AI 추천 🔨
          ├── feature/team-C           내 모임 · 매칭 🔨
          └── feature/chat-D           채팅 · 시간표 · FCM 🔨

<br/>

---

<br/>

## 🗓 개발 기간

<br/>

    2026.03.12  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  2026.06.05

      1주차          2주차         3주차~5주차      6주차
      세팅+인증UI → Firebase연동 → AI+매칭+채팅 → 통합+발표
      ✅ 완료        ✅ 완료        🔨 진행중       📅 예정

<br/>

---

<br/>

## 👥 팀원 소개

<br/>

<div align="center">

| 역할 | 이름 | 담당 | 상태 |
|------|------|------|------|
| 👑 **A (팀장)** | 김지원 | Firebase 세팅 · 로그인 · 온보딩 · 학생증 인증 · 관리자 시스템 | ✅ 완료 |
| 🤖 **B** | 정우진 | 피드 · Gemini AI 추천 · 팀 상세 · 프로필 수정 | 🔨 개발 중 |
| 👥 **C** | 이상혁 | 내 모임 · 모임 생성 · 매칭 로직 · 마이페이지 | 🔨 개발 중 |
| 💬 **D** | 유예원 | 채팅 · 시간표 · FCM 푸시 알림 · UI 애니메이션 | 🔨 개발 중 |

</div>

<br/>

---

<br/>

<div align="center">

**BugZero 팀** · 한성대학교 캡스톤 디자인 2026

[![GitHub](https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/HSU-jiwonKim/meety-android-compose)

</div>
