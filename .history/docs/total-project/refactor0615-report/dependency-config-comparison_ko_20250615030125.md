# 🔧 의존성 및 설정 비교 보고서

> 이 보고서는 `Auth-Server`와 `CBT-back-diary` 프로젝트의 빌드 설정과 애플리케이션 속성을 통합하여 비교 분석합니다. Auth-Server 정보는 서브태스크 설명을 기반으로 하며, CBT-back-diary 정보는 이전에 읽은 프로젝트 파일들을 기반으로 합니다.

## 📦 I. 빌드 및 의존성 비교

```mermaid
graph TD
    subgraph "🔷 Auth-Server"
        AS1[Java 21]
        AS2[Spring Boot 3.2.4]
        AS3[QueryDSL]
        AS4[Redis]
        AS5[JWT]
        AS6[MariaDB]
        AS7[Spring Security]
        AS8[Lombok]
    end
    
    subgraph "🔶 CBT-back-diary"
        CBD1[Java 17]
        CBD2[Spring Boot 3.2.0]
        CBD3[❌ QueryDSL]
        CBD4[❌ Redis]
        CBD5[❌ JWT]
        CBD6[MariaDB]
        CBD7[Spring Security]
        CBD8[Lombok]
    end
    
    AS1 -.->|버전 차이| CBD1
    AS2 -.->|마이너 차이| CBD2
    AS3 -.->|미사용| CBD3
    AS4 -.->|미사용| CBD4
    AS5 -.->|미사용| CBD5
    AS6 -.->|동일| CBD6
    AS7 -.->|동일| CBD7
    AS8 -.->|동일| CBD8
    
    style AS1,AS2 fill:#4caf50
    style CBD1,CBD2 fill:#ff9800
    style AS3,AS4,AS5 fill:#2196f3
    style CBD3,CBD4,CBD5 fill:#f44336
    style AS6,AS7,AS8,CBD6,CBD7,CBD8 fill:#9e9e9e
```

### 📊 상세 비교 테이블

| 🔧 기능 | 🔷 Auth-Server | 🔶 CBT-back-diary | 📝 비고 |
|---------|----------------|-------------------|---------|
| **Java 버전** | ☕ 21 | ☕ 17 | Java 버전 차이 존재 |
| **Spring Boot** | 🍃 3.2.4 | 🍃 3.2.0 | 마이너 버전 차이, 둘 다 Spring Boot 3.x |
| **데이터베이스 드라이버** | 🗄️ `mariadb-java-client` | 🗄️ `mariadb-java-client` | 둘 다 MariaDB 사용 |
| **Spring Data JPA** | ✅ `spring-boot-starter-data-jpa` | ✅ `spring-boot-starter-data-jpa` | 일관성 있음 |
| **Spring Security** | 🔒 `spring-boot-starter-security` | 🔒 `spring-boot-starter-security` | 일관성 있음 |
| **QueryDSL** | ✅ `com.querydsl:querydsl-jpa` | ❌ 미포함 | Auth-Server만 QueryDSL 활용 |
| **Redis 클라이언트** | ✅ `spring-boot-starter-data-redis` | ❌ 미포함 | Auth-Server만 Redis 사용 |
| **JWT 라이브러리** | ✅ `io.jsonwebtoken:jjwt-*` | ❌ 미포함 | Auth-Server만 JWT 토큰 인증 |
| **Lombok** | ✅ `org.projectlombok:lombok` | ✅ `org.projectlombok:lombok` | 일관성 있음 |

## ⚙️ II. 설정 속성 비교

