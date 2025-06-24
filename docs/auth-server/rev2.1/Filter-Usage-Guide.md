# 필터 사용 가이드

이 가이드는 Auth-server의 PluggableFilter 아키텍처에 대한 포괄적인 개요를 제공하며, 다양한 보안 및 요청 처리 작업을 위한 필터를 생성, 관리 및 적용하는 방법을 자세히 설명합니다.

## 아키텍처 개요

PluggableFilter 아키텍처는 Spring Security 프레임워크 내에서 동적이고 구성 가능한 요청 필터링을 허용하도록 설계되었습니다. 개발자와 관리자가 런타임에 (REST API를 통해) 또는 프로그래밍 방식으로 필터 동작을 추가, 제거 또는 수정할 수 있게 해줍니다. 이는 고정된 보안 구성에서 필터 로직을 분리하고, 중앙 레지스트리를 사용하여 필터와 그 조건들을 관리함으로써 달성됩니다.

## PluggableFilter 인터페이스

사용자 정의 필터는 `PluggableFilter` 인터페이스를 구현하여 생성됩니다. `PluggableFilter` 자체가 `javax.servlet.Filter`를 상속하므로, 구현체는 표준 서블릿 필터 생명주기 메서드에 대한 로직도 제공해야 합니다.

`PluggableFilter`에서 구현해야 할 주요 메서드들:

- **`String getFilterId()`**:

  - **목적**: 필터의 고유 식별자를 반환합니다. 이 ID는 `FilterRegistry`와 REST API를 통해 필터를 관리하는 데 중요합니다.
  - **구현**: 일관성 있고 고유한 문자열을 반환해야 합니다 (예: "jwtVerificationFilter", "requestLoggingFilter").

- **`void configure(HttpSecurity http)`**:

  - **목적**: 이 메서드는 `FilterRegistry`가 `SecurityFilterChain`을 구성할 때 호출됩니다. 필터 인스턴스가 자신을 `HttpSecurity` 체인에 추가하는 곳입니다.
  - **사용법**: 일반적으로 `http.addFilterBefore()`, `http.addFilterAfter()`, 또는 `http.addFilterAt()`을 사용하여 체인 내에서 필터를 올바르게 배치합니다. 예를 들어: `http.addFilterBefore(this, UsernamePasswordAuthenticationFilter.class);`

- **`int getOrder()`**:

  - **목적**: `getBeforeFilter()` 또는 `getAfterFilter()`에 의해 레지스트리가 구성하는 체인의 다른 _알려진_ 필터에 대한 상대적 위치가 명시적으로 설정되지 않은 경우 필터의 실행 우선순위를 정의합니다. 낮은 숫자 값은 높은 우선순위를 의미합니다 (더 일찍 실행됨).
  - **사용법**: 정수 값을 반환합니다. 레지스트리를 통해 등록된 여러 필터가 동일한 순서를 가지거나 명시적인 상대적 위치가 없는 경우, 해당 순서를 벗어난 시퀀스는 엄격하게 보장되지 않을 수 있습니다.

- **`Class<? extends Filter> getBeforeFilter()`**:

  - **목적**: (선택사항) 이 필터가 `SecurityFilterChain`에서 다른 특정 필터 클래스 *이전*에 배치되어야 함을 지정합니다.
  - **사용법**: 이 필터가 앞서야 할 필터의 `.class`를 반환합니다 (예: `UsernamePasswordAuthenticationFilter.class`). 필요하지 않다면 `null`을 반환합니다. 지정된 경우, `FilterRegistry`는 필터의 `configure` 메서드를 호출할 때 이를 준수하려고 시도합니다.

- **`Class<? extends Filter> getAfterFilter()`**:
  - **목적**: (선택사항) 이 필터가 `SecurityFilterChain`에서 다른 특정 필터 클래스 *이후*에 배치되어야 함을 지정합니다.
  - **사용법**: 이 필터가 뒤따라야 할 필터의 `.class`를 반환합니다 (예: `ExceptionTranslationFilter.class`). 필요하지 않다면 `null`을 반환합니다.

