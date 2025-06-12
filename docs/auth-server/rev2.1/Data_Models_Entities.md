# 엔티티

이 문서는 Auth-Server 백엔드에서 사용되는 모든 JPA 엔티티에 대한 상세 설명을 제공합니다. 각 엔티티는 데이터베이스 테이블에 매핑되며, 애플리케이션이 영속화하는 데이터의 구조를 정의합니다.

---

## 1. AuthProvider

**테이블명**: `Auth_Provider`

**설명**: 시스템에서 지원하는 다양한 인증 제공자(내부 서버 인증, 외부 OAuth2 제공자 등)에 대한 정보를 저장합니다.

**필드:**

| 필드명                | 타입                           | DB 컬럼명         | 제약조건 및 어노테이션                                  | 설명                                                                 |
|---------------------|--------------------------------|-------------------|------------------------------------------------------------|---------------------------------------------------------------------|
| `id`                | `Integer`                      | `id`              | `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)` | 기본키, 자동 증가.                                                  |
| `providerName`      | `String`                       | `provider_name`   | `@Column(nullable = false, length = 50)`                   | 인증 제공자 이름 (예: "server", "google").                        |
| `description`       | `String`                       | `description`     |                                                            | 제공자에 대한 설명.                                                  |
| `isActive`          | `Boolean`                      | `is_active`       | `@Builder.Default = true`                                  | 현재 활성화 여부. 기본값 true.                                      |
| `userAuthentications` | `List<UserAuthentication>`     | (Mapped by)       | `@OneToMany(mappedBy = "authProvider", cascade = CascadeType.ALL)` | 이 제공자와 연결된 사용자 인증 목록.                                 |

**Enums:**

### 1.1. ProviderType

**설명**: 인증 제공자 유형을 정의합니다.

| Enum 상수 | 코드 값   | 설명           |
|-----------|----------|----------------|
| `SERVER`  | "server" | 내부 서버 인증 |
| `GOOGLE`  | "google" | 구글 OAuth2    |
| `KAKAO`   | "kakao"  | 카카오 OAuth2  |
| `NAVER`   | "naver"  | 네이버 OAuth2  |
| `FACEBOOK`| "facebook"| 페이스북 OAuth2|
| `GITHUB`  | "github" | 깃허브 OAuth2  |

**ProviderType enum 내 메서드:**
*   `getCode()`: 제공자 타입의 문자열 코드 반환
*   `getDescription()`: 제공자 타입의 설명 반환
*   `fromCode(String code)`: 문자열 코드로 ProviderType enum 반환. 없으면 기본값은 SERVER

---

## 2. Diary

**테이블명**: `Diary`

**설명**: 사용자가 작성한 다이어리(일기) 엔트리를 나타냅니다. 일기 내용, 타임스탬프, AI가 생성한 대안적 사고 등이 저장됩니다.

**필드:**

| 필드명           | 타입              | DB 컬럼명       | 제약조건 및 어노테이션                                               | 설명                                                                  |
|------------------|-------------------|------------------|-----------------------------------------------------------------------|------------------------------------------------------------------------|
| `id`             | `Long`            | `id`             | `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)`          | 기본키, 자동 증가.                                                     |
| `user`           | `User`            | `user_id`        | `@ManyToOne(fetch = FetchType.LAZY)`, `@JoinColumn(nullable = false)`    | 이 일기를 작성한 사용자. Lazily fetched.                                |
| `createdAt`      | `LocalDateTime`   | `created_at`     |                                                                       | 생성 시각. 영속화 시 자동 설정.                                        |
| `updatedAt`      | `LocalDateTime`   | `updated_at`     |                                                                       | 마지막 수정 시각. 자동 갱신.                                           |
| `title`          | `String`          | `title`          |                                                                       | 일기 제목(선택).                                                       |
| `content`        | `String`          | `content`        | `@Column(nullable = false)`                                             | 일기 본문. 필수.                                                       |
| `alternativeThought` | `String`      | `alternative_thought` |                                                               | AI가 생성한 대안적 사고 등.                                            |
| `diaryAnswer`    | `DiaryAnswer`     | (Mapped by)      | `@OneToOne(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true)` | 이 일기와 연관된 분석/답변. 모든 연산 cascade, orphan 제거.            |

**라이프사이클 콜백:**
*   `@PrePersist protected void onCreate()`: 영속화 전 `createdAt`, `updatedAt`를 현재 시각으로 설정
*   `@PreUpdate protected void onUpdate()`: 업데이트 전 `updatedAt`을 현재 시각으로 설정

---

## 3. DiaryAnswer

**테이블명**: `Diary_Answer`

