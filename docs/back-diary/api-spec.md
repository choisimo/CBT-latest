
# OSS Emotion 백엔드 API 명세서

이 문서는 `oss-emotion` 백엔드 서비스의 API 엔드포인트 명세를 정의합니다.

 이 서비스는 `mariadb-emotion.sql`에 정의된 MariaDB 데이터베이스와 상호 작용하며, 사용자별 데이터, 일기 게시물, AI 기반 감정 분석을 관리하는 역할을 담당합니다.

**인증**: 별도로 명시되지 않는 한, 인증이 필요한 엔드포인트는 `Authorization` 헤더에 Bearer 토큰 형식으로 JWT 토큰(별도의 `Auth-server`에서 발급)을 포함해야 합니다.

## 사용자 관리

### `GET /api/users/me`

-   **설명**: 현재 인증된 사용자의 상세 정보를 가져옵니다.
-   **인증**: 유효한 JWT 토큰이 필요합니다.
-   **요청**: 없음.
-   **응답 (성공 200 OK)**:
    ```json
    {
      "userId": "string (Users.id에서 가져온 사용자의 고유 ID)",
      "nickname": "string (Users.user_name에서 가져온 닉네임)",
      "email": "user@example.com (Users.email에서 가져온 이메일)",
      "emailVerified": true, // 참고: 실제 이메일 인증 상태는 Auth-server에서 관리하거나 동기화가 필요할 수 있습니다.
      "providerType": "server | google | kakao (User_Authentication 및 Auth_Provider 테이블에서 파생)",
      "role": "USER | ADMIN (Users.user_role에서 가져온 역할)"
    }
    ```
-   **응답 (오류 401 Unauthorized)**: 토큰이 없거나, 유효하지 않거나, 만료된 경우.
-   **응답 (오류 404 Not Found)**: 토큰과 연결된 사용자 정보를 `Users` 테이블에서 찾을 수 없는 경우.

## 일기 게시물 관리

**참고**: `mariadb-emotion.sql`의 `Diary.id`는 `BIGINT AUTO_INCREMENT` 타입입니다. 아래 예시에서는 일반적인 API 관행(예: UUID)과의 일관성을 위해 `postId`를 "string"으로 표기했지만, 이는 실제 백엔드 구현 결정에 따라 맞춰져야 합니다.

### `POST /api/diaryposts`

-   **설명**: 새로운 일기 게시물을 생성합니다.
-   **인증**: 필수. 게시물의 `user_id`는 인증된 사용자의 토큰에서 가져옵니다.
-   **요청 본문 (application/json)**:
    ```json
    {
      "date": "YYYY-MM-DD (string, Diary.created_at의 날짜 부분에 매핑되거나, 별도의 날짜 컬럼이 추가된 경우 해당 컬럼에 매핑)",
      "title": "string (최대 255자, Diary.title에 매핑)",
      "content": "string (text, Diary.content에 매핑)"
    }
    ```
-   **응답 (성공 201 Created)**:
    ```json
    {
      "postId": "string (새로 생성된 게시물의 ID, Diary.id에서 가져옴)",
      "date": "YYYY-MM-DD (게시물의 날짜)",
      "title": "string (Diary.title에서 가져옴)",
      "content": "string (Diary.content에서 가져옴)",
      "createdAt": "YYYY-MM-DDTHH:mm:ssZ (Diary.created_at에서 가져옴)",
      "updatedAt": "YYYY-MM-DDTHH:mm:ssZ (Diary.updated_at에서 가져옴)",
      "aiResponse": false // 새 게시물의 기본값, Diary.alternative_thought는 NULL일 가능성이 높음
    }
    ```
-   **응답 (오류 400 Bad Request)**: 유효성 검사 실패 시 (예: 필수 필드 누락, 잘못된 날짜 형식, 내용 길이 초과).
-   **응답 (오류 401 Unauthorized)**

### `GET /api/diaryposts/{postId}`

