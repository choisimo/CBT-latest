#!/bin/bash

echo "🧹 CBT Diary 프로젝트 정리 및 원상복구"
echo "======================================="
echo ""

cd /home/nodove/workspace/CBT-Diary/React_Native-main/React_Native-main

echo "📋 추가된 파일들:"
echo "✅ 실행 스크립트들:"
echo "  - scripts/run-android.sh (개선된 실행)"
echo "  - scripts/basic-run.sh (간단한 실행)"
echo "  - scripts/reload-app.sh (앱 리로드)"
echo "  - scripts/debug-android.sh (디버깅)"
echo ""
echo "✅ 설정/진단 도구들:"
echo "  - scripts/diagnose.sh (환경 진단)"
echo "  - scripts/setup-android-sdk.sh (SDK 설정)"
echo "  - scripts/android-env.sh (환경변수)"
echo ""
echo "✅ 문서:"
echo "  - TROUBLESHOOTING.md (문제 해결)"
echo ""

echo "🔄 복원된 파일들:"
echo "  - App.tsx → 원래 CBT Diary 구조"
echo "  - index.js → App 컴포넌트 사용"
echo ""

echo "❌ 삭제된 파일들:"
echo "  - SimpleApp.tsx (테스트용)"
echo ""

echo "🎯 현재 앱 구조:"
echo "  App.tsx"
echo "  ├── AuthProvider"
echo "  ├── GestureHandlerRootView"
echo "  └── RootNavigator"
echo "      ├── AuthContext 확인"
echo "      └── AppStack / AuthStack"
echo ""

echo "🚀 실행 방법:"
echo "1. ./scripts/diagnose.sh     # 환경 확인"
echo "2. ./scripts/basic-run.sh    # 앱 실행"
echo ""
echo "또는 수동 실행:"
echo "1. npx react-native start"
echo "2. npx react-native run-android"
echo ""

echo "✅ 정리 완료! 원래 CBT Diary 앱으로 복원되었습니다."