**설명**: 특정 다이어리 엔트리와 연관된 분석/답변을 저장합니다. 주로 감정 분석, 인지행동치료(CBT) 프롬프트 결과 등이 포함됩니다.

**필드:**

| 필드명           | 타입              | DB 컬럼명         | 제약조건 및 어노테이션                                            | 설명                                                                  |
|------------------|-------------------|--------------------|--------------------------------------------------------------------|------------------------------------------------------------------------|
| `id`             | `Long`            | `id`               | `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)`        | 기본키, 자동 증가.                                                     |
| `diary`          | `Diary`           | `diary_id`         | `@OneToOne(fetch = FetchType.LAZY)`, `@JoinColumn(nullable = false)`    | 이 분석이 속한 다이어리. Lazily fetched.                                |
| `createdAt`      | `LocalDateTime`   | `created_at`       |                                                                    | 생성 시각.                                                             |
| `updatedAt`      | `LocalDateTime`   | `updated_at`       |                                                                    | 마지막 수정 시각.                                                      |
| `emotionDetection`| `String`          | `emotion_detection` |                                                                   | 감정 분석 결과 텍스트.                                                 |
| `automaticThought`| `String`          | `automatic_thought` |                                                                  | 자동적 부정 사고(ANTs) 등 인지 패턴.                                   |
| `promptForChange` | `String`          | `prompt_for_change` |                                                                  | 사고/행동 변화를 유도하는 프롬프트.                                    |
| `alternativeThought`| `String`       | `alternative_thought`|                                                                | 대안적, 더 건설적인 사고 제안.                                         |
| `status`         | `EmotionStatus`   | `status`           | `@Enumerated(EnumType.STRING)`, `@Column(nullable = false)`, 기본값: `NEUTRAL` | 분석 결과로 분류된 전체 감정 상태.                                     |

**Enums:**

### 3.1. EmotionStatus

**설명**: 분석 후 일기의 감정 상태를 분류합니다.

| Enum 상수 | 설명                                  |
|-----------|----------------------------------------|
| `POSITIVE`| 긍정적 감정 우세                       |
| `NEGATIVE`| 부정적 감정 우세                       |
| `NEUTRAL` | 중립 또는 혼합 감정                    |

---

## 4. SettingsOption

**테이블명**: `Settings_option` (클래스명은 `SettingsOption`, 어노테이션의 테이블명은 `Settings_option`)

**설명**: 개별 애플리케이션 설정/옵션을 정의합니다. 키, 기본값, 데이터 타입, 설명, 사용자 편집 가능 여부 등을 저장합니다.

**필드:**

| 필드명           | 타입                        | DB 컬럼명      | 제약조건 및 어노테이션                                                       | 설명                                                                  |
|------------------|-----------------------------|----------------|-------------------------------------------------------------------------------|------------------------------------------------------------------------|
| `id`             | `Integer`                   | `id`           | `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)`                  | 기본키, 자동 증가.                                                     |
| `settingKey`     | `String`                    | `setting_key`  | `@Column(nullable = false, unique = true, length = 50)`                       | 설정을 식별하는 고유 키 (예: "theme.color", "notification.enabled"). |
| `defaultValue`   | `String`                    | `default_value`| `@Column(nullable = false)`                                                   | 이 설정의 기본값(문자열로 저장).                                       |
| `dataType`       | `DataType` (enum)           | `data_type`    | `@Enumerated(EnumType.STRING)`, `@Column(nullable = false)`                   | 값의 데이터 타입 (예: STRING, BOOLEAN).                                 |
| `description`    | `String`                    | `description`  |                                                                               | 이 설정이 제어하는 내용에 대한 설명.                                    |
| `isUserEditable` | `Boolean`                   | `is_user_editable`| 기본값: `true`                                                             | 사용자가 이 설정을 수정할 수 있는지 여부. 기본값 true.                 |
| `userCustomSettings` | `List<UserCustomSetting>` | (Mapped by)    | `@OneToMany(mappedBy = "settingsOption", cascade = CascadeType.ALL)`        | 이 기본 옵션을 오버라이드하는 사용자별 커스텀 설정 목록.                |

**Enums:**

### 4.1. DataType

**설명**: 설정 값의 데이터 타입을 나타냅니다.

| Enum 상수 | 설명                         |
|-----------|-----------------------------|
| `STRING`  | 값이 문자열                  |
| `NUMBER`  | 값이 숫자                    |
| `BOOLEAN` | 값이 불리언                  |
| `JSON`    | 값이 JSON 문자열             |

---

## 5. User

**테이블명**: `Users`

