# OpenAI GPT API 통합 서비스

OpenAI GPT API를 활용한 종합적인 AI 채팅 서비스입니다. CLI 도구와 웹 API 서버를 모두 제공하여 다양한 환경에서 활용할 수 있습니다.

## 🎯 서비스 개요

이 프로젝트는 OpenAI GPT API를 쉽게 사용할 수 있도록 두 가지 인터페이스를 제공합니다:

- **🤖 CLI 도구**: 터미널에서 직접 AI와 대화할 수 있는 명령줄 인터페이스
- **🌐 웹 API**: RESTful API를 통해 웹 애플리케이션에서 활용할 수 있는 FastAPI 서버

## 📁 프로젝트 구조

```
ai/
├── ai_cli/                   # CLI 도구 모듈
│   ├── chatbot.py           # 대화형 챗봇 인터페이스
│   ├── examples.py          # 다양한 사용 예제
│   └── README.md            # CLI 도구 상세 문서
├── ai_api/                  # API 서버 모듈
│   ├── api_server.py        # FastAPI 서버
│   ├── openai_service.py    # OpenAI API 서비스 클래스
│   └── README.md            # API 서버 상세 문서
├── main.py                  # CLI 통합 메뉴 프로그램
├── __init__.py              # 패키지 초기화
├── Dockerfile               # Docker 컨테이너 설정
├── requirements.txt         # 필요한 패키지 목록
├── setup_venv.py           # 가상환경 자동 설정 스크립트
└── README.md               # 이 파일 (서비스 개요)
```

## 🚀 주요 기능

### 🤖 CLI 도구 기능
- **대화형 챗봇**: 터미널에서 실시간 AI 채팅
- **다양한 예제**: 8가지 실용적인 사용 사례
- **대화 기록 관리**: 채팅 내용 저장/불러오기
- **고급 설정**: 모델 변경, 창의성 조정 등

### 🌐 웹 API 기능
- **RESTful API**: HTTP 기반 AI 채팅 서비스
- **스트리밍 응답**: Server-Sent Events로 실시간 응답
- **자동 문서화**: Swagger UI/ReDoc 지원
- **CORS 지원**: 웹 애플리케이션 통합 가능

### 🔧 공통 기능
- **커스텀 프롬프트**: 시스템 프롬프트로 AI 성격 조정
- **다중 모델 지원**: GPT-3.5, GPT-4 등 다양한 모델
- **환경 변수 지원**: 안전한 API 키 관리
- **Docker 지원**: 컨테이너 기반 배포

## 📦 빠른 시작

### 1. 환경 설정
```bash
# 프로젝트 클론 후
cd ai

# 가상환경 자동 설정
python setup_venv.py

# 가상환경 활성화 (Windows)
venv\Scripts\activate

# 가상환경 활성화 (Linux/Mac)
source venv/bin/activate
```

### 2. API 키 설정
```bash
# .env 파일에 OpenAI API 키 설정
echo "OPENAI_API_KEY=your-api-key-here" > .env
```

### 3. 서비스 실행

#### CLI 도구 실행
```bash
python main.py
```

#### 웹 API 서버 실행
```bash
uvicorn ai_api.api_server:app --reload --host 0.0.0.0 --port 8000
```

#### Docker로 실행
```bash
# 프로젝트 루트에서
docker-compose up ai-service
```

## 📂 모듈별 상세 문서

### 🤖 CLI 도구 (ai_cli/)
터미널 기반 대화형 인터페이스와 예제 모음
- **상세 문서**: [ai_cli/README.md](ai_cli/README.md)
- **주요 기능**: 대화형 챗봇, 다양한 예제, 대화 기록 관리

### 🌐 API 서비스 (ai_api/)
FastAPI 기반 웹 서비스와 OpenAI 서비스 클래스
- **상세 문서**: [ai_api/README.md](ai_api/README.md)
- **주요 기능**: RESTful API, 스트리밍 응답, 자동 문서화



## ⚙️ 환경 변수

```env
# 필수 설정
OPENAI_API_KEY=your-openai-api-key

# 선택적 설정
DEFAULT_MODEL=gpt-3.5-turbo
DEFAULT_TEMPERATURE=0.7
DEFAULT_MAX_TOKENS=2000
LOG_LEVEL=INFO
```

## 🛡️ 보안 고려사항

- **API 키 보안**: 환경 변수로 안전하게 관리
- **CORS 설정**: 웹 애플리케이션 통합 시 적절한 도메인 제한
- **Rate Limiting**: API 호출 제한으로 비용 관리
- **로깅**: 민감한 정보 제외한 적절한 로깅

## 📊 모니터링

- **헬스 체크**: `/health` 엔드포인트로 서비스 상태 확인
- **로깅**: 구조화된 로그로 서비스 모니터링
- **메트릭**: API 사용량 및 성능 지표 수집

## 🚨 문제 해결

### 일반적인 문제
- **API 키 오류**: `.env` 파일 및 환경 변수 확인
- **패키지 오류**: 가상환경 활성화 및 패키지 재설치
- **포트 충돌**: 다른 포트 사용 또는 기존 프로세스 종료

### 지원
- **문서**: 각 모듈별 상세 README 참조
- **예제**: `ai_cli/examples.py`의 다양한 사용 예제
- **API 문서**: `http://localhost:8000/docs` (서버 실행 시)

## 📄 라이센스

이 프로젝트는 교육 및 연구 목적으로 자유롭게 사용할 수 있습니다.

---

**🎯 추천 시작 방법:**
1. **CLI 체험**: `python main.py` → 메뉴 3번에서 예제 실행
2. **API 테스트**: 서버 실행 후 `http://localhost:8000/docs`에서 API 테스트
3. **통합 개발**: 각 모듈의 상세 문서를 참조하여 프로젝트에 통합 