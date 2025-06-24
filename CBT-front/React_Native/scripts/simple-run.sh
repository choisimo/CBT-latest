#!/bin/bash

echo "🚀 CBT Diary - 간단 실행 스크립트"
echo "=================================="

# 디렉토리 확인
cd /home/nodove/workspace/CBT-Diary/React_Native-main/React_Native-main
echo "📁 현재 디렉토리: $(pwd)"

# Android 환경변수 설정
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$PATH:$ANDROID_HOME/platform-tools"

echo "🔧 환경변수 설정 완료"
echo "ANDROID_HOME: $ANDROID_HOME"

# 기존 프로세스 정리
echo "🧹 기존 프로세스 정리..."
pkill -f "metro" 2>/dev/null || true
sleep 1

# Metro 서버 시작 (백그라운드)
echo "🚀 Metro 서버 시작..."
npx react-native start --port 8081 &
METRO_PID=$!
echo "Metro PID: $METRO_PID"

# 잠시 대기
echo "⏳ 5초 대기..."
sleep 5

# Android 실행
echo "📱 Android 앱 실행..."
npx react-native run-android

echo ""
echo "✅ 완료!"
echo "Metro PID: $METRO_PID (종료하려면: kill $METRO_PID)"
