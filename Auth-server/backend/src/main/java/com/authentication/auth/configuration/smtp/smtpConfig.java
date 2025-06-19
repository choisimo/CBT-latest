package com.authentication.auth.configuration.smtp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class smtpConfig {

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private int port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Value("${spring.mail.properties.mail.smtp.auth}")
    private String auth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable}")
    private String starttlsEnable;

    @Value("${spring.mail.properties.mail.smtp.socketFactory.class}")
    private String socketFactoryClass;

    @Value("${spring.mail.properties.mail.debug}")
    private String debug;

    @Value("${spring.mail.properties.mail.smtp.ssl.trust}")
    private String sslTrust;

    @Value("${spring.mail.properties.mail.smtp.ssl.protocols}")
    private String sslProtocols;

    @Bean
    public JavaMailSender mailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties javaMailProperties = new Properties();
        javaMailProperties.put("mail.transport.protocol", "smtp");
        javaMailProperties.put("mail.smtp.auth", auth);
        javaMailProperties.put("mail.smtp.socketFactory.class", socketFactoryClass);
        javaMailProperties.put("mail.smtp.starttls.enable", starttlsEnable);
        javaMailProperties.put("mail.debug", debug); //디버깅 정보 출력
        javaMailProperties.put("mail.smtp.ssl.trust", sslTrust); //smtp 서버의 ssl 인증서를 신뢰
        javaMailProperties.put("mail.smtp.ssl.protocols", sslProtocols); //사용할 ssl 프로토콜 버젼
        mailSender.setJavaMailProperties(javaMailProperties);
        return mailSender;
    }
}
