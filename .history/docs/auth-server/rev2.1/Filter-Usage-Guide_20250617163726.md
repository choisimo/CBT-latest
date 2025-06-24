# 필터 사용 가이드

이 가이드는 Auth-server의 PluggableFilter 아키텍처에 대한 포괄적인 개요를 제공하며, 다양한 보안 및 요청 처리 작업을 위한 필터를 생성, 관리 및 적용하는 방법을 자세히 설명합니다.

## 아키텍처 개요

PluggableFilter 아키텍처는 Spring Security 프레임워크 내에서 동적이고 구성 가능한 요청 필터링을 허용하도록 설계되었습니다. 개발자와 관리자가 런타임에 (REST API를 통해) 또는 프로그래밍 방식으로 필터 동작을 추가, 제거 또는 수정할 수 있게 해줍니다. 이는 고정된 보안 구성에서 필터 로직을 분리하고, 중앙 레지스트리를 사용하여 필터와 그 조건들을 관리함으로써 달성됩니다.

## PluggableFilter 인터페이스

사용자 정의 필터는 `PluggableFilter` 인터페이스를 구현하여 생성됩니다. `PluggableFilter` 자체가 `javax.servlet.Filter`를 상속하므로, 구현체는 표준 서블릿 필터 생명주기 메서드에 대한 로직도 제공해야 합니다.

`PluggableFilter`에서 구현해야 할 주요 메서드들:

-   **`String getFilterId()`**:
    *   **목적**: 필터의 고유 식별자를 반환합니다. 이 ID는 `FilterRegistry`와 REST API를 통해 필터를 관리하는 데 중요합니다.
    *   **구현**: 일관성 있고 고유한 문자열을 반환해야 합니다 (예: "jwtVerificationFilter", "requestLoggingFilter").

-   **`void configure(HttpSecurity http)`**:
    *   **목적**: 이 메서드는 `FilterRegistry`가 `SecurityFilterChain`을 구성할 때 호출됩니다. 필터 인스턴스가 자신을 `HttpSecurity` 체인에 추가하는 곳입니다.
    *   **사용법**: 일반적으로 `http.addFilterBefore()`, `http.addFilterAfter()`, 또는 `http.addFilterAt()`을 사용하여 체인 내에서 필터를 올바르게 배치합니다. 예를 들어: `http.addFilterBefore(this, UsernamePasswordAuthenticationFilter.class);`

-   **`int getOrder()`**:
    *   **목적**: `getBeforeFilter()` 또는 `getAfterFilter()`에 의해 레지스트리가 구성하는 체인의 다른 *알려진* 필터에 대한 상대적 위치가 명시적으로 설정되지 않은 경우 필터의 실행 우선순위를 정의합니다. 낮은 숫자 값은 높은 우선순위를 의미합니다 (더 일찍 실행됨).
    *   **사용법**: 정수 값을 반환합니다. 레지스트리를 통해 등록된 여러 필터가 동일한 순서를 가지거나 명시적인 상대적 위치가 없는 경우, 해당 순서를 벗어난 시퀀스는 엄격하게 보장되지 않을 수 있습니다.

-   **`Class<? extends Filter> getBeforeFilter()`**:
    *   **목적**: (선택사항) 이 필터가 `SecurityFilterChain`에서 다른 특정 필터 클래스 *이전*에 배치되어야 함을 지정합니다.
    *   **사용법**: 이 필터가 앞서야 할 필터의 `.class`를 반환합니다 (예: `UsernamePasswordAuthenticationFilter.class`). 필요하지 않다면 `null`을 반환합니다. 지정된 경우, `FilterRegistry`는 필터의 `configure` 메서드를 호출할 때 이를 준수하려고 시도합니다.

-   **`Class<? extends Filter> getAfterFilter()`**:
    *   **목적**: (선택사항) 이 필터가 `SecurityFilterChain`에서 다른 특정 필터 클래스 *이후*에 배치되어야 함을 지정합니다.
    *   **사용법**: 이 필터가 뒤따라야 할 필터의 `.class`를 반환합니다 (예: `ExceptionTranslationFilter.class`). 필요하지 않다면 `null`을 반환합니다.

구현해야 할 표준 `javax.servlet.Filter` 메서드들:

-   **`void init(FilterConfig filterConfig) throws ServletException`**: 필터가 초기화될 때 컨테이너에 의해 호출됩니다.
-   **`void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException`**: 필터의 핵심 로직을 포함합니다. 체인의 다음 필터로 제어권을 넘기기 위해 `chain.doFilter(request, response)`를 호출하는 것이 중요합니다.
-   **`void destroy()`**: 필터가 서비스에서 제거될 때 컨테이너에 의해 호출됩니다.

## FilterCondition 인터페이스

-   **목적**: `FilterCondition` 인터페이스는 주어진 HTTP 요청에 대해 `PluggableFilter`가 적용되어야 하는지 또는 건너뛰어야 하는지를 결정합니다. 관련된 조건 중 하나라도 `true`로 평가되면 (*건너뛰기* 조건이 충족됨을 의미), 해당 요청에 대해 필터의 `doFilter` 로직이 우회됩니다.

-   **구현 예시: `PathPatternFilterCondition`**:
    *   **작동 방식**: 이 조건은 요청의 URL 경로가 지정된 Ant 스타일 경로 패턴(예: `/auth/login`, `/api/**`) 중 하나와 일치하는지 그리고 HTTP 메서드가 지정된 메서드(예: "GET", "POST") 중 하나와 일치하는지 확인합니다.
    *   요청 URL이 패턴과 일치하고 메서드가 지정된 메서드 중 하나와 일치하면 (또는 메서드가 지정되지 않은 경우 해당 패턴에 대해 모든 메서드와 일치), 조건이 `true`로 평가되고 필터가 건너뛰어집니다.

## FilterRegistry

-   **목적**: `FilterRegistry`는 `PluggableFilter` 인스턴스와 관련된 `FilterCondition`들을 저장, 검색 및 관리하는 중앙 구성 요소입니다. 이러한 필터들을 `SecurityFilterChain`에 적용하는 것을 조율합니다.

-   **핵심 메서드: `filterRegistry.configureFilters(HttpSecurity http)`**:
    *   **설명**: 이 메서드는 등록된 필터들을 Spring Security에 통합하는 핵심입니다. 일반적으로 `SecurityConfig`(또는 동등한 보안 설정 클래스) 내에서 한 번 호출됩니다. 등록된 모든 `PluggableFilter`들을 반복하며, 각 필터에 대해 먼저 `getBeforeFilter()`와 `getAfterFilter()`를 확인하여 특정하고 알려진 Spring Security 필터에 상대적으로 배치되어야 하는지 확인합니다. 그렇다면 필터의 `configure(http)` 메서드를 직접 호출하여 필터가 자신을 추가할 수 있게 합니다 (예: `http.addFilterBefore(this, SomeFilter.class)`). 상대적 위치가 지정되지 않은 경우, `getOrder()` 값에 의존하여 필터들을 정렬한 다음 `configure(http)` 메서드를 호출합니다.

-   **코드 레벨 작업**:
    *   **`registry.registerFilter(PluggableFilter filter)`**: `PluggableFilter` 인스턴스를 레지스트리에 추가합니다. 필터는 다음 `configureFilters` 호출 시 (보통 시작 시 또는 재구성이 트리거된 경우) `SecurityFilterChain`에 포함될 수 있게 됩니다.
    *   **`registry.addCondition(String filterId, FilterCondition condition)`**: `FilterCondition`을 등록된 필터(`filterId`로 식별)와 연결합니다. 이 조건 (또는 다른 관련 조건)이 `true`로 평가되면 필터가 건너뛰어집니다. 각 조건은 고유한 ID를 가져야 하며, 일반적으로 조건의 구현 내에서 설정됩니다.
    *   **`registry.removeCondition(String filterId, String conditionId)`**: 필터(`filterId`로 식별)에서 특정 `FilterCondition`(`conditionId`로 식별)을 제거합니다.
    *   **`registry.unregisterFilter(String filterId)`**: 레지스트리에서 필터(`filterId`로 식별)를 제거합니다. 필터는 다음 재구성 후 더 이상 `SecurityFilterChain`의 일부가 되지 않습니다.
    *   **`registry.clearAll()`**: 레지스트리에서 모든 필터와 조건들을 제거합니다. 이는 주로 테스트 시나리오나 애플리케이션 재시작 또는 동적 재구성 프로세스 중에 깨끗한 상태를 보장하는 데 유용합니다.

## REST Management API (AdminFilterController)