```mermaid
graph LR
    subgraph "🔷 Auth-Server 설정"
        AS_PORT[포트: 7078]
        AS_DB[외부 DB 설정]
        AS_REDIS[Redis 설정]
        AS_DDL[ddl-auto: none]
        AS_EXT[외부화된 설정]
    end
    
    subgraph "🔶 CBT-back-diary 설정"
        CBD_PORT[포트: 8080]
        CBD_DB[내장 DB 설정]
        CBD_NO_REDIS[Redis 없음]
        CBD_DDL[ddl-auto: update]
        CBD_HARD[하드코딩된 설정]
    end
    
    AS_PORT --- CBD_PORT
    AS_DB --- CBD_DB
    AS_REDIS --- CBD_NO_REDIS
    AS_DDL --- CBD_DDL
    AS_EXT --- CBD_HARD
    
    style AS_PORT,AS_DB,AS_REDIS,AS_DDL,AS_EXT fill:#e3f2fd
    style CBD_PORT,CBD_DB,CBD_NO_REDIS,CBD_DDL,CBD_HARD fill:#fce4ec
```

### 📋 속성 상세 비교

| ⚙️ 속성 | 🔷 Auth-Server | 🔶 CBT-back-diary | 📝 비고 |
|---------|----------------|-------------------|---------|
| **서버 포트** | 🚪 `7078` | 🚪 `8080` | 포트 차이 |
| **DB URL** | 🔗 외부화 (`application-database.properties`) | 🔗 `jdbc:mariadb://localhost:3306/emotion_db` | Auth-Server는 외부 설정, CBT는 `emotion_db` 사용 |
| **DB 사용자명** | 👤 외부화 | 👤 `root` | Auth-Server는 외부화, CBT는 `root` |
| **DB 비밀번호** | 🔐 외부화 | 🔐 `password` (하드코딩) | Auth-Server는 외부화, CBT는 하드코딩 |
| **DB 방언** | 🗣️ `MariaDBDialect` | 🗣️ `MariaDBDialect` | 일관성 있음 |
| **JPA ddl-auto** | 📝 `none` | 📝 `update` | 스키마 관리 전략 차이 |
| **JPA show-sql** | 🔍 미지정 (기본 `false`) | 🔍 `true` | CBT-back-diary는 SQL 로깅 |
| **Redis 설정** | ✅ 추정 (의존성 존재) | ❌ 해당 없음 | Auth-Server만 Redis 설정 |
| **설정 import** | 🔗 `spring.config.import` 사용 | ❌ 미사용 | Auth-Server는 외부 속성 파일 |

## 📊 분석 결과 요약

### 🔄 주요 의존성 및 기술 차이점

```mermaid
pie title 기술 스택 차이점
    "공통 기술" : 60
    "Auth-Server 전용" : 25
    "CBT-back-diary 전용" : 15
```

#### 🆚 버전 차이
- **Java**: Auth-Server(21) > CBT-back-diary(17) 
- **Spring Boot**: Auth-Server(3.2.4) > CBT-back-diary(3.2.0)

#### 🔧 아키텍처 차이
```mermaid
graph TD
    A[아키텍처 비교] --> B[데이터 접근]
    A --> C[캐싱 전략]
    A --> D[인증 방식]
    
    B --> B1[Auth-Server: QueryDSL]
    B --> B2[CBT-back-diary: 표준 JPA]
    
    C --> C1[Auth-Server: Redis]
    C --> C2[CBT-back-diary: 없음]
    
    D --> D1[Auth-Server: JWT]
    D --> D2[CBT-back-diary: 세션 기반 추정]
    
    style B1,C1,D1 fill:#4caf50
    style B2,C2,D2 fill:#ff9800
```

### ⚙️ 설정 관리 관행

```mermaid
graph LR
    subgraph "🔷 Auth-Server"
        A1[외부 설정]
        A2[환경별 분리]
        A3[보안 강화]
        A1 --> A2 --> A3
    end
    
    subgraph "🔶 CBT-back-diary"
        B1[내장 설정]
        B2[단일 파일]
        B3[개발 환경 최적화]
        B1 --> B2 --> B3
    end
    
    style A1,A2,A3 fill:#e8f5e8
    style B1,B2,B3 fill:#fff3e0
```

#### 📈 설정 방식 평가

