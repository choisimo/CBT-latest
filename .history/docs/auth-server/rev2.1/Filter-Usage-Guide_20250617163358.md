# 필터 사용 가이드

이 가이드는 Auth-server의 PluggableFilter 아키텍처에 대한 포괄적인 개요를 제공하며, 다양한 보안 및 요청 처리 작업을 위한 필터를 생성, 관리 및 적용하는 방법을 자세히 설명합니다.

## 아키텍처 개요

PluggableFilter 아키텍처는 Spring Security 프레임워크 내에서 동적이고 구성 가능한 요청 필터링을 허용하도록 설계되었습니다. 개발자와 관리자가 런타임에 (REST API를 통해) 또는 프로그래밍 방식으로 필터 동작을 추가, 제거 또는 수정할 수 있게 해줍니다. 이는 고정된 보안 구성에서 필터 로직을 분리하고, 중앙 레지스트리를 사용하여 필터와 그 조건들을 관리함으로써 달성됩니다.

## PluggableFilter Interface

Custom filters are created by implementing the `PluggableFilter` interface. Since `PluggableFilter` itself inherits from `javax.servlet.Filter`, implementations must also provide logic for the standard servlet filter lifecycle methods.

Key methods to implement from `PluggableFilter`:

-   **`String getFilterId()`**:
    *   **Purpose**: Returns a unique identifier for the filter. This ID is crucial for managing the filter through the `FilterRegistry` and the REST API.
    *   **Implementation**: Should return a consistent, unique string (e.g., "jwtVerificationFilter", "requestLoggingFilter").

-   **`void configure(HttpSecurity http)`**:
    *   **Purpose**: This method is called by the `FilterRegistry` when it configures the `SecurityFilterChain`. It's where the filter instance adds itself to the `HttpSecurity` chain.
    *   **Usage**: Typically, you'll use `http.addFilterBefore()`, `http.addFilterAfter()`, or `http.addFilterAt()` to position your filter correctly within the chain. For example: `http.addFilterBefore(this, UsernamePasswordAuthenticationFilter.class);`

-   **`int getOrder()`**:
    *   **Purpose**: Defines the execution priority of the filter if its position isn't explicitly set by `getBeforeFilter()` or `getAfterFilter()` relative to another *known* filter in the chain being configured by the registry. A lower numerical value means higher priority (it runs earlier).
    *   **Usage**: Return an integer value. If multiple filters registered via the registry have the same order or no explicit relative positioning, their sequence might not be strictly guaranteed beyond that order.

-   **`Class<? extends Filter> getBeforeFilter()`**:
    *   **Purpose**: (Optional) Specifies that this filter should be placed *before* another specific filter class in the `SecurityFilterChain`.
    *   **Usage**: Return the `.class` of the filter you want this one to precede (e.g., `UsernamePasswordAuthenticationFilter.class`). If not needed, return `null`. If specified, the `FilterRegistry` will attempt to honor this when calling the filter's `configure` method.

-   **`Class<? extends Filter> getAfterFilter()`**:
    *   **Purpose**: (Optional) Specifies that this filter should be placed *after* another specific filter class in the `SecurityFilterChain`.
    *   **Usage**: Return the `.class` of the filter you want this one to follow (e.g., `ExceptionTranslationFilter.class`). If not needed, return `null`.

Standard `javax.servlet.Filter` methods to implement:

-   **`void init(FilterConfig filterConfig) throws ServletException`**: Called by the container when the filter is initialized.
-   **`void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException`**: Contains the core logic of the filter. It's crucial to call `chain.doFilter(request, response)` to pass control to the next filter in the chain.
-   **`void destroy()`**: Called by the container when the filter is taken out of service.

## FilterCondition Interface

-   **Purpose**: The `FilterCondition` interface determines whether a `PluggableFilter` should be applied or skipped for a given HTTP request. If any of its associated conditions evaluate to `true` (meaning the condition for *skipping* is met), the filter's `doFilter` logic will be bypassed for that request.

-   **Example Implementation: `PathPatternFilterCondition`**:
    *   **How it works**: This condition checks if the request's URL path matches any of the specified Ant-style path patterns (e.g., `/auth/login`, `/api/**`) AND if the HTTP method matches any of the specified methods (e.g., "GET", "POST").
    *   If the request URL matches a pattern and the method matches one of the specified methods (or if no methods are specified, matching any method for that pattern), the condition evaluates to `true`, and the filter is skipped.

## FilterRegistry

-   **Purpose**: The `FilterRegistry` is a central component responsible for storing, retrieving, and managing `PluggableFilter` instances and their associated `FilterCondition`s. It orchestrates the application of these filters to the `SecurityFilterChain`.

-   **Key method: `filterRegistry.configureFilters(HttpSecurity http)`**:
    *   **Explanation**: This method is the linchpin for integrating registered filters into Spring Security. It is typically called once within your `SecurityConfig` (or equivalent security setup class). It iterates through all registered `PluggableFilter`s, and for each filter, it first checks its `getBeforeFilter()` and `getAfterFilter()` to see if it needs to be positioned relative to a specific, known Spring Security filter. If so, it directly calls the filter's `configure(http)` method, allowing the filter to add itself (e.g., `http.addFilterBefore(this, SomeFilter.class)`). If no relative positioning is specified, it relies on the `getOrder()` value to sort filters and then calls their `configure(http)` method.

-   **Code-level operations**:
    *   **`registry.registerFilter(PluggableFilter filter)`**: Adds a `PluggableFilter` instance to the registry. The filter is then available to be included in the `SecurityFilterChain` during the next `configureFilters` call (usually at startup, or if reconfiguration is triggered).
    *   **`registry.addCondition(String filterId, FilterCondition condition)`**: Associates a `FilterCondition` with a registered filter (identified by `filterId`). The filter will be skipped if this condition (or any other associated condition) evaluates to `true`. Each condition should have a unique ID, typically set within the condition's implementation.
    *   **`registry.removeCondition(String filterId, String conditionId)`**: Removes a specific `FilterCondition` (identified by `conditionId`) from a filter (identified by `filterId`).
    *   **`registry.unregisterFilter(String filterId)`**: Removes a filter (identified by `filterId`) from the registry. The filter will no longer be part of the `SecurityFilterChain` after the next reconfiguration.
    *   **`registry.clearAll()`**: Removes all filters and their conditions from the registry. This is primarily useful for testing scenarios or to ensure a clean state during application restarts or dynamic reconfiguration processes.

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
