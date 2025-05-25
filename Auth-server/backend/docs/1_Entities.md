# Entities

This document provides a detailed description of all JPA entities used in the Auth-Server backend. Each entity is mapped to a database table and defines the structure of the data persisted by the application.

---

## 1. AuthProvider

**Table Name**: `Auth_Provider`

**Description**: Stores information about different authentication providers supported by the system, such as internal server authentication or external OAuth2 providers.

**Fields**:

| Field Name          | Type                           | Database Column   | Constraints & Annotations                                  | Description                                                                 |
|---------------------|--------------------------------|-------------------|------------------------------------------------------------|-----------------------------------------------------------------------------|
| `id`                | `Integer`                      | `id`              | `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)` | Primary key, auto-incremented.                                              |
| `providerName`      | `String`                       | `provider_name`   | `@Column(nullable = false, length = 50)`                   | The name of the authentication provider (e.g., "server", "google").        |
| `description`       | `String`                       | `description`     |                                                            | A textual description of the provider.                                      |
| `isActive`          | `Boolean`                      | `is_active`       | `@Builder.Default = true`                                  | Flag indicating if the provider is currently active. Defaults to true.      |
| `userAuthentications` | `List<UserAuthentication>`     | (Mapped by)       | `@OneToMany(mappedBy = "authProvider", cascade = CascadeType.ALL)` | List of user authentications associated with this provider.                 |

**Enums**:

### 1.1. ProviderType

**Description**: Defines the types of authentication providers.

| Enum Constant | Code Value   | Description           |
|---------------|--------------|-----------------------|
| `SERVER`      | "server"     | 내부 서버 인증        |
| `GOOGLE`      | "google"     | 구글 OAuth2           |
| `KAKAO`       | "kakao"      | 카카오 OAuth2         |
| `NAVER`       | "naver"      | 네이버 OAuth2         |
| `FACEBOOK`    | "facebook"   | 페이스북 OAuth2       |
| `GITHUB`      | "github"     | 깃허브 OAuth2         |

**Methods (within ProviderType enum)**:

*   `getCode()`: Returns the string code of the provider type.
*   `getDescription()`: Returns the textual description of the provider type.
*   `fromCode(String code)`: Static method to get a `ProviderType` enum from its string code. Defaults to `SERVER` if not found.

---

## 2. Diary

**Table Name**: `Diary`

**Description**: Represents a diary entry made by a user. It stores the content of the diary, timestamps, and potentially an AI-generated alternative thought.

**Fields**:

| Field Name           | Type              | Database Column       | Constraints & Annotations                                               | Description                                                                  |
|----------------------|-------------------|-----------------------|-------------------------------------------------------------------------|------------------------------------------------------------------------------|
| `id`                 | `Long`            | `id`                  | `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)`              | Primary key, auto-incremented.                                               |
| `user`               | `User`            | `user_id`             | `@ManyToOne(fetch = FetchType.LAZY)`, `@JoinColumn(nullable = false)`    | The user who created this diary entry. Lazily fetched.                       |
| `createdAt`          | `LocalDateTime`   | `created_at`          |                                                                         | Timestamp when the diary entry was created. Set automatically on persist.    |
| `updatedAt`          | `LocalDateTime`   | `updated_at`          |                                                                         | Timestamp when the diary entry was last updated. Set automatically.          |
| `title`              | `String`          | `title`               |                                                                         | The title of the diary entry (optional).                                     |
| `content`            | `String`          | `content`             | `@Column(nullable = false)`                                             | The main content of the diary entry. Cannot be null.                         |
| `alternativeThought` | `String`          | `alternative_thought` |                                                                         | An alternative perspective or thought, possibly AI-generated.                |
| `diaryAnswer`        | `DiaryAnswer`     | (Mapped by)           | `@OneToOne(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true)` | An associated answer or analysis related to this diary entry. Cascades all operations and removes orphans. |

**Lifecycle Callbacks**:

*   `@PrePersist protected void onCreate()`: Sets `createdAt` and `updatedAt` to the current time before the entity is persisted.
*   `@PreUpdate protected void onUpdate()`: Sets `updatedAt` to the current time before the entity is updated.

---

## 3. DiaryAnswer

**Table Name**: `Diary_Answer`

**Description**: Stores the analysis or answer related to a specific diary entry. This often includes results from emotion detection or cognitive behavioral therapy (CBT) related prompts.

