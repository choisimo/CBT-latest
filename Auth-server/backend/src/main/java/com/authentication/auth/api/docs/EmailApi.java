package com.authentication.auth.api.docs;

import com.authentication.auth.dto.email.CustomEmailRequest;
import com.authentication.auth.dto.email.EmailSendResponse;
import com.authentication.auth.dto.email.EmailCheckResponse;
import com.authentication.auth.dto.smtp.SmtpEmailRequest;
import com.authentication.auth.dto.smtp.EmailCheckDto;
import com.authentication.auth.dto.response.ErrorResponse;
import com.authentication.auth.dto.smtp.EmailFindByIdRequest;
import com.authentication.auth.dto.token.PrincipalDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.ExampleObject;

@Tag(name = "Email API", description = "Email-related functionalities including verification and password management.")
@RequestMapping("/api")
public interface EmailApi {

    @Operation(summary = "이메일 인증 코드 발송", description = "회원가입 등을 위한 이메일 인증 코드를 발송합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "이메일 전송 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EmailSendResponse.class),
                            examples = @ExampleObject(name = "이메일 성공 응답", value = "{\"message\": \"A temporary code has been sent to your email\"}"))),
            @ApiResponse(responseCode = "400", description = "이미 가입된 이메일 또는 잘못된 요청",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                @ExampleObject(name = "이미 가입된 이메일", summary = "400 Already Registered", value = "{\"timestamp\": \"2023-10-27T10:10:00Z\", \"status\": 400, \"error\": \"Bad Request\", \"message\": \"Email 'user@example.com' is already registered.\", \"path\": \"/api/public/emailSend\"}"),
                                @ExampleObject(name = "잘못된 이메일 형식", summary = "400 Invalid Format", value = "{\"timestamp\": \"2023-10-27T10:11:00Z\", \"status\": 400, \"error\": \"Bad Request\", \"message\": \"Invalid email format provided.\", \"path\": \"/api/public/emailSend\"}")
                            })),
            @ApiResponse(responseCode = "500", description = "이메일 전송 실패 (서버 오류)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "서버 오류 응답", value = "{\"timestamp\": \"2023-10-27T10:12:00Z\", \"status\": 500, \"error\": \"Internal Server Error\", \"message\": \"Failed to send verification email due to a server issue.\", \"path\": \"/api/public/emailSend\"}")))
    })
    @PostMapping("/public/emailSend")
    ResponseEntity<EmailSendResponse> sendEmailAuthCode(
            @RequestBody(
                description = "Email address to send verification code to.",
                required = true,
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SmtpEmailRequest.class),
                    examples = {
                        @ExampleObject(
                            name = "정상 이메일 발송 요청",
                            summary = "유효한 이메일 주소로 인증 코드를 요청합니다.",
                            value = "{\"email\": \"newuser@example.com\"}"
                        ),
                        @ExampleObject(
                            name = "잘못된 형식의 이메일 주소",
                            summary = "이메일 형식이 올바르지 않은 경우입니다. (400 Bad Request 예상)",
                            value = "{\"email\": \"invalid-email\"}"
                        ),
                        @ExampleObject(
                            name = "이미 가입된 이메일 주소",
                            summary = "이미 시스템에 등록된 이메일 주소로 인증 코드를 요청하는 경우입니다. (400 Bad Request 예상)",
                            value = "{\"email\": \"registereduser@example.com\"}"
                        )
                    }
                )
            ) SmtpEmailRequest emailRequest);

    @Operation(summary = "커스텀 이메일 발송 (관리자/내부용)", description = "지정된 수신자에게 커스텀 제목과 내용으로 이메일을 발송합니다.",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "이메일 전송 성공",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = EmailSendResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "이메일 전송 실패",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/private/customEmailSend")
    ResponseEntity<EmailSendResponse> sendCustomEmail(@RequestBody CustomEmailRequest customEmailRequest);

    @Operation(summary = "이메일 인증 코드 확인", description = "발송된 이메일 인증 코드의 유효성을 확인합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "이메일 코드 유효", 
                         content = @Content(mediaType = "application/json", 
                                            schema = @Schema(implementation = EmailCheckResponse.class),
                                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(name = "코드 유효 응답", value = "{\"message\": \"email code is valid\"}"))),
            @ApiResponse(responseCode = "401", description = "이메일 코드 무효 또는 만료", 
                         content = @Content(mediaType = "application/json", 
                                            schema = @Schema(implementation = ErrorResponse.class),
                                            examples = {
                                                @io.swagger.v3.oas.annotations.media.ExampleObject(name = "잘못된 코드", summary = "401 Invalid Code", value = "{\"timestamp\": \"2023-10-27T10:15:00Z\", \"status\": 401, \"error\": \"Unauthorized\", \"message\": \"Email verification code 'XXXXXX' is invalid or expired for email 'user@example.com'.\", \"path\": \"/api/public/emailCheck\"}"),
                                                @io.swagger.v3.oas.annotations.media.ExampleObject(name = "코드 없음 또는 만료됨", summary = "401 No Code/Expired", value = "{\"timestamp\": \"2023-10-27T10:16:00Z\", \"status\": 401, \"error\": \"Unauthorized\", \"message\": \"No verification code found for email 'nonexistent@example.com' or code has expired.\", \"path\": \"/api/public/emailCheck\"}")
                                            }))
    })
    @PostMapping("/public/emailCheck")
    ResponseEntity<EmailCheckResponse> emailCheck(
            @RequestBody(
                description = "Email address and verification code to check.",
                required = true,
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = EmailCheckDto.class),
                    examples = {
                        @ExampleObject(
                            name = "유효한 인증 코드 확인",
                            summary = "올바른 이메일과 인증 코드를 제출한 경우입니다. (202 Accepted 예상)",
                            value = "{\"email\": \"newuser@example.com\", \"code\": \"A1B2C3\"}"
                        ),
                        @ExampleObject(
                            name = "잘못된 인증 코드",
                            summary = "이메일은 올바르지만 인증 코드가 틀린 경우입니다. (401 Unauthorized 예상)",
                            value = "{\"email\": \"newuser@example.com\", \"code\": \"WRONGCODE\"}"
                        ),
                        @ExampleObject(
                            name = "존재하지 않는 이메일의 코드 확인",
                            summary = "인증 코드가 발송된 적 없는 이메일로 코드 확인을 시도하는 경우입니다. (401 Unauthorized 예상)",
                            value = "{\"email\": \"unknown@example.com\", \"code\": \"ANYCODE\"}"
                        )
                    }
                )
            ) EmailCheckDto emailCheckDto);

    @Operation(summary = "임시 비밀번호 이메일 전송 (인증된 사용자)", description = "현재 로그인된 사용자의 이메일로 임시 비밀번호를 발송합니다.",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "임시 비밀번호 전송 성공",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = EmailSendResponse.class))),
            @ApiResponse(responseCode = "500", description = "임시 비밀번호 전송 실패",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/protected/sendEmailPassword")
    ResponseEntity<EmailSendResponse> sendTemporaryPasswordToAuthenticatedUser(@AuthenticationPrincipal PrincipalDetails principalDetails);

    @Operation(summary = "아이디로 이메일 찾아 임시 비밀번호 전송", description = "사용자 ID를 기반으로 등록된 이메일을 찾아 임시 비밀번호를 발송합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "임시 비밀번호 전송 성공",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = EmailSendResponse.class))),
            @ApiResponse(responseCode = "500", description = "임시 비밀번호 전송 실패 또는 사용자를 찾을 수 없음",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/public/findPassWithEmail")
    ResponseEntity<EmailSendResponse> findPassWithEmail(@RequestBody EmailFindByIdRequest userIdRequest);
}
