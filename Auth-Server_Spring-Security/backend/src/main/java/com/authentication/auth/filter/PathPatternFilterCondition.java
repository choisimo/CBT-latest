package com.authentication.auth.filter;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;

import java.util.HashSet;
import java.util.Set;

/**
 * @Author: choisimo
 * @Date: 2025-05-05
 * @Description: 경로 패턴 기반 필터 조건
 * @Details: URL 패턴과 HTTP 메소드 기반으로 필터 적용 여부를 결정
 */
public class PathPatternFilterCondition implements FilterCondition {
    
    private final Set<String> patterns = new HashSet<>();
    private final Set<HttpMethod> methods = new HashSet<>();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final String description;
    
    /**
     * 경로 패턴만 지정하는 생성자
     * @param description 조건 설명
     * @param patterns 포함할 URL 패턴 (Ant-style)
     */
    public PathPatternFilterCondition(String description, String... patterns) {
        this.description = description;
        for (String pattern : patterns) {
            this.patterns.add(pattern);
        }
    }
    
    /**
     * 경로 패턴과 HTTP 메소드를 지정하는 생성자
     * @param description 조건 설명
     * @param methods 포함할 HTTP 메소드
     * @param patterns 포함할 URL 패턴 (Ant-style)
     */
    public PathPatternFilterCondition(String description, HttpMethod[] methods, String... patterns) {
        this(description, patterns);
        for (HttpMethod method : methods) {
            this.methods.add(method);
        }
    }
    
    /**
     * 패턴 추가
     * @param pattern 추가할 패턴
     * @return 현재 객체 (체이닝용)
     */
    public PathPatternFilterCondition addPattern(String pattern) {
        this.patterns.add(pattern);
        return this;
    }
    
    /**
     * HTTP 메소드 추가
     * @param method 추가할 HTTP 메소드
     * @return 현재 객체 (체이닝용)
     */
    public PathPatternFilterCondition addMethod(HttpMethod method) {
        this.methods.add(method);
        return this;
    }

    @Override
    public boolean shouldNotFilter(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        
        // 어느 하나의 패턴이라도 맞으면 필터 제외
        boolean matchesPattern = patterns.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, requestPath));
        
        // HTTP 메소드 제한이 없거나, 요청 메소드가 지정된 메소드 중 하나와 일치하면 필터 제외
        boolean matchesMethod = methods.isEmpty() || 
                               methods.contains(HttpMethod.valueOf(request.getMethod()));
        
        return matchesPattern && matchesMethod;
    }

    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return "PathPatternFilterCondition{" +
                "description='" + description + '\'' +
                ", patterns=" + patterns +
                ", methods=" + methods +
                '}';
    }
}
