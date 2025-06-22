package com.authentication.auth.service.smtp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;

import com.authentication.auth.dto.email.CustomEmailRequest;
import com.authentication.auth.exception.CustomException;
import com.authentication.auth.exception.ErrorType;
import com.authentication.auth.repository.UserRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.web.util.UriComponentsBuilder; // Added for deep link construction
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${server.email.sender}")
    private String sender_email;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final ResourceLoader resourceLoader;

    private String randomNum() {
        String rand = UUID.randomUUID().toString().replace("-", "");
        String result = "";
        for (int i = 0; i < 8; i++){
            result += rand.charAt(i);
        }
        return result;
    }

    public String joinEmail(String email){
        String verificationToken = randomNum(); // This is the token for the link
        String from_email = sender_email;
        String to_email = email;
        String title = "회원 가입 인증 이메일 입니다.";
        String content;

        // Construct the deep link for email verification
        String verificationLink = UriComponentsBuilder.fromUriString("mycbtapp://verify-email")
                .queryParam("token", verificationToken)
                .build().toUriString();

        log.info("Generated email verification link: {}", verificationLink);

        try {
            org.springframework.core.io.Resource resource = resourceLoader.getResource("classpath:templates/email/email_join_welcome.html");
            try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                content = FileCopyUtils.copyToString(reader);
            }
            // 템플릿에 서비스명 및 인증 코드/링크 삽입
            content = content.replace("{{serviceName}}", "CBT-Diary");
            content = content.replace("{{verificationCode}}", verificationToken);
            content = content.replace("{{verificationLink}}", verificationLink);
        } catch (IOException e) {
            log.error("Failed to load email template for join welcome: {}", e.getMessage());
            throw new CustomException(ErrorType.EMAIL_TEMPLATE_LOAD_FAILURE, "회원가입 이메일 템플릿 로드에 실패했습니다.");
        }

        mailSend(from_email, to_email, title, content); // mailSend 내부에서 예외 처리
        return verificationToken; // Return the token, which the client might use to confirm/resend etc.
    }

    public String sendTemporalPassword(String email){
        String rand = randomNum();
        String from_email = sender_email;
        String to_email = email;
        String title = "비밀번호 변경 이메일입니다.";
        String content;

        try {
            org.springframework.core.io.Resource resource = resourceLoader.getResource("classpath:templates/email/email_temporal_password.html");
            try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                content = FileCopyUtils.copyToString(reader);
            }
            content = content.replace("{{temporaryPassword}}", rand);
        } catch (IOException e) {
            log.error("Failed to load email template for temporary password: {}", e.getMessage());
            // 템플릿 로드 실패 시에도 CustomException 발생
            throw new CustomException(ErrorType.EMAIL_TEMPLATE_LOAD_FAILURE, "임시 비밀번호 이메일 템플릿 로드에 실패했습니다.");
        }

        mailSend(from_email, to_email, title, content); // mailSend 내부에서 예외 처리
        return rand;
    }

    public void mailSend(String from_email, String to_email, String title, String content) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "utf-8");
            helper.setFrom(from_email);
            helper.setTo(to_email);
            helper.setSubject(title);
            helper.setText(content, true); // true indicates HTML content
            mailSender.send(message);
            log.info("Email sent successfully to {}", to_email);
        } catch(MessagingException e){
            log.error("Failed to send email to {}: {}", to_email, e.getMessage());
            throw new CustomException(ErrorType.EMAIL_SEND_FAILURE, "이메일 발송 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Transactional // 이 메서드가 DB 작업을 직접 수행하지 않으므로 @Transactional이 필수적인지 검토 필요.
                  // 여기서는 일관성을 위해 유지하거나, 제거를 고려할 수 있음.
    public void sendCustomEmail(CustomEmailRequest request) {
        // CustomEmailRequest DTO에 @NotBlank 등의 validation annotation을 사용하는 것이 좋음.
        // Service layer에서 null/blank 체크를 최소화.
        if (request.email() == null || request.email().isBlank()) {
            throw new CustomException(ErrorType.INVALID_REQUEST_PARAMETER, "수신자 이메일 주소는 비어있을 수 없습니다.");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new CustomException(ErrorType.INVALID_REQUEST_PARAMETER, "이메일 제목은 비어있을 수 없습니다.");
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new CustomException(ErrorType.INVALID_REQUEST_PARAMETER, "이메일 내용은 비어있을 수 없습니다.");
        }

        String from_email = sender_email;
        String to_email = request.email();
        String title = request.title();
        String content = request.content(); // HTML 여부에 따라 setText(content, true) 필요할 수 있음

        // mailSend 내부에서 예외를 throw하므로, 여기서 별도 try-catch로 로그만 남길 필요 없음.
        // 만약 특정 로깅이나 추가 처리가 필요하다면 try-catch 사용.
        mailSend(from_email, to_email, title, content);
        log.info("Custom email sent to {}", to_email);
    }


    public boolean checkIsExistEmail(String userEmail){
        if (userEmail == null || userEmail.isBlank()) {
            // 또는 ErrorType.INVALID_REQUEST_PARAMETER 등을 사용하여 CustomException throw
            throw new CustomException(ErrorType.INVALID_REQUEST_PARAMETER, "이메일 주소가 비어있습니다.");
        }
        return userRepository.existsByEmail(userEmail);
    }
}
