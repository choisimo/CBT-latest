package com.authentication.auth.filter;
package com.authentication.auth.filter;

import com.authentication.auth.service.redis.RedisService;
import com.authentication.auth.utility.JwtUtility;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Author: choisimo
 * @Date: 2025-05-05
 * @Description: 필터 레지스트리 구성 클래스
 * @Details: 애플리케이션에서 사용할 보안 필터를 등록하고 관리합니다.
 */
@Slf4j
@Configuration
public class FilterRegistry {

    @Value("${application.domain:localhost}")
    private String domain;

    @Value("${application.cookie.domain:localhost}")
    private String cookieDomain;

    /**
     * JWT 검증 필터 빈 생성
     * @param jwtUtility JWT 유틸리티
     * @param redisService Redis 서비스
     * @param userDetailsService 사용자 세부 정보 서비스
     * @param objectMapper 객체 매퍼
     * @return 구성된 JWT 검증 필터
     */
    @Bean
    public JwtVerificationFilter jwtVerificationFilter(
            JwtUtility jwtUtility,
            RedisService redisService,
            UserDetailsService userDetailsService,
            ObjectMapper objectMapper) {
        
        List<String> excludePaths = Arrays.asList(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/password/reset",
            "/api/health",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/error"
        );
        
        log.info("JWT 검증 필터 등록. 제외 경로: {}", excludePaths);
        return new JwtVerificationFilter(jwtUtility, redisService, userDetailsService, objectMapper, excludePaths);
    }

    /**
     * 인증 필터 빈 생성
     * @param authenticationManager 인증 관리자
     * @param jwtUtility JWT 유틸리티
     * @param objectMapper 객체 매퍼
     * @param redisService Redis 서비스
     * @return 구성된 인증 필터
     */
    @Bean
    public AuthenticationFilter authenticationFilter(
            AuthenticationManager authenticationManager,
            JwtUtility jwtUtility,
            ObjectMapper objectMapper,
            RedisService redisService) {
        
        log.info("인증 필터 등록. 도메인: {}, 쿠키 도메인: {}", domain, cookieDomain);
        return new AuthenticationFilter(authenticationManager, jwtUtility, objectMapper, redisService, domain, cookieDomain);
    }

    /**
     * 모든 필터를 등록하여 보안 구성에 적용
     * @param http HTTP 보안 구성
     * @param filters 등록할 필터 목록
     * @throws Exception 보안 구성 예외
     */
    public void registerFilters(HttpSecurity http, List<PluggableFilter> filters) throws Exception {
        log.info("{} 필터 등록 중...", filters.size());
        
        // 필터를 order 기준으로 정렬
        filters.sort((f1, f2) -> Integer.compare(f1.getOrder(), f2.getOrder()));
        
        // 각 필터를 구성에 적용
        for (PluggableFilter filter : filters) {
            log.debug("필터 등록: {}, 순서: {}", filter.getClass().getSimpleName(), filter.getOrder());
            filter.configure(http);
        }
    }

    /**
     * 구성 가능한 모든 필터 수집
     * @param authenticationFilter 인증 필터
     * @param jwtVerificationFilter JWT 검증 필터
     * @return 구성 가능한 필터 목록
     */
    @Bean
    public List<PluggableFilter> pluggableFilters(
            AuthenticationFilter authenticationFilter,
            JwtVerificationFilter jwtVerificationFilter) {
        
        List<PluggableFilter> filters = new ArrayList<>();
        filters.add(jwtVerificationFilter);
        filters.add(authenticationFilter);
        
        log.info("총 {} 필터가 등록되었습니다.", filters.size());
        return filters;
    }
}
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.Filter;
import java.io.IOException;

