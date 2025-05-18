package main

import (
	"bufio"
	"flag"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"strings"
)

func main() {
	// 명령줄 플래그 정의
	var (
		startDir   = flag.String("d", ".", "검색 시작 디렉토리 지정 (기본값: 현재 디렉토리)")
		outputFile = flag.String("o", "merged_source_files.txt", "출력 파일 지정 (기본값: merged_source_files.txt)")
		helpFlag   = flag.Bool("h", false, "도움말 표시")
	)

	// 긴 형식의 플래그 별칭 추가
	flag.StringVar(startDir, "directory", ".", "검색 시작 디렉토리 지정 (기본값: 현재 디렉토리)")
	flag.StringVar(outputFile, "output", "merged_source_files.txt", "출력 파일 지정 (기본값: merged_source_files.txt)")
	flag.BoolVar(helpFlag, "help", false, "도움말 표시")

	// 사용법 메시지 설정
	flag.Usage = func() {
		fmt.Fprintf(os.Stderr, "사용법: %s [옵션]\n", os.Args[0])
		fmt.Fprintln(os.Stderr, "옵션:")
		fmt.Fprintln(os.Stderr, "  -d, --directory DIR  검색 시작 디렉토리 지정 (기본값: 현재 디렉토리)")
		fmt.Fprintln(os.Stderr, "  -o, --output FILE    출력 파일 지정 (기본값: merged_source_files.txt)")
		fmt.Fprintln(os.Stderr, "  -h, --help           도움말 표시")
	}

	// 플래그 파싱
	flag.Parse()

	// 도움말 표시
	if *helpFlag {
		flag.Usage()
		os.Exit(0)
	}

	// 시작 디렉토리 유효성 검사
	dirInfo, err := os.Stat(*startDir)
	if err != nil || !dirInfo.IsDir() {
		fmt.Fprintf(os.Stderr, "오류: 디렉토리 '%s'가 존재하지 않습니다.\n", *startDir)
		os.Exit(1)
	}

	// 출력 파일 생성
	outFile, err := os.Create(*outputFile)
	if err != nil {
		fmt.Fprintf(os.Stderr, "오류: 출력 파일 '%s'을 생성할 수 없습니다: %v\n", *outputFile, err)
		os.Exit(1)
	}
	defer outFile.Close()

	// 버퍼 라이터 생성 (성능 향상)
	writer := bufio.NewWriter(outFile)
	defer writer.Flush()

	fmt.Printf("디렉토리 '%s'에서 .h 및 .cpp 파일을 검색하여 '%s'에 병합합니다...\n", *startDir, *outputFile)

	// 파일 카운터 초기화
	fileCount := 0

	// 디렉토리 탐색 및 파일 처리
	err = filepath.Walk(*startDir, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			fmt.Fprintf(os.Stderr, "디렉토리 탐색 중 오류 발생: %v\n", err)
			return err
		}

		// 파일이며 확장자가 .h 또는 .cpp인 경우에만 처리
		if !info.IsDir() && (strings.HasSuffix(path, ".h") || strings.HasSuffix(path, ".cpp")) {
			fileCount++

			// 진행 상황 표시
			fmt.Printf("처리 중: %s\n", path)

			// 파일 구분자 추가
			fmt.Fprintf(writer, "\n\n===== %s =====\n\n", path)

			// 파일 내용을 결과 파일에 추가
			if err := appendFileContent(path, writer); err != nil {
				fmt.Fprintf(os.Stderr, "파일 '%s' 처리 중 오류 발생: %v\n", path, err)
				// 오류가 있더라도 계속 진행
			}
		}
		return nil
	})

	if err != nil {
		fmt.Fprintf(os.Stderr, "파일 탐색 중 오류 발생: %v\n", err)
		os.Exit(1)
	}

	fmt.Printf("완료: %d개의 파일이 '%s'에 병합되었습니다.\n", fileCount, *outputFile)
}

// appendFileContent 함수는 소스 파일의 내용을 writer에 추가합니다.
func appendFileContent(filepath string, writer *bufio.Writer) error {
	srcFile, err := os.Open(filepath)
	if err != nil {
		return fmt.Errorf("파일 열기 오류: %w", err)
	}
	defer srcFile.Close()

	// 버퍼 리더 생성 (성능 향상)
	reader := bufio.NewReader(srcFile)

	// 파일 내용을 결과 파일에 복사
	_, err = io.Copy(writer, reader)
	if err != nil {
		return fmt.Errorf("파일 복사 오류: %w", err)
	}

	return nil
}
