# Chapter 4.2: 공통 도메인 모델 (엔티티, DTO, 예외)

이 문서는 `common-domain` 모듈에 정의된 주요 데이터 구조를 설명합니다. 여기에는 JPA 엔티티(일부 상황에서는 DTO 역할도 함), 그리고 공통 에러 상황을 위한 커스텀 예외가 포함됩니다.

## 공통 데이터 모델 / 엔티티

`common-domain` 모듈에는 현재 다음과 같은 주요 데이터 모델이 포함되어 있습니다. 이 모델은 JPA 엔티티이지만, 도메인에서 공유되거나 핵심적인 데이터 구조를 정의합니다.

### SettingsOption (`com.authentication.auth.domain.SettingsOption`)

이 엔티티는 애플리케이션 내에서 설정 가능한 옵션을 나타냅니다. 설정의 속성, 기본값, 사용자 편집 가능 여부 등을 정의합니다. 앱 전체 또는 사용자별 설정을 저장/관리하는 데 사용됩니다.

| 필드            | 타입    | JPA 어노테이션                                       | 설명                                      |
|------------------|---------|-------------------------------------------------------|--------------------------------------------|
| `id`             | Integer | `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)` | 설정 옵션의 고유 식별자.                  |
| `settingKey`     | String  | `@Column(name = "setting_key", nullable = false, unique = true, length = 100)` | 설정을 식별하는 고유 키 (예: "notifications.enabled"). |
| `defaultValue`   | String  | `@Column(name = "default_value", nullable = false, length = 255)`      | 이 설정의 기본값.                         |
| `dataType`       | String  | `@Column(name = "data_type", nullable = false, length = 20)`          | 설정의 데이터 타입 (예: "BOOLEAN", "STRING", "INTEGER"). |
| `description`    | String  | `@Column(length = 255, nullable = true)`              | 이 설정이 제어하는 내용에 대한 간단한 설명. |
| `isUserEditable` | Boolean | `@Column(name = "is_user_editable", nullable = false)` | 사용자가 이 설정을 수정할 수 있는지 여부. 기본값은 `true`. |

**사용 예시:**
*   주로 JPA 엔티티로서 데이터베이스에 설정 정의를 저장하는 데 사용됩니다.
*   이 클래스의 인스턴스(또는 projection)는 API 응답에서 설정 정보를 제공하거나, 설정 업데이트 요청 시 DTO처럼 사용될 수 있습니다.

## 공통 커스텀 예외

현재 분석 시점 기준으로, `common-domain` 모듈에는 커스텀 예외 클래스가 정의되어 있지 않습니다. 애플리케이션에서 사용하는 예외는 표준 Java 예외이거나, 다른 모듈(예: `backend` 모듈)에서 정의된 커스텀 예외일 가능성이 높습니다. 추후 이 모듈에 공통 예외가 추가되면 이곳에 문서화할 예정입니다.