-   **설명**: ID로 특정 일기 게시물을 조회합니다.
-   **인증**: 필수. 사용자는 자신의 게시물에만 접근할 수 있도록 접근 제어가 필요합니다.
-   **경로 매개변수**: `postId` (string/number, `Diary.id`에 해당)
-   **응답 (성공 200 OK)**:
    ```json
    {
      "id": "string (Diary.id에서 가져온 postId)",
      "date": "YYYY-MM-DD (게시물의 날짜)",
      "title": "string (Diary.title에서 가져옴)",
      "content": "string (Diary.content에서 가져옴)",
      "aiResponse": true, // boolean, Diary.alternative_thought가 NULL이 아니면 true
      "aiAlternativeThoughts": "string (optional, Diary.alternative_thought에서 가져옴)",
      "isNegative": false, // boolean, Diary.is_negative에서 가져옴
      "createdAt": "YYYY-MM-DDTHH:mm:ssZ (Diary.created_at에서 가져옴)",
      "updatedAt": "YYYY-MM-DDTHH:mm:ssZ (Diary.updated_at에서 가져옴)"
    }
    ```
-   **응답 (오류 401 Unauthorized)**
-   **응답 (오류 403 Forbidden)**: 사용자가 이 게시물에 접근할 권한이 없는 경우.
-   **응답 (오류 404 Not Found)**: `postId`에 해당하는 게시물이 존재하지 않는 경우.

### `PUT /api/diaryposts/{postId}`

-   **설명**: 기존 일기 게시물을 수정합니다.
-   **인증**: 필수. 사용자는 자신의 게시물만 수정할 수 있어야 합니다.
-   **경로 매개변수**: `postId` (string/number, `Diary.id`에 해당)
-   **요청 본문 (application/json)**: (부분 업데이트 허용)
    ```json
    {
      "title": "string (optional, Diary.title에 매핑)",
      "content": "string (optional, Diary.content에 매핑)"
      // 일기 항목의 날짜는 일반적으로 수정하지 않습니다.
      // 이 엔드포인트를 통해 is_negative나 alternative_thought를 직접 수정하는 것은 허용되지 않아야 합니다. 이들은 분석 결과입니다.
    }
    ```
-   **응답 (성공 200 OK)**:
    ```json
    {
      "id": "string (Diary.id에서 가져온 postId)",
      "date": "YYYY-MM-DD (게시물의 날짜)",
      "title": "string (수정된 Diary.title)",
      "content": "string (수정된 Diary.content)",
      "aiResponse": true, // 현재 값, boolean, Diary.alternative_thought가 NULL이 아니면 true
      "aiAlternativeThoughts": "string (optional, 현재 Diary.alternative_thought)",
      "isNegative": false, // 현재 Diary.is_negative 값
      "createdAt": "YYYY-MM-DDTHH:mm:ssZ (Diary.created_at에서 가져옴)",
      "updatedAt": "YYYY-MM-DDTHH:mm:ssZ (수정 후 Diary.updated_at)"
    }
    ```
-   **응답 (오류 400 Bad Request)**: 유효성 검사 실패 시.
-   **응답 (오류 401 Unauthorized)**
-   **응답 (오류 403 Forbidden)**: 사용자가 이 게시물을 수정할 권한이 없는 경우.
-   **응답 (오류 404 Not Found)**: `postId`에 해당하는 게시물이 존재하지 않는 경우.

### `DELETE /api/diaryposts/{postId}`

-   **설명**: 일기 게시물을 삭제합니다.
-   **인증**: 필수. 사용자는 자신의 게시물만 삭제할 수 있어야 합니다.
-   **경로 매개변수**: `postId` (string/number, `Diary.id`에 해당)
-   **응답 (성공 204 No Content)**: 성공적인 삭제를 나타냅니다.
-   **응답 (오류 401 Unauthorized)**
-   **응답 (오류 403 Forbidden)**: 사용자가 이 게시물을 삭제할 권한이 없는 경우.
-   **응답 (오류 404 Not Found)**: `postId`에 해당하는 게시물이 존재하지 않는 경우.

## AI 분석 엔드포인트

### `POST /api/diaries/{postId}/analysis`

-   **설명**: 특정 일기 게시물에 대한 AI 분석을 시작합니다. 분석 결과는 `Diary.alternative_thought`와 `Diary.is_negative` 필드를 업데이트합니다.
-   **인증**: 필수. 사용자는 자신의 게시물에 대해서만 분석을 요청할 수 있어야 합니다.
-   **경로 매개변수**: `postId` (string/number, `Diary.id`에 해당)
-   **요청 본문**: 비어 있거나, 향후 분석 매개변수를 포함할 수 있습니다.
-   **응답 (성공 202 Accepted)**: 분석이 비동기적으로 시작된 경우.
    ```json
    {
      "message": "분석 요청을 수신했으며 게시물 " + postId + "에 대해 처리 중입니다.",
      "statusUrl": "/api/diaries/{postId}/analysis" // 상태 및 결과 확인을 위한 폴링 URL
    }
    ```
