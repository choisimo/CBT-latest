package com.authentication.auth.controller;

import com.authentication.auth.api.docs.EmailApi;
import com.authentication.auth.dto.response.ApiResponse;
import com.authentication.auth.exception.CustomException;
import com.authentication.auth.exception.ErrorType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.authentication.auth.dto.email.CustomEmailRequest;
import com.authentication.auth.dto.email.EmailSendResponse;
import com.authentication.auth.dto.email.EmailCheckResponse;
import com.authentication.auth.dto.email.EmailCheckDto;
import com.authentication.auth.dto.smtp.EmailFindByIdRequest;
import com.authentication.auth.dto.smtp.SmtpEmailRequest;
import com.authentication.auth.dto.token.PrincipalDetails;
import com.authentication.auth.service.redis.RedisService;
import com.authentication.auth.service.smtp.EmailService;
import com.authentication.auth.service.users.UserService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class EmailController implements EmailApi {

    private final EmailService emailService;
    private final UserService userService;
    private final RedisService redisService;

    @Override
    @PostMapping("/public/emailSend")
    public ResponseEntity<ApiResponse<EmailSendResponse>> sendEmailAuthCode(@RequestBody @Valid SmtpEmailRequest request) {
        // Assume emailService.checkIsExistEmail and emailService.joinEmail throw CustomException on failure
        if (emailService.checkIsExistEmail(request.email())) {
            // This logic should ideally be in the service layer, throwing a specific CustomException
            throw new CustomException(ErrorType.EMAIL_ALREADY_EXISTS, "해당 이메일은 이미 사용 중입니다.");
        }
        String code = emailService.joinEmail(request.email()); // Can throw CustomException for send failure
        this.redisService.saveEmailCode(request.email(), code);
        EmailSendResponse response = new EmailSendResponse("인증 코드가 이메일로 발송되었습니다.");
        return ResponseEntity.ok(ApiResponse.success(response, "이메일 인증 코드 발송 성공"));
    }

    /**
     * 모바일 회원가입용 이메일 인증 코드 전송 엔드포인트 (기존 /public/emailSend 와 동일 로직)
     */
    @PostMapping("/public/emailCode")
    public ResponseEntity<ApiResponse<EmailSendResponse>> sendEmailCode(@RequestBody @Valid SmtpEmailRequest request) {
        if (emailService.checkIsExistEmail(request.email())) {
            throw new CustomException(ErrorType.EMAIL_ALREADY_EXISTS, "해당 이메일은 이미 사용 중입니다.");
        }
        String code = emailService.joinEmail(request.email());
        this.redisService.saveEmailCode(request.email(), code);
        EmailSendResponse response = new EmailSendResponse("인증 코드가 이메일로 발송되었습니다.");
        return ResponseEntity.ok(ApiResponse.success(response, "이메일 인증 코드 발송 성공"));
    }

    @Override
    @PostMapping("/private/customEmailSend")
    public ResponseEntity<ApiResponse<EmailSendResponse>> sendCustomEmail(@RequestBody @Valid CustomEmailRequest request) {
        // Basic validation for blank fields, can be enhanced with @NotBlank in DTO
        if (request.email() == null || request.email().isBlank()) {
            throw new CustomException(ErrorType.INVALID_REQUEST_BODY, "이메일 주소는 비어있을 수 없습니다.");
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new CustomException(ErrorType.INVALID_REQUEST_BODY, "이메일 내용은 비어있을 수 없습니다.");
        }
        emailService.sendCustomEmail(request); // Assume this throws CustomException on failure
        EmailSendResponse response = new EmailSendResponse("커스텀 이메일이 성공적으로 발송되었습니다.");
        return ResponseEntity.ok(ApiResponse.success(response, "커스텀 이메일 발송 성공"));
    }

    @PostMapping("/public/emailCheck")
    public ResponseEntity<ApiResponse<EmailCheckResponse>> emailCheck(@RequestBody @Valid EmailCheckDto checkdto) {
        boolean isValid = redisService.checkEmailCode(checkdto.email(), checkdto.code());
        if (isValid) {
            EmailCheckResponse response = new EmailCheckResponse("이메일 인증 코드가 유효합니다.");
            return ResponseEntity.ok(ApiResponse.success(response, "이메일 코드 확인 성공"));
        } else {
            // No need to return a body here, GlobalExceptionHandler will handle it.
            throw new CustomException(ErrorType.INVALID_EMAIL_CODE, "이메일 인증 코드가 유효하지 않습니다.");
        }
    }

    @PostMapping("/public/check/email/IsDuplicate")
    public ResponseEntity<ApiResponse<Boolean>> checkEmailIsDuplicate(@RequestBody String email) {
        boolean isDuplicate = emailService.checkIsExistEmail(email);
        return ResponseEntity.ok(ApiResponse.success(isDuplicate, "이메일 중복 확인이 완료되었습니다."));
    }

    @Override
    @PostMapping("/protected/sendEmailPassword")
    public ResponseEntity<ApiResponse<EmailSendResponse>> sendTemporaryPasswordToAuthenticatedUser(@AuthenticationPrincipal PrincipalDetails principalDetails) {
        String email = principalDetails.getUser().getEmail();
        // emailService.sendTemporalPassword might throw EmailSendException (CustomException)
        String temporalPassword = emailService.sendTemporalPassword(email);
        // userService.UpdateUserPassword might throw UserUpdateFailedException (CustomException)
        userService.UpdateUserPassword(email, temporalPassword);
        EmailSendResponse response = new EmailSendResponse("임시 비밀번호가 이메일로 발송되었습니다.");
        return ResponseEntity.ok(ApiResponse.success(response, "임시 비밀번호 발송 성공"));
    }

    @Override
    @PostMapping("/public/findPassWithEmail")
    public ResponseEntity<ApiResponse<EmailSendResponse>> findPassWithEmail(@RequestBody @Valid EmailFindByIdRequest emailFindById) {
        // Similar to sendTemporaryPasswordToAuthenticatedUser, service methods should throw CustomExceptions
        String email = emailFindById.email();
        String temporalPassword = emailService.sendTemporalPassword(email);
        userService.UpdateUserPassword(email, temporalPassword);
        EmailSendResponse response = new EmailSendResponse("임시 비밀번호가 이메일로 발송되었습니다.");
        return ResponseEntity.ok(ApiResponse.success(response, "아이디로 비밀번호 찾기 - 임시 비밀번호 발송 성공"));
    }
}
