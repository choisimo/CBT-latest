# 🔧 서비스 계층 비교 보고서

> 이 보고서는 `Auth-Server`와 `CBT-back-diary` 프로젝트의 핵심 서비스 클래스들을 분석하고 비교하여, 각각의 책임, 로직, 그리고 잠재적 중복을 중점적으로 검토합니다.

## 👥 1. UserService 비교 분석

```mermaid
graph TD
    subgraph "🔷 Auth-Server UserService"
        AS_USER[UserService]
        AS_JOIN[사용자 등록]
        AS_EMAIL[이메일 조회]
        AS_PWD[비밀번호 업데이트]
        AS_CHECK[사용자명 중복 검사]

        AS_USER --> AS_JOIN
        AS_USER --> AS_EMAIL
        AS_USER --> AS_PWD
        AS_USER --> AS_CHECK
    end

    subgraph "🔶 CBT-back-diary UserService"
        CBD_USER[UserService]
        CBD_CURRENT[현재 사용자 정보 조회]
        CBD_MOCK[MOCK 데이터 사용]

        CBD_USER --> CBD_CURRENT
        CBD_CURRENT --> CBD_MOCK
    end

    style AS_USER fill:#4caf50
    style CBD_USER fill:#ff9800
    style AS_JOIN,AS_EMAIL,AS_PWD,AS_CHECK fill:#e3f2fd
    style CBD_CURRENT,CBD_MOCK fill:#fce4ec
```

### 🔷 Auth-Server: `UserService`

#### 📋 주요 책임

- ✅ 사용자 등록 (서비스 가입)
- 📧 사용자 ID로 이메일 조회
- 🔐 사용자 비밀번호 업데이트
- 🔍 사용자명(사용자 ID) 중복 검사

#### 🛠️ 핵심 메서드

| 메서드                               | 기능                | 구현 세부사항                                                                             |
| ------------------------------------ | ------------------- | ----------------------------------------------------------------------------------------- |
| `join(JoinRequest)`                  | 📝 신규 사용자 생성 | 비밀번호 암호화, 사용자명/이메일 중복 검사, 초기 상태 설정 (`WAITING`, `isPremium=false`) |
| `getEmailByUserId(String)`           | 📧 이메일 조회      | `userName`으로 사용자 이메일 페치                                                         |
| `UpdateUserPassword(String, String)` | 🔐 비밀번호 변경    | 새 비밀번호 암호화 후 업데이트                                                            |
| `checkUserNameIsDuplicate(String)`   | 🔍 중복 검사        | `userName` 존재 여부 확인                                                                 |

**📊 트랜잭션 사용**: 모든 공개 메서드에 `@Transactional` 적용 (읽기-쓰기 기본 동작)

### 🔶 CBT-back-diary: `UserService`

#### 📋 주요 책임

- 👤 "현재" 사용자 세부 정보 조회
- 🔧 `MOCK_USER_ID_LONG` 의존 (개발 중 또는 Spring Security 컨텍스트 통합 대기)

#### 🛠️ 핵심 메서드

| 메서드                    | 기능                | 구현 세부사항                                                                                                                                 |
| ------------------------- | ------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- |
| `getCurrentUserDetails()` | 👤 현재 사용자 정보 | 사용자 세부사항(ID, 닉네임, 이메일, 이메일 인증, 제공자 타입, 역할)을 `UserDto`로 반환. Mock ID로 DB 조회, 실패시 하드코딩된 Mock 데이터 사용 |

**📊 트랜잭션 사용**: `@Transactional(readOnly = true)` 적용

### 🔍 비교 분석

```mermaid
graph LR
    subgraph "기능 비교"
        A[사용자 등록] --> A1[Auth-Server ✅]
        A --> A2[CBT-back-diary ❌]

        B[사용자 조회] --> B1[Auth-Server 🔍 ID별]
        B --> B2[CBT-back-diary 👤 현재 사용자]

        C[비밀번호 관리] --> C1[Auth-Server ✅]
        C --> C2[CBT-back-diary ❌]

        D[중복 검사] --> D1[Auth-Server ✅]
        D --> D2[CBT-back-diary ❌]
    end

    style A1,B1,C1,D1 fill:#4caf50
    style A2,C2,D2 fill:#f44336
    style B2 fill:#ff9800
```