-   **응답 (성공 200 OK)**: 분석이 동기적으로 처리되는 경우 (복잡한 AI 모델에서는 드물지만 간단한 모델에서는 가능). 응답 본문은 `GET /api/diaries/{postId}/analysis` (완료 상태)와 동일합니다.
-   **응답 (오류 400 Bad Request)**: `postId`가 유효하지 않거나, 게시물 내용이 너무 짧거나 분석에 부적합한 경우.
-   **응답 (오류 401 Unauthorized)**
-   **응답 (오류 403 Forbidden)**: 사용자에게 권한이 없는 경우.
-   **응답 (오류 404 Not Found)**: `postId`에 해당하는 일기 게시물을 찾을 수 없는 경우.
-   **응답 (오류 409 Conflict)**: 이 게시물에 대한 분석이 이미 완료된 경우 (즉, `Diary.alternative_thought`가 NULL이 아닌 경우). 재분석은 다른 엔드포인트나 매개변수가 필요할 수 있습니다.

### `GET /api/diaries/{postId}/analysis`

-   **설명**: 특정 일기 게시물의 AI 분석 결과를 조회합니다. 이 데이터는 `Diary` 테이블 자체(`alternative_thought`, `is_negative`)에서 가져옵니다.
-   **인증**: 필수. 사용자는 자신의 게시물 분석 결과에만 접근할 수 있어야 합니다.
-   **경로 매개변수**: `postId` (string/number, `Diary.id`에 해당)
-   **응답 (성공 200 OK - 분석 완료 및 사용 가능)**:
    ```json
    {
      "postId": "string (Diary.id)",
      "analysis": {
        "alternativeThought": "string (Diary.alternative_thought에서 가져옴)",
        "status": "POSITIVE | NEGATIVE | NEUTRAL (Diary.is_negative에서 파생됨, 예: true -> NEGATIVE)",
        // 감정 탐지, 요약, 신뢰도와 같은 추가 필드는 더 많은 스키마나 별도의 분석 테이블이 필요합니다.
        // 현재로서는 Diary 테이블에 직접 있는 정보에 집중합니다.
        "analyzedAt": "YYYY-MM-DDTHH:mm:ssZ (분석이 완료된 타임스탬프, 분석 시 Diary.updated_at이 업데이트된다면 해당 값)"
      }
    }
    ```
-   **응답 (성공 200 OK - 분석 미수행)**:
    ```json
    {
      "postId": "string (Diary.id)",
      "message": "이 일기 항목에 대한 AI 분석이 아직 수행되지 않았습니다."
      // 선택적으로 POST 엔드포인트를 호출하도록 링크나 제안을 포함할 수 있습니다.
    }
    ```
-   **응답 (성공 202 Accepted - 분석 진행 중 - 백엔드가 상세 상태 폴링을 지원하는 경우)**:
    ```json
    {
      "postId": "string (Diary.id)",
      "message": "분석이 현재 진행 중입니다.",
      "progress": 50 // 예시: 진행률 백분율 (0-100) - 백엔드에서 진행 상태를 추적해야 함.
    }
    ```
-   **응답 (오류 401 Unauthorized)**
-   **응답 (오류 403 Forbidden)**: 사용자에게 권한이 없는 경우.
-   **응답 (오류 404 Not Found)**: `postId`에 대한 분석을 찾을 수 없거나 게시물 자체를 찾을 수 없는 경우.

## 외부 인증 엔드포인트 (`Auth-server`에서 처리)

이 엔드포인트들은 전체 시스템의 일부이지만 별도의 `Auth-server` 컴포넌트에서 관리됩니다. `oss-emotion` 백엔드는 이 서버에서 발급된 JWT에 의존합니다.

-   `POST /api/auth/login` (외부: `Auth-server`)
-   `POST /api/auth/logout` (외부: `Auth-server`)
-   `POST /auth/api/protected/refresh` (외부: `Auth-server`)
-   `POST /api/public/join` (외부: `Auth-server` - 사용자 등록용)
-   `POST /api/auth/verify-email` (외부: `Auth-server` - 회원가입 후 이메일 인증용)

**참고**: `Auth-server` 엔드포인트의 정확한 경로(예: `/api` 접두사 사용 여부 등)는 `Auth-server` 자체의 문서에서 확인해야 합니다.