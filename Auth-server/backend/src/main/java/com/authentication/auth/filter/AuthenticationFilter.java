package com.authentication.auth.filter;

import com.authentication.auth.exception.ErrorType;
import com.authentication.auth.others.constants.FilterOrder;
import com.authentication.auth.others.constants.SecurityConstants;
import com.authentication.auth.configuration.token.JwtUtility;
import com.authentication.auth.dto.response.ApiResponse;
import com.authentication.auth.dto.response.LoginResponseDto;
import com.authentication.auth.dto.token.TokenDto;
import com.authentication.auth.dto.token.PrincipalDetails;
import com.authentication.auth.service.redis.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 사용자 인증 필터
 * 사용자 로그인 요청을 처리하고 JWT 토큰을 생성하는 필터
 */
@Slf4j
@Component
public class AuthenticationFilter extends UsernamePasswordAuthenticationFilter implements PluggableFilter {
    
    private final AuthenticationManager authenticationManager;
    private final JwtUtility jwtUtility;
    private final ObjectMapper objectMapper;
    private final RedisService redisService;
    private final String domain;
    private final String cookieDomain;
    private final FilterRegistry filterRegistry;
    
    /**
     * AuthenticationFilter 생성자
     * @param authenticationManager Spring Security의 AuthenticationManager
     * @param jwtUtility JWT 토큰 생성 및 검증 유틸리티
     * @param objectMapper JSON 직렬화/역직렬화를 위한 ObjectMapper
     * @param redisService Redis 관련 서비스 로직 처리
     * @param domain 사이트 도메인 주입 (Value)
     * @param cookieDomain 쿠키 도메인 주입 (Value)
     * @param filterRegistry 필터 레지스트리
     * @Description 의존성 주입 및 로그인 처리 URL 설정
     */
    @Autowired
    public AuthenticationFilter(
            @Lazy AuthenticationManager authenticationManager,
            JwtUtility jwtUtility,
            ObjectMapper objectMapper,
            RedisService redisService,
            @Value("${site.domain}") String domain,
            @Value("${server.cookie.domain}") String cookieDomain,
            FilterRegistry filterRegistry) {
        this.authenticationManager = authenticationManager;
        this.jwtUtility = jwtUtility;
        this.objectMapper = objectMapper;
        // Ensure JavaTimeModule and other registered modules are detected for serialization (e.g., LocalDateTime)
        this.objectMapper.findAndRegisterModules();
        this.redisService = redisService;
        this.domain = domain;
        this.cookieDomain = cookieDomain;
        this.filterRegistry = filterRegistry;
        setFilterProcessesUrl(SecurityConstants.LOGIN_PATH.getValue());
        setAuthenticationManager(authenticationManager);
    }
    
    /**
     * 필터 초기화 및 레지스트리에 등록
     * @Description AuthenticationFilter를 FilterRegistry에 등록하고, 필터링 조건을 설정합니다.
     *              공개 API 경로는 인증 필터 적용에서 제외하고, 로그인 경로는 POST 메소드만 허용합니다.
     */
    @PostConstruct
    public void init() {
        // 필터 레지스트리에 이 필터 등록
        filterRegistry.registerFilter(this);
        
        // 기본 필터 조건 설정 - 공개 API는 인증 필터 적용하지 않음
        PathPatternFilterCondition publicApiCondition = new PathPatternFilterCondition(
                "공개 API 경로 제외",
                SecurityConstants.getPublicPaths().toArray(new String[0])
        );
        
        // 로그인 경로에는 POST 메소드만 허용하고 다른 메소드는 필터링
        PathPatternFilterCondition loginPathCondition = new PathPatternFilterCondition(
                "로그인 경로 메소드 제한",
                new HttpMethod[] {HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE},
                SecurityConstants.LOGIN_PATH.getValue()
        );
        
        // 필터 레지스트리에 조건 추가
        filterRegistry.addCondition(getFilterId(), publicApiCondition);
        filterRegistry.addCondition(getFilterId(), loginPathCondition);
    }

