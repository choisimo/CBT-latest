#!/bin/bash

echo "🚀 CBT Diary React Native 실행 스크립트 v2.0"
echo "============================================="
echo ""

# 현재 디렉토리로 이동
cd /home/nodove/workspace/CBT-Diary/React_Native-main/React_Native-main
echo "📁 작업 디렉토리: $(pwd)"

# 0. 사전 진단
echo "0️⃣ 시스템 진단..."
echo "Node.js: $(node --version 2>/dev/null || echo '❌ 미설치')"
echo "npm: $(npm --version 2>/dev/null || echo '❌ 미설치')"
echo "Java: $(java -version 2>&1 | head -1 || echo '❌ 미설치')"

# Android SDK 경로 자동 검색
ANDROID_PATHS=(
    "$HOME/Android/Sdk"
    "$HOME/.local/share/android-sdk"
    "/opt/android-sdk"
    "/usr/local/android-sdk"
    "$HOME/snap/android-studio/current/android-studio/bin"
)

FOUND_SDK=""
for path in "${ANDROID_PATHS[@]}"; do
    if [ -d "$path" ]; then
        echo "✅ 발견된 Android 경로: $path"
        if [ -f "$path/platform-tools/adb" ] || [ -d "$path/platform-tools" ]; then
            FOUND_SDK="$path"
            echo "🎯 유효한 Android SDK: $FOUND_SDK"
            break
        fi
    fi
done

# 1. Android SDK 환경변수 설정
echo "1️⃣ Android SDK 환경 설정..."
if [ -n "$FOUND_SDK" ]; then
    export ANDROID_HOME="$FOUND_SDK"
    export ANDROID_SDK_ROOT="$ANDROID_HOME"
    export PATH="$PATH:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin"
    echo "✅ Android SDK 설정: $ANDROID_HOME"
else
    echo "⚠️  Android SDK를 찾을 수 없습니다."
    echo "📋 해결방법:"
    echo "1. Android Studio 실행"
    echo "2. SDK Manager에서 Android 15 (API 35) 설치"
    echo "3. 설치 경로 확인 후 ANDROID_HOME 환경변수 설정"
    echo ""
    # 기본 경로로 시도
    export ANDROID_HOME="$HOME/Android/Sdk"
    export PATH="$PATH:$ANDROID_HOME/platform-tools"
fi

# ADB 연결 확인
echo "📱 ADB 연결 상태:"
if command -v adb >/dev/null 2>&1; then
    adb devices -l 2>/dev/null || echo "ADB 명령어 실행 실패"
else
    echo "❌ ADB 명령어를 찾을 수 없습니다."
fi

# 2. 기존 프로세스 정리
echo "2️⃣ 기존 프로세스 정리..."
pkill -f "react-native" 2>/dev/null || true
pkill -f "metro" 2>/dev/null || true
pkill -f "node.*metro" 2>/dev/null || true
sleep 2

# 3. Node.js 의존성 확인
echo "3️⃣ Node.js 의존성 확인..."
if [ ! -d "node_modules" ]; then
    echo "📦 npm install 실행 중..."
    npm install --legacy-peer-deps
else
    echo "✅ node_modules 존재"
fi

# 4. 간단한 테스트부터 시작
echo "4️⃣ 기본 설정 확인..."
if [ ! -f "android/gradlew" ]; then
    echo "❌ gradlew 파일이 없습니다."
    exit 1
fi

if [ ! -x "android/gradlew" ]; then
    echo "🔧 gradlew 실행 권한 부여..."
    chmod +x android/gradlew
fi

# 5. Metro 서버만 먼저 시작해보기
echo "5️⃣ Metro 서버 테스트..."
echo "📡 Metro 서버를 백그라운드로 시작합니다..."

# Metro 포트 확인
if lsof -i :8081 >/dev/null 2>&1; then
    echo "⚠️  포트 8081이 이미 사용 중입니다. 기존 프로세스를 종료합니다."
    lsof -ti:8081 | xargs kill -9 2>/dev/null || true
    sleep 2
fi

# Metro 시작
timeout 30s npx react-native start --port 8081 &
METRO_PID=$!
echo "Metro PID: $METRO_PID"

# Metro 시작 확인
echo "⏳ Metro 서버 시작 확인 (15초 대기)..."
sleep 15

if kill -0 $METRO_PID 2>/dev/null; then
    echo "✅ Metro 서버가 정상적으로 실행 중입니다."
    
    # 6. Android 빌드 시도
    echo "6️⃣ Android 빌드 및 실행..."
    echo "📱 에뮬레이터나 연결된 기기를 확인하세요."
    
    # 환경 정보 출력
    echo ""
    echo "🔍 최종 환경 정보:"
    echo "ANDROID_HOME: ${ANDROID_HOME:-'미설정'}"
    echo "Node.js: $(node --version 2>/dev/null || echo 'Not found')"
    echo "npm: $(npm --version 2>/dev/null || echo 'Not found')"
    echo "React Native CLI: $(npx react-native --version 2>/dev/null | head -1 || echo 'Not found')"
    
    # Android 실행 시도
    echo ""
    echo "🎯 Android 앱 빌드 시작..."
    
    # 더 자세한 로그와 함께 실행
    if npx react-native run-android --verbose --port 8081; then
        echo ""
        echo "🎉 성공! 앱이 실행되었습니다!"
    else
        echo ""
        echo "❌ Android 실행 실패"
        echo ""
        echo "🔧 문제 해결 방법:"
        echo "1. Android Studio에서 에뮬레이터 실행"
        echo "2. USB 디버깅이 활성화된 실제 기기 연결"
        echo "3. 'adb devices' 명령으로 기기 인식 확인"
        echo "4. Android SDK 설치 및 환경변수 설정 확인"
    fi
else
    echo "❌ Metro 서버 시작 실패"
    echo ""
    echo "🔧 대안:"
    echo "1. 수동으로 Metro 서버 시작: npx react-native start"
    echo "2. 별도 터미널에서 Android 실행: npx react-native run-android"
fi

# 7. 결과 및 정리
echo ""
echo "📝 참고사항:"
echo "- Metro 서버 PID: $METRO_PID"
echo "- Metro 중단: kill $METRO_PID"
echo "- Metro 웹페이지: http://localhost:8081"
echo "- 로그 확인: npx react-native log-android"
echo ""
echo "✅ 스크립트 실행 완료!"