**Fields**:

| Field Name           | Type              | Database Column         | Constraints & Annotations                                            | Description                                                                  |
|----------------------|-------------------|-------------------------|----------------------------------------------------------------------|------------------------------------------------------------------------------|
| `id`                 | `Long`            | `id`                    | `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)`           | Primary key, auto-incremented.                                               |
| `diary`              | `Diary`           | `diary_id`              | `@OneToOne(fetch = FetchType.LAZY)`, `@JoinColumn(nullable = false)`    | The diary entry this answer/analysis pertains to. Lazily fetched.            |
| `createdAt`          | `LocalDateTime`   | `created_at`            |                                                                      | Timestamp when this answer was created.                                      |
| `updatedAt`          | `LocalDateTime`   | `updated_at`            |                                                                      | Timestamp when this answer was last updated.                                 |
| `emotionDetection`   | `String`          | `emotion_detection`     |                                                                      | Text describing the detected emotions from the diary content.                |
| `automaticThought`   | `String`          | `automatic_thought`     |                                                                      | Identified automatic negative thoughts (ANTs) or similar cognitive patterns. |
| `promptForChange`    | `String`          | `prompt_for_change`     |                                                                      | A prompt or question designed to encourage a change in thinking or behavior. |
| `alternativeThought` | `String`          | `alternative_thought`   |                                                                      | A suggested alternative, more constructive thought.                          |
| `status`             | `EmotionStatus`   | `status`                | `@Enumerated(EnumType.STRING)`, `@Column(nullable = false)`, Default: `NEUTRAL` | The overall emotional status derived from the analysis.                      |

**Enums**:

### 3.1. EmotionStatus

**Description**: Represents the classified emotional status of the diary entry after analysis.

| Enum Constant | Description                                  |
|---------------|----------------------------------------------|
| `POSITIVE`    | Indicates a predominantly positive sentiment.  |
| `NEGATIVE`    | Indicates a predominantly negative sentiment.  |
| `NEUTRAL`     | Indicates a neutral or mixed sentiment.        |

---

## 4. SettingsOption

**Table Name**: `Settings_option` (Note: The class name is `SettingsOption` but the table name in the annotation is `Settings_option`)

**Description**: Defines an individual application setting or option. It stores the key, default value, data type, description, and whether the setting is editable by users.

**Fields**:

| Field Name           | Type                        | Database Column      | Constraints & Annotations                                                       | Description                                                                  |
|----------------------|-----------------------------|----------------------|---------------------------------------------------------------------------------|------------------------------------------------------------------------------|
| `id`                 | `Integer`                   | `id`                 | `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)`                      | Primary key, auto-incremented.                                               |
| `settingKey`         | `String`                    | `setting_key`        | `@Column(nullable = false, unique = true, length = 50)`                         | Unique key identifying the setting (e.g., "theme.color", "notification.enabled"). |
| `defaultValue`       | `String`                    | `default_value`      | `@Column(nullable = false)`                                                     | The default value for this setting, stored as a string.                      |
| `dataType`           | `DataType` (enum)           | `data_type`          | `@Enumerated(EnumType.STRING)`, `@Column(nullable = false)`                       | The data type of the setting's value (e.g., STRING, BOOLEAN).                |
| `description`        | `String`                    | `description`        |                                                                                 | A textual description of what the setting controls.                          |
| `isUserEditable`     | `Boolean`                   | `is_user_editable`   | Default: `true`                                                                 | Flag indicating if users are allowed to modify this setting. Defaults to true. |
| `userCustomSettings` | `List<UserCustomSetting>`   | (Mapped by)          | `@OneToMany(mappedBy = "settingsOption", cascade = CascadeType.ALL)`            | List of custom user settings that override this default option.              |

**Enums**:

### 4.1. DataType

**Description**: Represents the data type of a setting's value.

| Enum Constant | Description                         |
|---------------|-------------------------------------|
| `STRING`      | The setting value is a string.      |
| `NUMBER`      | The setting value is a number.      |
| `BOOLEAN`     | The setting value is a boolean.     |
| `JSON`        | The setting value is a JSON string. |

---

## 5. User

**Table Name**: `Users`

**Description**: Represents an application user. It stores user credentials, profile information, status, roles, and associations with other entities like diaries and settings.

**Fields**:

