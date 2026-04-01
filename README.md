<div align="center">

<br/>

```
███╗   ███╗███████╗███████╗████████╗██╗   ██╗
████╗ ████║██╔════╝██╔════╝╚══██╔══╝╚██╗ ██╔╝
██╔████╔██║█████╗  █████╗     ██║    ╚████╔╝ 
██║╚██╔╝██║██╔══╝  ██╔══╝     ██║     ╚██╔╝  
██║ ╚═╝ ██║███████╗███████╗   ██║      ██║   
╚═╝     ╚═╝╚══════╝╚══════╝   ╚═╝      ╚═╝   
```

### 🤝 대학생 팀 모임 매칭 서비스

[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
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

| 기능 | 설명 |
|------|------|
| 🤖 **AI 팀 추천** | Gemini AI가 MBTI·관심사 기반으로 나에게 맞는 팀 추천 |
| 💜 **스와이프 매칭** | 좋아요 / 패스로 간편하게 팀 매칭 |
| 💬 **실시간 채팅** | 매칭된 팀과 Firebase 기반 실시간 채팅 |
| 🪪 **학생 인증** | 학생증 업로드로 신뢰할 수 있는 매칭 환경 |
| 📅 **시간표 공유** | 공강 시간 기반 모임 일정 조율 |

<br/>

---

<br/>

## 📱 앱 화면

<br/>

<div align="center">

| 로그인 | 피드 (AI 추천) | 채팅 | 마이페이지 |
|--------|---------------|------|-----------|
| 🔐 | ✦ | 💬 | 👤 |
| Firebase Auth | Gemini AI 연동 | 실시간 채팅 | 프로필 관리 |

</div>

<br/>

---

<br/>

## 🛠 기술 스택

<br/>

```
📱 Client
├── Language     Kotlin
├── UI           XML Layout + ViewBinding
├── Navigation   Navigation Component
└── Async        Kotlin Coroutine

🔥 Backend (Firebase)
├── Auth         Firebase Authentication
├── Database     Cloud Firestore
├── Storage      Firebase Storage
└── Push         Firebase Cloud Messaging

🤖 AI
└── Recommend    Google Gemini API (gemini-pro)

🎨 UI/UX
└── Animation    Lottie

🔧 Tools
├── Version      Git / GitHub
└── Build        Gradle Groovy DSL
```

<br/>

---

<br/>

## 🗂 프로젝트 구조

<br/>

```
meety-android/
├── app/src/main/java/com/bugzero/meety/
│   ├── MainActivity.kt
│   └── ui/
│       ├── auth/          # 로그인 · 온보딩 · 학생증 인증
│       ├── feed/          # 피드 · AI 추천 · 팀 상세
│       ├── team/          # 내 모임 · 모임 생성 · 마이페이지
│       └── chat/          # 채팅 목록 · 채팅방 · 시간표
└── res/
    ├── layout/            # Fragment XML
    ├── navigation/        # nav_graph.xml
    ├── values/            # colors · strings · themes
    ├── drawable/          # 버튼 · 카드 배경
    └── font/              # Noto Sans KR
```

<br/>

---

<br/>

## 🌿 브랜치 전략

<br/>

```
main        최종 발표용 (직접 push 금지)
 └── dev    통합 테스트 브랜치
      ├── feature/A    팀장 · 로그인 · 인증
      ├── feature/B    피드 · AI 추천
      ├── feature/C    내 모임 · 매칭
      └── feature/D    채팅 · 시간표 · UI
```

<br/>

---

<br/>

## 🗓 개발 기간

<br/>

```
2025.03.12  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  2025.04.04

  1주차       2주차        3주차        4주차
  세팅+UI  →  Firebase  →  AI연동   →  발표준비
```

<br/>

---

<br/>

## 👥 팀원 소개

<br/>

<div align="center">

| 역할 | 담당 |
|------|------|
| 👑 **A (팀장)** | Firebase 세팅 · 로그인 · 온보딩 · 학생증 인증 |
| 🤖 **B** | 피드 · Gemini AI 추천 · 팀 상세 · 프로필 수정 |
| 👥 **C** | 내 모임 · 모임 생성 · 매칭 로직 · 마이페이지 |
| 💬 **D** | 채팅 · 시간표 · FCM · Lottie 애니메이션 |

</div>

<br/>

---

<br/>

<div align="center">

**BugZero 팀** · 한성대학교 캡스톤 디자인 2026

[![GitHub](https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/HSU-jiwonKim/meety-android)

</div>
