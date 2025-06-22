#!/bin/bash

echo "🚀 React Native 디버깅 스크립트 시작..."
echo ""

# 1. 프로세스 정리
echo "1️⃣ 기존 프로세스 정리..."
pkill -f "react-native" 2>/dev/null || true
pkill -f "metro" 2>/dev/null || true
pkill -f "gradle" 2>/dev/null || true

# 2. ADB 상태 확인
echo ""
echo "2️⃣ ADB 기기 연결 상태:"
adb devices -l

# 3. 캐시 정리
echo ""
echo "3️⃣ 캐시 정리..."
cd /home/nodove/workspace/CBT-Diary/React_Native-main/React_Native-main

# React Native 캐시 정리
npx react-native clean 2>/dev/null || true
rm -rf node_modules/.cache 2>/dev/null || true
rm -rf /tmp/metro-* 2>/dev/null || true

# Android 캐시 정리
cd android
./gradlew clean
cd ..

# 4. Metro 서버 시작 (백그라운드)
echo ""
echo "4️⃣ Metro 서버 시작..."
npx react-native start --reset-cache &
METRO_PID=$!

# 잠시 대기
sleep 5

# 5. Android 앱 빌드 및 실행
echo ""
echo "5️⃣ Android 앱 실행..."
npx react-native run-android --verbose

echo ""
echo "✅ 스크립트 완료!"
echo "Metro PID: $METRO_PID"
echo "Metro를 중단하려면: kill $METRO_PID"
