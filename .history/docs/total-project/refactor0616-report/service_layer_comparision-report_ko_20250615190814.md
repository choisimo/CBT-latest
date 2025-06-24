# 서비스 레이어 비교 분석 보고서

> 📋 **보고서 개요**  
> 본 보고서는 `Auth-Server`와 `CBT-back-diary` 프로젝트의 핵심 서비스 클래스들을 분석하고 비교하여, 각각의 책임, 로직, 그리고 잠재적 중복 영역을 파악합니다.

---

## 📌 목차

- [1. UserService 비교](#1-userservice-비교)
- [2. CBT-back-diary: DiaryService](#2-cbt-back-diary-diaryservice)
- [3. Auth-Server: 전문 서비스들](#3-auth-server-전문-서비스들)
- [핵심 발견사항 요약](#핵심-발견사항-요약)

---

## 1. UserService 비교

### 🔐 Auth-Server: `UserService` 분석

#### 주요 책임
```mermaid
graph TB
    A[UserService] --> B[사용자 등록]
    A --> C[이메일 조회]
    A --> D[비밀번호 업데이트]
    A --> E[사용자명 중복 확인]
    
    B --> F[비밀번호 암호화]
    B --> G[중복 검사]
    B --> H[초기 상태 설정]
    
    style A fill:#e3f2fd
    style B fill:#e8f5e8
    style C fill:#fff3e0
    style D fill:#f3e5f5
    style E fill:#fce4ec
```

#### 핵심 메서드 분석

| 메서드 | 기능 | 트랜잭션 | 보안 수준 |
|:-------|:-----|:---------|:----------|
| `join()` | 🆕 신규 사용자 생성 | `@Transactional` | 🔴 높음 |
| `getEmailByUserId()` | 📧 이메일 조회 | `@Transactional` | 🟡 중간 |
| `UpdateUserPassword()` | 🔑 비밀번호 변경 | `@Transactional` | 🔴 높음 |
| `checkUserNameIsDuplicate()` | ✅ 중복 확인 | `@Transactional` | 🟢 낮음 |

### 🎯 CBT-back-diary: `UserService` 분석

#### 현재 구현 상태
```mermaid
graph LR
    A[UserService] --> B[getCurrentUserDetails]
    B --> C{Mock 사용자 ID}
    C --> D[DB 조회]
    C --> E[하드코딩 데이터]
    D --> F[UserDto 반환]
    E --> F
    
    style A fill:#e8f5e8
    style C fill:#fff3e0
    style D fill:#e3f2fd
    style E fill:#ffebee
```

#### 핵심 특징

| 특징 | 설명 | 상태 | 개선 필요도 |
|:-----|:-----|:-----|:-----------|
| **사용자 조회** | 현재 사용자 정보 반환 | 🔶 Mock 기반 | 🔴 높음 |
| **Provider 타입** | OAuth 제공자 식별 | 🔶 부분 구현 | 🟡 중간 |
| **트랜잭션 관리** | `@Transactional(readOnly = true)` | ✅ 적절 | 🟢 낮음 |

### 🔄 UserService 비교 요약

```mermaid
graph TB
    subgraph "Auth-Server UserService"
        A1[완전한 사용자 생명주기 관리]
        A2[비밀번호 암호화]
        A3[상태 관리]
        A4[중복 검사]
    end
    
    subgraph "CBT-back-diary UserService"
        B1[사용자 정보 조회]
        B2[Provider 타입 결정]
        B3[Mock 데이터 기반]
        B4[OAuth 통합 준비]
    end
    
    A1 --> C[통합 가능성]
    B1 --> C
    
    style A1 fill:#e8f5e8
    style B1 fill:#e3f2fd
    style C fill:#fff3e0
```

#### 주요 차이점

| 측면 | Auth-Server | CBT-back-diary | 통합 권장사항 |
|:-----|:------------|:---------------|:-------------|
| **기능 범위** | 🟢 완전한 CRUD | 🔶 조회 중심 | Auth-Server 기반 확장 |
| **인증 방식** | 🔐 직접 등록 + OAuth | 🔶 OAuth 준비 단계 | OAuth 통합 완료 |
| **데이터 관리** | 🗄️ 실제 DB 연동 | 🔶 Mock 데이터 | 실제 데이터 통합 |

---

## 2. CBT-back-diary: DiaryService

### 📖 DiaryService 핵심 기능

```mermaid
graph TD
    A[DiaryService] --> B[createDiaryPost]
    A --> C[getDiaryPostById]
    A --> D[updateDiaryPost]
    A --> E[mapToDto]
    
    B --> F[사용자 연결]
    B --> G[제목/내용 설정]
    B --> H[날짜 설정]
    
    C --> I[ID 기반 조회]
    D --> J[제목/내용 수정]
    E --> K[DTO 변환]
    
    style A fill:#e8f5e8
    style B fill:#e3f2fd
    style C fill:#fff3e0
    style D fill:#f3e5f5
    style E fill:#fce4ec
```

### 🔧 메서드별 기능 분석

| 메서드 | 기능 | 트랜잭션 | 현재 상태 | 개선 필요사항 |
|:-------|:-----|:---------|:----------|:-------------|
| `createDiaryPost()` | 📝 다이어리 생성 | `@Transactional` | 🔶 Mock 사용자 | 실제 사용자 컨텍스트 |
| `getDiaryPostById()` | 🔍 다이어리 조회 | `@Transactional(readOnly = true)` | ✅ 완성 | 권한 검사 추가 |
| `updateDiaryPost()` | ✏️ 다이어리 수정 | `@Transactional` | ✅ 완성 | 권한 검사 추가 |
| `mapToDto()` | 🔄 DTO 변환 | N/A | ✅ 완성 | - |

### 🚨 주요 개선 필요사항

```mermaid
graph LR
    A[현재 상태] --> B[Mock 사용자 ID]
    B --> C[보안 이슈]
    C --> D[개선 방향]
    
    D --> E[Spring Security 통합]
    D --> F[사용자 권한 검사]
    D --> G[실제 사용자 컨텍스트]
    
    style B fill:#ffebee
    style C fill:#ffcdd2
    style E fill:#e8f5e8
    style F fill:#e3f2fd
    style G fill:#fff3e0
```

---

## 3. Auth-Server: 전문 서비스들

### 🔐 OAuth2Service 분석

#### 핵심 역할 및 기능
```mermaid
graph TB
    A[OAuth2Service] --> B[토큰 교환]
    A --> C[사용자 프로필 조회]
    A --> D[사용자 프로비저닝/연결]
    A --> E[애플리케이션 토큰 발급]
    A --> F[리프레시 토큰 관리]
    
    B --> G[Kakao/Naver/Google]
    D --> H[신규 사용자 생성]
    D --> I[기존 사용자 업데이트]
    E --> J[JWT 생성]
    F --> K[Redis 저장]
    
    style A fill:#e3f2fd
    style B fill:#e8f5e8
    style C fill:#fff3e0
    style D fill:#f3e5f5
    style E fill:#fce4ec
    style F fill:#e0f2f1
```

#### OAuth2 플로우 다이어그램
```mermaid
sequenceDiagram
    participant User as 사용자
    participant App as 애플리케이션
    participant OAuth as OAuth 제공자
    participant Service as OAuth2Service
    participant DB as 데이터베이스
    participant Redis as Redis
    
    User->>App: 로그인 요청
    App->>OAuth: 인증 요청
    OAuth->>User: 인증 페이지 제공
    User->>OAuth: 인증 정보 입력
    OAuth->>App: 인증 코드 반환
    App->>Service: 토큰 교환 요청
    Service->>OAuth: 토큰 교환
    OAuth->>Service: 액세스 토큰
    Service->>OAuth: 사용자 정보 조회
    OAuth->>Service: 사용자 프로필
    Service->>DB: 사용자 확인/생성
    Service->>Redis: 리프레시 토큰 저장
    Service->>App: JWT 토큰 발급
    App->>User: 로그인 완료
```

### 🔑 TokenService 분석

#### 토큰 갱신 프로세스
```mermaid
graph TD
    A[만료된 JWT] --> B[TokenService]
    B --> C[토큰 구조 검증]
    C --> D{유효한 구조?}
    D -->|Yes| E[Redis에서 리프레시 토큰 확인]
    D -->|No| F[거부]
    E --> G{리프레시 토큰 유효?}
    G -->|Yes| H[새 JWT 생성]
    G -->|No| F
    H --> I[새 토큰 반환]
    
    style A fill:#ffebee
    style B fill:#e3f2fd
    style H fill:#e8f5e8
    style I fill:#fff3e0
    style F fill:#ffcdd2
```

### 🛡️ PrincipalDetailService 분석

#### Spring Security 통합
```mermaid
graph LR
    A[Spring Security] --> B[PrincipalDetailService]
    B --> C[loadUserByUsername]
    C --> D[UserRepository 조회]
    D --> E[User 엔티티]
    E --> F[PrincipalDetails 래핑]
    F --> G[인증/인가 처리]
    
    style A fill:#e3f2fd
    style B fill:#e8f5e8
    style C fill:#fff3e0
    style G fill:#f3e5f5
```

---

## 🎯 핵심 발견사항 요약

### 🔄 UserService 중복 및 차이점

#### 중복 가능성 분석
```mermaid
pie title UserService 기능 비교
    "Auth-Server 고유 기능" : 60
    "공통 기능 영역" : 20
    "CBT-back-diary 고유 기능" : 20
```

| 영역 | 중복도 | 통합 난이도 | 권장사항 |
|:-----|:-------|:-----------|:---------|
| **사용자 등록** | 🔴 높음 | 🟡 중간 | Auth-Server 로직 활용 |
| **사용자 조회** | 🟡 중간 | 🟢 낮음 | 인터페이스 통합 |
| **인증 관리** | 🔴 높음 | 🔴 높음 | OAuth2 통합 필요 |

### 📊 서비스 레이어 아키텍처 비교

```mermaid
graph TB
    subgraph "Auth-Server 아키텍처"
        A1[UserService]
        A2[OAuth2Service]
        A3[TokenService]
        A4[PrincipalDetailService]
        
        A1 --> A5[완전한 사용자 관리]
        A2 --> A6[OAuth 통합]
        A3 --> A7[토큰 관리]
        A4 --> A8[Spring Security]
    end
    
    subgraph "CBT-back-diary 아키텍처"
        B1[UserService]
        B2[DiaryService]
        
        B1 --> B3[기본 사용자 조회]
        B2 --> B4[다이어리 CRUD]
    end
    
    A5 --> C[통합 방향]
    B3 --> C
    B4 --> C
    
    style A1 fill:#e3f2fd
    style A2 fill:#e8f5e8
    style B1 fill:#fff3e0
    style B2 fill:#f3e5f5
    style C fill:#fce4ec
```

### 🎯 통합 권장사항

#### 단계별 통합 전략
```mermaid
graph TD
    A[현재 상태] --> B[1단계: 인증 통합]
    B --> C[2단계: 사용자 서비스 통합]
    C --> D[3단계: 도메인 서비스 연결]
    D --> E[최종: 완전 통합]
    
    B --> F[OAuth2Service 활용]
    C --> G[Auth-Server UserService 확장]
    D --> H[DiaryService 사용자 컨텍스트 연결]
    
    style A fill:#ffebee
    style B fill:#e3f2fd
    style C fill:#e8f5e8
    style D fill:#fff3e0
    style E fill:#f3e5f5
```

#### 우선순위별 작업 항목

| 순위 | 작업 | 복잡도 | 영향도 | 설명 |
|:-----|:-----|:-------|:-------|:-----|
| 🥇 **1순위** | OAuth2 통합 | 🔴 높음 | 🔴 높음 | CBT-back-diary에 OAuth2 인증 적용 |
| 🥈 **2순위** | 사용자 컨텍스트 | 🟡 중간 | 🔴 높음 | Mock 사용자 → 실제 사용자 컨텍스트 |
| 🥉 **3순위** | 권한 검사 | 🟡 중간 | 🟡 중간 | 다이어리 접근 권한 검사 로직 |
| 4순위 | 서비스 통합 | 🟢 낮음 | 🟡 중간 | 공통 인터페이스 정의 및 구현 |

### 💡 최종 통합 비전

```mermaid
graph TB
    A[통합 서비스 레이어] --> B[통합 UserService]
    A --> C[OAuth2Service]
    A --> D[TokenService]
    A --> E[DiaryService]
    A --> F[SecurityService]
    
    B --> G[사용자 생명주기 완전 관리]
    C --> H[다중 OAuth 제공자 지원]
    D --> I[JWT 기반 인증]
    E --> J[도메인별 비즈니스 로직]
    F --> K[통합 보안 관리]
    
    style A fill:#e8f5e8
    style B fill:#e3f2fd
    style C fill:#fff3e0
    style D fill:#f3e5f5
    style E fill:#fce4ec
    style F fill:#e0f2f1
```

> 🎯 **핵심 결론**: Auth-Server는 더 완전한 인증 및 핵심 사용자 관리 기반을 제공하며, CBT-back-diary는 다이어리 특화 비즈니스 로직을 제공합니다. 통합 시 Auth-Server를 인증 기반으로 활용하고 CBT-back-diary의 도메인 로직을 확장하는 것이 효과적입니다.

---

**📅 작성일**: 2024년 6월 16일  
**🔍 분석 범위**: 서비스 레이어 아키텍처, 비즈니스 로직 비교  
**📊 분석 대상**: UserService, DiaryService, OAuth2Service, TokenService, PrincipalDetailService
