#!/bin/bash

# CBT-Diary App Deployment Script
# This script helps deploy the React Native app to Android devices

echo "🚀 CBT-Diary App Deployment Script"
echo "=================================="

APK_PATH="./android/app/build/outputs/apk/release/app-release.apk"

# Check if APK exists
if [ ! -f "$APK_PATH" ]; then
    echo "❌ Release APK not found at $APK_PATH"
    echo "Please build the release APK first:"
    echo "   cd android && ./gradlew assembleRelease"
    exit 1
fi

echo "✅ Found release APK: $APK_PATH"
echo "📱 APK Size: $(ls -lh $APK_PATH | awk '{print $5}')"

# Check connected devices
echo ""
echo "📱 Checking connected Android devices..."
DEVICES=$(adb devices | grep -v "List of devices" | grep "device$" | wc -l)

if [ $DEVICES -eq 0 ]; then
    echo "⚠️  No Android devices connected."
    echo ""
    echo "To install the APK:"
    echo "1. Connect an Android device via USB with Developer Options enabled"
    echo "2. Or start an Android emulator"
    echo "3. Run this script again"
    echo ""
    echo "Alternative: Copy the APK to your device and install manually:"
    echo "   APK Location: $APK_PATH"
    exit 1
fi

echo "✅ Found $DEVICES connected device(s)"
echo ""

# List devices
echo "📋 Connected devices:"
adb devices | grep "device$" | nl -v 0

echo ""
echo "🔧 Installing CBT-Diary app..."

# Install APK
if adb install -r "$APK_PATH"; then
    echo ""
    echo "🎉 CBT-Diary app installed successfully!"
    echo ""
    echo "📱 You can now:"
    echo "   • Find the app on your device"
    echo "   • Test the signup/login functionality"
    echo "   • Verify remote server connectivity"
    echo ""
    echo "🌐 App connects to: https://auth.nodove.com"
    echo "🛡️ Includes robust error handling for production use"
else
    echo ""
    echo "❌ Installation failed. Try:"
    echo "   • Enable 'Install from unknown sources' on your device"
    echo "   • Check if the device has enough storage space"
    echo "   • Try installing manually by copying the APK to your device"
    echo ""
    echo "APK Location: $APK_PATH"
fi
