# 🗄️ 데이터 접근 계층 비교 보고서

> 이 보고서는 `Auth-Server`와 `CBT-back-diary` 프로젝트의 리포지토리 계층을 분석하고 비교하여, 구조, 커스텀 쿼리, QueryDSL 사용량 및 잠재적 결합도를 중점적으로 검토합니다.

## 📋 분석 대상 리포지토리 파일

```mermaid
graph TD
    subgraph "🔷 Auth-Server 리포지토리"
        AS1[AuthProviderRepository.java]
        AS2[UserAuthenticationRepository.java]
        AS3[UserRepository.java]
        AS4[UserRepositoryCustom.java]
        AS5[UserRepositoryImpl.java]
    end

    subgraph "🔶 CBT-back-diary 리포지토리"
        CBD1[AuthProviderRepository.java]
        CBD2[DiaryRepository.java]
        CBD3[UserAuthenticationRepository.java]
        CBD4[UserRepository.java]
    end

    style AS1,AS2,AS3,AS4,AS5 fill:#e3f2fd
    style CBD1,CBD2,CBD3,CBD4 fill:#fce4ec
```

> ⚠️ **참고**: Auth-Server의 `Diary`, `Report`, `EmailVerification`, `SettingsOption`, `UserCustomSetting` 리포지토리는 `Auth-server/backend/src/main/java/com/authentication/auth/repository/` 디렉토리에서 발견되지 않았습니다.

## 🔍 리포지토리 상세 분석

### 🔷 1. Auth-Server 리포지토리

#### 🔐 AuthProviderRepository

```java
extends JpaRepository<AuthProvider, Integer>
```

| 메서드                                    | 타입           | 설명                             |
| ----------------------------------------- | -------------- | -------------------------------- |
| `findByProviderName(String providerName)` | 표준 파생 쿼리 | 제공자 이름으로 인증 제공자 검색 |

#### 🔑 UserAuthenticationRepository

```java
extends JpaRepository<UserAuthentication, UserAuthenticationId>
```

| 메서드                                                       | 타입      | 설명                                            |
| ------------------------------------------------------------ | --------- | ----------------------------------------------- |
| `findByAuthProvider_ProviderNameAndSocialId(String, String)` | 파생 쿼리 | 제공자 이름과 소셜 ID로 사용자 인증 레코드 검색 |

#### 👤 UserRepository

```java
extends JpaRepository<User, Long> and UserRepositoryCustom
```

| 메서드                              | 타입      | 설명                     |
| ----------------------------------- | --------- | ------------------------ |
| `findByUserName(String userName)`   | 파생 쿼리 | 사용자명으로 사용자 검색 |
| `existsByEmail(String email)`       | 파생 쿼리 | 이메일 존재 여부 확인    |
| `existsByUserName(String userName)` | 파생 쿼리 | 사용자명 존재 여부 확인  |

#### 🛠️ UserRepositoryCustom 인터페이스

```java
// 커스텀 메서드 정의 (QueryDSL 또는 복잡한 JPQL 사용)
```

| 메서드                                              | 반환 타입      | 설명                     |
| --------------------------------------------------- | -------------- | ------------------------ |
| `updatePassword(String userId, String newPassword)` | `long`         | 사용자 비밀번호 업데이트 |
| `findAllEmail()`                                    | `List<String>` | 모든 이메일 주소 조회    |

#### ⚙️ UserRepositoryImpl 구현체

```mermaid
classDiagram
    class UserRepositoryCustom {
        <<interface>>
        +updatePassword(String userId, String newPassword) long
        +findAllEmail() List~String~
    }

    class UserRepositoryImpl {
        -JPAQueryFactory queryFactory
        +updatePassword(String userId, String newPassword) long
        +findAllEmail() List~String~
    }

    UserRepositoryCustom <|.. UserRepositoryImpl

    note for UserRepositoryImpl "QueryDSL 사용\n@Transactional 적용"
```