/**
 * @Author : choisimo 
 * @Date : 2025-05-05
 * @Description : filterRegistry class
 * @Details : 등록된 필터들을 순서대로 등록하는 클래스 (order 기반)
 * @Usage : Spring Security 의 addFilterBefore, addFilterAfter, addFilter 메소드를 사용하여 필터를 등록할 때 사용
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class filterRegistry {
    private final List<PluggableFilter> filters = new ArrayList<>();
    
    @Autowired
    public filterRegistry(List<PluggableFilter> pluggableFilters) {
        // 자동으로 모든 PluggableFilter 구현체를 주입받음
        this.filters.addAll(pluggableFilters);
        log.info("총 {} 개의 필터가 등록되었습니다.", filters.size());
    }

    public void addFilter(PluggableFilter filter) {
        filters.add(filter);
    }

    /**
     * 정렬된 필터를 Spring Security 필터 체인에 등록합니다.
     * 필터 순서는 위상 정렬 알고리즘을 통해 결정됩니다.
     * @param http HttpSecurity 구성 객체
     * @throws Exception 구성 중 발생할 수 있는 예외
     */
    public void configureFilters(HttpSecurity http) throws Exception {
        List<PluggableFilter> sortedFilters = topologicalSort();
        
        for (PluggableFilter filter : sortedFilters) {
            log.info("필터 등록: {}, 순서: {}", filter.getClass().getSimpleName(), filter.getOrder());
            filter.configure(http);
        }
    }
    
    /**
     * 필터 의존성을 기반으로 위상 정렬을 수행합니다.
     * @return 정렬된 필터 목록
     */
    private List<PluggableFilter> topologicalSort() {
        Map<Class<?>, PluggableFilter> filterMap = new HashMap<>();
        Map<PluggableFilter, List<PluggableFilter>> graph = new HashMap<>();
        Set<PluggableFilter> visited = new HashSet<>();
        List<PluggableFilter> result = new ArrayList<>();
        
        // 필터 맵 초기화
        for (PluggableFilter filter : filters) {
            filterMap.put(filter.getClass(), filter);
            graph.put(filter, new ArrayList<>());
        }
        
        // 그래프 구성
        for (PluggableFilter filter : filters) {
            // 이 필터 이전에 실행되어야 하는 필터
            Class<? extends Filter> beforeFilter = filter.getBeforeFilter();
            if (beforeFilter != null && filterMap.containsKey(beforeFilter)) {
                graph.get(filterMap.get(beforeFilter)).add(filter);
            }
            
            // 이 필터 이후에 실행되어야 하는 필터
            Class<? extends Filter> afterFilter = filter.getAfterFilter();
            if (afterFilter != null && filterMap.containsKey(afterFilter)) {
                graph.get(filter).add(filterMap.get(afterFilter));
            }
        }
        
        // DFS를 통한 위상 정렬
        for (PluggableFilter filter : filters) {
            if (!visited.contains(filter)) {
                dfs(filter, graph, visited, result);
            }
        }
        
        // 순서(Order) 기반 추가 정렬
        result.sort(Comparator.comparingInt(PluggableFilter::getOrder));
        
        return result;
    }
    
    /**
     * 깊이 우선 탐색을 통한 위상 정렬 수행
     */
    private void dfs(PluggableFilter filter, Map<PluggableFilter, List<PluggableFilter>> graph,
                     Set<PluggableFilter> visited, List<PluggableFilter> result) {
        visited.add(filter);
        
        for (PluggableFilter dependency : graph.get(filter)) {
            if (!visited.contains(dependency)) {
                dfs(dependency, graph, visited, result);
            }
        }
        
        result.add(filter);
    }
}
     */
    public void registerFilters(HttpSecurity http) throws Exception {
        List<PluggableFilter> sortedFilters = topologicalSort(filters);
        
        for (PluggableFilter filter : sortedFilters) {
            try {
                if (filter.getBeforeFilter() != null) {
                    // 지정된 필터 이전에 실행
                    http.addFilterBefore(
                        new FilterWrapper(filter), 
                        (Class<? extends Filter>) filter.getBeforeFilter()
                    );
                } else if (filter.getAfterFilter() != null) {
                    // 지정된 필터 이후에 실행
                    http.addFilterAfter(
                        new FilterWrapper(filter), 
                        (Class<? extends Filter>) filter.getAfterFilter()
                    );
                } else {
                    // 기본 순서로 실행 (UsernamePasswordAuthenticationFilter 위치)
                    http.addFilterBefore(new FilterWrapper(filter), UsernamePasswordAuthenticationFilter.class);
                }
            } catch (Exception e) {
                throw new RuntimeException("Filter registration failed: " + filter.getClass().getSimpleName(), e);
            }
        }
    }

    private List<PluggableFilter> topologicalSort(List<PluggableFilter> filters) {

        // 그래프 인접 리스트 생성
        Map<Class<?>, List<Class<?>>> adjacencyList = new HashMap<>();
        
        // 각 필터의 진입 차수 저장
        Map<Class<?>, Integer> inDegree = new HashMap<>();

        // 필터 클래스를 필터 인스턴스에 매핑
        Map<Class<?>, PluggableFilter> filterMap = new HashMap<>();    


        // 그래프와 진입 차수 초기화
        for (PluggableFilter filter : filters) {
            Class<?> filterClass = filter.getClass();
            adjacencyList.putIfAbsent(filterClass, new ArrayList<>());
            inDegree.putIfAbsent(filterClass, 0);
            filterMap.put(filterClass, filter);
        }

    // before/after 관계를 기반으로 그래프 구축
    for (PluggableFilter filter : filters) {
        Class<?> filterClass = filter.getClass();
        
        if (filter.getBeforeFilter() != null) {
            // A가 B 이전에 오면, A에서 B로 간선 추가
            adjacencyList.get(filterClass).add(filter.getBeforeFilter());
            inDegree.merge(filter.getBeforeFilter(), 1, Integer::sum);
        }
        
        if (filter.getAfterFilter() != null) {
            // A가 B 이후에 오면, B에서 A로 간선 추가
            adjacencyList.putIfAbsent(filter.getAfterFilter(), new ArrayList<>());
            adjacencyList.get(filter.getAfterFilter()).add(filterClass);
            inDegree.merge(filterClass, 1, Integer::sum);
        }
    }
    
    // 위상 정렬 수행
    return performKahnsAlgorithm(adjacencyList, inDegree, filterMap);
}

