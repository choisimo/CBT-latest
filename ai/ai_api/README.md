# OpenAI GPT API 서비스

OpenAI GPT API를 활용한 FastAPI 기반 웹 서비스입니다. RESTful API를 통해 AI 채팅 기능을 제공합니다.

## 📁 프로젝트 구조

```
ai_api/
├── api_server.py        # FastAPI 서버
├── openai_service.py    # OpenAI API 서비스 클래스
└── README.md           # 이 파일
```

## 🚀 주요 기능

- 🌐 **RESTful API**: HTTP 기반 AI 채팅 서비스
- 🔄 **스트리밍 응답**: Server-Sent Events로 실시간 응답
- 💾 **대화 기록 관리**: 세션 기반 대화 관리
- 🎯 **커스텀 프롬프트**: 시스템 프롬프트 지원
- 📊 **자동 문서화**: Swagger UI/ReDoc 지원
- 🛡️ **CORS 지원**: 웹 애플리케이션 통합 가능

## 🔧 서버 실행

### 1. 직접 실행
```bash
# 가상환경 활성화 후
uvicorn ai_api.api_server:app --reload --host 0.0.0.0 --port 8000
```

### 2. Docker로 실행
```bash
# Docker 이미지 빌드
docker build -t chatgpt-ai-service .

# 컨테이너 실행
docker run -p 8000:8000 -e OPENAI_API_KEY=your-api-key chatgpt-ai-service
```

### 3. Docker Compose로 실행
```bash
# 프로젝트 루트에서
docker-compose up ai-service
```

## 📡 API 엔드포인트

### 1. 서비스 상태 확인
```http
GET /
```
**응답:**
```json
{
  "service": "ChatGPT Clone AI Service",
  "status": "running",
  "version": "1.0.0",
  "timestamp": "2024-01-01T12:00:00"
}
```

### 2. 헬스 체크
```http
GET /health
```
**응답:**
```json
{
  "status": "healthy",
  "openai_api": "connected",
  "timestamp": "2024-01-01T12:00:00"
}
```

### 3. 일반 채팅
```http
POST /chat
```
**요청 본문:**
```json
{
  "message": "안녕하세요!",
  "conversation_history": [
    {"role": "user", "content": "이전 메시지"},
    {"role": "assistant", "content": "이전 응답"}
  ],
  "system_prompt": "당신은 도움이 되는 AI 어시스턴트입니다.",
  "model": "gpt-3.5-turbo",
  "temperature": 0.7,
  "max_tokens": 1000
}
```

**응답:**
```json
{
  "response": "안녕하세요! 무엇을 도와드릴까요?",
  "usage": {
    "prompt_tokens": 10,
    "completion_tokens": 15,
    "total_tokens": 25
  },
  "model": "gpt-3.5-turbo",
  "timestamp": "2024-01-01T12:00:00"
}
```

### 4. 스트리밍 채팅 시작
```http
POST /chat/stream
```
**요청 본문:**
```json
{
  "message": "긴 답변이 필요한 질문",
  "system_prompt": "자세히 설명해주세요.",
  "model": "gpt-3.5-turbo",
  "temperature": 0.7
}
```

**응답:**
```json
{
  "stream_id": "uuid-string",
  "status": "started"
}
```

### 5. 스트리밍 응답 받기
```http
GET /chat/stream/{stream_id}
```
**응답 (Server-Sent Events):**
```
event: chunk
data: {"type": "START", "stream_id": "uuid"}

event: chunk
data: {"type": "CHUNK", "content": "안녕"}

event: chunk
data: {"type": "CHUNK", "content": "하세요!"}

event: chunk
data: {"type": "END", "full_content": "안녕하세요!"}
```

### 6. 스트리밍 세션 중지
```http
DELETE /chat/stream/{stream_id}
```

### 7. 사용 가능한 모델 목록
```http
GET /models
```

## 📚 API 문서

서버 실행 후 다음 URL에서 자동 생성된 API 문서를 확인할 수 있습니다:

- **Swagger UI**: `http://localhost:8000/docs`
- **ReDoc**: `http://localhost:8000/redoc`
- **OpenAPI JSON**: `http://localhost:8000/openapi.json`

## 🔍 API 테스트 예제

