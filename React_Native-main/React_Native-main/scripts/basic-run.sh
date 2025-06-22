#!/bin/bash

echo "🎯 CBT Diary - 단순 실행 모드"
echo "============================="
echo ""

cd /home/nodove/workspace/CBT-Diary/React_Native-main/React_Native-main

# 1. 환경 확인
echo "📋 환경 확인:"
echo "✓ Node.js: $(node --version 2>/dev/null || echo '❌ 없음')"
echo "✓ npm: $(npm --version 2>/dev/null || echo '❌ 없음')"
echo "✓ 프로젝트: $([ -f package.json ] && echo '✅ 정상' || echo '❌ 없음')"

# 2. 포트 정리
echo ""
echo "🧹 포트 8081 정리..."
lsof -ti:8081 | xargs kill -9 2>/dev/null || true
sleep 1

# 3. gradlew 권한 확인
echo "🔧 Gradle 권한 확인..."
chmod +x android/gradlew 2>/dev/null || true

# 4. 기본 실행 시도
echo ""
echo "🚀 기본 실행 시도..."
echo "방법 1: Metro만 먼저 시작"
echo ""

# Metro 서버 시작
echo "📡 Metro 서버 시작 중..."
npx react-native start --port 8081 --reset-cache &
METRO_PID=$!

echo "Metro PID: $METRO_PID"
echo "Metro 웹페이지: http://localhost:8081"
echo ""

# 잠시 대기
echo "⏳ 10초 대기 후 Android 실행..."
sleep 10

# Android 실행 시도
echo "📱 Android 실행 시도..."
echo ""

# 간단한 실행
if npx react-native run-android; then
    echo ""
    echo "🎉 성공! 앱이 실행 중입니다."
else
    echo ""
    echo "⚠️  실행 실패. 다음을 확인해주세요:"
    echo ""
    echo "📋 체크리스트:"
    echo "1. Android Studio에서 에뮬레이터 실행"
    echo "   - AVD Manager → 에뮬레이터 선택 → Start"
    echo ""
    echo "2. 또는 실제 Android 기기 연결"
    echo "   - USB 디버깅 활성화"
    echo "   - 기기 인식 확인: adb devices"
    echo ""
    echo "3. 수동 실행 방법:"
    echo "   터미널 1: npx react-native start"
    echo "   터미널 2: npx react-native run-android"
    echo ""
    echo "4. 웹 브라우저에서 Metro 확인:"
    echo "   http://localhost:8081"
fi

echo ""
echo "📝 현재 실행 중인 프로세스:"
echo "Metro PID: $METRO_PID"
echo "종료하려면: kill $METRO_PID"
echo ""
echo "🔚 스크립트 완료"
