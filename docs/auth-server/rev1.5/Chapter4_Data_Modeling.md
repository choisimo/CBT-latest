# Chapter 4: Data Modeling

## 4.1. Conceptual Data Model

The core data entities for the Emotion-based AI Diary Application are:

*   **User:** Represents an individual using the application. Stores profile information, credentials, and status. This is the central entity.
*   **Diary:** Represents a single diary entry made by a User. Contains the text content, title, and timestamps.
*   **DiaryAnalysis (or DiaryAnswer):** Stores the results of AI-based analysis performed on a Diary entry. This includes detected emotions, identified automatic thoughts, and suggested alternative thoughts. This is closely related to a Diary. (Note: `Data_Models_Entities.md` calls this `DiaryAnswer`, while `schema.sql` does not have a separate table for it, suggesting its data might be in `Diary` table or, as preferred for AI results, in a NoSQL store like MongoDB).
*   **SettingsOption:** Defines a global application setting, its default value, type, and whether it's user-editable.
*   **UserCustomSetting:** Stores a User's specific overridden value for a SettingsOption.
*   **AuthProvider:** Stores details about authentication providers (e.g., "server", "google").
*   **UserAuthentication:** Links a User to an AuthProvider and stores their social ID for that provider.
*   **Report (from schema.sql):** Represents a consolidated report based on multiple diary entries for a user.
*   **DiaryReportLink (from schema.sql):** A join table linking Diaries to Reports in a many-to-many relationship.

**Key Relationships (Conceptual):**
*   A `User` can have many `Diary` entries. (One-to-Many)
*   A `Diary` entry belongs to one `User`. (Many-to-One)
*   A `Diary` entry can have one `DiaryAnalysis` result (One-to-One, if analysis is directly linked).
*   A `User` can have many `UserCustomSetting` records. (One-to-Many)
*   A `SettingsOption` can be customized by many Users (via `UserCustomSetting`). (One-to-Many)
*   A `User` can have multiple `UserAuthentication` methods (e.g., one for local, one for Google). (One-to-Many)
*   An `AuthProvider` can be associated with many `UserAuthentication` records. (One-to-Many)
*   A `User` can have many `Report`s. (One-to-Many)
*   A `Diary` can be part of many `Report`s, and a `Report` can include many `Diary` entries (Many-to-Many, via `DiaryReportLink`).

## 4.2. Logical Data Model

This section describes entities with key attributes without specifying RDBMS/NoSQL details.

*   **User**
    *   Attributes: User ID (PK), Username (Login ID), Hashed Password, Email, Nickname, Role (USER, ADMIN), Status (ACTIVE, WAITING), Premium Status, Timestamps (Created, Updated, Last Login).
    *   Relationships: Has many Diaries, Has many UserCustomSettings, Has many UserAuthentications, Has many Reports.
*   **Diary**
    *   Attributes: Diary ID (PK), Title, Content, Alternative Thought (AI-generated, stored with diary), Negative Emotion Flag, Timestamps (Created, Updated).
    *   Relationships: Belongs to one User, (Conceptually) Has one DiaryAnalysis.
*   **DiaryAnalysis (DiaryAnswer)**
    *   Attributes: Analysis ID (PK), Detected Emotions, Automatic Thoughts, Prompt for Change, Alternative Thought (AI-generated), Overall Emotion Status, Timestamp (Analyzed At).
    *   Relationships: Belongs to one Diary.
*   **SettingsOption**
    *   Attributes: Setting ID (PK), Setting Key (unique), Default Value, Data Type (STRING, BOOLEAN, NUMBER, JSON), Description, User Editable Flag.
    *   Relationships: Has many UserCustomSettings.
*   **UserCustomSetting**
    *   Attributes: User ID (FK), Setting ID (FK), Override Value, Timestamps (Created, Updated).
    *   Relationships: Belongs to one User, Belongs to one SettingsOption. (Composite PK: UserID, SettingID)
*   **AuthProvider**
    *   Attributes: Provider ID (PK), Provider Name (unique, e.g., "server", "google"), Description, Active Flag.
    *   Relationships: Has many UserAuthentications.
*   **UserAuthentication**
    *   Attributes: User ID (FK), Provider ID (FK), Social ID (from provider), Email (from provider), Timestamps (Created, Updated).
    *   Relationships: Belongs to one User, Belongs to one AuthProvider. (Composite PK: UserID, ProviderID)