구현해야 할 표준 `javax.servlet.Filter` 메서드들:

- **`void init(FilterConfig filterConfig) throws ServletException`**: 필터가 초기화될 때 컨테이너에 의해 호출됩니다.
- **`void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException`**: 필터의 핵심 로직을 포함합니다. 체인의 다음 필터로 제어권을 넘기기 위해 `chain.doFilter(request, response)`를 호출하는 것이 중요합니다.
- **`void destroy()`**: 필터가 서비스에서 제거될 때 컨테이너에 의해 호출됩니다.

## FilterCondition 인터페이스

- **목적**: `FilterCondition` 인터페이스는 주어진 HTTP 요청에 대해 `PluggableFilter`가 적용되어야 하는지 또는 건너뛰어야 하는지를 결정합니다. 관련된 조건 중 하나라도 `true`로 평가되면 (_건너뛰기_ 조건이 충족됨을 의미), 해당 요청에 대해 필터의 `doFilter` 로직이 우회됩니다.

- **구현 예시: `PathPatternFilterCondition`**:
  - **작동 방식**: 이 조건은 요청의 URL 경로가 지정된 Ant 스타일 경로 패턴(예: `/auth/login`, `/api/**`) 중 하나와 일치하는지 그리고 HTTP 메서드가 지정된 메서드(예: "GET", "POST") 중 하나와 일치하는지 확인합니다.
  - 요청 URL이 패턴과 일치하고 메서드가 지정된 메서드 중 하나와 일치하면 (또는 메서드가 지정되지 않은 경우 해당 패턴에 대해 모든 메서드와 일치), 조건이 `true`로 평가되고 필터가 건너뛰어집니다.

## FilterRegistry

- **목적**: `FilterRegistry`는 `PluggableFilter` 인스턴스와 관련된 `FilterCondition`들을 저장, 검색 및 관리하는 중앙 구성 요소입니다. 이러한 필터들을 `SecurityFilterChain`에 적용하는 것을 조율합니다.

- **핵심 메서드: `filterRegistry.configureFilters(HttpSecurity http)`**:

  - **설명**: 이 메서드는 등록된 필터들을 Spring Security에 통합하는 핵심입니다. 일반적으로 `SecurityConfig`(또는 동등한 보안 설정 클래스) 내에서 한 번 호출됩니다. 등록된 모든 `PluggableFilter`들을 반복하며, 각 필터에 대해 먼저 `getBeforeFilter()`와 `getAfterFilter()`를 확인하여 특정하고 알려진 Spring Security 필터에 상대적으로 배치되어야 하는지 확인합니다. 그렇다면 필터의 `configure(http)` 메서드를 직접 호출하여 필터가 자신을 추가할 수 있게 합니다 (예: `http.addFilterBefore(this, SomeFilter.class)`). 상대적 위치가 지정되지 않은 경우, `getOrder()` 값에 의존하여 필터들을 정렬한 다음 `configure(http)` 메서드를 호출합니다.

- **코드 레벨 작업**:
  - **`registry.registerFilter(PluggableFilter filter)`**: `PluggableFilter` 인스턴스를 레지스트리에 추가합니다. 필터는 다음 `configureFilters` 호출 시 (보통 시작 시 또는 재구성이 트리거된 경우) `SecurityFilterChain`에 포함될 수 있게 됩니다.
  - **`registry.addCondition(String filterId, FilterCondition condition)`**: `FilterCondition`을 등록된 필터(`filterId`로 식별)와 연결합니다. 이 조건 (또는 다른 관련 조건)이 `true`로 평가되면 필터가 건너뛰어집니다. 각 조건은 고유한 ID를 가져야 하며, 일반적으로 조건의 구현 내에서 설정됩니다.
  - **`registry.removeCondition(String filterId, String conditionId)`**: 필터(`filterId`로 식별)에서 특정 `FilterCondition`(`conditionId`로 식별)을 제거합니다.
  - **`registry.unregisterFilter(String filterId)`**: 레지스트리에서 필터(`filterId`로 식별)를 제거합니다. 필터는 다음 재구성 후 더 이상 `SecurityFilterChain`의 일부가 되지 않습니다.
  - **`registry.clearAll()`**: 레지스트리에서 모든 필터와 조건들을 제거합니다. 이는 주로 테스트 시나리오나 애플리케이션 재시작 또는 동적 재구성 프로세스 중에 깨끗한 상태를 보장하는 데 유용합니다.

