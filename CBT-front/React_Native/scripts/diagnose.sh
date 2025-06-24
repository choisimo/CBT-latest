#!/bin/bash

echo "🔍 CBT Diary 진단 스크립트"
echo "========================="
echo ""

cd /home/nodove/workspace/CBT-Diary/React_Native-main/React_Native-main

echo "📍 1. 기본 환경"
echo "프로젝트 경로: $(pwd)"
echo "Node.js: $(node --version 2>/dev/null || echo '❌ 미설치')"
echo "npm: $(npm --version 2>/dev/null || echo '❌ 미설치')"
echo "React Native CLI: $(npx react-native --version 2>/dev/null | head -1 || echo '❌ 미설치')"

echo ""
echo "📍 2. 프로젝트 파일"
echo "package.json: $([ -f package.json ] && echo '✅' || echo '❌')"
echo "App.tsx: $([ -f App.tsx ] && echo '✅' || echo '❌')"
echo "android/gradlew: $([ -f android/gradlew ] && echo '✅' || echo '❌')"
echo "node_modules: $([ -d node_modules ] && echo '✅' || echo '❌')"

echo ""
echo "📍 3. Android 환경"
echo "ANDROID_HOME: ${ANDROID_HOME:-'❌ 미설정'}"
echo "adb 명령어: $(which adb 2>/dev/null || echo '❌ 없음')"
echo "java: $(java -version 2>&1 | head -1 2>/dev/null || echo '❌ 없음')"

echo ""
echo "📍 4. 포트 상태"
if lsof -i :8081 >/dev/null 2>&1; then
    echo "포트 8081: ⚠️  사용 중"
    echo "사용 중인 프로세스:"
    lsof -i :8081 2>/dev/null || echo "확인 불가"
else
    echo "포트 8081: ✅ 사용 가능"
fi

echo ""
echo "📍 5. 권한 확인"
echo "gradlew 실행권한: $([ -x android/gradlew ] && echo '✅' || echo '❌')"

echo ""
echo "📍 6. 추천 조치"
echo ""

# Android 환경 확인
if [ -z "$ANDROID_HOME" ]; then
    echo "🔧 Android SDK 설정 필요:"
    echo "1. Android Studio > SDK Manager에서 API 35 설치"
    echo "2. 환경변수 설정: export ANDROID_HOME=\$HOME/Android/Sdk"
    echo "3. PATH 추가: export PATH=\$PATH:\$ANDROID_HOME/platform-tools"
    echo ""
fi

# Metro 서버 상태 확인
if lsof -i :8081 >/dev/null 2>&1; then
    echo "🔧 포트 8081 정리:"
    echo "lsof -ti:8081 | xargs kill -9"
    echo ""
fi

# 실행 방법 제안
echo "🚀 권장 실행 순서 (원래 CBT Diary 앱):"
echo "1. Android Studio에서 에뮬레이터 실행"
echo "2. 터미널 1: npx react-native start"
echo "3. 터미널 2: npx react-native run-android"
echo ""
echo "또는: ./scripts/basic-run.sh 실행"
echo ""
echo "📱 실행 후 확인사항:"
echo "- CBT Diary 인증/네비게이션 구조"
echo "- AuthContext 및 RootNavigator 정상 작동"
echo "- 원래 앱 UI 표시"

echo ""
echo "✅ 진단 완료!"