**설명**: 애플리케이션 사용자를 나타냅니다. 사용자 자격 증명, 프로필 정보, 상태, 역할, 다이어리/설정 등과의 연관관계를 저장합니다.

**필드:**

| 필드명        | 타입                          | DB 컬럼명 | 제약조건 및 어노테이션                                               | 설명                                                                  |
|---------------|-------------------------------|-----------|-----------------------------------------------------------------------|------------------------------------------------------------------------|
| `id`          | `Long`                        | `id`      | `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)`          | 기본키, 자동 증가.                                                     |
| `password`    | `String`                      | `password`| `@Column(nullable = false)`                                             | 사용자의 해시된 비밀번호.                                              |
| `userName`    | `String`                      | `user_name`| `@Column(nullable = false, length = 30, unique = true)`               | 로그인용 고유 사용자명.                                                |
| `nickname`    | `String`                      | `nickname`| `@Column(length = 50, unique = true)`                                 | 사용자 닉네임(고유).                                                    |
| `email`       | `String`                      | `email`   | `@Column(nullable = false, length = 100, unique = true)`              | 이메일(고유, 필수).                                                     |
| `createdAt`   | `LocalDateTime`               | `created_at`|                                                                   | 계정 생성 시각. 영속화 시 자동 설정.                                   |
| `updatedAt`   | `LocalDateTime`               | `updated_at`|                                                                  | 마지막 수정 시각. 자동 갱신.                                           |
| `lastLogin`   | `LocalDateTime`               | `last_login`|                                                                 | 마지막 로그인 시각.                                                    |
| `userRole`    | `UserRole` (enum)             | `user_role`| `@Enumerated(EnumType.STRING)`, `@Column(nullable = false)`, 기본값: `USER` | 사용자 역할(예: USER, ADMIN). 기본값 USER.                             |
| `isPremium`   | `Boolean`                     | `is_premium`| `@Builder.Default = false`                                            | 프리미엄 계정 여부. 기본값 false.                                       |
| `isActive`    | `UserStatus` (enum)           | `is_active`| `@Enumerated(EnumType.STRING)`, `@Column(nullable = false)`, 기본값: `WAITING` | 계정 상태(예: ACTIVE, WAITING). 기본값 WAITING.                        |
| `diaries`     | `List<Diary>`                 | (Mapped by)| `@OneToMany(mappedBy = "user")`, `@Builder.Default`                  | 사용자가 작성한 다이어리 목록.                                          |
| `authentications` | `List<UserAuthentication>`  | (Mapped by)| `@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)`, `@Builder.Default` | 사용자와 연결된 인증 방법 목록.                                         |
| `customSettings`  | `List<UserCustomSetting>`   | (Mapped by)| `@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)`, `@Builder.Default` | 사용자가 적용한 커스텀 설정 목록.                                       |

**라이프사이클 콜백:**
*   `@PrePersist protected void onCreate()`: 영속화 전 `createdAt`, `updatedAt`를 현재 시각으로 설정
*   `@PreUpdate protected void onUpdate()`: 업데이트 전 `updatedAt`을 현재 시각으로 설정

**Enums:**

### 5.1. UserRole

**설명**: 사용자가 가질 수 있는 역할을 정의합니다.

| Enum 상수 | 설명   |
|-----------|--------|
| `USER`    | 일반 사용자 |
| `ADMIN`   | 관리자    |

### 5.2. UserStatus

**설명**: 사용자 계정의 상태를 정의합니다.

| Enum 상수 | 값         | 설명             |
|-----------|------------|------------------|
| `ACTIVE`  | "active"   | 활성화된 계정    |
| `WAITING` | "waiting"  | 이메일 인증 대기 |
| `BLOCKED` | "blocked"  | 차단된 계정      |
| `SUSPEND` | "suspend"  | 일시 정지 계정   |
| `DELETE`  | "delete"   | 삭제된 계정      |

**UserStatus enum 내 메서드:**
*   `getValue()`: 상태의 문자열 값 반환 (예: "active")
*   `getDescription()`: 상태의 설명 반환
*   `toString()`: 오버라이드, 상태의 value 반환

---

## 6. UserAuthentication & UserAuthenticationId

### 6.1. UserAuthenticationId (Embeddable)

**설명**: `UserAuthentication` 엔티티의 복합 기본키. `userId`와 `authProviderId`를 조합.

**필드:**

| 필드명       | 타입      | DB 컬럼명    | 설명                                      |
|--------------|-----------|-------------|--------------------------------------------|
| `userId`     | `Long`    | `user_id`   | User 엔티티의 ID를 참조하는 외래키.        |
| `authProviderId` | `Integer` | `auth_provider_id` | AuthProvider 엔티티의 ID를 참조하는 외래키. |

