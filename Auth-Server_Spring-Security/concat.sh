#!/bin/bash

# 출력 파일 설정
backend_output="concat/backend.txt"
frontend_output="concat/frontend.txt"

# 디렉토리 설정
backend_src_dir="backend/src"
frontend_src_dir="frontend"

# 파일 초기화
> "$backend_output"
> "$frontend_output"

# 백엔드 코드 수집 함수
collect_backend_code() {
    echo "========== 백엔드 코드 수집 시작 =========="
    
    # Java 파일 찾기 (테스트 및 빌드 디렉토리 제외)
    find "$backend_src_dir" -type f -name "*.java" ! -path "*/test/*" ! -path "*/build/*" | sort | while read -r file; do
        echo "처리 중: $file"
        # 클래스 주석 (JavaDoc 스타일) 추출
        class_comment=$(sed -n '/\/\*\*/,/\*\//p' "$file" | head -n 10)
        # 파일 전체 내용 추출
        class_content=$(cat "$file")
        
        # 출력 파일에 기록
        echo "// 파일: $file" >> "$backend_output"
        if [ -n "$class_comment" ]; then
            echo "$class_comment" >> "$backend_output"
        fi
        echo "$class_content" >> "$backend_output"
        echo -e "\n\n// ===================================\n" >> "$backend_output"
    done
    
    echo "========== 백엔드 코드 수집 완료 =========="
}

# 프론트엔드 코드 수집 함수
collect_frontend_code() {
    echo "========== 프론트엔드 코드 수집 시작 =========="
    
    # JS, TS, Vue 파일 찾기 (node_modules 및 빌드 디렉토리 제외)
    find "$frontend_src_dir" -type f \( -name "*.js" -o -name "*.ts" -o -name "*.vue" \) ! -path "*/node_modules/*" ! -path "*/dist/*" ! -path "*/build/*" | sort | while read -r file; do
        echo "처리 중: $file"
        # 상단 주석 블록 추출
        top_comment=$(sed -n '/^\/\*/,/^\*\//p' "$file" | head -n 10)
        # 파일 전체 내용 추출
        file_content=$(cat "$file")
        
        # 출력 파일에 기록
        echo "// 파일: $file" >> "$frontend_output"
        if [ -n "$top_comment" ]; then
            echo "$top_comment" >> "$frontend_output"
        fi
        echo "$file_content" >> "$frontend_output"
        echo -e "\n\n// ===================================\n" >> "$frontend_output"
    done
    
    echo "========== 프론트엔드 코드 수집 완료 =========="
}

# 메인 실행 부분
echo "코드 수집 시작..."

# 백엔드 디렉토리 존재 확인
if [ -d "$backend_src_dir" ]; then
    collect_backend_code
else
    echo "경고: 백엔드 소스 디렉토리($backend_src_dir)를 찾을 수 없습니다."
fi

# 프론트엔드 디렉토리 존재 확인
if [ -d "$frontend_src_dir" ]; then
    collect_frontend_code
else
    echo "경고: 프론트엔드 디렉토리($frontend_src_dir)를 찾을 수 없습니다."
fi

# 결과 보고
echo "코드 수집 완료!"
if [ -f "$backend_output" ]; then
    backend_size=$(du -h "$backend_output" | cut -f1)
    echo "백엔드 코드($backend_output): $backend_size"
fi
if [ -f "$frontend_output" ]; then
    frontend_size=$(du -h "$frontend_output" | cut -f1)
    echo "프론트엔드 코드($frontend_output): $frontend_size"
fi