*   **Report**
    *   Attributes: Report ID (PK), Summary Title, Change Process description, Timestamps (Created, Updated).
    *   Relationships: Belongs to one User, Has many DiaryReportLinks.
*   **DiaryReportLink**
    *   Attributes: Diary ID (FK), Report ID (FK).
    *   Relationships: Belongs to one Diary, Belongs to one Report. (Composite PK: DiaryID, ReportID)

## 4.3. Physical Data Model

### 4.3.1. MariaDB (RDB)

*   **ERD:** (An ERD image is not available. Relationships are described textually based on JPA annotations from `Data_Models_Entities.md` and FK constraints in `schema.sql`.)
    *   `Users` is central.
    *   `Users` to `Diary`: One-to-Many (A User has many Diaries). `Diary.user_id` FK to `Users.id`.
    *   `Users` to `User_Authentication`: One-to-Many. `User_Authentication.user_id` FK to `Users.id`.
    *   `Auth_Provider` to `User_Authentication`: One-to-Many. `User_Authentication.auth_provider_id` FK to `Auth_Provider.id`.
    *   `Users` to `User_custom_setting`: One-to-Many. `User_custom_setting.user_id` FK to `Users.id`.
    *   `Settings_option` to `User_custom_setting`: One-to-Many. `User_custom_setting.setting_id` FK to `Settings_option.id`.
    *   `Users` to `Report`: One-to-Many. `Report.user_id` FK to `Users.id`.
    *   `Diary` to `Diary_Report_Link`: One-to-Many. `Diary_Report_Link.diary_id` FK to `Diary.id`.
    *   `Report` to `Diary_Report_Link`: One-to-Many. `Diary_Report_Link.report_id` FK to `Report.id`.
    *   (Note: `DiaryAnswer` as a separate table is not in `schema.sql`. If it were, it would have a One-to-One relationship with `Diary`.)

