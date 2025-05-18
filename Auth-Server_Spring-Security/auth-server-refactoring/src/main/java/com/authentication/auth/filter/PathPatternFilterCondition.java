package com.authentication.auth.filter;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 경로 패턴 기반 필터 조건
 * URL 패턴과 HTTP 메소드 기반으로 필터 적용 여부를 결정
 */
public record PathPatternFilterCondition(
    String description,
    Set<String> patterns,
    Set<HttpMethod> methods
) implements FilterCondition {
    
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    
    /**
     * 경로 패턴만 지정하는 생성자
     * @param description 조건 설명
     * @param patterns 포함할 URL 패턴 (Ant-style)
     */
    public PathPatternFilterCondition(String description, String... patterns) {
        this(description, new HashSet<>(Arrays.asList(patterns)), new HashSet<>());
    }
    
    /**
     * 경로 패턴과 HTTP 메소드를 지정하는 생성자
     * @param description 조건 설명
     * @param methods 포함할 HTTP 메소드
     * @param patterns 포함할 URL 패턴 (Ant-style)
     */
    public PathPatternFilterCondition(String description, HttpMethod[] methods, String... patterns) {
        this(description, new HashSet<>(Arrays.asList(patterns)), new HashSet<>(Arrays.asList(methods)));
    }
    
    /**
     * 패턴 추가
     * @param pattern 추가할 패턴
     * @return 새 조건 객체
     */
    public PathPatternFilterCondition withPattern(String pattern) {
        Set<String> newPatterns = new HashSet<>(this.patterns);
        newPatterns.add(pattern);
        return new PathPatternFilterCondition(this.description, newPatterns, this.methods);
    }
    
    /**
     * HTTP 메소드 추가
     * @param method 추가할 HTTP 메소드
     * @return 새 조건 객체
     */
    public PathPatternFilterCondition withMethod(HttpMethod method) {
        Set<HttpMethod> newMethods = new HashSet<>(this.methods);
        newMethods.add(method);
        return new PathPatternFilterCondition(this.description, this.patterns, newMethods);
    }

    @Override
    public boolean shouldNotFilter(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        
        // 어느 하나의 패턴이라도 맞으면 필터 제외
        boolean matchesPattern = patterns.stream()
                .anyMatch(pattern -> PATH_MATCHER.match(pattern, requestPath));
        
        // HTTP 메소드 제한이 없거나, 요청 메소드가 지정된 메소드 중 하나와 일치하면 필터 제외
        boolean matchesMethod = methods.isEmpty() || 
                               methods.contains(HttpMethod.valueOf(request.getMethod()));
        
        return matchesPattern && matchesMethod;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
