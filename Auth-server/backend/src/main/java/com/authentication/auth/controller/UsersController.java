package com.authentication.auth.controller;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.authentication.auth.configuration.token.JwtUtility;
import com.authentication.auth.dto.response.ApiResponse;
import com.authentication.auth.dto.token.TokenDto;
import com.authentication.auth.dto.users.JoinRequest;
import com.authentication.auth.dto.users.UserNameCheckRequestDto;
import com.authentication.auth.dto.users.LoginRequest;
import com.authentication.auth.dto.users.LoginResponse;
import com.authentication.auth.dto.response.ApiResponse;
import com.authentication.auth.exception.CustomException;
import com.authentication.auth.exception.ErrorType;
import com.authentication.auth.others.constants.SecurityConstants;
import com.authentication.auth.service.file.FileService;
import com.authentication.auth.service.redis.RedisService;
import com.authentication.auth.service.smtp.EmailService;
import com.authentication.auth.service.users.UserService;
import com.authentication.auth.dto.EmailRequestDto;
import com.authentication.auth.dto.EmailVerificationRequestDto;

import java.util.*;
import jakarta.validation.Valid;

/**
 * 사용자 관리 컨트롤러
 * 회원가입, 프로필 업로드, 중복 체크 등을 처리합니다.
 */

@RestController
@RequestMapping("/api")
@Slf4j
public class UsersController {

    private final UserService userService;
    private final EmailService emailService;
    private final RedisService redisService;
    private final JwtUtility jwtUtility;
    private final FileService fileService;

    public UsersController(UserService userService, EmailService emailService, RedisService redisService, JwtUtility jwtUtility, FileService fileService) {
        this.userService = userService;
        this.emailService = emailService;
        this.redisService = redisService;
        this.jwtUtility = jwtUtility;
        this.fileService = fileService;
    }



    @PostMapping("/public/join")
    public ResponseEntity<ApiResponse<String>> join(@Valid @RequestBody JoinRequest request) throws Exception {
        // // 이메일 인증 코드 확인
        // if (!redisService.checkEmailCode(request.email(), request.emailAuthCode())) {
        //     throw new CustomException(ErrorType.INVALID_EMAIL_CODE, "이메일 인증 코드가 유효하지 않거나 만료되었습니다.");
        // }
        
        // 회원가입 처리 (사용자는 "WAITING" 상태로 저장됨)
        com.authentication.auth.domain.User newUser = userService.join(request);
        
        // 사용자 활성화 및 인증 코드 삭제
        userService.activateUser(newUser.getEmail());
        
        return ResponseEntity.ok(ApiResponse.success(null, "회원가입 및 이메일 인증이 성공적으로 완료되었습니다."));
    }

    // Login is now handled by AuthenticationFilter at /api/login
    // Removed the duplicate login endpoint to avoid conflicts

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

    @PostMapping("/public/check/nickname/IsDuplicate")
    public ResponseEntity<ApiResponse<Boolean>> checkNicknameIsDuplicate(@RequestBody UserNameCheckRequestDto requestDto) {
        log.info("/check/nickname/IsDuplicate : {}", requestDto.nickname());
        boolean isDuplicate = userService.checkNicknameIsDuplicate(requestDto.nickname());
        String message = isDuplicate ? "이미 사용 중인 닉네임입니다." : "사용 가능한 닉네임입니다.";
        return ResponseEntity.ok(ApiResponse.success(isDuplicate, message));
    }

    @PostMapping("/public/check/userId/IsDuplicate")
    public ResponseEntity<ApiResponse<Boolean>> checkUserIdIsDuplicate(@RequestBody com.authentication.auth.dto.users.UserIdCheckRequestDto requestDto) {
        Integer userId = requestDto.userId();
        log.info("/check/userId/IsDuplicate : {}", userId);
        boolean isDuplicate = userService.checkUserIdIsDuplicate(userId);
        String message = isDuplicate ? "이미 사용 중인 사용자 ID입니다." : "사용 가능한 사용자 ID입니다.";
        return ResponseEntity.ok(ApiResponse.success(isDuplicate, message));
    }

    @PostMapping("/public/check/loginId/IsDuplicate")
    public ResponseEntity<ApiResponse<Boolean>> checkLoginIdIsDuplicate(@RequestBody com.authentication.auth.dto.users.LoginIdCheckRequestDto requestDto) {
        String loginId = requestDto.loginId();
        log.info("/check/loginId/IsDuplicate : {}", loginId);
        boolean isDuplicate = userService.checkLoginIdIsDuplicate(loginId);
        String message = isDuplicate ? "이미 사용 중인 로그인 ID입니다." : "사용 가능한 로그인 ID입니다.";
        return ResponseEntity.ok(ApiResponse.success(isDuplicate, message));
    }

    // 이메일 인증 코드 발송 API
    @PostMapping("/public/emailCode")
    public ResponseEntity<ApiResponse<Void>> sendEmailCode(@RequestBody EmailRequestDto requestDto) {
        // TODO: 이미 가입된 이메일인지 확인하는 로직 추가
        emailService.sendVerificationCode(requestDto.getEmail());
        return ResponseEntity.ok(ApiResponse.success(null, "인증 코드가 성공적으로 발송되었습니다."));
    }

    // 이메일 인증 코드 확인 API
    @PostMapping("/public/emailCheck")
    public ResponseEntity<ApiResponse<Boolean>> verifyEmailCode(@Valid @RequestBody EmailVerificationRequestDto requestDto) {
        log.info("/public/emailCheck - email: {}", requestDto.getEmail());
        
        boolean isVerified = emailService.verifyCode(requestDto.getEmail(), requestDto.getCode());
        String message = isVerified ? "이메일 인증이 완료되었습니다." : "인증 코드가 일치하지 않습니다.";
        
        return ResponseEntity.ok(ApiResponse.success(isVerified, message));
    }

    

    

    

    
}