#### 🎯 유사점 및 차이점

| 측면              | 🔷 Auth-Server               | 🔶 CBT-back-diary        | 📝 분석                             |
| ----------------- | ---------------------------- | ------------------------ | ----------------------------------- |
| **기능 범위**     | 🌟 포괄적 (등록, 관리, 검증) | 🔍 제한적 (조회 중심)    | Auth-Server가 더 완전한 사용자 관리 |
| **비즈니스 로직** | 🔐 직접 등록, 비밀번호 해싱  | 🔗 OAuth 흐름 의존       | 현재 직접적 중복 최소               |
| **트랜잭션 관리** | ⚠️ 읽기 전용 최적화 여지     | ✅ 적절한 읽기 전용 사용 | CBT가 더 정확한 트랜잭션 관리       |
| **통합 가능성**   | 🏗️ 기반 서비스로 활용        | 🔧 특화 기능 추가        | 높은 통합 잠재력                    |

## 📖 2. CBT-back-diary: `DiaryService`

```mermaid
graph TD
    A[DiaryService] --> B[CRUD 작업]
    B --> C[생성: createDiaryPost]
    B --> D[조회: getDiaryPostById]
    B --> E[수정: updateDiaryPost]

    C --> F[사용자 연결 - MOCK_USER_ID]
    D --> G[@Transactional readOnly]
    E --> H[AI 필드 제외 업데이트]

    A --> I[mapToDto: 엔티티-DTO 변환]

    style A fill:#2196f3
    style C,D,E fill:#4caf50
    style F fill:#ff9800
    style G,H fill:#9c27b0
    style I fill:#607d8b
```

### 📋 주요 책임

- 📝 일기 항목에 대한 CRUD (생성, 읽기, 업데이트) 작업 관리

### 🛠️ 핵심 기능

| 메서드                                          | 트랜잭션                          | 기능 설명                                                      |
| ----------------------------------------------- | --------------------------------- | -------------------------------------------------------------- |
| `createDiaryPost(CreateDiaryPostRequest)`       | `@Transactional`                  | 새 일기 항목 생성, 사용자 연결 (현재 `MOCK_USER_ID_LONG` 사용) |
| `getDiaryPostById(Long)`                        | `@Transactional(readOnly = true)` | ID로 특정 일기 게시물 조회                                     |
| `updateDiaryPost(Long, UpdateDiaryPostRequest)` | `@Transactional`                  | 제목과 내용 업데이트 (AI 관련 필드 제외)                       |
| `mapToDto(Diary)`                               | Private                           | `Diary` 엔티티를 `DiaryPostDto`로 변환                         |

**⚠️ 현재 제한사항**:

- `MOCK_USER_ID_LONG` 사용으로 실제 사용자 컨텍스트 통합 대기
- 사용자별 접근 권한 검사를 위한 TODO 항목 존재

## 🔐 3. Auth-Server: 전문 서비스들

### 🌐 OAuth2Service

```mermaid
graph TD
    A[OAuth2Service] --> B[토큰 교환]
    A --> C[사용자 프로필 조회]
    A --> D[사용자 프로비저닝/연결]
    A --> E[애플리케이션 토큰 발급]
    A --> F[리프레시 토큰 관리]

    B --> B1[인증 코드 → 액세스 토큰]
    C --> C1[외부 제공자 API 호출]
    D --> D1[기존 사용자 확인]
    D --> D2[신규 사용자 생성]
    E --> E1[JWT 생성]
    F --> F1[Redis 저장]
    F --> F2[HTTP-only 쿠키 설정]

    style A fill:#4caf50
    style B,C,D,E,F fill:#2196f3
    style B1,C1,D1,D2,E1,F1,F2 fill:#fff3e0
```

#### 🎯 핵심 책임

