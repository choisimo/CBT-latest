package com.authentication.auth.filter;

import com.authentication.auth.configuration.token.JwtUtility;
import com.authentication.auth.dto.response.ApiResponse;
import com.authentication.auth.exception.ErrorType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 토큰 검증 필터
 * 요청에 포함된 JWT 토큰을 검증하고 사용자 인증 정보를 설정
 */
@Slf4j
public class JwtVerificationFilter extends OncePerRequestFilter {

    private final JwtUtility jwtUtility;
    private final ObjectMapper objectMapper;

    /**
     * JwtVerificationFilter 생성자
     * @param jwtUtility JWT 토큰 생성 및 검증 유틸리티
     * @param objectMapper JSON 직렬화/역직렬화를 위한 ObjectMapper
     */
    public JwtVerificationFilter(JwtUtility jwtUtility, ObjectMapper objectMapper) {
        this.jwtUtility = jwtUtility;
        this.objectMapper = objectMapper;
    }

    /**
     * 필터의 주요 비즈니스 로직 구현 (JWT 검증)
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param filterChain 필터 체인
     * @throws IOException 입출력 예외
     * @throws ServletException 서블릿 예외
     * @Description 요청 헤더에서 JWT 토큰을 추출하여 유효성을 검증하고, 유효한 경우 SecurityContextHolder에 인증 정보를 설정합니다.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        log.debug("JWT 검증 필터 실행: {}", path);

        try {
            String token = extractToken(request);

            if (token != null && jwtUtility.validateJWT(token)) {
                Authentication authentication = jwtUtility.getAuthentication(token);
                if (authentication != null) {
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Security Context에 '{}' 인증 정보를 저장했습니다, uri: {}", authentication.getName(), request.getRequestURI());
                } else {
                    log.debug("유효한 JWT 토큰이지만, 인증 정보 생성에 실패했습니다. uri: {}", request.getRequestURI());
                    // Depending on policy, might send an error or allow request to proceed unauthenticated
                }
            } else if (token == null) {
                log.debug("JWT 토큰이 없습니다, uri: {}", request.getRequestURI());
                // If token is required for the path and is missing, an error might be appropriate.
                // However, shouldNotFilter should handle public paths. If it reaches here and token is null,
                // it might be an implicitly protected path or a misconfiguration.
                // For now, let it proceed, relying on subsequent security mechanisms or endpoint-specific checks.
            } else { // Token is not null but invalid
                log.warn("유효하지 않은 JWT 토큰입니다, uri: {}", request.getRequestURI());
                sendErrorResponse(response, ErrorType.INVALID_TOKEN);
                return; // Stop processing if token is invalid
            }

            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다, uri: {}: {}", request.getRequestURI(), e.getMessage());
            sendErrorResponse(response, ErrorType.TOKEN_EXPIRED);
        } catch (JwtException e) {
            log.error("JWT 처리 중 오류 발생, uri: {}: {}", request.getRequestURI(), e.getMessage());
            sendErrorResponse(response, ErrorType.INVALID_TOKEN);
        } catch (Exception e) {
            log.error("JwtVerificationFilter 처리 중 예기치 않은 오류 발생, uri: {}: {}", request.getRequestURI(), e.getMessage(), e);
            //sendErrorResponse(response, ErrorType.INTERNAL_SERVER_ERROR);
            throw e;
        }
    }

    /**
     * 요청에서 JWT 토큰 추출
     * @param request HTTP 요청
     * @return String 추출된 JWT 토큰 (없거나 형식이 맞지 않으면 null)
     * @Description HTTP 요청의 Authorization 헤더에서 'Bearer ' 접두사를 가진 JWT 토큰을 추출합니다.
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        return null;
    }

    /**
     * 오류 응답 전송
     * @param response HTTP 응답
     * @param errorType 오류 유형
     * @throws IOException 입출력 예외
     * @Description 지정된 오류 유형으로 API 응답을 생성하여 클라이언트에게 전송합니다.
     */
    private void sendErrorResponse(HttpServletResponse response, ErrorType errorType) throws IOException {
        if (response.isCommitted()) {
            // Another component (e.g., Spring error handler) already started the response
            log.warn("Response already committed, skipping error response for {}", errorType);
            return;
        }
        response.setStatus(errorType.getStatusCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        objectMapper.writeValue(response.getOutputStream(), 
                               ApiResponse.error(errorType));
    }
}