## REST 관리 API (AdminFilterController)

Auth-server는 런타임에 필터를 관리하기 위한 REST API를 제공합니다.

- **기본 경로**: `/api/admin/filters`
- **필수 권한**: 이러한 엔드포인트에 대한 액세스는 일반적으로 `ROLE_ADMIN`이 필요합니다.

### 현재 필터 목록 조회

- **엔드포인트:** `GET /api/admin/filters`
- **응답:** 현재 등록된 모든 `PluggableFilter`들의 목록을 제공합니다. 각 필터에 대해 응답에는 ID, 완전한 클래스명, 그리고 현재 적용된 조건들의 목록(설명과 세부사항 포함)이 포함됩니다.

### 조건 추가 (특정 경로/메서드에 대한 필터 부분적 "비활성화")

- **엔드포인트:** `POST /api/admin/filters/{filterId}/conditions`
- **목적**: 지정된 필터에 새로운 조건을 추가합니다. 이는 특정 요청 경로나 HTTP 메서드에 대해 필터를 "비활성화"하는 데 자주 사용됩니다.
- **요청 본문 JSON 예시 (`PathPatternFilterCondition`용):**
  ```json
  {
    "description": "로그인/회원가입은 인증 제외",
    "patterns": ["/auth/login", "/auth/join"],
    "methods": ["POST"]
  }
  ```
  - **설명**:
    - `description`: 조건에 대한 사람이 읽을 수 있는 설명.
    - `patterns`: Ant 스타일 URL 패턴 배열.
    - `methods`: (선택사항) HTTP 메서드 배열 (예: "GET", "POST"). 생략되거나 비어있으면 지정된 패턴에 대해 모든 HTTP 메서드에 조건이 적용됩니다.
- **동작**: 컨트롤러는 일반적으로 제공된 JSON 본문을 사용하여 `PathPatternFilterCondition`(또는 요청에 따른 다른 적합한 `FilterCondition` 구현)을 생성한 다음 `filterRegistry.addCondition(filterId, newCondition)`을 호출합니다.

### 조건 제거

- **엔드포인트:** `DELETE /api/admin/filters/{filterId}/conditions/{conditionId}`
- **목적**: 필터(`filterId`로 식별)에서 특정 조건(`conditionId`로 식별)을 제거합니다.
- **동작**: `filterRegistry.removeCondition(filterId, conditionId)`를 호출합니다.

### 필터 활성화/비활성화 (전역적으로)

이러한 엔드포인트는 _모든_ 요청에 대해 필터를 효과적으로 활성화하거나 비활성화하는 방법을 제공합니다.

- **엔드포인트 (비활성화):** `POST /api/admin/filters/{filterId}?action=disable`

  - **동작**: 지정된 필터에 특별한, 종종 내부적으로 관리되는 `FilterCondition`을 추가하여 _모든_ 요청에 대해 건너뛰도록 합니다. 이는 필터를 등록 해제하지 않고 효과적으로 비활성화합니다. 조건은 모든 경로와 메서드와 일치하도록 구성될 수 있습니다.

- **엔드포인트 (활성화):** `POST /api/admin/filters/{filterId}?action=enable`
  - **동작**: 비활성화 동작으로 추가된 특정 "비활성화" 조건을 제거합니다. 다른 조건들이 존재하면 그대로 유지됩니다. "비활성화" 조건이 모든 요청을 건너뛰게 하는 유일한 조건이었다면, 필터가 다시 활성화됩니다 (다른 조건들에 따라).

