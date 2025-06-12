# 동적 필터 관리

이 섹션은 Auth-Server에 구현된 동적 필터 관리 시스템에 대해 설명합니다. 이 시스템을 통해 관리자는 코드 변경이나 애플리케이션 재배포 없이도 보안 필터 조건을 동적으로 추가, 제거, 구성할 수 있습니다.

## 개요

동적 필터 관리는 주로 `FilterRegistry`와 `AdminFilterController` 컴포넌트에 의해 처리됩니다. 이 시스템은 REST API를 통해 제공된 구성에 따라 런타임에 애플리케이션의 보안 동작을 조정할 수 있게 해줍니다.

## 주요 구성요소

### `FilterRegistry`

*   **목적**: 동적 필터를 관리하고 등록하는 중앙 컴포넌트입니다. 여러 개의 `PluggableFilter` 인스턴스를 보유하며, 각 인스턴스는 여러 개의 `FilterCondition`을 가질 수 있습니다.
*   **위치**: `com.authentication.auth.filter.FilterRegistry.java`
*   **기능**:
    *   애플리케이션 시작 시 `PluggableFilter` 인스턴스를 등록합니다.
    *   고유 ID로 `PluggableFilter`를 조회하는 메서드를 제공합니다.
    *   각 `PluggableFilter`에 연결된 `FilterCondition`의 라이프사이클을 관리합니다.

### `PluggableFilter`

*   **목적**: 동적으로 구성 가능한 보안 필터를 나타내는 인터페이스입니다. 구체적인 구현체(예: `PathPatternFilter`)가 이 인터페이스를 구현합니다.
*   **위치**: `com.authentication.auth.filter.PluggableFilter.java`
*   **기능**:
    *   동적 필터를 위한 공통 인터페이스를 정의합니다.
    *   필터의 동작을 결정하는 `FilterCondition` 목록을 관리합니다.
    *   조건을 추가, 제거, 평가하는 메서드를 제공합니다.

### `FilterCondition`

*   **목적**: `PluggableFilter`가 평가하는 단일 조건을 나타내는 인터페이스입니다. 구체적인 구현체는 특정 기준(예: 경로 패턴, HTTP 메서드 등)을 정의합니다.
*   **위치**: `com.authentication.auth.filter.FilterCondition.java`
*   **기능**:
    *   조건이 충족되는지 판단하는 `evaluate` 메서드를 정의합니다.
    *   사람이 이해할 수 있는 설명(`description`)을 포함합니다.

### `PathPatternFilterCondition`

*   **목적**: URL 경로 패턴과 HTTP 메서드를 기반으로 요청을 평가하는 `FilterCondition`의 구체적 구현체입니다.
*   **위치**: `com.authentication.auth.filter.PathPatternFilterCondition.java`
*   **기능**:
    *   들어오는 요청 경로가 구성된 패턴과 일치하는지 확인합니다.
    *   요청의 HTTP 메서드가 허용된 메서드 목록에 포함되어 있는지 확인합니다.

### `AdminFilterController`

*   **목적**: 관리자가 동적 필터 및 조건을 관리할 수 있도록 엔드포인트를 제공하는 REST 컨트롤러입니다.
*   **위치**: `com.authentication.auth.controller.AdminFilterController.java`
*   **엔드포인트**:
    *   `GET /api/admin/filters`: 등록된 모든 동적 필터를 나열합니다.
    *   `POST /api/admin/filters/{filterId}/conditions`: 지정된 동적 필터에 새 조건을 추가합니다.
    *   `DELETE /api/admin/filters/{filterId}/conditions/{conditionId}`: 지정된 동적 필터에서 조건을 제거합니다.
    *   `POST /api/admin/filters/{filterId}/status`: 특정 동적 필터를 활성화 또는 비활성화합니다. 이를 통해 필터가 런타임에 요청을 처리할지 여부를 제어할 수 있습니다.

## 동작 방식

1.  **초기화**: 애플리케이션 시작 시, `PluggableFilter` 인스턴스가 `FilterRegistry`에 등록됩니다.
2.  **API 관리**: 관리자는 `AdminFilterController`의 REST 엔드포인트를 사용하여
    *   기존 동적 필터를 조회합니다.
    *   `PluggableFilter`에 새 `FilterCondition`을 추가합니다. 예를 들어, 특정 경로에 대한 공개 접근을 허용하는 `PathPatternFilterCondition`을 추가할 수 있습니다.
    *   기존 `FilterCondition`을 제거합니다.
3.  **런타임 평가**: HTTP 요청이 들어오면, 관련 `PluggableFilter`(예: `AuthorizationFilter`가 `PathPatternFilter`를 사용할 수 있음)는 `FilterRegistry`에서 연결된 `FilterCondition`을 가져와 평가합니다. 평가 결과에 따라 필터는 요청을 허용, 거부 또는 특정 로직을 적용할지 결정합니다.

## 예시 사용 사례

관리자는 API를 사용해 `jwtVerificationFilter`에 새로운 공개 엔드포인트(`/api/public/new-feature`)에 대한 접근을 허용하는 `PathPatternFilterCondition`을 추가할 수 있습니다. 이 작업은 애플리케이션을 재배포하지 않고도 가능하므로, 접근 제어를 유연하게 관리할 수 있습니다.
