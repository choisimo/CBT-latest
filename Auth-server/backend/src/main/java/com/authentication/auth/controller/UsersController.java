package com.authentication.auth.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.authentication.auth.configuration.token.JwtUtility;
import com.authentication.auth.dto.response.ApiResponse;
import com.authentication.auth.dto.token.TokenDto;
import com.authentication.auth.dto.users.JoinRequest;
import com.authentication.auth.dto.users.UserNameCheckRequestDto;
import com.authentication.auth.dto.response.ApiResponse;
import com.authentication.auth.exception.CustomException;
import com.authentication.auth.exception.ErrorType;
import com.authentication.auth.others.constants.SecurityConstants;
import com.authentication.auth.service.file.FileService;
import com.authentication.auth.service.redis.RedisService;
import com.authentication.auth.service.smtp.EmailService;
import com.authentication.auth.service.users.UserService;

import java.util.*;

/**
 * 사용자 관리 컨트롤러
 * 회원가입, 프로필 업로드, 중복 체크 등을 처리합니다.
 */

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UsersController implements UserApi {

    @Value("${site.domain}")
    private String domain;
    
    private final UserService userService;
    private final EmailService emailService;
    private final RedisService redisService;
    private final JwtUtility jwtUtility;
    private final FileService fileService;

    @Override
    @PostMapping("/public/join")
    public ResponseEntity<ApiResponse<String>> join(@RequestBody JoinRequest request) {
        // 이메일 인증 코드 확인
        if (!redisService.checkEmailCode(request.email(), request.code())) {
            throw new CustomException(ErrorType.INVALID_EMAIL_CODE, "이메일 인증이 완료되지 않았습니다.");
        }
        
        // 회원가입 처리 (예외는 GlobalExceptionHandler에서 처리)
        userService.join(request);
        
        return ResponseEntity.ok(ApiResponse.success(null, "회원가입이 성공적으로 완료되었습니다."));
    }

    @Override
    @PostMapping("/public/profileUpload")
    public ResponseEntity<ApiResponse<Map<String, String>>> fileUpload(@RequestParam("profile") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new CustomException(ErrorType.EMPTY_FILE, "업로드할 파일이 없습니다.");
        }
        
        Map<String, String> uploadResults = new HashMap<>();
        
        for (MultipartFile file : files) {
            // FileService에 파일 저장 위임 (예외는 GlobalExceptionHandler에서 처리)
            String fileUrl = fileService.storeProfileImage(file);
            uploadResults.put("fileName", fileUrl);
        }
        
        return ResponseEntity.ok(ApiResponse.success(uploadResults, "프로필 이미지가 성공적으로 업로드되었습니다."));
    }

    @Override
    @PostMapping("/public/check/nickname/IsDuplicate")
    public ResponseEntity<ApiResponse<Boolean>> checkUserNameIsDuplicate(@RequestBody UserNameCheckRequestDto requestDto) {
        log.info("/check/userName/IsDuplicate : {}", requestDto.userName());
        boolean isDuplicate = userService.checkUserNameIsDuplicate(requestDto.userName());
        return ResponseEntity.ok(ApiResponse.success(isDuplicate, "닉네임 중복 확인이 완료되었습니다."));
    }

    @Override
    @PostMapping("/public/check/userId/IsDuplicate")
    public ResponseEntity<ApiResponse<Boolean>> checkUserIdIsDuplicate(@RequestBody UserNameCheckRequestDto requestDto) {
        log.info("/check/userId/IsDuplicate : {}", requestDto.userName());
        boolean isDuplicate = userService.checkUserNameIsDuplicate(requestDto.userName());
        return ResponseEntity.ok(ApiResponse.success(isDuplicate, "사용자 ID 중복 확인이 완료되었습니다."));
    }

    @Override
    @PostMapping("/public/clean/userTokenCookie")
    public ResponseEntity<ApiResponse<String>> cleanUserTokenCookie(HttpServletRequest request, HttpServletResponse response) {
        String cookieName = "refreshToken";
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    cookie.setMaxAge(0);
                    cookie.setPath("/");
                    cookie.setHttpOnly(true);
                    response.addCookie(cookie);
                }
            }
        }
        return ResponseEntity.ok(ApiResponse.success(null, "리프레시 토큰이 성공적으로 삭제되었습니다."));
    }

    /**
     * 새로운 쿠키를 프론트엔드에 전송합니다.
     */
    private void sendFrontNewCookie(HttpServletResponse response, int status, TokenDto tokendto) {
        response.setStatus(status);
        response.addHeader(SecurityConstants.TOKEN_HEADER.getValue(), SecurityConstants.TOKEN_PREFIX.getValue() + tokendto.accessToken());
        Cookie refreshTokenCookie = new Cookie("refreshToken", tokendto.refreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setDomain(domain);
        refreshTokenCookie.setPath("/");
        response.addCookie(refreshTokenCookie);
    }

    /**
     * 요청에서 리프레시 토큰을 추출합니다.
     */
    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        log.error("[getRefreshTokenFromCookie] cookie 에서 refreshToken 찾기 실패");
        return null;
    }

    /**
     * Redis에서 리프레시 토큰 일치 여부를 확인합니다.
     */
    private boolean RedisMatchRToken(String userId, String RToken) {
        return redisService.findRToken(userId, "server", RToken);
    }
}