The Auth-server exposes a REST API for managing filters at runtime.

-   **Base path**: `/api/admin/filters`
-   **Required Role**: Access to these endpoints typically requires `ROLE_ADMIN`.

### List Current Filters

-   **Endpoint:** `GET /api/admin/filters`
-   **Response:** Provides a list of all currently registered `PluggableFilter`s. For each filter, the response includes its ID, fully qualified class name, and a list of its currently applied conditions (including their descriptions and details).

### Add Condition (Partially "Disable" Filter for specific paths/methods)

-   **Endpoint:** `POST /api/admin/filters/{filterId}/conditions`
-   **Purpose**: Adds a new condition to the specified filter. This is often used to "disable" the filter for certain request paths or HTTP methods.
-   **Request Body JSON Example (for `PathPatternFilterCondition`):**
    ```json
    {
      "description": "로그인/회원가입은 인증 제외",
      "patterns": ["/auth/login", "/auth/join"],
      "methods": ["POST"]
    }
    ```
    *   **Explanation**:
        *   `description`: A human-readable description of the condition.
        *   `patterns`: An array of Ant-style URL patterns.
        *   `methods`: (Optional) An array of HTTP methods (e.g., "GET", "POST"). If omitted or empty, the condition applies to all HTTP methods for the specified patterns.
-   **Action**: The controller typically creates a `PathPatternFilterCondition` (or another suitable `FilterCondition` implementation based on the request) using the provided JSON body and then calls `filterRegistry.addCondition(filterId, newCondition)`.

### Remove Condition

-   **Endpoint:** `DELETE /api/admin/filters/{filterId}/conditions/{conditionId}`
-   **Purpose**: Removes a specific condition (identified by `conditionId`) from the filter (identified by `filterId`).
-   **Action**: Calls `filterRegistry.removeCondition(filterId, conditionId)`.

### Enable/Disable Filter (Globally)

These endpoints provide a way to effectively enable or disable a filter for *all* requests.

-   **Endpoint (Disable):** `POST /api/admin/filters/{filterId}?action=disable`
    *   **Action**: Adds a special, often internally managed, `FilterCondition` to the specified filter that causes it to be skipped for *all* requests. This effectively disables the filter without unregistering it. The condition might be configured to match all paths and methods.

-   **Endpoint (Enable):** `POST /api/admin/filters/{filterId}?action=enable`
    *   **Action**: Removes the specific "disable" condition that was added by the disable action. If other conditions exist, they remain in effect. If the "disable" condition was the only one making it skip all requests, the filter becomes active again (subject to its other conditions).

## Code-Level Usage Example (Without REST API)

Filters and their conditions can also be managed programmatically. This is useful for initial setup or when dynamic runtime changes via REST are not required.

```java
@Component
@RequiredArgsConstructor
public class JwtVerificationFilterConfigurer {
    private final FilterRegistry registry;
    private final JwtVerificationFilter jwtFilter;   // An instance of PluggableFilter

    @PostConstruct
    public void init() {
        // Register the filter with the registry
        registry.registerFilter(jwtFilter);

        // Add a condition to exclude specific paths from this filter's processing
        // For example, Swagger UI and API docs paths
        registry.addCondition(
            jwtFilter.getFilterId(), // Get the unique ID from the filter instance
            new PathPatternFilterCondition(
                "Swagger Exclude", // A description for the condition
                new String[]{"/swagger-ui/**", "/v3/api-docs/**"}, // URL patterns
                null // Apply to all HTTP methods for these patterns
            )
        );
    }

    // Further operations (examples):
    public void removeSwaggerExclusion(String filterId) {
        // Assuming the condition was given an ID like "Swagger Exclude" or one was generated
        registry.removeCondition(filterId, "Swagger Exclude");
    }

    public void temporarilyUnregisterFilter(String filterId) {
        registry.unregisterFilter(filterId);
    }

    public void clearAllFiltersAndConditions() {
        registry.clearAll(); // Useful for testing or full reset
    }
}
```
-   **Explanation**:
    *   `@PostConstruct` ensures `init()` is called after dependency injection.
    *   `jwtFilter.getFilterId()` is used to provide the unique ID when adding conditions.
    *   `PathPatternFilterCondition` is instantiated with a description, path patterns, and optionally HTTP methods.
    *   **Modification**: Achieved by calling `addCondition` or `removeCondition` with the filter's ID and the condition's details/ID.
    *   **Deletion**: `unregisterFilter(filterId)` removes the filter entirely from the registry's management.
    *   **Clearing**: `clearAll()` empties the registry, removing all filters and conditions.

