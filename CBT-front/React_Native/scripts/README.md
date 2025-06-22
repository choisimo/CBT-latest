# 📂 CBT Diary Scripts Directory

이 디렉토리는 CBT Diary React Native 프로젝트의 모든 shell script들을 정리한 곳입니다.

## 📋 목차

- [🚀 실행 스크립트](#-실행-스크립트)
- [🔧 설정 및 진단 도구](#-설정-및-진단-도구)
- [🛠️ 유틸리티 스크립트](#️-유틸리티-스크립트)
- [📝 사용법](#-사용법)
- [⚠️ 주의사항](#️-주의사항)

---

## 🚀 실행 스크립트

### `basic-run.sh`
**CBT Diary 앱 기본 실행 스크립트**
- 환경 확인 → 포트 정리 → Metro 서버 시작 → Android 앱 실행
- 가장 간단하고 안정적인 실행 방법
- 초보자 권장

```bash
./scripts/basic-run.sh
```

### `simple-run.sh`
**간단한 실행 스크립트**
- 최소한의 설정으로 빠른 실행
- 환경변수 자동 설정
- 개발 중 빠른 테스트용

```bash
./scripts/simple-run.sh
```

### `run-android.sh`
**고급 Android 실행 스크립트**
- 상세한 환경 진단 및 자동 SDK 검색
- 다양한 오류 상황 대응
- 상세한 로그 및 디버깅 정보 제공

```bash
./scripts/run-android.sh
```

---

## 🔧 설정 및 진단 도구

### `diagnose.sh`
**종합 환경 진단 도구**
- Node.js, npm, React Native CLI 확인
- Android SDK 및 ADB 상태 점검
- 포트 사용 상태 확인
- 권한 및 파일 존재 여부 검사
- 문제 해결 방법 제안

```bash
./scripts/diagnose.sh
```

### `setup-android-sdk.sh`
**Android SDK 자동 설정 도구**
- Android SDK 경로 자동 검색
- 환경변수 자동 설정 (~/.bashrc, ~/.zshrc)
- 수동 경로 입력 지원

```bash
./scripts/setup-android-sdk.sh
```

### `android-env.sh`
**Android 환경변수 설정 파일**
- 환경변수 수동 설정용
- 다양한 설치 경로 예제 포함

```bash
source ./scripts/android-env.sh
```

---

## 🛠️ 유틸리티 스크립트

### `debug-android.sh`
**디버깅 전용 스크립트**
- 모든 캐시 정리 (React Native, Android Gradle)
- 기존 프로세스 강제 종료
- 상세한 로그와 함께 실행

```bash
./scripts/debug-android.sh
```

### `reload-app.sh`
**앱 리로드 스크립트**
- Metro 서버 상태 확인 및 재시작
- 에뮬레이터의 앱 강제 종료 후 재실행
- 개발 중 빠른 리로드용

```bash
./scripts/reload-app.sh
```

### `cleanup-summary.sh`
**프로젝트 정리 현황 요약**
- 현재 프로젝트 상태 정리
- 추가/수정/삭제된 파일 목록
- 실행 방법 안내

```bash
./scripts/cleanup-summary.sh
```

---

## 📝 사용법

### 🎯 처음 실행하는 경우
```bash
# 1. 환경 진단
./scripts/diagnose.sh

# 2. Android SDK 설정 (필요시)
./scripts/setup-android-sdk.sh

# 3. 앱 실행
./scripts/basic-run.sh
```

### 🔄 개발 중인 경우
```bash
# 빠른 실행
./scripts/simple-run.sh

# 문제가 있을 때
./scripts/debug-android.sh

# 앱만 리로드
./scripts/reload-app.sh
```

### 🐛 문제 해결
```bash
# 종합 진단
./scripts/diagnose.sh

# 환경 재설정
./scripts/setup-android-sdk.sh
source ./scripts/android-env.sh

# 캐시 정리 후 실행
./scripts/debug-android.sh
```

---

## ⚠️ 주의사항

### 📋 사전 요구사항
- **Node.js** (v18 이상 권장)
- **npm** 또는 **yarn**
- **Android Studio** (Android SDK 포함)
- **Java JDK** (Android Studio와 함께 설치됨)

### 🔧 필수 설정
1. **Android Studio 설치**
2. **Android SDK 설치** (API 35 권장)
3. **에뮬레이터 생성** 또는 **실제 기기 연결**
4. **USB 디버깅 활성화** (실제 기기 사용시)

### 💡 팁
- 스크립트 실행 전 **에뮬레이터 미리 실행** 권장
- 문제 발생시 `./scripts/diagnose.sh` 먼저 실행
- Metro 서버가 실행 중이라면 **새 터미널**에서 스크립트 실행
- 권한 문제시: `chmod +x scripts/*.sh`

### 🚨 문제 해결
1. **Android SDK 문제**: `./scripts/setup-android-sdk.sh` 실행
2. **포트 충돌**: `lsof -ti:8081 | xargs kill -9`
3. **캐시 문제**: `./scripts/debug-android.sh` 실행
4. **권한 문제**: `chmod +x android/gradlew`

---

## 📞 도움말

더 자세한 문제 해결 방법은 프로젝트 루트의 `TROUBLESHOOTING.md` 파일을 참조하세요.

### 🔗 관련 파일들
- `TROUBLESHOOTING.md` - 상세한 문제 해결 가이드
- `package.json` - 프로젝트 의존성
- `android/` - Android 네이티브 설정

---

*🎯 **목표**: 원활한 CBT Diary React Native 앱 개발 환경 구축*

*📅 **마지막 업데이트**: 2025년 6월 22일*