private List<PluggableFilter> performKahnsAlgorithm(
        Map<Class<?>, List<Class<?>>> adjacencyList,
        Map<Class<?>, Integer> inDegree,
        Map<Class<?>, PluggableFilter> filterMap) {
    
    List<PluggableFilter> sortedFilters = new ArrayList<>();
    Queue<Class<?>> queue = new LinkedList<>();
    
    // 진입 차수가 0인 노드(의존성이 없는 필터)부터 시작
    for (Map.Entry<Class<?>, Integer> entry : inDegree.entrySet()) {
        if (entry.getValue() == 0) {
            queue.add(entry.getKey());
        }
    }
    
    // 위상 정렬 순서대로 노드 처리
    while (!queue.isEmpty()) {
        Class<?> current = queue.poll();
        PluggableFilter filter = filterMap.get(current);
        
        if (filter != null) {
            sortedFilters.add(filter);
        }
        
        // 각 종속 필터의 진입 차수를 감소시키고 준비됐는지 확인
        for (Class<?> dependent : adjacencyList.getOrDefault(current, Collections.emptyList())) {
            inDegree.put(dependent, inDegree.get(dependent) - 1);
            if (inDegree.get(dependent) == 0) {
                queue.add(dependent);
            }
        }
    }
    
    // 종속성 그래프에 순환이 있는지 확인
    if (sortedFilters.size() != filterMap.size()) {
        throw new IllegalStateException("필터 간에 순환 의존성이 감지되었습니다");
    }
    
    return sortedFilters;
}

/**
 * 필터 어댑터 클래스
 * Spring Security Filter와 PluggableFilter 간의 호환성 제공
 */
private static class FilterWrapper extends GenericFilterBean {
    private final PluggableFilter delegate;

    public FilterWrapper(PluggableFilter delegate) {
        this.delegate = delegate;
    }

    @Override
    public void doFilter(
        ServletRequest request,
        ServletResponse response,
        FilterChain chain
    ) throws ServletException, IOException {
        delegate.doFilter(request, response, chain);
    }
}
}