**📊 QueryDSL 사용 현황**:

- ✅ **사용**: `JPAQueryFactory` 활용
- 🔄 `updatePassword`: 사용자명 기반 비밀번호 업데이트 (`@Transactional`)
- 📧 `findAllEmail`: 모든 이메일 주소 조회
- ❌ **페이징**: 커스텀 메서드에서 미구현 (표준 JpaRepository 메서드에서는 지원)

### 🔶 2. CBT-back-diary 리포지토리

#### 🔐 AuthProviderRepository

```java
extends JpaRepository<AuthProvider, Integer>
```

| 메서드                                    | 타입           | 설명                                 |
| ----------------------------------------- | -------------- | ------------------------------------ |
| `findByProviderName(String providerName)` | 표준 파생 쿼리 | Auth-Server와 동일한 메서드 시그니처 |

#### 📖 DiaryRepository

```java
extends JpaRepository<Diary, Long>
```

| 메서드                                            | 페이징 지원 | 설명                           |
| ------------------------------------------------- | ----------- | ------------------------------ |
| `findByIdAndUserId(Long diaryId, Long userId)`    | ❌          | 특정 사용자의 특정 일기 검색   |
| `findAllByUserId(Long userId, Pageable pageable)` | ✅          | 사용자의 모든 일기 페이징 조회 |

**📄 페이징**: `findAllByUserId`에서 명시적으로 `Pageable` 사용

#### 🔑 UserAuthenticationRepository

```java
extends JpaRepository<UserAuthentication, UserAuthenticationId>
```

| 메서드                                      | 타입        | 설명                                    |
| ------------------------------------------- | ----------- | --------------------------------------- |
| `findByUserId(Long userId)`                 | 파생 쿼리   | 사용자 ID로 인증 정보 목록 조회         |
| `findByUserIdAndProviderName(Long, String)` | 커스텀 JPQL | 사용자 ID와 제공자명으로 인증 정보 조회 |
| `findFirstByUserId(Long userId)`            | 파생 쿼리   | 사용자의 첫 번째 인증 레코드 조회       |

**🔍 JPQL 커스텀 쿼리 예시**:

```sql
SELECT ua FROM UserAuthentication ua
WHERE ua.user.id = :userId AND ua.authProvider.providerName = :providerName
```

**📊 QueryDSL 사용 현황**: ❌ 파생 쿼리와 JPQL만 사용

#### 👤 UserRepository

```java
extends JpaRepository<User, Long>
```

| 메서드                            | 타입      | 설명                             |
| --------------------------------- | --------- | -------------------------------- |
| `findByEmail(String email)`       | 파생 쿼리 | 이메일로 사용자 검색             |
| `findByUserName(String userName)` | 파생 쿼리 | 사용자명(닉네임)으로 사용자 검색 |

**📊 QueryDSL 사용 현황**: ❌ 파생 쿼리만 사용

## 📈 분석 결과 요약

### 🏗️ 리포지토리 구조 비교

```mermaid
graph LR
    subgraph "🔷 Auth-Server 접근법"
        AS1[Spring Data JPA]
        AS2[커스텀 인터페이스]
        AS3[QueryDSL 구현]
        AS1 --> AS2
        AS2 --> AS3
    end

    subgraph "🔶 CBT-back-diary 접근법"
        CBD1[Spring Data JPA]
        CBD2[파생 쿼리]
        CBD3[JPQL 쿼리]
        CBD1 --> CBD2
        CBD1 --> CBD3
    end

    style AS1,CBD1 fill:#4caf50
    style AS2,AS3 fill:#ff9800
    style CBD2,CBD3 fill:#2196f3
```

**핵심 차이점**:

- **🔷 Auth-Server**: 커스텀 리포지토리 인터페이스(`UserRepositoryCustom`)와 구현체(`UserRepositoryImpl`) 패턴을 통해 QueryDSL 활용
- **🔶 CBT-back-diary**: 파생 쿼리 메서드와 `@Query` 어노테이션을 통한 JPQL 활용

