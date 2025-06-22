package com.authentication.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.authentication.auth.repository")
public class MongoConfig {
    // AbstractMongoClientConfiguration 상속 제거
    // Spring Boot의 자동 설정이 application.properties와 환경 변수를 사용하도록 함
} 