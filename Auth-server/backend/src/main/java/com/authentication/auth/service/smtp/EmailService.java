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
import com.authentication.auth.repository.UserRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
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
        String rand = randomNum();
        String from_email = sender_email;
        String to_email = email;
        String title = "회원 가입 인증 이메일 입니다.";
        String content;

        try {
            org.springframework.core.io.Resource resource = resourceLoader.getResource("classpath:templates/email/email_join_welcome.html");
            try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                content = FileCopyUtils.copyToString(reader);
            }
            content = content.replace("{{verificationCode}}", rand);
        } catch (IOException e) {
            log.error("Failed to load email template for join welcome", e);
            content = "이메일 인증 코드: " + rand; // Fallback content
        }

        mailSend(from_email, to_email, title, content);
        return rand;
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
            log.error("Failed to load email template for temporary password", e);
            content = "임시 비밀번호: " + rand; // Fallback content
        }

        mailSend(from_email, to_email, title, content);
        return rand;
    }




    public void mailSend(String from_email, String to_email, String title, String content) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "utf-8");
            helper.setFrom(from_email);
            helper.setTo(to_email);
            helper.setSubject(title);
            helper.setText(content, true);
            mailSender.send(message);

        } catch(MessagingException e){
            log.error("messagingException", e);
        }
    }

    @Transactional
    public void sendCustomEmail(CustomEmailRequest request) {
        try {
            String from_email = sender_email;
            String to_email = request.email();
            String title = request.title();
            String content = request.content();
            mailSend(from_email, to_email, title, content);
        } catch (Exception e) {
            log.error("message send Exception : ", e);
        }
    }

    public boolean checkIsExistEmail(String userEmail){
        return userRepository.existsByEmail(userEmail);
    }

}
