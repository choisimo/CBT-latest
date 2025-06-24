# Android SDK 환경변수 설정
# 이 파일을 수정한 후 'source scripts/android-env.sh' 실행

# 일반적인 Android SDK 경로들 (실제 설치 경로로 수정하세요)
# export ANDROID_HOME="$HOME/Android/Sdk"
# export ANDROID_HOME="/opt/android-sdk"
# export ANDROID_HOME="/usr/local/android-sdk"

# Android Studio를 통해 설치한 경우의 일반적인 경로
export ANDROID_HOME="$HOME/Android/Sdk"

# Android SDK 관련 PATH 설정
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$PATH:$ANDROID_HOME/emulator"
export PATH="$PATH:$ANDROID_HOME/platform-tools"
export PATH="$PATH:$ANDROID_HOME/tools"
export PATH="$PATH:$ANDROID_HOME/tools/bin"

echo "Android SDK 환경변수 설정됨:"
echo "ANDROID_HOME: $ANDROID_HOME"
echo ""
echo "SDK 경로 확인:"
if [ -d "$ANDROID_HOME" ]; then
    echo "✅ Android SDK 디렉토리 존재"
    if [ -f "$ANDROID_HOME/platform-tools/adb" ]; then
        echo "✅ ADB 도구 발견"
    else
        echo "❌ ADB 도구 없음"
    fi
    if [ -d "$ANDROID_HOME/platforms" ]; then
        echo "✅ Android 플랫폼 발견"
        ls "$ANDROID_HOME/platforms" 2>/dev/null || echo "플랫폼 목록을 읽을 수 없음"
    else
        echo "❌ Android 플랫폼 없음"
    fi
else
    echo "❌ Android SDK 디렉토리가 존재하지 않습니다: $ANDROID_HOME"
    echo ""
    echo "Android Studio를 열고 다음 단계를 따르세요:"
    echo "1. File > Settings (또는 Android Studio > Preferences)"
    echo "2. Appearance & Behavior > System Settings > Android SDK"
    echo "3. SDK Platforms 탭에서 Android 15 (API 35) 설치"
    echo "4. SDK Tools 탭에서 필요한 도구들 설치"
    echo "5. 설치 경로를 확인하고 이 파일의 ANDROID_HOME 수정"
fi