## 코드 레벨 사용 예시 (REST API 없이)

필터와 조건들은 프로그래밍 방식으로도 관리할 수 있습니다. 이는 초기 설정이나 REST를 통한 동적 런타임 변경이 필요하지 않을 때 유용합니다.

```java
@Component
@RequiredArgsConstructor
public class JwtVerificationFilterConfigurer {
    private final FilterRegistry registry;
    private final JwtVerificationFilter jwtFilter;   // PluggableFilter의 인스턴스

    @PostConstruct
    public void init() {
        // 레지스트리에 필터 등록
        registry.registerFilter(jwtFilter);

        // 이 필터의 처리에서 특정 경로를 제외하는 조건 추가
        // 예를 들어, Swagger UI와 API 문서 경로들
        registry.addCondition(
            jwtFilter.getFilterId(), // 필터 인스턴스에서 고유 ID 가져오기
            new PathPatternFilterCondition(
                "Swagger 제외", // 조건에 대한 설명
                new String[]{"/swagger-ui/**", "/v3/api-docs/**"}, // URL 패턴들
                null // 이러한 패턴에 대해 모든 HTTP 메서드에 적용
            )
        );
    }

    // 추가 작업들 (예시):
    public void removeSwaggerExclusion(String filterId) {
        // 조건이 "Swagger 제외"와 같은 ID를 가지거나 생성되었다고 가정
        registry.removeCondition(filterId, "Swagger 제외");
    }

    public void temporarilyUnregisterFilter(String filterId) {
        registry.unregisterFilter(filterId);
    }

    public void clearAllFiltersAndConditions() {
        registry.clearAll(); // 테스트나 완전한 리셋에 유용
    }
}
```

- **설명**:
  - `@PostConstruct`는 의존성 주입 후 `init()`이 호출되도록 보장합니다.
  - `jwtFilter.getFilterId()`는 조건을 추가할 때 고유 ID를 제공하는 데 사용됩니다.
  - `PathPatternFilterCondition`은 설명, 경로 패턴, 그리고 선택적으로 HTTP 메서드와 함께 인스턴스화됩니다.
  - **수정**: 필터의 ID와 조건의 세부사항/ID로 `addCondition` 또는 `removeCondition`을 호출하여 수행됩니다.
  - **삭제**: `unregisterFilter(filterId)`는 레지스트리의 관리에서 필터를 완전히 제거합니다.
  - **지우기**: `clearAll()`은 레지스트리를 비워 모든 필터와 조건을 제거합니다.

## 필터 구현 예시

다음은 `JwtVerificationFilter`와 같은 `PluggableFilter` 구현의 예시입니다.