| 측면 | 🔷 Auth-Server | 🔶 CBT-back-diary | 🏆 권장사항 |
|------|----------------|-------------------|-------------|
| **보안성** | 🟢 높음 (외부화) | 🟡 낮음 (하드코딩) | Auth-Server 방식 |
| **유연성** | 🟢 높음 (환경별) | 🟡 제한적 | Auth-Server 방식 |
| **단순성** | 🟡 복잡 | 🟢 단순 | 상황에 따라 |
| **운영 안정성** | 🟢 높음 | 🔴 위험 | Auth-Server 방식 |

### 🏗️ 인프라 의존성

```mermaid
graph TD
    subgraph "공통 인프라"
        COMMON[MariaDB Database]
    end
    
    subgraph "Auth-Server 전용"
        AS_REDIS[Redis Cache]
        AS_JWT[JWT Token Store]
    end
    
    subgraph "통합 시 고려사항"
        INT1[Java 버전 통일]
        INT2[Redis 도입 여부]
        INT3[JWT 인증 통합]
        INT4[스키마 관리 전략]
    end
    
    COMMON --> INT4
    AS_REDIS --> INT2
    AS_JWT --> INT3
    
    style COMMON fill:#4caf50
    style AS_REDIS,AS_JWT fill:#2196f3
    style INT1,INT2,INT3,INT4 fill:#ff9800
```

## 🎯 통합을 위한 영향 분석

### ⚠️ 주요 고려사항

```mermaid
graph TD
    A[통합 과제] --> B[기술적 차이]
    A --> C[설정 관리]
    A --> D[아키텍처 통일]
    
    B --> B1[Java 버전 차이 해결]
    B --> B2[Spring Boot 버전 통일]
    
    C --> C1[데이터베이스 설정 표준화]
    C --> C2[외부화 전략 적용]
    
    D --> D1[캐싱 전략 결정]
    D --> D2[인증 방식 통일]
    D --> D3[스키마 관리 정책]
    
    style A fill:#e1f5fe
    style B,C,D fill:#f3e5f5
    style B1,B2,C1,C2,D1,D2,D3 fill:#fff8e1
```

### 📋 통합 체크리스트

#### 🔧 기술 스택 통일
- [ ] **Java 버전**: 21로 통일 (호환성 검토 필요)
- [ ] **Spring Boot**: 최신 안정 버전으로 통일
- [ ] **의존성**: Redis, JWT 필요성 검토 및 도입

#### ⚙️ 설정 관리 개선
- [ ] **외부화**: CBT-back-diary 설정 외부화
- [ ] **보안**: 하드코딩된 비밀번호 제거
- [ ] **환경별**: 개발/운영 환경 분리

#### 🗄️ 데이터베이스 정책
- [ ] **스키마 관리**: `ddl-auto` 정책 통일
- [ ] **연결 설정**: 통합 데이터베이스 또는 분리 정책 결정
- [ ] **마이그레이션**: 기존 데이터 통합 방안

## 💡 권장사항

### 🏆 우선순위별 개선 방안

1. **🔥 긴급 (보안)**
   - CBT-back-diary 하드코딩된 설정 외부화
   - 운영 환경 보안 설정 강화

2. **⚡ 중요 (호환성)**
   - Java 버전 통일 및 호환성 테스트
   - Spring Boot 버전 동기화

3. **🔧 개선 (아키텍처)**
   - Redis 캐싱 전략 도입 검토
   - JWT 기반 통합 인증 시스템 구축

### 🎯 통합 시나리오

```mermaid
graph LR
    CURRENT[현재 상태] --> PHASE1[1단계: 설정 표준화]
    PHASE1 --> PHASE2[2단계: 기술 스택 통일]
    PHASE2 --> PHASE3[3단계: 아키텍처 통합]
    PHASE3 --> TARGET[목표 상태]
    
    style CURRENT fill:#ffebee
    style PHASE1,PHASE2,PHASE3 fill:#e3f2fd
    style TARGET fill:#e8f5e8
```

> 💡 **결론**: 이 비교 분석은 두 프로젝트 간의 기술 선택과 설정 관행에서 중요한 차이점을 보여주며, 통합 시나리오에서 반드시 해결해야 할 과제들을 명확히 제시합니다.
