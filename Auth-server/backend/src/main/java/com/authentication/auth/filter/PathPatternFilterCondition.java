package com.authentication.auth.filter;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 경로 패턴 기반 필터 조건
 * URL 패턴과 HTTP 메소드 기반으로 필터 적용 여부를 결정
 */
public record PathPatternFilterCondition(
    String id,
    String description,
    Set<String> patterns,
    Set<HttpMethod> methods
) implements FilterCondition {
    
    /**
     * 객체 인스턴스 생성 시 싱글톤 패턴 사용 
     * - AntPathMatcher는 스레드 안전하고, 재사용 가능
     * - 성능 향상 및 메모리 절약을 위해 싱글톤으로 사용
     */
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /**
     * 경로 패턴만 지정하는 생성자
     * @param description 조건 설명
     * @param patterns 포함할 URL 패턴 (Ant-style)
     * @Description 주어진 설명과 URL 패턴들로 PathPatternFilterCondition을 생성합니다. HTTP 메소드는 모든 메소드를 허용합니다.
     * 가변 인자(varargs: String...) 를 사용하여 여러 개의 패턴을 매개변수로 받을 수 있습니다.
     */
    public PathPatternFilterCondition(String description, String... patterns) {
        this(UUID.randomUUID().toString(), description, new HashSet<>(Arrays.asList(patterns)), new HashSet<>());
    }
    
    /**
     * 경로 패턴과 HTTP 메소드를 지정하는 생성자
     * @param description 조건 설명
     * @param methods 포함할 HTTP 메소드 배열
     * @param patterns 포함할 URL 패턴 (Ant-style)
     * @Description 주어진 설명, HTTP 메소드 배열, URL 패턴들로 PathPatternFilterCondition을 생성합니다.
     */
    public PathPatternFilterCondition(String description, HttpMethod[] methods, String... patterns) {
        this(UUID.randomUUID().toString(), description, new HashSet<>(Arrays.asList(patterns)), new HashSet<>(Arrays.asList(methods)));
    }
    
    /**
     * 패턴 추가
     * @param pattern 추가할 패턴
     * @return 새 조건 객체
     */
    public PathPatternFilterCondition withPattern(String pattern) {
        Set<String> newPatterns = new HashSet<>(this.patterns);
        newPatterns.add(pattern);
        return new PathPatternFilterCondition(this.id, this.description, newPatterns, this.methods);
    }
    
    /**
     * HTTP 메소드 추가
     * @param method 추가할 HTTP 메소드
     * @return 새 조건 객체
     */
    public PathPatternFilterCondition withMethod(HttpMethod method) {
        Set<HttpMethod> newMethods = new HashSet<>(this.methods);
        newMethods.add(method);
        return new PathPatternFilterCondition(this.id, this.description, this.patterns, newMethods);
    }

    /**
     * 모든 매개변수를 받는 기본 레코드 생성자
     * @param id 고유 식별자
     * @param description 조건 설명 문자열
     * @param patterns 매칭할 URL 패턴 Set
     * @param methods 매칭할 HTTP 메소드 Set
     * @Description PathPatternFilterCondition 레코드의 전체 필드를 초기화합니다.
     */
    public PathPatternFilterCondition {
        // 레코드 생성자는 필드 초기화 외의 로직을 가질 수 없음
    }

    @Override
    public String getId() {
        return this.id;
    }

    /**
     * 요청에 필터를 적용하지 않을지 여부를 결정
     * @param request HTTP 요청
     * @return boolean 필터를 적용하지 않으려면 true, 적용하려면 false
     * @Description 현재 요청의 URI가 지정된 패턴 중 하나와 일치하고, HTTP 메소드가 지정된 메소드 중 하나와 일치하는 경우 true를 반환합니다.
     *              (즉, 필터를 건너뛰어야 함을 의미)
     *              만약 patterns나 methods가 비어있다면, 해당 조건은 무시됩니다 (모든 경로 또는 모든 메소드에 해당).
     */
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

    /**
     * 조건 설명 반환
     * @return String 조건 설명
     * @Description 이 조건 인스턴스에 제공된 설명을 반환합니다.
     */
    @Override
    public String description() {
        return description;
    }

    /**
     * URL 패턴 Set 반환
     * @return Set<String> URL 패턴 Set
     * @Description 이 조건에 설정된 URL 패턴들을 반환합니다.
     */
    @Override
    public Set<String> patterns() {
        return patterns;
    }

    /**
     * HTTP 메소드 Set 반환
     * @return Set<HttpMethod> HTTP 메소드 Set
     * @Description 이 조건에 설정된 HTTP 메소드들을 반환합니다.
     */
    @Override
    public Set<HttpMethod> methods() {
        return methods;
    }

    @Override
    public String getDescription() {
        return this.description();
    }
}