### curl 예제
```bash
# 헬스 체크
curl http://localhost:8000/health

# 일반 채팅
curl -X POST http://localhost:8000/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "파이썬에 대해 설명해주세요",
    "temperature": 0.7
  }'

# 스트리밍 채팅 시작
curl -X POST http://localhost:8000/chat/stream \
  -H "Content-Type: application/json" \
  -d '{
    "message": "머신러닝에 대해 자세히 설명해주세요"
  }'
```

## 🔧 OpenAIService 클래스

### 주요 메서드
- `chat()`: 일반 채팅 (전체 응답 한 번에)
- `chat_stream()`: 스트리밍 채팅 (실시간 응답)
- `chat_with_history()`: 대화 기록 포함 채팅
- `chat_stream_with_history()`: 대화 기록 포함 스트리밍
- `generate_title()`: 대화 내용 기반 제목 생성
- `save_conversation()`: 대화 기록 저장
- `load_conversation()`: 대화 기록 불러오기
- `clear_conversation()`: 대화 기록 초기화
- `get_conversation_summary()`: 대화 통계

### 환경변수 지원
- `OPENAI_API_KEY`: OpenAI API 키 (필수)
- `DEFAULT_MODEL`: 기본 사용 모델 (기본값: gpt-3.5-turbo)
- `DEFAULT_TEMPERATURE`: 기본 창의성 수준 (기본값: 0.7)
- `DEFAULT_MAX_TOKENS`: 기본 최대 토큰 수 (기본값: 2000)

## ⚙️ 환경 변수 설정

```env
# 필수 환경 변수
OPENAI_API_KEY=your-openai-api-key

# 선택적 환경 변수
DEFAULT_MODEL=gpt-3.5-turbo
DEFAULT_TEMPERATURE=0.7
DEFAULT_MAX_TOKENS=2000
LOG_LEVEL=INFO
```

## 🛡️ 보안 고려사항

### 1. API 키 보안
- 환경 변수로 API 키 관리
- Docker secrets 사용 권장
- API 키 로깅 금지

### 2. CORS 설정
```python
app.add_middleware(
    CORSMiddleware,
    allow_origins=["https://yourdomain.com"],  # 운영 환경에서는 특정 도메인만 허용
    allow_credentials=True,
    allow_methods=["GET", "POST", "DELETE"],
    allow_headers=["*"],
)
```

### 3. Rate Limiting
- 요청 제한 설정 고려
- IP 기반 차단 구현
- 토큰 기반 인증 추가

## 📊 모니터링

### 1. 로깅
- 모든 요청/응답 로깅
- 에러 상황 상세 로깅
- 성능 메트릭 수집

### 2. 헬스 체크
- `/health` 엔드포인트 정기 확인
- OpenAI API 연결 상태 모니터링
- 리소스 사용량 모니터링

## 🚨 문제 해결

### API 연결 오류
```
ConnectionError: Failed to connect to OpenAI API
```
**해결방법:**
- API 키 확인
- 네트워크 연결 확인
- OpenAI 서비스 상태 확인

### 스트리밍 세션 오류
```
404: 스트리밍 세션을 찾을 수 없습니다
```
**해결방법:**
- 스트림 ID 확인
- 세션 만료 시간 확인
- 새로운 스트리밍 세션 시작

### 메모리 사용량 증가
**해결방법:**
- 스트리밍 세션 정리
- 메모리 제한 설정
- 가비지 컬렉션 최적화

## 📈 성능 최적화

### 1. 캐싱
- Redis 캐시 사용
- 응답 캐싱
- 토큰 캐싱

### 2. 비동기 처리
- 비동기 요청 처리
- 백그라운드 작업
- 웹소켓 지원

### 3. 로드 밸런싱
- 다중 인스턴스
- 자동 스케일링
- 헬스 체크 기반 라우팅

## 📄 라이센스

이 API 서비스는 교육 목적으로 자유롭게 사용할 수 있습니다.

---

**🎯 빠른 시작:**
1. `uvicorn ai_api.api_server:app --reload` 실행
2. `http://localhost:8000/docs` 에서 API 문서 확인
3. `/health` 엔드포인트로 서버 상태 확인
4. `/chat` 엔드포인트로 첫 번째 채팅 시도 