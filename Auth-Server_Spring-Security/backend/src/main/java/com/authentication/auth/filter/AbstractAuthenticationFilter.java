package com.authentication.auth.filter;

import com.authentication.auth.configuration.token.JwtUtility;
import com.authentication.auth.service.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @Author: choisimo
 * @Date: 2025-05-05
 * @Description: 인증 필터 추상 클래스
 * @Details: 사용자 로그인 요청을 처리하고 JWT 토큰을 생성하는 필터의 기본 구조 정의
 * 
 * 추상(abstract) 클래스 vs 구체적 구현 클래스 차이점:
 * 1. 추상 클래스: 
 *    - 불완전한 설계를 제공하며 상속을 통해 구체적 구현을 강제합니다.
 *    - 자식 클래스가 반드시 구현해야 하는 메소드를 정의할 수 있습니다.
 *    - 공통 기능을 제공하면서 확장성을 보장합니다.
 *    - 이 방식은 프레임워크나 라이브러리를 설계할 때 유용합니다.
 *    
 * 2. 구체적 구현 클래스: 
 *    - 직접 인스턴스화 가능하고 모든 메소드가 구현되어 있습니다.
 *    - 특정한 비즈니스 로직에 맞게 최적화되어 있습니다.
 *    - 상속보다는 특정 인터페이스 구현에 중점을 둡니다.
 *    - 이 방식은 실제 애플리케이션 로직을 구현할 때 적합합니다.
 *    
 * 접근 제한자 사용:
 * - private: 클래스 내부에서만 접근 가능한 필드들(authenticationManager 등)은 캡슐화를 위해 사용
 * - protected: 상속 관계에서 자식 클래스에게 접근을 허용하기 위해 일부 메소드에 사용
 * - public: 외부에서 호출 가능한 API를 제공하기 위해 사용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public abstract class AbstractAuthenticationFilter extends UsernamePasswordAuthenticationFilter implements PluggableFilter {
    
    private final AuthenticationManager authenticationManager;
    private final JwtUtility jwtUtility;
    private final ObjectMapper objectMapper;
    private final RedisService redisService;
    private final String domain;
    private final String cookieDomain;
    
    /**
     * 추상 클래스 구현에서는 공통된 인증 로직을 제공합니다.
     * 이 메소드는 public으로 선언되어 Spring Security 필터 체인에서 직접 호출될 수 있습니다.
     * 
     * 반면 구체적 구현 클래스에서는 doFilterInternal 메소드를 활용해 더 세부적인 인증 로직을 구현합니다.
     * 이 방식은 JWT 토큰 검증과 같은 특정 인증 방식에 최적화되어 있습니다.
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        log.info("인증 시도");
        try {
            // 사용자 인증 로직
            // 로그인 요청에서 사용자 이름과 비밀번호 추출
            // 인증 관리자를 통해 인증 시도
            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getParameter("username"), request.getParameter("password"))
            );
        } catch (Exception e) {
            log.error("인증 시도 중 오류 발생: ", e);
            throw new AuthenticationException("인증 실패") {};
        }
    }
package com.authentication.auth.filter;

import com.authentication.auth.configuration.token.JwtUtility;
import com.authentication.auth.service.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.Map;

/**
 * @Author: choisimo
 * @Date: 2025-05-05
 * @Description: 추상 인증 필터
 * @Details: 인증 처리를 위한 기본 기능을 제공하는 추상 필터 클래스
 */
@Slf4j
public abstract class AbstractAuthenticationFilter extends UsernamePasswordAuthenticationFilter implements PluggableFilter {
    
    protected final AuthenticationManager authenticationManager;
    protected final JwtUtility jwtUtility;
    protected final ObjectMapper objectMapper;
    protected final RedisService redisService;
    protected final String domain;
    protected final String cookieDomain;
    
    protected AbstractAuthenticationFilter(
            AuthenticationManager authenticationManager,
            JwtUtility jwtUtility,
            ObjectMapper objectMapper,
            RedisService redisService,
            String domain,
            String cookieDomain) {
        this.authenticationManager = authenticationManager;
        this.jwtUtility = jwtUtility;
        this.objectMapper = objectMapper;
        this.redisService = redisService;
        this.domain = domain;
        this.cookieDomain = cookieDomain;
        
        setAuthenticationManager(authenticationManager);
    }
    
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) 
            throws AuthenticationException {
        try {
            // 요청 본문에서 사용자 정보 파싱
            Map<String, String> credentials = objectMapper.readValue(
                    request.getInputStream(), Map.class);
            
            String username = credentials.get("username");
            String password = credentials.get("password");
            
            log.debug("사용자 로그인 시도: {}", username);
            
            // 인증 토큰 생성 및 인증 요청
            UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(username, password);
            
            return authenticationManager.authenticate(authToken);
        } catch (IOException e) {
            log.error("인증 요청 처리 중 오류 발생", e);
            throw new RuntimeException("인증 요청을 처리할 수 없습니다", e);
        }
    }
    
    @Override
    public String getFilterId() {
        return this.getClass().getSimpleName();
    }
}
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                           FilterChain chain, Authentication authResult) throws IOException, ServletException {
        log.info("인증 성공");
        // JWT 토큰 생성 및 응답에 추가
        // SecurityContext에 인증 정보 설정
    }