    /**
     * 실제 인증 시도
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @return Authentication 인증 객체
     * @throws AuthenticationException 인증 실패 시 예외 발생
     * @Description 사용자가 제출한 username과 password를 기반으로 인증을 시도합니다.
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) 
            throws AuthenticationException {
        log.info("인증 시도: {}", request.getRequestURI());
        
        try {
            // 요청 바디에서 사용자 자격 증명 추출
            Map<String, String> credentials = objectMapper.readValue(request.getInputStream(), Map.class);
            // 프론트엔드에서 email 또는 loginId 중 무엇이든 보낼 수 있도록 유연하게 처리
            String identifier = credentials.getOrDefault("email", null);
            if (identifier == null || identifier.isBlank()) {
                identifier = credentials.getOrDefault("loginId", credentials.get("username")); // username 호환
            }
            String password = credentials.get("password");

            log.info("로그인 요청 payload: {}", credentials);
            log.info("추출된 identifier: {}", identifier);

            if (identifier == null || identifier.isBlank() || password == null || password.isBlank()) {
                log.error("로그인 요청에 식별자 또는 비밀번호가 누락되었습니다. identifier={}, passwordNull={}", identifier, (password == null));
                throw new AuthenticationException(ErrorType.INVALID_REQUEST.getMessage()) {};
            }

            // 인증 토큰 생성 및 인증 시도
            UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken(identifier, password);
                
            return authenticationManager.authenticate(authToken);
        } catch (IOException e) {
            log.error("인증 요청 처리 중 오류 발생: {}", e.getMessage());
            throw new AuthenticationException(ErrorType.AUTHENTICATION_FAILED.getMessage()) {};
        }
    }

    /**
     * 인증 성공 시 처리
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param chain 필터 체인
     * @param authResult 인증 결과 객체
     * @throws IOException 입출력 예외
     * @throws ServletException 서블릿 예외
     * @Description 인증에 성공하면 JWT 액세스 토큰과 리프레시 토큰을 생성하여 쿠키와 응답 본문에 담아 반환합니다.
     */
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
                                        Authentication authResult) throws IOException, ServletException {
        log.info("인증 성공: 토큰 생성 시작");
        
        // 사용자 정보 추출
        String username = ((PrincipalDetails) authResult.getPrincipal()).getUsername();
        
        // 토큰 생성
        TokenDto tokenDto = jwtUtility.buildToken(username, authResult.getAuthorities());
        
        // 리프레시 토큰을 Redis에 저장
        redisService.saveRToken(username, SecurityConstants.DEFAULT_PROVIDER.getValue(), tokenDto.refreshToken());
        redisService.saveAccessToken(tokenDto.refreshToken(), tokenDto.accessToken(), username);
        
        response.addHeader(SecurityConstants.TOKEN_HEADER.getValue(), 
                          SecurityConstants.TOKEN_PREFIX.getValue() + tokenDto.accessToken());
        
        // 응답 본문에도 토큰 정보 포함
        var user = ((PrincipalDetails) authResult.getPrincipal()).getUser();
        var roles = authResult.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .collect(java.util.stream.Collectors.toList());
        var loginResponse = LoginResponseDto.of(user, tokenDto, roles);

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                ApiResponse.success(loginResponse, "로그인 성공"));
        
        // SecurityContext에 인증 정보 설정
        SecurityContextHolder.getContext().setAuthentication(authResult);
        
        log.info("인증 성공: 토큰 생성 완료 및 쿠키 설정");
    }
    
    /**
     * 인증 실패 시 처리
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param failed 인증 실패 예외 객체
     * @throws IOException 입출력 예외
     * @throws ServletException 서블릿 예외
     * @Description 인증에 실패하면 적절한 오류 응답을 클라이언트에게 반환합니다.
     */
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                          AuthenticationException failed) throws IOException, ServletException {
        log.error("인증 실패: {}", failed.getMessage());
        
        SecurityContextHolder.clearContext();
        
        response.setStatus(ErrorType.AUTHENTICATION_FAILED.getStatusCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        objectMapper.writeValue(response.getOutputStream(), 
                               ApiResponse.error(ErrorType.AUTHENTICATION_FAILED));
    }
    
    /**
     * 오류 응답 전송
     * @param response HTTP 응답
     * @param errorType 오류 유형
     * @param message 오류 메시지
     * @throws IOException 입출력 예외
     * @Description 지정된 오류 유형과 메시지로 API 응답을 생성하여 클라이언트에게 전송합니다.
     */
    private void sendErrorResponse(HttpServletResponse response, ErrorType errorType, String message) throws IOException {
        response.setStatus(errorType.getStatusCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", errorType.getMessage());
        errorResponse.put("message", message);
        
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }

    /**
     * 쿠키 생성
     * @param name 쿠키 이름
     * @param value 쿠키 값
     * @param maxAge 쿠키 유효 시간 (초)
     * @return Cookie 생성된 쿠키 객체
     * @Description 지정된 이름, 값, 유효 시간을 갖는 HTTP 쿠키를 생성합니다. HttpOnly, Secure, Path, Domain 속성을 설정합니다.
     */
    private Cookie createCookie(String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath(SecurityConstants.COOKIE_PATH.getValue());
        cookie.setDomain(cookieDomain);
        cookie.setMaxAge(maxAge);
        return cookie;
    }

    /**
     * 필터 ID 반환
     * @return String 필터의 고유 ID
     * @Description 이 필터의 고유 ID를 반환합니다. 일반적으로 클래스 이름으로 설정됩니다.
     */
    @Override
    public String getFilterId() {
        return this.getClass().getSimpleName();
    }
    
    /**
     * HttpSecurity에 필터 구성 (UsernamePasswordAuthenticationFilter는 이미 SecurityFilterChain에 의해 관리됨)
     * @param http HttpSecurity 객체
     * @throws Exception 구성 중 예외 발생 시
     * @Description UsernamePasswordAuthenticationFilter를 상속하므로, Spring Security가 자동으로 구성합니다. 
     *              별도의 HttpSecurity 설정이 필요하지 않습니다.
     */
    @Override
    public void configure(HttpSecurity http) throws Exception {
        // Register this custom filter at the position of UsernamePasswordAuthenticationFilter
        http.addFilterAt(this, UsernamePasswordAuthenticationFilter.class);
    }

    /**
     * 필터 실행 순서 반환
     * @return int 필터 실행 순서 값
     * @Description {@link FilterOrder}에 정의된 AUTHENTICATION_FILTER의 순서 값을 반환합니다.
     */
    @Override
    public int getOrder() {
        return FilterOrder.AUTHENTICATION.getOrder();
    }
    
    /**
     * 필터 적용 여부 결정
     * @param request HTTP 요청
     * @return boolean 필터를 적용하지 않으려면 true, 적용하려면 false
     * @Description FilterRegistry에 등록된 조건들을 확인하여 현재 요청에 이 필터를 적용할지 여부를 결정합니다.
     */
    public boolean shouldNotFilter(HttpServletRequest request) {
        // shouldApplyFilter returns true if the filter SHOULD be applied.
        // shouldNotFilter should return true if the filter should NOT be applied.
        // Therefore, we invert the result of shouldApplyFilter.
        return !filterRegistry.shouldApplyFilter(this.getFilterId(), request);
    }

    @Override
    public Class<? extends Filter> getAfterFilter() {
        return null; // To be implemented if a specific filter needs to run after this
    }

    @Override
    public Class<? extends Filter> getBeforeFilter() {
        return null; // To be implemented if a specific filter needs to run before this
    }
}
