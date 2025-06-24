#!/bin/bash
# 프로덕션 모드 실행 스크립트

set -e

echo "🚀 프로덕션 모드로 앱 실행 시작..."

# Android SDK 환경변수 설정
source scripts/android-env.sh

echo "📱 기존 앱 종료..."
adb shell am force-stop com.myapp || true

echo "🧹 빌드 캐시 정리..."
./android/gradlew -p android clean

echo "🔨 프로덕션 릴리즈 빌드..."
./android/gradlew -p android assembleRelease

echo "📦 APK 설치..."
adb install android/app/build/outputs/apk/release/app-release.apk

echo "🚀 프로덕션 앱 실행..."
adb shell am start -n com.myapp/.MainActivity

echo "✅ 프로덕션 모드 실행 완료!"
echo "📊 현재 설정:"
echo "   API URL: https://auth.nodove.com"
echo "   빌드 타입: Release"
echo "   모드: 프로덕션"
