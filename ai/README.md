# CBT Diary AI Service

**OpenAI GPT API와 MongoDB를 활용한 다이어리 AI 분석 서비스**

## 🎯 주요 기능

- **AI 다이어리 분석**: OpenAI GPT를 활용한 감정 분석 및 인지 왜곡 분석
- **MongoDB 저장**: 분석 결과를 MongoDB에 구조화하여 저장
- **RESTful API**: Auth-server와 통합 가능한 API 제공
- **실시간 스트리밍**: 스트리밍 채팅 지원
- **감정 통계**: 사용자별 감정 분석 통계 제공

## 🏗️ 아키텍처

```
Auth-server (MariaDB) ←→ AI Service (FastAPI + MongoDB)
                            ↓
                        OpenAI GPT API
```

## 📦 설치 및 실행

### 1. 의존성 설치
```bash
pip install -r requirements.txt
```

### 2. 환경변수 설정
`.env` 파일 생성:
```bash
# OpenAI API 설정
OPENAI_API_KEY=your-openai-api-key-here

# MongoDB 설정
MONGODB_URL=mongodb://localhost:27017
MONGODB_DATABASE=ai_cbt_diary

# 서비스 설정
AI_SERVICE_HOST=0.0.0.0
AI_SERVICE_PORT=8000
```

### 3. MongoDB 실행
```bash
# Docker로 MongoDB 실행
docker run -d -p 27017:27017 --name mongodb mongo:latest

# 또는 로컬에 설치된 MongoDB 사용
mongod
```

### 4. AI 서버 시작
```bash
python run_ai_server.py
```

## 📖 API 사용법

### 다이어리 분석
```bash
curl -X POST "http://localhost:8000/diary/analyze" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 123,
    "diaryId": 456,
    "title": "오늘의 일기",
    "content": "오늘은 정말 힘든 하루였다...",
    "weather": "맑음"
  }'
```

### 사용자 다이어리 목록 조회
```bash
curl "http://localhost:8000/user/123/diaries?limit=10"
```

### 감정 통계 조회
```bash
curl "http://localhost:8000/user/123/emotions/stats"
```

## 🗄️ MongoDB 컬렉션 구조

### diaries 컬렉션
```javascript
{
  "_id": "6492a48f5e3b2e1f8a7b3d9c",
  "userId": 123,
  "diaryId": 456,
  "title": "오늘의 일기",
  "content": "오늘은 정말 힘든 하루였다...",
  "weather": "맑음",
  "createdAt": "2025-06-21T10:00:00Z",
  "updatedAt": "2025-06-21T10:00:00Z",
  "report": {
    "status": "COMPLETED",
    "analysisDate": "2025-06-21T10:01:00Z",
    "emotions": [
      {"name": "슬픔", "score": 0.8},
      {"name": "불안", "score": 0.6}
    ],
    "cognitiveDistortions": [
      {
        "type": "극단적 사고",
        "originalSentence": "정말 힘든 하루였다",
        "alternativeThought": "힘들었지만 극복할 수 있는 하루였다"
      }
    ],
    "solutions": ["충분한 휴식 취하기", "긍정적인 활동 찾기"],
    "confidence": 0.85,
    "processingTime": 2.34
  }
}
```

## 🔧 Auth-server 통합

Auth-server에서 AI 서비스 호출:

```java
@Service
public class AiServiceClient {
    
    @Autowired
    private WebClient webClient;
    
    public Mono<AiAnalysisResponse> analyzeDiary(Long diaryId, String content) {
        DiaryAnalysisRequest request = DiaryAnalysisRequest.builder()
            .userId(getCurrentUserId())
            .diaryId(diaryId)
            .title("일기 제목")
            .content(content)
            .build();
            
        return webClient.post()
            .uri("http://localhost:8000/diary/analyze")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(AiAnalysisResponse.class);
    }
}
```

## 🎮 개발 및 테스트

### 개발 모드 실행
```bash
python run_ai_server.py
```

### API 문서 확인
서버 실행 후 브라우저에서:
- Swagger UI: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

### MongoDB 데이터 확인
```bash
# MongoDB 연결
mongo ai_cbt_diary

# 컬렉션 조회
db.diaries.find().pretty()
```

## 📝 주요 파일 구조

```
ai/
├── ai_api/
│   ├── api_server.py      # FastAPI 서버
│   ├── mongo_service.py   # MongoDB 서비스
│   ├── models.py          # 데이터 모델
│   ├── openai_service.py  # OpenAI 서비스
│   └── prompts.py         # AI 프롬프트
├── config.py              # 환경 설정
├── run_ai_server.py       # 서버 실행 스크립트
├── requirements.txt       # 의존성
└── README.md
```

## 🚀 배포

### Docker 배포
```bash
docker build -t cbt-diary-ai .
docker run -p 8000:8000 cbt-diary-ai
```

### 환경별 설정
- `개발`: MongoDB 로컬, OpenAI API 테스트 키
- `운영`: MongoDB Atlas, OpenAI API 운영 키

---

**Made with ❤️ for CBT Diary Project**