- **🔄 토큰 교환**: OAuth2 인증 코드 그랜트 플로우 관리
- **👤 사용자 프로필 검색**: 액세스 토큰으로 외부 제공자에서 프로필 정보 가져오기
- **🔗 사용자 연결**: 로컬 DB에서 OAuth ID 연결된 사용자 확인/생성
- **🎫 토큰 발급**: 성공적인 OAuth 인증 후 애플리케이션별 JWT 생성
- **♻️ 리프레시 토큰**: OAuth 제공자의 리프레시 토큰을 Redis에 저장

#### 🛠️ 지원 제공자

```mermaid
graph LR
    OAUTH[OAuth2Service] --> KAKAO[Kakao]
    OAUTH --> NAVER[Naver]
    OAUTH --> GOOGLE[Google]

    style OAUTH fill:#4caf50
    style KAKAO fill:#ffeb3b
    style NAVER fill:#4caf50
    style GOOGLE fill:#f44336
```

### 🔄 TokenService

```mermaid
graph TD
    A[TokenService] --> B[JWT 리프레시 관리]
    B --> C[만료된 토큰 검증]
    B --> D[구조적 유효성 확인]
    B --> E[Redis 리프레시 토큰 확인]
    B --> F[새 JWT 생성]

    C --> C1[시그니처 검사]
    C --> C2[클레임 확인]
    E --> E1[사용자-제공자별 토큰 검증]

    style A fill:#ff9800
    style B fill:#2196f3
    style C,D,E,F fill:#4caf50
    style C1,C2,E1 fill:#fff3e0
```

#### 🎯 핵심 기능

- 📋 만료된 JWT와 제공자 정보 수신
- ✅ 만료된 토큰의 구조적 유효성 검증
- 🔍 Redis에서 해당 리프레시 토큰 존재 및 유효성 확인
- 🆕 새로운 JWT 생성

### 🔒 PrincipalDetailService

```mermaid
graph TD
    A[PrincipalDetailService] --> B[UserDetailsService 구현]
    B --> C[loadUserByUsername]
    C --> D[사용자명으로 User 조회]
    C --> E[PrincipalDetails 래핑]
    E --> F[Spring Security 인증]
    E --> G[권한 부여 정보]

    style A fill:#9c27b0
    style B fill:#2196f3
    style C,D,E fill:#4caf50
    style F,G fill:#ff9800
```

#### 🎯 역할

- 🔗 Spring Security의 `UserDetailsService` 인터페이스 구현
- 👤 인증 과정에서 사용자명으로 `User` 조회
- 🛡️ `UserDetails`를 구현한 `PrincipalDetails`로 래핑

## 📊 주요 발견사항 요약

### 🔄 UserService 중복 및 차이점

```mermaid
pie title UserService 기능 분포
    "Auth-Server 전용" : 70
    "CBT-back-diary 전용" : 10
    "공통 영역" : 20
```

| 측면              | 🔷 Auth-Server           | 🔶 CBT-back-diary     | 🎯 통합 방향          |
| ----------------- | ------------------------ | --------------------- | --------------------- |
| **완성도**        | 🌟 포괄적 사용자 관리    | 🔧 최소한의 조회 기능 | Auth-Server 기반 통합 |
| **비즈니스 로직** | 🔐 직접 등록, 검증, 관리 | 📋 OAuth 기반 조회    | 높은 통합 잠재력      |
| **트랜잭션 관리** | ⚠️ 개선 여지             | ✅ 올바른 읽기 전용   | CBT 방식 적용 권장    |

### 📖 DiaryService 특징

```mermaid
graph LR
    A[DiaryService] --> B[표준 CRUD]
    A --> C[Mock 사용자 ID]
    A --> D[엔티티-DTO 매핑]

    B --> E[생성/조회/수정]
    C --> F[인증 컨텍스트 통합 필요]
    D --> G[깔끔한 분리]

    style A fill:#2196f3
    style B,D fill:#4caf50
    style C fill:#ff9800
    style E,G fill:#e8f5e8
    style F fill:#ffebee
```

### 🔐 Auth-Server 전문 서비스

