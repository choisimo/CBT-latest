#!/bin/bash

echo "🧹 프로젝트 정리 스크립트"
echo "======================="
echo ""

cd /home/nodove/workspace/CBT-Diary/React_Native-main/React_Native-main

echo "📋 삭제할 파일들:"
echo "- SimpleApp.tsx (테스트 파일)"
echo "- *.sh (루트 디렉토리의 shell script들)"
echo ""

# SimpleApp.tsx 삭제
if [ -f "SimpleApp.tsx" ]; then
    rm -f SimpleApp.tsx
    echo "✅ SimpleApp.tsx 삭제됨"
else
    echo "ℹ️  SimpleApp.tsx 없음"
fi

# 루트 디렉토리의 shell script들 삭제
for script in *.sh; do
    if [ -f "$script" ]; then
        rm -f "$script"
        echo "✅ $script 삭제됨"
    fi
done

echo ""
echo "🔧 scripts 디렉토리 권한 설정..."
chmod +x scripts/*.sh 2>/dev/null || true

echo ""
echo "📂 최종 scripts 디렉토리 내용:"
ls -la scripts/

echo ""
echo "✅ 정리 완료!"
echo ""
echo "🚀 이제 다음 명령어로 앱을 실행할 수 있습니다:"
echo "   ./scripts/basic-run.sh"
echo ""
echo "📖 자세한 사용법은 다음을 참조하세요:"
echo "   scripts/README.md"
