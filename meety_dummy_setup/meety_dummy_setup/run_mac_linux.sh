#!/bin/bash
echo ""
echo "============================================="
echo "  Meety Firebase 더미데이터 세팅 실행기"
echo "============================================="
echo ""

# serviceAccountKey.json 체크
if [ ! -f "serviceAccountKey.json" ]; then
    echo "[오류] serviceAccountKey.json 파일이 없습니다!"
    echo ""
    echo "  1. Firebase 콘솔 접속: https://console.firebase.google.com"
    echo "  2. meety-compose 프로젝트 선택"
    echo "  3. 프로젝트 설정(톱니바퀴) -> 서비스 계정 탭"
    echo "  4. '새 비공개 키 생성' 클릭 -> JSON 다운로드"
    echo "  5. 다운로드한 파일을 이 폴더에 serviceAccountKey.json 으로 저장"
    echo ""
    exit 1
fi

# Python 확인
if ! command -v python3 &>/dev/null; then
    echo "[오류] python3이 없습니다. 설치 후 다시 실행하세요."
    exit 1
fi

echo "[1/2] firebase-admin 설치 중..."
pip3 install firebase-admin --quiet

echo "[2/2] 더미데이터 세팅 실행..."
echo ""
python3 setup_dummy_data.py