### 🔗 QueryDSL 사용량 비교

```mermaid
graph TD
    A[QueryDSL 사용 비교] --> B[🔷 Auth-Server]
    A --> C[🔶 CBT-back-diary]

    B --> B1[✅ 적극 사용]
    B --> B2[배치 비밀번호 업데이트]
    B --> B3[특정 프로젝션 쿼리]
    B --> B4[JPAQueryFactory + Q-types]

    C --> C1[❌ 사용 안함]
    C --> C2[파생 쿼리 선호]
    C --> C3[JPQL로 커스텀 요구사항 해결]

    style B fill:#4caf50
    style C fill:#ff9800
    style B1,B2,B3,B4 fill:#e8f5e8
    style C1,C2,C3 fill:#fff3e0
```

### 🔗 결합도 및 도메인 간 데이터 접근

```mermaid
erDiagram
    USER {
        Long id PK
        String userName
        String email
    }

    DIARY {
        Long id PK
        Long userId FK
        String content
    }

    USER_AUTHENTICATION {
        Long userId FK
        Integer authProviderId FK
        String socialId
    }

    AUTH_PROVIDER {
        Integer id PK
        String providerName
    }

    USER ||--o{ DIARY : "owns"
    USER ||--o{ USER_AUTHENTICATION : "has"
    AUTH_PROVIDER ||--o{ USER_AUTHENTICATION : "provides"
```

**🔍 결합도 분석**:

| 프로젝트           | 리포지토리                     | 결합도 유형     | 설명                               |
| ------------------ | ------------------------------ | --------------- | ---------------------------------- |
| **CBT-back-diary** | `DiaryRepository`              | 자연스러운 결합 | `userId` 기반 필터링 (외래키 관계) |
| **CBT-back-diary** | `UserAuthenticationRepository` | 본질적 결합     | 사용자-인증제공자 연결 목적        |
| **공통**           | `AuthProviderRepository`       | 중복 가능성     | 거의 동일한 구조로 통합 가능       |

### 📊 데이터 접근 패턴 복잡도

```mermaid
graph TD
    A[데이터 접근 패턴] --> B[🔷 Auth-Server]
    A --> C[🔶 CBT-back-diary]

    B --> B1[복잡한 쿼리 도구]
    B --> B2[성능 최적화 지향]
    B --> B3[커스텀 로직 분리]

    C --> C1[단순한 접근법]
    C --> C2[일반적 사용 사례]
    C --> C3[대용량 데이터셋 고려]

    B1 --> B4[QueryDSL 활용]
    B2 --> B5[복잡한 기준 처리]
    B3 --> B6[UserRepositoryCustom 분리]

    C1 --> C7[파생 쿼리 + JPQL]
    C2 --> C8[표준 Spring Data JPA]
    C3 --> C9[DiaryRepository의 Pageable]

    style B fill:#4caf50
    style C fill:#2196f3
    style B1,B2,B3,B4,B5,B6 fill:#e8f5e8
    style C1,C2,C3,C7,C8,C9 fill:#e3f2fd
```

### 📄 페이징 메커니즘

```mermaid
graph TD
    A[페이징 요구사항] --> B{프로젝트 선택}
    B -->|Auth-Server| C[표준 JpaRepository 메서드]
    B -->|CBT-back-diary| D[명시적 Pageable 파라미터]

    C --> E[자동 페이징 지원]
    D --> F[DiaryRepository.findAllByUserId]

    C --> G[커스텀 메서드는 페이징 미구현]
    D --> H[페이징 고려한 설계]

    style E,H fill:#4caf50
    style F fill:#2196f3
    style G fill:#ff9800
```

**페이징 현황**:

