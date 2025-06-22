package com.authentication.auth.filter;

import com.authentication.auth.configuration.token.JwtUtility;
import com.authentication.auth.dto.response.ApiResponse;
import com.authentication.auth.dto.response.LoginResponseDto;
import com.authentication.auth.dto.token.PrincipalDetails;
import com.authentication.auth.dto.token.TokenDto;
import com.authentication.auth.exception.ErrorType;
import com.authentication.auth.others.constants.SecurityConstants;
import com.authentication.auth.service.redis.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class AuthenticationFilter extends OncePerRequestFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtUtility jwtUtility;
    private final ObjectMapper objectMapper;
    private final RedisService redisService;
    private final String cookieDomain;
    private final int accessTokenValidity;
    private final AntPathRequestMatcher requestMatcher;

    private static final String SPRING_SECURITY_FORM_USERNAME_KEY = "loginId";
    private static final String SPRING_SECURITY_FORM_PASSWORD_KEY = "password";

    public AuthenticationFilter(
            AuthenticationManager authenticationManager,
            JwtUtility jwtUtility,
            ObjectMapper objectMapper,
            RedisService redisService,
            String cookieDomain,
            int accessTokenValidity) {
        this.authenticationManager = authenticationManager;
        this.jwtUtility = jwtUtility;
        this.objectMapper = objectMapper;
        this.redisService = redisService;
        this.cookieDomain = cookieDomain;
        this.accessTokenValidity = accessTokenValidity;
        this.requestMatcher = new AntPathRequestMatcher("/api/auth/login", "POST");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (!requestMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        System.out.println("--- [AuthenticationFilter] Matched request. Attempting authentication for: " + request.getRequestURI() + " ---");

        try {
            Map<String, String> credentials = objectMapper.readValue(request.getInputStream(), Map.class);
            String identifier = credentials.getOrDefault(SPRING_SECURITY_FORM_USERNAME_KEY, "");
            String password = credentials.getOrDefault(SPRING_SECURITY_FORM_PASSWORD_KEY, "");

            if (!isValidIdentifier(identifier)) {
                sendErrorResponse(response, ErrorType.AUTHENTICATION_FAILED, "Username or email must be provided.");
                return;
            }

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(identifier, password);
            Authentication authResult = authenticationManager.authenticate(authToken);

            // Successful authentication logic
            SecurityContextHolder.getContext().setAuthentication(authResult);
            PrincipalDetails principal = (PrincipalDetails) authResult.getPrincipal();
            TokenDto tokenDto = jwtUtility.buildToken(authResult.getName(), authResult.getAuthorities());

            redisService.saveRToken(
                    principal.getUser().getEmail(),
                    "local",
                    tokenDto.refreshToken()
            );

            Cookie accessTokenCookie = createCookie(SecurityConstants.COOKIE_ACCESS_TOKEN.getValue(), tokenDto.accessToken(), accessTokenValidity);
            Cookie refreshTokenCookie = createCookie(SecurityConstants.COOKIE_REFRESH_TOKEN.getValue(), tokenDto.refreshToken(), SecurityConstants.REFRESH_TOKEN_TTL_SECONDS.getIntValue());

            response.addCookie(accessTokenCookie);
            response.addCookie(refreshTokenCookie);

            LoginResponseDto loginResponseDto = LoginResponseDto.of(
                    principal.getUser(),
                    tokenDto,
                    principal.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList())
            );

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), ApiResponse.success(loginResponseDto));
            log.info("인증 성공: {}", principal.getUser().getEmail());

        } catch (AuthenticationException failed) {
            // Unsuccessful authentication logic
            SecurityContextHolder.clearContext();
            log.warn("인증 실패: {}", failed.getMessage());
            sendErrorResponse(response, ErrorType.AUTHENTICATION_FAILED, failed.getMessage());
        }
    }

    private void sendErrorResponse(HttpServletResponse response, ErrorType errorType, String message) throws IOException {
        response.setStatus(errorType.getStatusCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> details = message != null ? Map.of("detail", message) : null;
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(errorType, details));
    }

    private Cookie createCookie(String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath(SecurityConstants.COOKIE_PATH.getValue());
        cookie.setDomain(cookieDomain);
        cookie.setMaxAge(maxAge);
        return cookie;
    }

    private boolean isValidIdentifier(String identifier) {
        return identifier != null
                && !identifier.isBlank()
                && !"NONE_PROVIDED".equalsIgnoreCase(identifier)
                && !"null".equalsIgnoreCase(identifier)
                && !"undefined".equalsIgnoreCase(identifier);
    }
}
