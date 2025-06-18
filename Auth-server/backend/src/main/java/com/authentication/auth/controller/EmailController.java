package com.authentication.auth.controller;

import com.authentication.auth.api.docs.EmailApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<EmailSendResponse> sendEmailAuthCode(@RequestBody @Valid SmtpEmailRequest request) {
        try {
            if (emailService.checkIsExistEmail(request.email())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new EmailSendResponse("user with this email already exist"));
            }
            String code = emailService.joinEmail(request.email());
            this.redisService.saveEmailCode(request.email(), code);
            return ResponseEntity.status(HttpStatus.OK).body(new EmailSendResponse("A temporary code has been sent to your email"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new EmailSendResponse("email sent failed"));
        }
    }

    @Override
    @PostMapping("/private/customEmailSend")
    public ResponseEntity<EmailSendResponse> sendCustomEmail(@RequestBody @Valid CustomEmailRequest request) {
        try {
            if (request.email() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new EmailSendResponse("email is blank"));
            }
            if (request.content() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new EmailSendResponse("content is blank"));
            }
            emailService.sendCustomEmail(request);
            return ResponseEntity.ok(new EmailSendResponse("custom email send success"));
        } catch (Exception e) {
            log.error("custom email send error : ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new EmailSendResponse("custom email send error"));
        }
    }

    @PostMapping("/public/emailCheck")
    public ResponseEntity<EmailCheckResponse> emailCheck(@RequestBody @Valid EmailCheckDto checkdto) {
        boolean isValid = redisService.checkEmailCode(checkdto.email(), checkdto.code());
        if (isValid) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(new EmailCheckResponse("email code is valid"));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new EmailCheckResponse("email code is invalid"));
        }
    }

    @Override
    @PostMapping("/protected/sendEmailPassword")
    public ResponseEntity<EmailSendResponse> sendTemporaryPasswordToAuthenticatedUser(@AuthenticationPrincipal PrincipalDetails principalDetails) {
        try {
            String userId = principalDetails.getUserId();
            String email = userService.getEmailByUserId(userId);
            String temporalPassword = emailService.sendTemporalPassword(email);
            userService.UpdateUserPassword(userId, temporalPassword);
            return ResponseEntity.status(HttpStatus.OK).body(new EmailSendResponse("A temporary password has been sent to your email"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new EmailSendResponse("error occurred while sending an temporary password"));
        }
    }

    @Override
    @PostMapping("/public/findPassWithEmail")
    public ResponseEntity<EmailSendResponse> findPassWithEmail(@RequestBody @Valid EmailFindByIdRequest emailFindById) {
        try {
            String email = userService.getEmailByUserId(emailFindById.userId());
            String temporalPassword = emailService.sendTemporalPassword(email);
            userService.UpdateUserPassword(emailFindById.userId(), temporalPassword);
            return ResponseEntity.status(HttpStatus.OK).body(new EmailSendResponse("A temporary password has been sent to your email"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new EmailSendResponse("Failed to send temporary password"));
        }
    }
}