| Field Name        | Type                          | Database Column | Constraints & Annotations                                               | Description                                                                  |
|-------------------|-------------------------------|-----------------|-------------------------------------------------------------------------|------------------------------------------------------------------------------|
| `id`              | `Long`                        | `id`            | `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)`              | Primary key, auto-incremented.                                               |
| `password`        | `String`                      | `password`      | `@Column(nullable = false)`                                             | User's hashed password.                                                      |
| `userName`        | `String`                      | `user_name`     | `@Column(nullable = false, length = 30, unique = true)`                 | Unique username for login.                                                   |
| `nickname`        | `String`                      | `nickname`      | `@Column(length = 50, unique = true)`                                   | User's display nickname, must be unique.                                     |
| `email`           | `String`                      | `email`         | `@Column(nullable = false, length = 100, unique = true)`                | User's email address, must be unique and is not nullable.                    |
| `createdAt`       | `LocalDateTime`               | `created_at`    |                                                                         | Timestamp when the user account was created. Set automatically on persist.   |
| `updatedAt`       | `LocalDateTime`               | `updated_at`    |                                                                         | Timestamp when the user account was last updated. Set automatically.         |
| `lastLogin`       | `LocalDateTime`               | `last_login`    |                                                                         | Timestamp of the user's last login.                                          |
| `userRole`        | `UserRole` (enum)             | `user_role`     | `@Enumerated(EnumType.STRING)`, `@Column(nullable = false)`, Default: `USER` | Role of the user (e.g., USER, ADMIN). Defaults to `USER`.                  |
| `isPremium`       | `Boolean`                     | `is_premium`    | `@Builder.Default = false`                                              | Flag indicating if the user has a premium account. Defaults to false.        |
| `isActive`        | `UserStatus` (enum)           | `is_active`     | `@Enumerated(EnumType.STRING)`, `@Column(nullable = false)`, Default: `WAITING` | Current status of the user account (e.g., ACTIVE, WAITING). Defaults to `WAITING`. |
| `diaries`         | `List<Diary>`                 | (Mapped by)     | `@OneToMany(mappedBy = "user")`, `@Builder.Default`                       | List of diary entries created by the user.                                   |
| `authentications` | `List<UserAuthentication>`    | (Mapped by)     | `@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)`, `@Builder.Default` | List of authentication methods linked to the user.                           |
| `customSettings`  | `List<UserCustomSetting>`     | (Mapped by)     | `@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)`, `@Builder.Default` | List of custom settings applied by the user.                                 |

**Lifecycle Callbacks**:

*   `@PrePersist protected void onCreate()`: Sets `createdAt` and `updatedAt` to the current time before the entity is persisted.
*   `@PreUpdate protected void onUpdate()`: Sets `updatedAt` to the current time before the entity is updated.

**Enums**:

### 5.1. UserRole

**Description**: Defines the roles a user can have within the application.

| Enum Constant | Description   |
|---------------|---------------|
| `USER`        | 일반 사용자   |
| `ADMIN`       | 관리자        |

### 5.2. UserStatus

**Description**: Defines the possible statuses of a user account.

| Enum Constant | Value       | Description             |
|---------------|-------------|-------------------------|
| `ACTIVE`      | "active"    | 활성화된 계정           |
| `WAITING`     | "waiting"   | 이메일 인증 대기 중     |
| `BLOCKED`     | "blocked"   | 차단된 계정             |
| `SUSPEND`     | "suspend"   | 일시 정지된 계정        |
| `DELETE`      | "delete"    | 삭제된 계정             |

**Methods (within UserStatus enum)**:

*   `getValue()`: Returns the string value of the status (e.g., "active").
*   `getDescription()`: Returns the textual description of the status.
*   `toString()`: Overridden to return the `value` of the status.

---

## 6. UserAuthentication & UserAuthenticationId

### 6.1. UserAuthenticationId (Embeddable)

**Description**: Represents the composite primary key for the `UserAuthentication` entity. It combines `userId` and `authProviderId`.

**Fields**:

| Field Name       | Type      | Database Column    | Description                                      |
|------------------|-----------|--------------------|--------------------------------------------------|
| `userId`         | `Long`    | `user_id`          | Foreign key referencing the `User` entity's ID.    |
| `authProviderId` | `Integer` | `auth_provider_id` | Foreign key referencing the `AuthProvider` entity's ID. |