## Filter Implementation Example

Here's an example of what a `PluggableFilter` implementation might look like, such as the `JwtVerificationFilter`.

```java
// Example: JwtVerificationFilter
public class JwtVerificationFilter implements PluggableFilter {
    // Unique ID for this filter instance
    private final String filterId = "jwtVerificationFilter";

    // ... other necessary fields like JwtTokenProvider, ObjectMapper, etc. ...

    public JwtVerificationFilter(/* ... dependencies ... */) {
        // ... initialize dependencies ...
    }

    @Override
    public String getFilterId() {
        return this.filterId;
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        // This filter should run before Spring Security's standard
        // UsernamePasswordAuthenticationFilter to validate JWTs for protected resources.
        http.addFilterBefore(this, UsernamePasswordAuthenticationFilter.class);
    }

    @Override
    public int getOrder() {
        // Example order, might be less relevant if getBeforeFilter/getAfterFilter is used
        // effectively with known Spring Security filters.
        return 10;
    }

    // Optional: Specify if this filter must run before a specific filter class
    @Override
    public Class<? extends Filter> getBeforeFilter() {
        // Can be used by the registry to ensure placement if configure() doesn't use http.addFilterBefore
        // with a well-known class. For this example, it's handled in configure().
        return UsernamePasswordAuthenticationFilter.class;
    }

    // Optional: Specify if this filter must run after a specific filter class
    @Override
    public Class<? extends Filter> getAfterFilter() {
        return null; // Not needed for this example
    }

    // --- javax.servlet.Filter methods ---

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization logic for the filter, e.g., loading configurations.
        // This is called by the servlet container, not directly by PluggableFilter setup.
        System.out.println(getFilterId() + " initialized.");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        // Example: Extract JWT from header, validate it.
        // String jwt = extractToken(httpRequest);
        // if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
        //     Authentication authentication = jwtTokenProvider.getAuthentication(jwt);
        //     SecurityContextHolder.getContext().setAuthentication(authentication);
        // }

        System.out.println("Processing request through " + getFilterId() + " for: " + httpRequest.getRequestURI());

        // CRITICAL: Proceed to the next filter in the chain.
        // If this is not called, the request processing stops here.
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // Cleanup logic for the filter.
        // Called by the servlet container.
        System.out.println(getFilterId() + " destroyed.");
    }
}
```
-   **`getFilterId()`**: Essential for the registry and API to identify and manage the filter.
-   **`init`, `doFilter`, `destroy`**: Standard servlet filter methods. The `doFilter` method contains the core request processing logic. Remember to call `chain.doFilter()` to pass the request along.

## Summary of Operations

Understanding the distinction between code-level and REST API operations is key:

-   **"Apply" Filter**:
    *   **Code**: `registry.registerFilter(myFilter);` (Filter becomes active after `configureFilters`).
    *   **REST API**: `POST /api/admin/filters/{filterId}?action=enable` (Removes a global "disable" condition, making the filter active subject to other conditions). Note: The filter must have been initially registered via code. The REST API doesn't create new filter *instances* or register them from scratch.

-   **"Modify" Filter (Adjust Scope)**:
    *   **Code**: `registry.addCondition(filterId, condition);` or `registry.removeCondition(filterId, conditionId);`
    *   **REST API**: `POST /api/admin/filters/{filterId}/conditions` (to add) or `DELETE /api/admin/filters/{filterId}/conditions/{conditionId}` (to remove).

-   **"Delete" / "Disable" Filter**:
    *   **Code (Actual Removal)**: `registry.unregisterFilter(filterId);` (The filter is completely removed from registry and security chain upon reconfiguration).
    *   **REST API (Effective Disabling)**: `POST /api/admin/filters/{filterId}?action=disable` (Adds a condition to skip all requests. The filter remains registered but inactive). It does *not* unregister the filter bean itself.

**Recommendation**: For runtime changes in operational environments (development, staging, production), using the **REST Management API is generally recommended**. This allows for dynamic adjustments without code redeployments. Code-level configuration is best suited for initial setup and defining the available set of filters.
