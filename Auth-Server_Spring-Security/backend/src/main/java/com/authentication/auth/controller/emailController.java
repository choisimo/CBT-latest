package com.career_block.auth.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.authentication.auth.DTO.smtp.customEmailRequest;
import com.authentication.auth.DTO.smtp.emailCheckDto;
import com.authentication.auth.DTO.smtp.emailFindById;
import com.authentication.auth.DTO.smtp.emailRequest;
import com.authentication.auth.DTO.token.principalDetails;
import com.authentication.auth.service.redis.redisService;
import com.authentication.auth.service.smtp.emailService;
import com.authentication.auth.service.users.userService;

@Slf4j
@Tag(name="email 관련", description = "email")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class emailController {

    private final emailService service;
    private final userService  userService;
    private final redisService  redisService;

    @Operation(summary="이메일 보내기", description = "이메일 코드 전송 관련")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "이메일 전송 성공", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "이메일 전송 실패", content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/public/emailSend")
    public ResponseEntity<?> emailSend(@RequestBody @Valid emailRequest request) {
        try {
            if(service.checkIsExistEmail(request.getEmail()))
            {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("user with this email already exist");
            }
            String code = service.joinEmail(request.getEmail());
            this.redisService.saveEmailCode(request.getEmail(), code);
            return ResponseEntity.status(HttpStatus.OK).body("A temporary code has been sent to your email");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("email sent failed");
        }
    }


    @Operation(summary="커스텀 이메일 보내기", description = "커스텀 이메일 전송 관련")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "이메일 전송 성공", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "이메일 전송 실패", content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/api/private/customEmailSend")
    public ResponseEntity<?> customEmailSend(@RequestBody @Valid customEmailRequest request) {
        try {
            if (request.getEmail() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("email is blank");
            }
            if (request.getContent() == null){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("there's no content in the request");
            }
            service.sendCustomEmail(request);
            return ResponseEntity.status(HttpStatus.OK).body("custom email send success");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("email sent failed");
        }
    }


    @Operation(summary = "이메일 코드 확인", description = "이메일로 전송된 코드를 확인합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "이메일 코드 유효", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "이메일 코드 무효", content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/public/emailCheck")
    public ResponseEntity<?> emailCodeCheck(@RequestBody @Valid emailCheckDto checkdto) {
        boolean isValid = redisService.checkEmailCode(checkdto.getEmail(), checkdto.getCode());
        if (isValid) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body("email code is valid");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("email code is invalid");
        }
    }

    @Operation(summary = "임시 비밀번호 이메일 전송", description = "인증된 사용자에게 임시 비밀번호를 이메일로 전송합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "임시 비밀번호 전송 성공", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "임시 비밀번호 전송 실패", content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/protected/sendEmailPassword")
    public ResponseEntity<?> emailSendPassword(@AuthenticationPrincipal principalDetails principalDetails) {
        try {
            String userId = principalDetails.getUserId();
            String email = userService.getEmailByUserId(userId);
            String temporalPassword = service.changePwEmail(email);
            userService.UpdateUserPassword(userId, temporalPassword);
            return ResponseEntity.status(HttpStatus.OK).body("A temporary password has been sent to your email");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error occurred while sending an temporary password");
        }
    }

    @Operation(summary = "이메일로 비밀번호 찾기", description = "사용자 ID를 통해 이메일로 임시 비밀번호를 전송합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "임시 비밀번호 전송 성공", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "임시 비밀번호 전송 실패", content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/public/findPassWithEmail")
    public ResponseEntity<?> findPassWithEmail(@RequestBody emailFindById emailFindById) {
        try {
            String email = userService.getEmailByUserId(emailFindById.getUserId());
            String temporalPassword = service.changePwEmail(email);
            userService.UpdateUserPassword(emailFindById.getUserId(), temporalPassword);
            return ResponseEntity.status(HttpStatus.OK).body("A temporary password has been sent to your email");
        } catch (Exception e) {
            log.error("Error find Password with Email sent", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error occurred while sending an temporary password");
        }
    }


}