```mermaid
graph TD
    A[Auth-Server 전문 서비스] --> B[OAuth2Service]
    A --> C[TokenService]
    A --> D[PrincipalDetailService]

    B --> B1[완전한 OAuth2 솔루션]
    B --> B2[다중 제공자 지원]
    B --> B3[사용자 프로비저닝]

    C --> C1[JWT 리프레시 로직]
    C --> C2[Redis 기반 검증]

    D --> D1[Spring Security 통합]
    D --> D2[사용자 세부사항 로드]

    style A fill:#4caf50
    style B,C,D fill:#2196f3
    style B1,B2,B3,C1,C2,D1,D2 fill:#fff3e0
```

## 🎯 통합 전략 및 권장사항

### 🏗️ 아키텍처 통합 방향

```mermaid
graph TD
    CURRENT[현재 상태] --> INTEGRATION[통합 전략]

    INTEGRATION --> AUTH_BASE[Auth-Server 기반]
    INTEGRATION --> DIARY_SPEC[CBT-back-diary 특화]

    AUTH_BASE --> AUTH1[사용자 관리 기반]
    AUTH_BASE --> AUTH2[OAuth2 인증 시스템]
    AUTH_BASE --> AUTH3[JWT 토큰 관리]

    DIARY_SPEC --> DIARY1[일기 도메인 로직]
    DIARY_SPEC --> DIARY2[CRUD 작업]
    DIARY_SPEC --> DIARY3[올바른 트랜잭션 관리]

    AUTH1 --> TARGET[통합된 시스템]
    DIARY1 --> TARGET

    style CURRENT fill:#ffebee
    style INTEGRATION fill:#e3f2fd
    style AUTH_BASE,DIARY_SPEC fill:#f3e5f5
    style TARGET fill:#e8f5e8
```

### 📋 통합 체크리스트

#### 🔧 서비스 통합

- [ ] **UserService**: Auth-Server 버전을 기반으로 통합
- [ ] **DiaryService**: Mock ID를 실제 인증 컨텍스트로 교체
- [ ] **OAuth 서비스**: Auth-Server의 OAuth2Service 활용
- [ ] **토큰 관리**: TokenService 통합

#### 💡 최적화 개선

- [ ] **트랜잭션**: 읽기 전용 작업에 `readOnly = true` 적용
- [ ] **보안 강화**: 사용자별 접근 권한 검사 구현
- [ ] **에러 처리**: 통일된 예외 처리 전략

### 🎯 우선순위별 실행 계획

```mermaid
gantt
    title 서비스 계층 통합 로드맵
    dateFormat YYYY-MM-DD
    section Phase 1: 기반 구축
    Auth-Server 기반 설정    :p1-1, 2024-01-01, 7d
    사용자 서비스 통합       :p1-2, after p1-1, 5d
    section Phase 2: 기능 통합
    OAuth2 서비스 적용      :p2-1, after p1-2, 7d
    일기 서비스 인증 통합    :p2-2, after p2-1, 5d
    section Phase 3: 최적화
    트랜잭션 최적화         :p3-1, after p2-2, 3d
    보안 강화              :p3-2, after p3-1, 5d
```

## 💡 결론 및 제안

### ✅ 주요 강점

1. **🔷 Auth-Server**: 완전한 인증 및 사용자 관리 기반 제공
2. **🔶 CBT-back-diary**: 도메인별 비즈니스 로직과 올바른 트랜잭션 관리
3. **🔗 상호 보완성**: 두 시스템의 강점이 서로 보완

### 🎯 통합 목표

```mermaid
graph LR
    A[현재] --> B[통합 후]

    subgraph "현재 상태"
        A1[Auth-Server: 인증 전문]
        A2[CBT-back-diary: 일기 전문]
    end

    subgraph "통합 목표"
        B1[통합 인증 시스템]
        B2[도메인 특화 서비스]
        B3[일관된 아키텍처]
    end

    A1 --> B1
    A2 --> B2
    B1 --> B3
    B2 --> B3

    style A1,A2 fill:#ffebee
    style B1,B2,B3 fill:#e8f5e8
```

> 💡 **최종 권장사항**: Auth-Server가 더 완전한 인증 및 핵심 사용자 관리 기반을 제공하는 반면, CBT-back-diary는 일기별 비즈니스 로직을 제공합니다. 통합 시 Auth-Server를 기반으로 하여 CBT-back-diary의 도메인 특화 기능을 추가하는 방향이 가장 효과적일 것입니다.
