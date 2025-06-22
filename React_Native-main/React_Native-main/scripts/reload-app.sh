#!/bin/bash

echo "🔄 CBT Diary 앱 리로드 스크립트"
echo "============================"

cd /home/nodove/workspace/CBT-Diary/React_Native-main/React_Native-main

# 1. Metro 서버 상태 확인
echo "📡 Metro 서버 상태 확인..."
if lsof -i :8081 >/dev/null 2>&1; then
    echo "✅ Metro 서버 실행 중"
else
    echo "❌ Metro 서버 중지됨. 재시작합니다..."
    
    # Metro 서버 시작
    npx react-native start --reset-cache &
    METRO_PID=$!
    echo "Metro PID: $METRO_PID"
    echo "⏳ 10초 대기..."
    sleep 10
fi

# 2. 에뮬레이터 상태 확인
echo "📱 에뮬레이터 상태 확인..."
adb devices

# 3. 앱 강제 종료 후 재실행
echo "🔄 앱 재실행..."
adb shell am force-stop com.myapp 2>/dev/null || true
sleep 2
adb shell am start -n com.myapp/.MainActivity

# 4. 로그 확인
echo "📋 앱 로그 확인..."
echo "다음 명령어로 로그를 확인할 수 있습니다:"
echo "adb logcat -s ReactNativeJS"
echo ""
echo "또는 React Native 로그:"
echo "npx react-native log-android"

echo ""
echo "✅ 리로드 완료!"
echo "💡 팁: 에뮬레이터에서 R키를 두 번 눌러서 수동 리로드할 수도 있습니다."
