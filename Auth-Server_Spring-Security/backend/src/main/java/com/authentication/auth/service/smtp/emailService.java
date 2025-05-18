package com.career_block.auth.service.smtp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.authentication.auth.DTO.smtp.customEmailRequest;
import com.authentication.auth.DTO.smtp.customEmailToAllRequest;
import com.authentication.auth.repository.usersRepository;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class emailService {

    @Value("${server.email.sender}")
    private String sender_email;
    private final usersRepository usersRepository;
    private final JavaMailSender mailSender;

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

        // HTML 이메일 컨텐츠
        String content =
                "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 30px; border: 1px solid #e0e0e0; border-radius: 8px; background-color: #ffffff; box-shadow: 0 4px 8px rgba(0, 0, 0, 0.05);\">\n" +
                        "    <h2 style=\"color: #4c5baf; text-align: center; margin-bottom: 20px;\">\uD83C\uDF89 Career Block 에 오신 것을 환영합니다!</h2>\n" +
                        "\n" +
                        "    <p style=\"font-size: 16px; color: #333333; line-height: 1.6; margin-bottom: 20px;\">\n" +
                        "        가입해주셔서 진심으로 감사합니다! 아래의 이메일 인증 코드를 사용하여 가입을 완료해주세요.\n" +
                        "    </p>\n" +
                        "\n" +
                        "    <div style=\"text-align: center; margin: 30px 0;\">\n" +
                        "        <span style=\"display: inline-block; font-size: 28px; font-weight: bold; color: #4c5baf; padding: 15px 30px; border: 2px dashed #4c5baf; border-radius: 5px; background-color: #f0f4ff;\">\n" +
                        "            " + rand + "\n" +
                        "        </span>\n" +
                        "    </div>\n" +
                        "\n" +
                        "    <p style=\"font-size: 16px; color: #333333; line-height: 1.6;\">\n" +
                        "        위 코드를 인증 페이지에 입력하여 가입을 완료해주세요. 이 코드의 유효 시간은 30분 입니다.\n" +
                        "    </p>\n" +
                        "\n" +
                        "    <p style=\"font-size: 14px; color: #666666; line-height: 1.6; margin-top: 30px;\">\n" +
                        "        만약 Career Block 가입을 시도하지 않았다면, 이 메시지를 무시해 주세요.\n" +
                        "    </p>\n" +
                        "\n" +
                        "    <hr style=\"border-top: 1px solid #e0e0e0; margin-top: 40px; margin-bottom: 20px;\">\n" +
                        "\n" +
                        "    <p style=\"text-align: center; font-size: 12px; color: #999999;\">\n" +
                        "        © 2024 Career Block. All rights reserved.\n" +
                        "    </p>\n" +
                        "</div>\n";

        mailSend(from_email, to_email, title, content);
        return rand;
    }


    public String changePwEmail(String email){
        String rand = randomNum();
        String from_email = sender_email;
        String to_email = email;
        String title = "비밀번호 변경 이메일입니다.";
        String content =
                "<!DOCTYPE html>" +
                        "<html>" +
                        "    <head>" +
                        "        <meta charset='UTF-8'>" +
                        "        <title>비밀번호 변경 안내</title>" +
                        "        <style>" +
                        "            body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f6f6f6; }" +
                        "            .container { background-color: #ffffff; padding: 40px; border-radius: 10px; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); }" +
                        "            h1 { color: #333333; }" +
                        "            p { color: #666666; }" +
                        "            .button { display: inline-block; padding: 10px 20px; color: #ffffff; background-color: #007bff; text-decoration: none; border-radius: 5px; }" +
                        "        </style>" +
                        "    </head>" +
                        "    <body>" +
                        "        <div class='container'>" +
                        "            <h1>비밀번호 변경 안내</h1>" +
                        "            <p>비밀번호를 잊어버리셨군요!</p>" +
                        "            <p>아래의 임시 비밀번호를 사용하여 비밀번호를 변경하세요:</p>" +
                        "            <h2>" + rand + "</h2>" +
                        "            <p><a href='gcp.nodove.com' class='button'>비밀번호 변경하기</a></p>" +
                        "        </div>" +
                        "    </body>" +
                        "</html>";
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
    public void sendCustomEmail(customEmailRequest request) {
        try {
            String from_email = sender_email;
            String to_email = request.getEmail();
            String title = request.getTitle();
            String content = request.getContent();
            mailSend(from_email, to_email, title, content);
        } catch (Exception e) {
            log.error("message send Exception : ", e);
        }
    }

    @Transactional
    public void sendCustomEmailToAll(customEmailToAllRequest request) {

    }

    public boolean checkIsExistEmail(String userEmail){
        return usersRepository.existsByEmail(userEmail);
    }

}
