#!/bin/bash

echo "🔧 Android SDK 환경 설정 스크립트"
echo "=================================="
echo ""

# Android Studio 일반적인 설치 경로들
POSSIBLE_PATHS=(
    "$HOME/Android/Sdk"
    "/opt/android-sdk"
    "/usr/local/android-sdk" 
    "$HOME/.local/share/android-sdk"
    "/snap/android-studio/current/android-studio/jbr"
    "$HOME/android-studio/jbr"
)

echo "🔍 Android SDK 경로 검색 중..."
ANDROID_SDK_PATH=""

for path in "${POSSIBLE_PATHS[@]}"; do
    if [ -d "$path" ]; then
        echo "✅ 발견: $path"
        if [ -f "$path/platform-tools/adb" ] || [ -d "$path/platforms" ]; then
            ANDROID_SDK_PATH="$path"
            echo "📍 유효한 Android SDK: $ANDROID_SDK_PATH"
            break
        fi
    fi
done

if [ -z "$ANDROID_SDK_PATH" ]; then
    echo "❌ Android SDK를 찾을 수 없습니다."
    echo ""
    echo "📋 Android SDK 수동 설치 방법:"
    echo "1. Android Studio 실행"
    echo "2. Configure > SDK Manager 또는 More Actions > SDK Manager"
    echo "3. SDK Platforms 탭에서 Android 15 (API 35) 설치"
    echo "4. SDK Tools 탭에서 다음 항목들 설치:"
    echo "   - Android SDK Build-Tools"
    echo "   - Android SDK Platform-Tools"
    echo "   - Android SDK Tools"
    echo ""
    echo "5. SDK 설치 경로 확인 (보통 ~/Android/Sdk)"
    echo ""
    read -p "Android SDK 설치 경로를 직접 입력하세요 (엔터로 건너뛰기): " MANUAL_PATH
    if [ ! -z "$MANUAL_PATH" ] && [ -d "$MANUAL_PATH" ]; then
        ANDROID_SDK_PATH="$MANUAL_PATH"
    fi
fi

if [ ! -z "$ANDROID_SDK_PATH" ]; then
    echo ""
    echo "🎉 Android SDK 발견: $ANDROID_SDK_PATH"
    echo ""
    echo "📝 환경변수 설정을 ~/.bashrc와 ~/.zshrc에 추가합니다..."
    
    # 환경변수 설정
    ENV_SETUP="
# Android SDK 환경변수
export ANDROID_HOME=\"$ANDROID_SDK_PATH\"
export ANDROID_SDK_ROOT=\"\$ANDROID_HOME\"
export PATH=\"\$PATH:\$ANDROID_HOME/emulator\"
export PATH=\"\$PATH:\$ANDROID_HOME/platform-tools\"
export PATH=\"\$PATH:\$ANDROID_HOME/tools\"
export PATH=\"\$PATH:\$ANDROID_HOME/tools/bin\"
"

    # .bashrc에 추가
    if [ -f "$HOME/.bashrc" ]; then
        if ! grep -q "ANDROID_HOME" "$HOME/.bashrc"; then
            echo "$ENV_SETUP" >> "$HOME/.bashrc"
            echo "✅ ~/.bashrc에 환경변수 추가됨"
        else
            echo "ℹ️  ~/.bashrc에 이미 Android 환경변수가 존재합니다"
        fi
    fi

    # .zshrc에 추가
    if [ -f "$HOME/.zshrc" ]; then
        if ! grep -q "ANDROID_HOME" "$HOME/.zshrc"; then
            echo "$ENV_SETUP" >> "$HOME/.zshrc"
            echo "✅ ~/.zshrc에 환경변수 추가됨"
        else
            echo "ℹ️  ~/.zshrc에 이미 Android 환경변수가 존재합니다"
        fi
    fi

    echo ""
    echo "🔄 환경변수 적용 중..."
    export ANDROID_HOME="$ANDROID_SDK_PATH"
    export ANDROID_SDK_ROOT="$ANDROID_HOME"
    export PATH="$PATH:$ANDROID_HOME/emulator"
    export PATH="$PATH:$ANDROID_HOME/platform-tools"
    export PATH="$PATH:$ANDROID_HOME/tools"
    export PATH="$PATH:$ANDROID_HOME/tools/bin"

    echo "✅ 환경변수 설정 완료!"
    echo ""
    echo "📋 설정된 환경변수:"
    echo "ANDROID_HOME: $ANDROID_HOME"
    echo "PATH: $PATH"
    echo ""
    echo "🎯 다음 단계:"
    echo "1. 새로운 터미널을 열거나 'source ~/.zshrc' 실행"
    echo "2. 'npx react-native doctor' 실행하여 설정 확인"
    echo "3. 'npx react-native run-android' 실행"
    
else
    echo ""
    echo "❌ Android SDK를 찾을 수 없습니다."
    echo "Android Studio를 통해 SDK를 설치해주세요."
fi

echo ""
echo "🏁 설정 스크립트 완료!"