### 6.2. UserAuthentication

**테이블명**: `User_Authentication`

**고유 제약조건:**
*   `("auth_provider_id", "social_id")`: 인증 제공자와 소셜 ID의 조합이 유일해야 함.

**설명**: `User`와 특정 `AuthProvider`(예: Google, Kakao)를 연결하고, 소셜 플랫폼에서의 사용자 ID 등 인증 관련 정보를 저장합니다.

**필드:**

| 필드명     | 타입                   | DB 컬럼명    | 제약조건 및 어노테이션                                                                    | 설명                                                                                    |
|------------|------------------------|--------------|------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------|
| `id`       | `UserAuthenticationId` | (Embedded ID)| `@EmbeddedId`                                                                            | 복합 기본키 (`userId`, `authProviderId`).                                                |
| `user`     | `User`                 | `user_id`    | `@ManyToOne(fetch = FetchType.LAZY)`, `@MapsId("userId")`, `@JoinColumn(name = "user_id")` | 이 인증 방법과 연결된 사용자. Embedded ID의 userId와 매핑.                               |
| `authProvider` | `AuthProvider`       | `auth_provider_id` | `@ManyToOne(fetch = FetchType.LAZY)`, `@MapsId("authProviderId")`, `@JoinColumn(name = "auth_provider_id")` | 사용된 인증 제공자. Embedded ID의 authProviderId와 매핑.                                 |
| `socialId` | `String`               | `social_id`  | `@Column(nullable = false)`                                                              | 외부 소셜 플랫폼에서의 사용자 고유 ID(예: Google ID, Kakao ID).                          |
| `email`    | `String`               | `email`      |                                                                                          | 이 소셜 인증에 연결된 이메일(있을 경우).                                                 |
| `createdAt`| `LocalDateTime`        | `created_at` |                                                                                          | 이 인증 방법이 연결된 시각. 영속화 시 자동 설정.                                         |
| `updatedAt`| `LocalDateTime`        | `updated_at` |                                                                                          | 마지막 수정 시각. 자동 갱신.                                                             |

**라이프사이클 콜백:**
*   `@PrePersist protected void onCreate()`: 영속화 전 `createdAt`, `updatedAt`를 현재 시각으로 설정
*   `@PreUpdate protected void onUpdate()`: 업데이트 전 `updatedAt`을 현재 시각으로 설정

---

## 7. UserCustomSetting & UserCustomSettingId

### 7.1. UserCustomSettingId (Embeddable)

**설명**: `UserCustomSetting` 엔티티의 복합 기본키. `userId`와 `settingId`를 조합.

**필드:**

| 필드명 | 타입    | DB 컬럼명 | 설명                                         |
|--------|---------|-----------|-----------------------------------------------|
| `userId`   | `Long`  | `user_id`   | User 엔티티의 ID를 참조하는 외래키.           |
| `settingId`| `Integer`| `setting_id`| SettingsOption 엔티티의 ID를 참조하는 외래키. |

### 7.2. UserCustomSetting

**테이블명**: `User_custom_setting`

**설명**: 특정 애플리케이션 설정(`SettingsOption`)에 대해 사용자가 오버라이드한 값을 저장합니다.

**필드:**

| 필드명       | 타입                  | DB 컬럼명  | 제약조건 및 어노테이션                                                                           | 설명                                                                                         |
|--------------|-----------------------|------------|-------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------|
| `id`         | `UserCustomSettingId` | (Embedded ID)| `@EmbeddedId`                                                                                   | 복합 기본키 (`userId`, `settingId`).                                                         |
| `user`       | `User`                | `user_id`  | `@ManyToOne(fetch = FetchType.LAZY)`, `@MapsId("userId")`, `@JoinColumn(name = "user_id")`      | 이 커스텀 설정의 소유자. Embedded ID의 userId와 매핑.                                         |
| `settingsOption` | `SettingsOption`   | `setting_id`| `@ManyToOne(fetch = FetchType.LAZY)`, `@MapsId("settingId")`, `@JoinColumn(name = "setting_id")` | 오버라이드 대상 애플리케이션 설정. Embedded ID의 settingId와 매핑.                            |
| `overrideValue`  | `String`            | `override_value`| `@Column(nullable = false)`                                                                     | 사용자가 지정한 오버라이드 값.                                                               |
| `createdAt`      | `LocalDateTime`     | `created_at` |                                                                                                 | 생성 시각. (명시적 라이프사이클 콜백 없음)                                                   |
| `updatedAt`      | `LocalDateTime`     | `updated_at` |                                                                                                 | 마지막 수정 시각. (명시적 라이프사이클 콜백 없음)                                             |

---
