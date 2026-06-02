@echo off
chcp 65001 >nul
echo.
echo =============================================
echo   Meety Firebase 더미데이터 세팅 실행기
echo =============================================
echo.

REM serviceAccountKey.json 체크
if not exist "serviceAccountKey.json" (
    echo [오류] serviceAccountKey.json 파일이 없습니다!
    echo.
    echo  1. Firebase 콘솔 접속: https://console.firebase.google.com
    echo  2. meety-compose 프로젝트 선택
    echo  3. 프로젝트 설정 ^(톱니바퀴^) -^> 서비스 계정 탭
    echo  4. "새 비공개 키 생성" 클릭 -^> JSON 다운로드
    echo  5. 다운로드한 파일을 이 폴더에 serviceAccountKey.json 으로 저장
    echo.
    pause
    exit /b 1
)

REM Python 설치 확인
python --version >nul 2>&1
if errorlevel 1 (
    echo [오류] Python이 설치되어 있지 않습니다.
    echo https://www.python.org/downloads/ 에서 설치하세요.
    pause
    exit /b 1
)

REM firebase-admin 설치
echo [1/2] firebase-admin 패키지 설치 중...
pip install firebase-admin --quiet

echo [2/2] 더미데이터 세팅 실행 중...
echo.
python setup_dummy_data.py

echo.
pause
