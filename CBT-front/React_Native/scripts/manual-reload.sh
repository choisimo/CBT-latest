#!/bin/bash

# React Native Hot Reload Helper Script
echo "🔄 Manual Reload for React Native App..."

# Method 1: Send reload broadcast
echo "📡 Sending reload broadcast..."
adb shell am broadcast -a com.facebook.react.development.RELOAD

# Method 2: Send to specific app
echo "📱 Sending reload to com.myapp..."
adb shell "am broadcast -a com.facebook.react.development.RELOAD -n com.myapp/.MainActivity"

echo "✅ Manual reload commands sent!"
echo ""
echo "💡 Alternative methods:"
echo "   - Open dev menu: adb shell input keyevent 82"
echo "   - Press 'r' in Metro terminal"
echo "   - Double-tap R in emulator"
