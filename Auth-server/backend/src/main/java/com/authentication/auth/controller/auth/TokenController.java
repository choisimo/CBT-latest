package com.authentication.auth.controller.auth;

import com.authentication.auth.api.docs.TokenApi;
import com.authentication.auth.dto.users.LoginRequest;
import com.authentication.auth.dto.users.LoginResponse;
import com.authentication.auth.dto.response.ApiResponse;
import com.authentication.auth.dto.token.TokenRefreshRequest;
import com.authentication.auth.dto.token.TokenRefreshResponse; // Assuming this DTO exists for refresh response
import com.authentication.auth.exception.CustomException;
import com.authentication.auth.exception.ErrorType;
import com.authentication.auth.service.token.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@RestController
@RequiredArgsConstructor // Using Lombok for constructor injection
public class TokenController implements TokenApi {

    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager; // Injected AuthenticationManager

    @Override
    @PostMapping("/login") // Ensuring PostMapping is present as per standard practices, overriding from interface
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest loginRequest, HttpServletResponse httpServletResponse) {
        log.info("Login attempt for user: {}", loginRequest.loginId());

        // Create authentication token
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginRequest.loginId(), loginRequest.password());

        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        // If authentication is successful, proceed with post-login actions
        LoginResponse loginResponse = tokenService.postLoginActions(authentication, httpServletResponse);

        return ResponseEntity.ok(ApiResponse.success(loginResponse, "로그인에 성공했습니다."));
    }

    @Override
    @PostMapping("/refresh") // Ensuring PostMapping is present
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshToken(HttpServletRequest httpRequest, HttpServletResponse httpResponse, @RequestBody TokenRefreshRequest request) throws IOException {

        if (request == null || request.expiredToken() == null || request.provider() == null) {
            throw new CustomException(ErrorType.INVALID_REQUEST_BODY, "유효하지 않은 토큰 갱신 요청입니다.");
        }
        // TokenService.refreshToken은 이제 TokenRefreshResponse DTO를 직접 반환하거나, 실패 시 CustomException을 throw 할 것으로 가정합니다.
        TokenRefreshResponse refreshResponse = tokenService.refreshToken(httpRequest, httpResponse, request);
        return ResponseEntity.ok(ApiResponse.success(refreshResponse, "토큰이 성공적으로 갱신되었습니다."));
    }
}