### 6.2. UserAuthentication

**Table Name**: `User_Authentication`

**Unique Constraints**:
*   `("auth_provider_id", "social_id")`: Ensures that the combination of an authentication provider and a social ID is unique.

**Description**: Links a `User` to a specific `AuthProvider` (e.g., Google, Kakao) and stores authentication-specific details like the user's ID on that social platform.

**Fields**:

| Field Name     | Type                   | Database Column    | Constraints & Annotations                                                                    | Description                                                                                    |
|----------------|------------------------|--------------------|----------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------|
| `id`           | `UserAuthenticationId` | (Embedded ID)      | `@EmbeddedId`                                                                                | Composite primary key (`userId`, `authProviderId`).                                            |
| `user`         | `User`                 | `user_id`          | `@ManyToOne(fetch = FetchType.LAZY)`, `@MapsId("userId")`, `@JoinColumn(name = "user_id")`       | The user associated with this authentication method. Maps `userId` part of the embedded ID.     |
| `authProvider` | `AuthProvider`         | `auth_provider_id` | `@ManyToOne(fetch = FetchType.LAZY)`, `@MapsId("authProviderId")`, `@JoinColumn(name = "auth_provider_id")` | The authentication provider used. Maps `authProviderId` part of the embedded ID.               |
| `socialId`     | `String`               | `social_id`        | `@Column(nullable = false)`                                                                  | The user's unique identifier on the external social platform (e.g., Google ID, Kakao ID).      |
| `email`        | `String`               | `email`            |                                                                                              | The email address associated with this specific social authentication (if provided).           |
| `createdAt`    | `LocalDateTime`        | `created_at`       |                                                                                              | Timestamp when this authentication method was linked. Set automatically on persist.          |
| `updatedAt`    | `LocalDateTime`        | `updated_at`       |                                                                                              | Timestamp when this authentication method was last updated. Set automatically.               |

**Lifecycle Callbacks**:

*   `@PrePersist protected void onCreate()`: Sets `createdAt` and `updatedAt` to the current time before the entity is persisted.
*   `@PreUpdate protected void onUpdate()`: Sets `updatedAt` to the current time before the entity is updated.

---

## 7. UserCustomSetting & UserCustomSettingId

### 7.1. UserCustomSettingId (Embeddable)

**Description**: Represents the composite primary key for the `UserCustomSetting` entity. It combines `userId` and `settingId`.

**Fields**:

| Field Name | Type    | Database Column | Description                                         |
|------------|---------|-----------------|-----------------------------------------------------|
| `userId`   | `Long`  | `user_id`       | Foreign key referencing the `User` entity's ID.       |
| `settingId`| `Integer`| `setting_id`    | Foreign key referencing the `SettingsOption` entity's ID. |

### 7.2. UserCustomSetting

**Table Name**: `User_custom_setting`

**Description**: Stores a user's specific overridden value for a particular application setting (`SettingsOption`).

**Fields**:

| Field Name       | Type                  | Database Column  | Constraints & Annotations                                                                           | Description                                                                                         |
|------------------|-----------------------|------------------|-----------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------|
| `id`             | `UserCustomSettingId` | (Embedded ID)    | `@EmbeddedId`                                                                                       | Composite primary key (`userId`, `settingId`).                                                      |
| `user`           | `User`                | `user_id`        | `@ManyToOne(fetch = FetchType.LAZY)`, `@MapsId("userId")`, `@JoinColumn(name = "user_id")`              | The user who owns this custom setting. Maps `userId` part of the embedded ID.                       |
| `settingsOption` | `SettingsOption`      | `setting_id`     | `@ManyToOne(fetch = FetchType.LAZY)`, `@MapsId("settingId")`, `@JoinColumn(name = "setting_id")`       | The application setting that is being overridden. Maps `settingId` part of the embedded ID.         |
| `overrideValue`  | `String`              | `override_value` | `@Column(nullable = false)`                                                                         | The value specified by the user to override the default setting.                                    |
| `createdAt`      | `LocalDateTime`       | `created_at`     |                                                                                                     | Timestamp when this custom setting was created. (Lifecycle callbacks not explicitly defined here). |
| `updatedAt`      | `LocalDateTime`       | `updated_at`     |                                                                                                     | Timestamp when this custom setting was last updated. (Lifecycle callbacks not explicitly defined here).|

---