- **🔶 CBT-back-diary**: `DiaryRepository`에서 명시적 `Pageable` 사용 (표준 Spring Data JPA 기능)
- **🔷 Auth-Server**: 커스텀 QueryDSL 메서드 `findAllEmail`은 페이징 미구현

## 🎯 종합 결론

### ✅ 강점 분석

#### 🔷 Auth-Server 강점

```mermaid
graph TD
    A[Auth-Server 강점] --> B[고급 쿼리 도구]
    A --> C[성능 최적화]
    A --> D[커스텀 로직 조직화]

    B --> B1[QueryDSL 활용]
    C --> C1[복잡한 기준 처리]
    D --> D1[인터페이스/구현체 분리]

    style A fill:#4caf50
    style B,C,D fill:#e8f5e8
    style B1,C1,D1 fill:#fff3e0
```

#### 🔶 CBT-back-diary 강점

```mermaid
graph TD
    A[CBT-back-diary 강점] --> B[단순하고 직관적]
    A --> C[페이징 지원]
    A --> D[유연한 쿼리]

    B --> B1[파생 쿼리 활용]
    C --> C1[명시적 Pageable]
    D --> D1[JPQL 활용]

    style A fill:#2196f3
    style B,C,D fill:#e3f2fd
    style B1,C1,D1 fill:#fff3e0
```

### ⚠️ 개선 필요사항

| 프로젝트              | 개선 영역            | 권장사항                                |
| --------------------- | -------------------- | --------------------------------------- |
| **🔷 Auth-Server**    | 커스텀 메서드 페이징 | QueryDSL 메서드에 페이징 지원 추가      |
| **🔶 CBT-back-diary** | 복잡한 쿼리 최적화   | 성능이 중요한 부분에 QueryDSL 도입 검토 |
| **공통**              | AuthProvider 중복    | 중앙화된 AuthProvider 관리              |

### 🔮 통합 권장사항

```mermaid
graph TD
    A[현재 상태] --> B[통합 방향]
    B --> C[최적 접근법]

    A1[서로 다른 데이터 접근 패턴] --> B1[강점 결합]
    B1 --> C1[QueryDSL + 파생 쿼리 혼합]

    A2[중복된 AuthProvider] --> B2[중앙화]
    B2 --> C2[단일 AuthProvider 관리]

    A3[페이징 정책 차이] --> B3[표준화]
    B3 --> C3[일관된 페이징 전략]

    style A1,A2,A3 fill:#ffebee
    style B1,B2,B3 fill:#e3f2fd
    style C1,C2,C3 fill:#e8f5e8
```

## 💡 최종 권장사항

### 🎯 통합 전략

1. **Auth-Server의 QueryDSL 인프라 활용**: 복잡한 쿼리와 성능 최적화
2. **CBT-back-diary의 페이징 패턴 채택**: 명시적이고 일관된 페이징 지원
3. **중복 제거**: AuthProvider 리포지토리 통합 또는 정규화

### 📋 실행 계획

```mermaid
gantt
    title 데이터 접근 계층 통합 로드맵
    dateFormat YYYY-MM-DD
    section Phase 1: 분석
    리포지토리 매핑 분석     :p1-1, 2025-06-15, 3d
    중복 제거 계획 수립      :p1-2, after p1-1, 2d
    section Phase 2: 통합
    AuthProvider 통합       :p2-1, after p1-2, 5d
    QueryDSL 패턴 표준화    :p2-2, after p2-1, 7d
    section Phase 3: 최적화
    페이징 정책 통일        :p3-1, after p2-2, 3d
    성능 테스트 및 최적화   :p3-2, after p3-1, 5d
```

> 💡 **결론**: 두 프로젝트 모두 Spring Data JPA를 효과적으로 활용하고 있으며, Auth-Server는 성능 최적화에, CBT-back-diary는 단순성과 페이징에 중점을 두고 있습니다. 통합 시 각각의 강점을 결합하여 더 강력하고 일관된 데이터 접근 계층을 구축할 수 있습니다.
