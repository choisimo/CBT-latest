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
import com.authentication.auth.dto.common.ApiResponse;
import com.authentication.auth.dto.token.TokenDto;
import com.authentication.auth.dto.users.JoinRequest;
import com.authentication.auth.dto.users.UserNameCheckRequestDto;
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
        
        return ResponseEntity.ok(ApiResponse.success("회원가입이 성공적으로 완료되었습니다."));
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
        
        return ResponseEntity.ok(ApiResponse.success("프로필 이미지가 성공적으로 업로드되었습니다.", uploadResults));
    }

    private boolean isValidExtension(String extension) {
        List<String> allowedExtensions = Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "tiff");
        return allowedExtensions.contains(extension);
    }

    private boolean isValidFileContent(File file, String extension) {
        try {
            String mimeType = Files.probeContentType(file.toPath());
            List<String> allowedMimeTypes = Arrays.asList(
                    "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp", "image/svg+xml", "image/tiff");
            return allowedMimeTypes.contains(mimeType);
        } catch (IOException e) {
            log.error("파일 내용 검증 중 오류 발생", e);
            return false;
        }
    }

    @Override
    @PostMapping("/public/check/nickname/IsDuplicate")
    public boolean checkUserNameIsDuplicate(@RequestBody UserNameCheckRequestDto requestDto) {
        log.info("/check/userName/IsDuplicate : {}", requestDto.userName());
        return userService.checkUserNameIsDuplicate(requestDto.userName());
    }

    @Override
    @PostMapping("/public/check/userId/IsDuplicate")
    public ResponseEntity<Boolean> checkUserIdIsDuplicate(@RequestBody UserNameCheckRequestDto requestDto) {
        log.info("/check/userId/IsDuplicate : {}", requestDto.userName());
        // Assuming userId and userName are effectively the same for duplication checks
        return ResponseEntity.ok(userService.checkUserNameIsDuplicate(requestDto.userName()));
    }

    @Override
    @PostMapping("/public/clean/userTokenCookie")
    public ResponseEntity<?> cleanUserTokenCookie(HttpServletRequest request, HttpServletResponse response) {
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
        return ResponseEntity.ok().body("refreshToken deleted");
    }

    private void sendFrontNewCookie(HttpServletResponse response, int status, TokenDto tokendto) {
        response.setStatus(status);
        response.addHeader(SecurityConstants.TOKEN_HEADER.getValue(), SecurityConstants.TOKEN_PREFIX.getValue() + tokendto.accessToken());
        Cookie refreshTokenCookie = new Cookie("refreshToken", tokendto.refreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setDomain(domain);
        refreshTokenCookie.setPath("/");
        response.addCookie(refreshTokenCookie);
    }

    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        // cookie 배열 가지고 오기
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

    private boolean RedisMatchRToken(String userId, String RToken) {
        return redisService.findRToken(userId, "server", RToken);
    }
}