```java
// 예시: JwtVerificationFilter
public class JwtVerificationFilter implements PluggableFilter {
    // 이 필터 인스턴스의 고유 ID
    private final String filterId = "jwtVerificationFilter";

    // ... JwtTokenProvider, ObjectMapper 등과 같은 다른 필요한 필드들 ...

    public JwtVerificationFilter(/* ... 의존성들 ... */) {
        // ... 의존성들 초기화 ...
    }

    @Override
    public String getFilterId() {
        return this.filterId;
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        // 이 필터는 보호된 리소스에 대한 JWT를 검증하기 위해 Spring Security의 표준
        // UsernamePasswordAuthenticationFilter 이전에 실행되어야 합니다.
        http.addFilterBefore(this, UsernamePasswordAuthenticationFilter.class);
    }

    @Override
    public int getOrder() {
        // 예시 순서, getBeforeFilter/getAfterFilter가 알려진 Spring Security 필터와 함께
        // 효과적으로 사용되는 경우 덜 관련성이 있을 수 있습니다.
        return 10;
    }

    // 선택사항: 이 필터가 특정 필터 클래스 이전에 실행되어야 함을 지정
    @Override
    public Class<? extends Filter> getBeforeFilter() {
        // configure()가 잘 알려진 클래스와 함께 http.addFilterBefore를 사용하지 않는 경우
        // 레지스트리가 배치를 보장하는 데 사용될 수 있습니다. 이 예시에서는 configure()에서 처리됩니다.
        return UsernamePasswordAuthenticationFilter.class;
    }

    // 선택사항: 이 필터가 특정 필터 클래스 이후에 실행되어야 함을 지정
    @Override
    public Class<? extends Filter> getAfterFilter() {
        return null; // 이 예시에서는 필요하지 않음
    }

    // --- javax.servlet.Filter 메서드들 ---

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 필터의 초기화 로직, 예: 구성 로딩.
        // 이는 PluggableFilter 설정이 아닌 서블릿 컨테이너에 의해 호출됩니다.
        System.out.println(getFilterId() + " 초기화됨.");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        // 예시: 헤더에서 JWT 추출, 검증.
        // String jwt = extractToken(httpRequest);
        // if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
        //     Authentication authentication = jwtTokenProvider.getAuthentication(jwt);
        //     SecurityContextHolder.getContext().setAuthentication(authentication);
        // }

        System.out.println(getFilterId() + "를 통해 요청 처리 중: " + httpRequest.getRequestURI());

        // 중요: 체인의 다음 필터로 진행.
        // 이것이 호출되지 않으면 요청 처리가 여기서 중단됩니다.
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // 필터의 정리 로직.
        // 서블릿 컨테이너에 의해 호출됩니다.
        System.out.println(getFilterId() + " 소멸됨.");
    }
}
```

- **`getFilterId()`**: 레지스트리와 API가 필터를 식별하고 관리하는 데 필수적입니다.
- **`init`, `doFilter`, `destroy`**: 표준 서블릿 필터 메서드들입니다. `doFilter` 메서드는 핵심 요청 처리 로직을 포함합니다. 요청을 전달하기 위해 `chain.doFilter()`를 호출하는 것을 잊지 마세요.

## 작업 요약

코드 레벨과 REST API 작업 간의 구별을 이해하는 것이 핵심입니다:

- **필터 "적용"**:

  - **코드**: `registry.registerFilter(myFilter);` (`configureFilters` 후 필터가 활성화됨).
  - **REST API**: `POST /api/admin/filters/{filterId}?action=enable` (전역 "비활성화" 조건을 제거하여 다른 조건들에 따라 필터를 활성화함). 참고: 필터는 코드를 통해 초기에 등록되어 있어야 합니다. REST API는 새로운 필터 *인스턴스*를 생성하거나 처음부터 등록하지 않습니다.

- **필터 "수정" (범위 조정)**:

  - **코드**: `registry.addCondition(filterId, condition);` 또는 `registry.removeCondition(filterId, conditionId);`
  - **REST API**: `POST /api/admin/filters/{filterId}/conditions` (추가용) 또는 `DELETE /api/admin/filters/{filterId}/conditions/{conditionId}` (제거용).

- **필터 "삭제" / "비활성화"**:
  - **코드 (실제 제거)**: `registry.unregisterFilter(filterId);` (재구성 시 필터가 레지스트리와 보안 체인에서 완전히 제거됨).
  - **REST API (효과적 비활성화)**: `POST /api/admin/filters/{filterId}?action=disable` (모든 요청을 건너뛰는 조건을 추가함. 필터는 등록된 상태로 유지되지만 비활성화됨). 필터 빈 자체를 등록 해제하지는 _않습니다_.

**권장사항**: 운영 환경(개발, 스테이징, 프로덕션)에서의 런타임 변경에는 **REST 관리 API 사용을 일반적으로 권장합니다**. 이를 통해 코드 재배포 없이 동적 조정이 가능합니다. 코드 레벨 구성은 초기 설정과 사용 가능한 필터 세트를 정의하는 데 가장 적합합니다.