*   **Table Definitions:** Based on `schema.sql` and `Data_Models_Entities.md`. `ON DELETE CASCADE` and `ON UPDATE CASCADE` are common for user-related data for consistency. `ON DELETE RESTRICT` for `Auth_Provider` is sensible to prevent accidental deletion of a provider still in use.

    *   **Table: `Users`**
        *   Description: Stores application user information.
        *   Columns:
            | Column Name | Data Type    | Length | Nullable | Default Value      | PK/FK/Index | Constraints | Description                                  |
            |-------------|--------------|--------|----------|--------------------|-------------|-------------|----------------------------------------------|
            | `id`        | `BIGINT`     |        | `NO`     | Auto Increment     | `PK`        |             | User's unique ID                             |
            | `email`     | `VARCHAR`    | 255    | `NO`     |                    | `Index`     | `UNIQUE`    | Email address (can be used for login)        |
            | `password`  | `VARCHAR`    | 255    | `NO`     |                    |             |             | Hashed password                              |
            | `user_name` | `VARCHAR`    | 50     | `NO`     |                    | `Index`     | `UNIQUE`    | Username or login ID (from entity: `userName`) |
            | `user_role` | `VARCHAR`    | 20     | `NO`     | `'USER'`           |             |             | User role (e.g., USER, ADMIN)                |
            | `is_premium`| `BOOLEAN`    |        | `NO`     | `FALSE`            |             |             | Premium account status                       |
            | `is_active` | `VARCHAR(20)`| 20     | `NO`     | `'ACTIVE'`         |             |             | Account active status (from entity: enum UserStatus like ACTIVE, WAITING) |
            | `last_login`| `TIMESTAMP`  |        | `YES`    | `NULL`             |             |             | Timestamp of last login                      |
            | `created_at`| `TIMESTAMP`  |        | `NO`     | `CURRENT_TIMESTAMP`|             |             | Account creation timestamp                   |
            | `updated_at`| `TIMESTAMP`  |        | `NO`     | `CURRENT_TIMESTAMP`|             | `ON UPDATE CURRENT_TIMESTAMP` | Last information update timestamp          |
        *   (Note: `schema.sql` uses `user_name` for nickname/login ID, and `email` for email. `Data_Models_Entities.md` User entity has `userName` for login ID and a separate `nickname` field. Assuming `schema.sql`'s `user_name` is the login ID, and `nickname` from entity is missing in `schema.sql` or needs to be added).

    *   **Table: `Auth_Provider`**
        *   Description: Stores supported authentication providers.
        *   Columns:
            | Column Name     | Data Type | Length | Nullable | Default Value | PK/FK/Index | Constraints | Description                               |
            |-----------------|-----------|--------|----------|---------------|-------------|-------------|-------------------------------------------|
            | `id`            | `INT`     |        | `NO`     | Auto Increment| `PK`        |             | Auth provider's unique ID                 |
            | `provider_name` | `VARCHAR` | 50     | `NO`     |               | `Index`     | `UNIQUE`    | Auth provider name (e.g., server, google) |
            | `description`   | `VARCHAR` | 255    | `YES`    | `NULL`        |             |             | Description of the auth provider          |
            | `is_active`     | `BOOLEAN` |        | `NO`     | `TRUE`        |             |             | Whether the provider is active            |

    *   **Table: `User_Authentication`**
        *   Description: Links users to their authentication methods (social logins).
        *   Columns:
            | Column Name        | Data Type | Length | Nullable | Default Value      | PK/FK/Index | Constraints | Description                               |
            |--------------------|-----------|--------|----------|--------------------|-------------|-------------|-------------------------------------------|
            | `user_id`          | `BIGINT`  |        | `NO`     |                    | `PK, FK`    |             | User's ID                                 |
            | `auth_provider_id` | `INT`     |        | `NO`     |                    | `PK, FK`    |             | Auth provider's ID                        |
            | `social_id`        | `VARCHAR` | 255    | `NO`     |                    | `Index`     | `UNIQUE` (with auth_provider_id) | User's ID from the social provider      |
            | `created_at`       | `TIMESTAMP`|       | `NO`     | `CURRENT_TIMESTAMP`|             |             | Link creation timestamp                   |
            | `updated_at`       | `TIMESTAMP`|       | `NO`     | `CURRENT_TIMESTAMP`|             | `ON UPDATE CURRENT_TIMESTAMP` | Link update timestamp                     |
        *   Foreign Keys:
            *   `user_id` REFERENCES `Users(id)` ON DELETE CASCADE ON UPDATE CASCADE
            *   `auth_provider_id` REFERENCES `Auth_Provider(id)` ON DELETE RESTRICT ON UPDATE CASCADE

    *   **Table: `Settings_option`**
        *   Description: Defines application-wide setting options and their defaults.
        *   Columns:
            | Column Name        | Data Type | Length | Nullable | Default Value | PK/FK/Index | Constraints | Description                             |
            |--------------------|-----------|--------|----------|---------------|-------------|-------------|-----------------------------------------|
            | `id`               | `INT`     |        | `NO`     | Auto Increment| `PK`        |             | Setting option's unique ID              |
            | `setting_key`      | `VARCHAR` | 100    | `NO`     |               | `Index`     | `UNIQUE`    | Unique key for the setting              |
            | `default_value`    | `VARCHAR` | 255    | `NO`     |               |             |             | Default value for the setting         |
            | `data_type`        | `VARCHAR` | 20     | `NO`     |               |             |             | Data type (BOOLEAN, STRING, etc.)       |
            | `description`      | `VARCHAR` | 255    | `YES`    | `NULL`        |             |             | Description of the setting option       |
            | `is_user_editable` | `BOOLEAN` |        | `NO`     | `TRUE`        |             |             | Can users override this setting?        |

    *   **Table: `User_custom_setting`**
        *   Description: Stores user-specific overrides for setting options.
        *   Columns:
            | Column Name      | Data Type | Length | Nullable | Default Value      | PK/FK/Index | Constraints | Description                               |
            |------------------|-----------|--------|----------|--------------------|-------------|-------------|-------------------------------------------|
            | `user_id`        | `BIGINT`  |        | `NO`     |                    | `PK, FK`    |             | User's ID                                 |
            | `setting_id`     | `INT`     |        | `NO`     |                    | `PK, FK`    |             | Setting option's ID                       |
            | `override_value` | `VARCHAR` | 255    | `NO`     |                    |             |             | User's overridden value for the setting |
            | `created_at`     | `TIMESTAMP`|       | `NO`     | `CURRENT_TIMESTAMP`|             |             | Custom setting creation timestamp         |
            | `updated_at`     | `TIMESTAMP`|       | `NO`     | `CURRENT_TIMESTAMP`|             | `ON UPDATE CURRENT_TIMESTAMP` | Custom setting update timestamp           |
        *   Foreign Keys:
            *   `user_id` REFERENCES `Users(id)` ON DELETE CASCADE ON UPDATE CASCADE
            *   `setting_id` REFERENCES `Settings_option(id)` ON DELETE CASCADE ON UPDATE CASCADE

    *   **Table: `Diary`**
        *   Description: Stores user's diary entries.
        *   Columns:
            | Column Name           | Data Type     | Length | Nullable | Default Value      | PK/FK/Index | Constraints | Description                             |
            |-----------------------|---------------|--------|----------|--------------------|-------------|-------------|-----------------------------------------|
            | `id`                  | `BIGINT`      |        | `NO`     | Auto Increment     | `PK`        |             | Diary entry's unique ID                 |
            | `user_id`             | `BIGINT`      |        | `NO`     |                    | `FK`        |             | ID of the user who wrote the diary      |
            | `title`               | `VARCHAR`     | 255    | `YES`    | `NULL`             |             |             | Title of the diary entry                |
            | `content`             | `TEXT`        |        | `NO`     |                    |             |             | Content of the diary entry              |
            | `alternative_thought` | `TEXT`        |        | `YES`    | `NULL`             |             |             | AI-generated alternative thought        |
            | `is_negative`         | `BOOLEAN`     |        | `YES`    | `FALSE`            |             |             | Whether the diary has negative sentiment|
            | `created_at`          | `TIMESTAMP`   |        | `NO`     | `CURRENT_TIMESTAMP`|             |             | Creation timestamp                      |
            | `updated_at`          | `TIMESTAMP`   |        | `NO`     | `CURRENT_TIMESTAMP`|             | `ON UPDATE CURRENT_TIMESTAMP` | Last update timestamp                   |
        *   Foreign Keys:
            *   `user_id` REFERENCES `Users(id)` ON DELETE CASCADE ON UPDATE CASCADE

    *   **Table: `Report`**
        *   Description: Stores consolidated reports based on multiple diary entries.
        *   Columns:
            | Column Name     | Data Type   | Length | Nullable | Default Value      | PK/FK/Index | Constraints | Description                   |
            |-----------------|-------------|--------|----------|--------------------|-------------|-------------|-------------------------------|
            | `id`            | `BIGINT`    |        | `NO`     | Auto Increment     | `PK`        |             | Report's unique ID            |
            | `user_id`       | `BIGINT`    |        | `NO`     |                    | `FK`        |             | ID of the user for the report |
            | `summary_title` | `VARCHAR`   | 255    | `NO`     |                    |             |             | Summary title of the report   |
            | `change_process`| `TEXT`      |        | `YES`    | `NULL`             |             |             | Description of thought change |
            | `created_at`    | `TIMESTAMP` |        | `NO`     | `CURRENT_TIMESTAMP`|             |             | Creation timestamp            |
            | `updated_at`    | `TIMESTAMP` |        | `NO`     | `CURRENT_TIMESTAMP`|             | `ON UPDATE CURRENT_TIMESTAMP` | Last update timestamp         |
        *   Foreign Keys:
            *   `user_id` REFERENCES `Users(id)` ON DELETE CASCADE ON UPDATE CASCADE

    *   **Table: `Diary_Report_Link`**
        *   Description: Links Diary entries to Reports (Many-to-Many).
        *   Columns:
            | Column Name | Data Type | Length | Nullable | Default Value | PK/FK/Index | Constraints | Description     |
            |-------------|-----------|--------|----------|---------------|-------------|-------------|-----------------|
            | `diary_id`  | `BIGINT`  |        | `NO`     |               | `PK, FK`    |             | Diary entry ID  |
            | `report_id` | `BIGINT`  |        | `NO`     |               | `PK, FK`    |             | Report ID       |
        *   Foreign Keys:
            *   `diary_id` REFERENCES `Diary(id)` ON DELETE CASCADE ON UPDATE CASCADE
            *   `report_id` REFERENCES `Report(id)` ON DELETE CASCADE ON UPDATE CASCADE

*   **DDL Scripts:**
    ```sql
    -- =================================================================================
    -- Table: Users
    -- Description: 애플리케이션 사용자 정보를 저장합니다.
    -- =================================================================================
    CREATE TABLE Users (
        id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '사용자 고유 ID',
        email VARCHAR(255) NOT NULL UNIQUE COMMENT '이메일 주소 (로그인 시 사용될 수 있음)',
        password VARCHAR(255) NOT NULL COMMENT '해시된 비밀번호',
        user_name VARCHAR(50) NOT NULL UNIQUE COMMENT '사용자 별명 또는 로그인 ID', -- In User entity, userName is login ID, nickname is separate. This schema's user_name seems to be login ID.
        user_role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '사용자 권한 (예: USER, ADMIN)',
        is_premium BOOLEAN NOT NULL DEFAULT FALSE COMMENT '프리미엄 계정 여부',
        is_active VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '계정 활성 상태 (예: ACTIVE, WAITING, BLOCKED)', -- Matches UserStatus enum from User entity
        last_login TIMESTAMP NULL COMMENT '마지막 로그인 시간',
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '계정 생성 시간',
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '마지막 정보 수정 시간'
    ) COMMENT '사용자 정보 테이블';


    -- =================================================================================
    -- Table: Auth_Provider
    -- Description: 시스템이 지원하는 인증 제공자(자체 서버, Google, Kakao 등) 목록을 저장합니다.
    -- =================================================================================
    CREATE TABLE Auth_Provider (
        id INT AUTO_INCREMENT PRIMARY KEY COMMENT '인증 제공자 고유 ID',
        provider_name VARCHAR(50) NOT NULL UNIQUE COMMENT '인증 제공자 이름 (예: server, google)',
        description VARCHAR(255) NULL COMMENT '인증 제공자 설명',
        is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '활성화 여부'
    ) COMMENT '인증 제공자 정보 테이블';


    -- =================================================================================
    -- Table: User_Authentication
    -- Description: 사용자와 인증 제공자를 연결하고, 소셜 로그인 시 제공되는 고유 ID를 저장합니다.
    -- =================================================================================
    CREATE TABLE User_Authentication (
        user_id BIGINT NOT NULL COMMENT '사용자 ID (FK)',
        auth_provider_id INT NOT NULL COMMENT '인증 제공자 ID (FK)',
        social_id VARCHAR(255) NOT NULL COMMENT '인증 제공사에서 발급한 사용자의 고유 ID',
        -- email VARCHAR(255) NULL COMMENT '소셜 제공자로부터 받은 이메일 (Data_Models_Entities.md UserAuthentication has this)', -- This column is in entity but not in schema.sql
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '인증 정보 연결 시간',
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '인증 정보 수정 시간',
        PRIMARY KEY (user_id, auth_provider_id),
        UNIQUE (auth_provider_id, social_id),
        FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE ON UPDATE CASCADE,
        FOREIGN KEY (auth_provider_id) REFERENCES Auth_Provider(id) ON DELETE RESTRICT ON UPDATE CASCADE
    ) COMMENT '사용자별 인증 수단 정보';


    -- =================================================================================
    -- Table: Settings_option
    -- Description: 애플리케이션의 모든 설정 항목에 대한 기본값을 정의합니다.
    -- =================================================================================
    CREATE TABLE Settings_option (
        id INT AUTO_INCREMENT PRIMARY KEY COMMENT '설정 항목 고유 ID',
        setting_key VARCHAR(100) NOT NULL UNIQUE COMMENT '설정 키 (예: notification.enabled)',
        default_value VARCHAR(255) NOT NULL COMMENT '설정의 기본값',
        data_type VARCHAR(20) NOT NULL COMMENT '데이터 타입 (예: BOOLEAN, STRING, NUMBER, JSON)', -- Matches DataType enum
        description VARCHAR(255) NULL COMMENT '설정 항목에 대한 설명',
        is_user_editable BOOLEAN NOT NULL DEFAULT TRUE COMMENT '사용자 수정 가능 여부'
    ) COMMENT '기본 설정 옵션';


    -- =================================================================================
    -- Table: User_custom_setting
    -- Description: 사용자가 기본 설정을 변경한 경우, 해당 값을 저장합니다.
    -- =================================================================================
    CREATE TABLE User_custom_setting (
        user_id BIGINT NOT NULL COMMENT '사용자 ID (FK)',
        setting_id INT NOT NULL COMMENT '설정 항목 ID (FK)',
        override_value VARCHAR(255) NOT NULL COMMENT '사용자가 덮어쓴 설정값',
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '커스텀 설정 생성 시간',
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '커스텀 설정 수정 시간',
        PRIMARY KEY (user_id, setting_id),
        FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE ON UPDATE CASCADE,
        FOREIGN KEY (setting_id) REFERENCES Settings_option(id) ON DELETE CASCADE ON UPDATE CASCADE
    ) COMMENT '사용자 커스텀 설정';


    -- =================================================================================
    -- Table: Diary
    -- Description: 사용자가 작성한 일기 데이터를 저장합니다.
    -- =================================================================================
    CREATE TABLE Diary (
        id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '일기 고유 ID',
        user_id BIGINT NOT NULL COMMENT '작성자 ID (FK)',
        title VARCHAR(255) NULL COMMENT '일기 제목',
        content TEXT NOT NULL COMMENT '일기 내용',
        alternative_thought TEXT NULL COMMENT 'AI가 생성한 대안적 사고 (Diary entity has this)',
        is_negative BOOLEAN DEFAULT FALSE COMMENT '부정적 감정 포함 여부 (Diary entity has this)',
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '작성 시간',
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '마지막 수정 시간',
        FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE ON UPDATE CASCADE
    ) COMMENT '일기 정보 테이블';

    -- Note: DiaryAnswer table from Data_Models_Entities.md is not in schema.sql.
    -- Its fields (emotion_detection, automatic_thought, prompt_for_change, alternative_thought, status)
    -- are expected to be stored in MongoDB or added to the Diary table if relational.
    -- The Diary table in schema.sql already has `alternative_thought` and `is_negative`.


    -- =================================================================================
    -- Table: Report
    -- Description: 여러 일기를 바탕으로 생성된 종합 리포트 정보를 저장합니다.
    -- (This table is from schema.sql, not explicitly in Data_Models_Entities.md but related)
    -- =================================================================================
    CREATE TABLE Report (
        id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '리포트 고유 ID',
        user_id BIGINT NOT NULL COMMENT '작성자 ID (FK)',
        summary_title VARCHAR(255) NOT NULL COMMENT '리포트 요약 제목',
        change_process TEXT NULL COMMENT '사고의 변화 과정',
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '리포트 생성 시간',
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '리포트 수정 시간',
        FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE ON UPDATE CASCADE
    ) COMMENT '종합 리포트';


    -- =================================================================================
    -- Table: Diary_Report_Link
    -- Description: 일기와 리포트의 다대다(N:M) 관계를 위한 연결 테이블입니다.
    -- (This table is from schema.sql, not explicitly in Data_Models_Entities.md but related)
    -- =================================================================================
    CREATE TABLE Diary_Report_Link (
        diary_id BIGINT NOT NULL COMMENT '일기 ID (FK)',
        report_id BIGINT NOT NULL COMMENT '리포트 ID (FK)',
        PRIMARY KEY (diary_id, report_id),
        FOREIGN KEY (diary_id) REFERENCES Diary(id) ON DELETE CASCADE ON UPDATE CASCADE,
        FOREIGN KEY (report_id) REFERENCES Report(id) ON DELETE CASCADE ON UPDATE CASCADE
    ) COMMENT '일기-리포트 연결 테이블';
    ```

*   **Sample SQL Queries:**
    ```sql
    -- Get user by username
    SELECT * FROM Users WHERE user_name = 'testuser123';

    -- Get all diaries for a specific user
    SELECT d.* FROM Diary d JOIN Users u ON d.user_id = u.id WHERE u.user_name = 'testuser123' ORDER BY d.created_at DESC;

    -- Get user's custom setting for 'theme.color'
    SELECT ucs.override_value
    FROM User_custom_setting ucs
    JOIN Users u ON ucs.user_id = u.id
    JOIN Settings_option so ON ucs.setting_id = so.id
    WHERE u.user_name = 'testuser123' AND so.setting_key = 'theme.color';
    ```

### 4.3.2. MongoDB (NoSQL)

*   **Purpose:**
    *   Storing AI-generated emotion analysis results from the Python AI worker, linked to specific diary entries. This data can be complex, semi-structured, and evolve with AI model improvements.
    *   Potentially storing application logs if an ELK-like stack uses MongoDB as a backend (though Elasticsearch is more common for ELK).
    *   Storing other non-relational or highly flexible data if needed in the future.

*   **Collection Structure (Example for DiaryAnalysis):**
    *   **Collection Name:** `diary_analysis_results`
    *   **JSON Schema Example Document:**
        ```json
        {
          "_id": "<ObjectId>", // MongoDB's unique ID
          "diaryEntryId": "<Long>", // Foreign key linking to Diary.id in MariaDB
          "userId": "<Long>", // Foreign key linking to User.id in MariaDB (for data ownership and query convenience)
          "analysisTimestamp": "<ISODate>", // When the analysis was performed
          "modelVersion": "emotion_v1.2_thought_v0.9", // Version of the AI models used
          "detectedEmotions": [ // Could be an array of detected emotions with scores
            { "emotion": "joy", "score": 0.85, "details": "Confidence level high" },
            { "emotion": "sadness", "score": 0.10, "details": "Minor trace detected" }
          ],
          "emotionSummary": "Predominantly Joyful", // A textual summary of overall emotion
          "emotionStatus": "POSITIVE", // Matches EmotionStatus enum (POSITIVE, NEGATIVE, NEUTRAL) from DiaryAnswer
          "automaticThoughts": [ // Array of identified automatic thoughts
            { "thought": "I might fail this project.", "category": "Catastrophizing", "intensity": 0.7 },
            { "thought": "I'm not good enough.", "category": "Self-criticism", "intensity": 0.6 }
          ],
          "cognitiveDistortions": ["Catastrophizing", "Self-criticism"], // List of identified cognitive distortions
          "alternativeThoughtsSuggested": [ // Array of AI-suggested alternative thoughts
            {
              "originalThought": "I might fail this project.",
              "suggestedAlternative": "While there are challenges, I can break down the project into smaller tasks and seek help if needed. Success is not guaranteed, but failure isn't either."
            }
          ],
          "promptForChange": "What's one small step you can take to address your concern about the project?", // From DiaryAnswer
          "rawModelOutput": { // Optional: for debugging or advanced reprocessing
            "emotionModel": { "...": "..." },
            "thoughtPatternModel": { "...": "..." }
          }
        }
        ```

*   **Indexing Strategy:**
    *   `diaryEntryId`: Essential for quickly retrieving analysis results for a specific diary entry. (Unique index if one-to-one).
    *   `userId`: Useful for querying all analysis results for a user, or for data segregation/archival.
    *   `analysisTimestamp`: For time-based queries or sorting.
    *   `emotionStatus`: If frequently querying by overall positive/negative/neutral status.

*   **Data Storage/Retrieval Examples:**
    *   **Storage:** Spring Data MongoDB would use classes annotated with `@Document(collection = "diary_analysis_results")`. A `MongoRepository` interface would provide save methods (e.g., `analysisResultRepository.save(analysisDoc)`).
    *   **Retrieval (Conceptual):**
        *   "Find analysis by diary_id": `analysisResultRepository.findByDiaryEntryId(diaryId);`
        *   "Find all analyses for a user created in the last month":
            ```java
            // In a custom repository or service method
            // Query query = new Query();
            // query.addCriteria(Criteria.where("userId").is(userId)
            //         .and("analysisTimestamp").gte(oneMonthAgo));
            // mongoTemplate.find(query, DiaryAnalysisDocument.class);
            ```

### 4.3.3. Redis (In-Memory Data Store)

*   **Purpose:**
    *   **Managing Authentication Tokens:** Storing JWT Refresh Tokens to validate their existence and manage sessions (`RedisService.saveRToken()`). Also stores Access Tokens linked to refresh tokens (`RedisService.saveAccessToken()`), possibly for specific validation or management scenarios.
    *   **Email Verification Codes:** Storing temporary codes sent for email verification, with a TTL for expiration (`RedisService.saveEmailCode()`).
    *   **Session Caching:** While the primary authentication is JWT based (stateless), Redis could be used for Spring Session if `spring-session-data-redis` dependency were added and configured. This is not explicitly confirmed from `build.gradle` files read so far but is a common use.
    *   **Other Caching:** General-purpose caching for frequently accessed data to reduce database load (not explicitly shown in `RedisService` but a common Redis use case).

*   **Key Patterns & Naming Conventions (from `RedisService.java`):**
    *   Refresh Tokens: `REFRESH:<provider>:<userId>` (e.g., `REFRESH:server:123`, `REFRESH:google:user_google_id_abc`)
    *   Access Tokens (linked to refresh token): `ACCESS:<first_20_chars_of_refresh_token>`
    *   Email Verification Codes: `EMAIL_CODE:<email_address>` (e.g., `EMAIL_CODE:user@example.com`)
    *   (Old/Generic Token Prefix, not used in specific methods but present as constant): `TOKEN:`

*   **Value Structures & Data Types:**
    *   Refresh Tokens: Stored as `String`.
    *   Access Tokens: Stored as `String`.
    *   Email Verification Codes: Stored as `String`.

*   **Data Expiration Policy (TTL):**
    *   **Refresh Tokens:** TTL set by `appProperties.getRefreshTokenValidity()` (converted to seconds).
    *   **Access Tokens (linked ones):** TTL set by `appProperties.getAccessTokenValidity()` (converted to seconds).
    *   **Email Verification Codes:** Fixed 1800 seconds (30 minutes).

*   **Redis Command Examples (mapped from `RedisService.java` methods):**
    *   `saveRToken(userId, provider, refreshToken)`:
        *   Key: `REFRESH:<provider>:<userId>`
        *   Command: `SETEX <key> <appProperties.getRefreshTokenValidity()> <refreshToken>`
    *   `saveAccessToken(refreshToken, accessToken, userId)`:
        *   Key: `ACCESS:<refreshToken.substring(0,20)>`
        *   Command: `SETEX <key> <appProperties.getAccessTokenValidity()> <accessToken>`
    *   `getAccessToken(refreshToken)`:
        *   Key: `ACCESS:<refreshToken.substring(0,20)>`
        *   Command: `GET <key>`
    *   `isRTokenExist(userId, provider, refreshToken)`:
        *   Key: `REFRESH:<provider>:<userId>`
        *   Command: `GET <key>` (then compares value)
    *   `deleteRToken(userId, provider)`:
        *   Key: `REFRESH:<provider>:<userId>`
        *   Command: `DEL <key>`
    *   `saveEmailCode(email, code)`:
        *   Key: `EMAIL_CODE:<email>`
        *   Command: `SETEX <key> 1800 <code>`
    *   `checkEmailCode(email, code)`:
        *   Key: `EMAIL_CODE:<email>`
        *   Command: `GET <key>` (then compares value)

(Note: `ON DELETE` and `ON UPDATE` clauses for FKs in MariaDB were taken directly from `schema.sql`. If they were not specified there, common practices like `CASCADE` for owned entities and `RESTRICT` or `SET NULL` for others would be inferred.)
The `DiaryAnswer` entity from `Data_Models_Entities.md` seems to be the conceptual source for the AI analysis data, which is better suited for MongoDB as designed in section 4.3.2. The `Diary` table in `schema.sql` includes `alternative_thought` and `is_negative`, which are also fields in the `Diary` entity in `Data_Models_Entities.md`, suggesting some AI-related fields might be directly in the `Diary` table for quick access, while more detailed analysis goes to MongoDB.I have successfully drafted Chapter 4: Data Modeling.

This chapter covers:
-   **Conceptual Data Model:** High-level overview of main entities (User, Diary, DiaryAnalysis/DiaryAnswer, SettingsOption, UserCustomSetting, AuthProvider, UserAuthentication, Report, DiaryReportLink) and their relationships.
-   **Logical Data Model:** More detailed attribute and relationship descriptions for these entities.
-   **Physical Data Model:**
    -   **MariaDB (RDB):**
        -   Textual description of ERD based on JPA annotations and `schema.sql`.
        -   Detailed table definitions (columns, types, constraints, PKs, FKs) derived from `schema.sql` and `Data_Models_Entities.md` for tables like `Users`, `Auth_Provider`, `User_Authentication`, `Settings_option`, `User_custom_setting`, `Diary`, `Report`, and `Diary_Report_Link`.
        -   The provided DDL script from `schema.sql` was included and commented. Differences between `schema.sql` and `Data_Models_Entities.md` (e.g., `DiaryAnswer` table, `User.nickname`) were noted.
        -   Sample SQL queries were provided.
    -   **MongoDB (NoSQL):**
        -   Purpose defined for storing AI analysis results.
        *   A sample JSON schema for a `diary_analysis_results` collection was provided, including fields like `diaryEntryId`, `userId`, `detectedEmotions`, `automaticThoughts`, etc., drawing conceptual parallels with the `DiaryAnswer` entity.
        -   Potential indexing strategies were suggested.
        -   Conceptual data storage/retrieval examples were mentioned.
    -   **Redis (In-Memory Data Store):**
        -   Purpose detailed based on `RedisService.java` (managing auth tokens, email verification codes).
        -   Key patterns and naming conventions (e.g., `REFRESH:<provider>:<userId>`) were extracted from `RedisService.java`.
        -   Value structures (strings) and data expiration policies (TTLs from `appProperties` or fixed) were documented.
        -   Redis commands corresponding to `RedisService.java` methods were listed.

The information was sourced from `Data_Models_Entities.md`, `schema.sql`, and `RedisService.java`. Inconsistencies or missing details (like `DiaryAnswer` table in `schema.sql`) were handled by cross-referencing and making logical inferences based on the available documentation and common practices, with AI analysis results primarily targeted for MongoDB.

The output is a markdown formatted file named `Chapter4_Data_Modeling.md`.
