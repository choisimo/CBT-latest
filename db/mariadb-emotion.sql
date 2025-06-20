USE oss_emotion;

-- 1) FK 검사 잠시 해제
SET FOREIGN_KEY_CHECKS = 0;

-- 2) 기존 객체 정리 (자식 → 부모 순서)
DROP TABLE IF EXISTS User_Authentication;
DROP TABLE IF EXISTS Auth_Provider;
DROP TABLE IF EXISTS Users;


-- =================================================================================
-- Table: Users
-- Description: 애플리케이션 사용자 정보를 저장합니다.
-- =================================================================================
CREATE TABLE Users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '사용자 고유 ID',
    email VARCHAR(255) NOT NULL UNIQUE COMMENT '이메일 주소 (로그인 시 사용될 수 있음)',
    password VARCHAR(255) NOT NULL COMMENT '해시된 비밀번호',
    user_name VARCHAR(50) NOT NULL UNIQUE COMMENT '사용자 별명 또는 로그인 ID',
    user_role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '사용자 권한 (예: USER, ADMIN)',
    is_premium BOOLEAN NOT NULL DEFAULT FALSE COMMENT '프리미엄 계정 여부',
    is_active VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '계정 활성 상태',
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
    data_type VARCHAR(20) NOT NULL COMMENT '데이터 타입 (예: BOOLEAN, STRING)',
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
    alternative_thought TEXT NULL COMMENT 'AI가 생성한 대안적 사고',
    is_negative BOOLEAN DEFAULT FALSE COMMENT '부정적 감정 포함 여부',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '작성 시간',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '마지막 수정 시간',
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE ON UPDATE CASCADE
) COMMENT '일기 정보 테이블';


-- =================================================================================
-- Table: Report
-- Description: 여러 일기를 바탕으로 생성된 종합 리포트 정보를 저장합니다.
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
-- =================================================================================
CREATE TABLE Diary_Report_Link (
    diary_id BIGINT NOT NULL COMMENT '일기 ID (FK)',
    report_id BIGINT NOT NULL COMMENT '리포트 ID (FK)',
    PRIMARY KEY (diary_id, report_id),
    FOREIGN KEY (diary_id) REFERENCES Diary(id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (report_id) REFERENCES Report(id) ON DELETE CASCADE ON UPDATE CASCADE
) COMMENT '일기-리포트 연결 테이블';


-- 5) FK 검사 다시 활성화
SET FOREIGN_KEY_CHECKS = 1;
