# 일기 분석 API 문서

## 개요
CBT-Diary 프로젝트의 일기 분석 및 관리 API입니다. 사용자가 작성한 일기를 AI로 분석하여 감정 정보, 요약, 코칭 메시지를 제공합니다.

**Base URL**: `/api/diary`

## 공통 응답 형식

### 성공 응답
- **Status Code**: `200 OK`
- **Content-Type**: `application/json`

### 오류 응답
- **Status Code**: `400 Bad Request`, `404 Not Found`, `500 Internal Server Error`
- **Content-Type**: `application/json`

---

## API 엔드포인트

### 1. 일기 분석 (CREATE)

**요청**
```http
POST /api/diary/analyze
Content-Type: application/json
```

**요청 본문 (DiaryRequestDto)**
```json
{
  "title": "오늘의 일기",
  "content": "오늘은 정말 힘든 하루였다. 회사에서 프로젝트가 잘 안되어서 스트레스를 많이 받았다.",
  "userId": "2"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `title` | String | ✅ | 일기 제목 |
| `content` | String | ✅ | 일기 내용 |
| `userId` | String | ✅ | 사용자 ID |

**응답 (AIResponseDto)**
```json
{
  "id": "64a7b8c9d1e2f3g4h5i6j7k8",
  "userId": "1",
  "diaryTitle": "오늘의 일기",
  "diaryContent": "오늘은 정말 힘든 하루였다. 회사에서 프로젝트가 잘 안되어서 스트레스를 많이 받았다.",
  "emotions": [
    {
      "category": "스트레스",
      "intensity": 7
    },
    {
      "category": "피로",
      "intensity": 6
    }
  ],
  "summary": "업무로 인한 스트레스와 피로감을 경험한 하루",
  "coaching": "힘든 하루를 보내셨군요. 스트레스를 받을 때는 잠시 휴식을 취하고 깊게 숨을 쉬어보세요. 내일은 더 나은 하루가 될 것입니다.",
  "createdAt": "2024-01-15T14:30:00",
  "updatedAt": "2024-01-15T14:30:00"
}
```

---

### 2. 전체 조회 (READ ALL)

**요청**
```http
GET /api/diary/responses/{userId}
```

**경로 매개변수**
| 매개변수 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `userId` | String | ✅ | 사용자 ID |

**응답**
```json
[
  {
    "id": "64a7b8c9d1e2f3g4h5i6j7k8",
    "userId": "2",
    "diaryTitle": "오늘의 일기",
    "diaryContent": "오늘은 정말 힘든 하루였다...",
    "emotions": [
      {
        "category": "슬픔",
        "intensity": 7
      }
    ],
    "summary": "업무로 인한 스트레스와 피로감을 경험한 하루",
    "coaching": "힘든 하루를 보내셨군요...",
    "createdAt": "2024-01-15T14:30:00",
    "updatedAt": "2024-01-15T14:30:00"
  }
]
```

---

### 3. 단건 조회 (READ ONE)

**요청**
```http
GET /api/diary/response/{id}
```

**경로 매개변수**
| 매개변수 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `id` | String | ✅ | AI 응답 ID |

**응답**
- **성공 시**: AIResponseDto 객체 (위와 동일한 구조)
- **실패 시**: `404 Not Found` (해당 ID의 응답이 없을 경우)

---

### 4. 날짜별 조회 (READ BY DATE)

**요청**
```http
GET /api/diary/responses/{userId}/range?startDate=2024-01-01&endDate=2024-01-31
```

**경로 매개변수**
| 매개변수 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `userId` | String | ✅ | 사용자 ID |

**쿼리 매개변수**
| 매개변수 | 타입 | 필수 | 설명 | 형식 |
|----------|------|------|------|------|
| `startDate` | String | ✅ | 조회 시작 날짜 | `yyyy-MM-dd` |
| `endDate` | String | ✅ | 조회 종료 날짜 | `yyyy-MM-dd` |

**응답**
```json
[
  {
    "id": "64a7b8c9d1e2f3g4h5i6j7k8",
    "userId": "1",
    "diaryTitle": "오늘의 일기",
    "diaryContent": "오늘은 정말 힘든 하루였다...",
    "emotions": [
      {
        "category": "불안",
        "intensity": 7
      }
    ],
    "summary": "업무로 인한 스트레스와 피로감을 경험한 하루",
    "coaching": "힘든 하루를 보내셨군요...",
    "createdAt": "2024-01-15T14:30:00",
    "updatedAt": "2024-01-15T14:30:00"
  }
]
```

---

### 5. 수정 (UPDATE)

**요청**
```http
PUT /api/diary/response/{id}
Content-Type: application/json
```

**경로 매개변수**
| 매개변수 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `id` | String | ✅ | AI 응답 ID |

**요청 본문 (AIResponseDto)**
```json
{
  "diaryTitle": "수정된 일기 제목",
  "diaryContent": "수정된 일기 내용"
}
```

**응답**
```json
{
  "id": "64a7b8c9d1e2f3g4h5i6j7k8",
  "userId": "2",
  "diaryTitle": "수정된 일기 제목",
  "diaryContent": "수정된 일기 내용",
  "emotions": [
    {
      "category": "불안",
      "intensity": 7
    }
  ],
  "summary": "업무로 인한 스트레스와 피로감을 경험한 하루",
  "coaching": "힘든 하루를 보내셨군요...",
  "createdAt": "2024-01-15T14:30:00",
  "updatedAt": "2024-01-15T16:45:00"
}
```

---

### 6. 삭제 (DELETE)

**요청**
```http
DELETE /api/diary/response/{id}
```

**경로 매개변수**
| 매개변수 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `id` | String | ✅ | AI 응답 ID |

**응답**
```json
"삭제 완료"
```

---

### 7. 상태 확인 (Health Check)

**요청**
```http
GET /api/diary/health
```

**응답**
```json
"Diary API 정상 동작"
```

---

## 데이터 모델

### AIResponseDto
```json
{
  "id": "String - AI 응답 고유 ID",
  "userId": "String - 사용자 ID",
  "diaryTitle": "String - 일기 제목",
  "diaryContent": "String - 일기 내용",
  "emotions": [
    {
      "category": "String - 감정 카테고리 (예: 스트레스, 기쁨, 슬픔)",
      "intensity": "Integer - 감정 강도 (1-10)"
    }
  ],
  "summary": "String - AI가 생성한 일기 요약",
  "coaching": "String - AI가 생성한 코칭 메시지",
  "createdAt": "LocalDateTime - 생성 시간",
  "updatedAt": "LocalDateTime - 수정 시간"
}
```

### DiaryRequestDto
```json
{
  "title": "String - 일기 제목 (필수)",
  "content": "String - 일기 내용 (필수)",
  "userId": "String - 사용자 ID (선택)"
}
```

---

## 오류 코드

| HTTP 상태 코드 | 설명 |
|---------------|------|
| `200 OK` | 요청 성공 |
| `400 Bad Request` | 잘못된 요청 (필수 필드 누락, 잘못된 형식) |
| `404 Not Found` | 요청한 리소스를 찾을 수 없음 |
| `500 Internal Server Error` | 서버 내부 오류 (AI 서버 연결 실패 등) |

---

## 사용 예시

### 일기 분석 요청 예시
```bash
curl -X POST http://localhost:8080/api/diary/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "title": "힘든 하루",
    "content": "오늘은 업무가 너무 많아서 스트레스를 많이 받았다. 집에 와서도 마음이 편하지 않다.",
    "userId": "1"
  }'
```

### 사용자별 전체 조회 예시
```bash
curl -X GET http://localhost:8080/api/diary/responses/userId
```

### 날짜별 조회 예시
```bash
curl -X GET "http://localhost:8080/api/diary/responses/userId/range?startDate=2024-01-01&endDate=2024-01-31"
```

---

## 주의사항

1. **CORS 설정**: 모든 출처(`*`)에서의 요청을 허용합니다.
2. **유효성 검사**: `DiaryRequestDto`의 `title`과 `content` 필드는 필수입니다.
3. **날짜 형식**: 날짜별 조회 시 `yyyy-MM-dd` 형식을 사용해야 합니다.
4. **AI 서버 의존성**: 일기 분석 API는 외부 AI 서버와 연동되므로, AI 서버 상태에 따라 응답 시간이 달라질 수 있습니다. 