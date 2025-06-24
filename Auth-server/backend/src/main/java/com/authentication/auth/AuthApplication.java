package com.authentication.auth;

import com.authentication.auth.configuration.oauth2.OauthProperties;
import com.authentication.auth.configuration.token.JwtProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;


import java.util.TimeZone;

@EnableConfigurationProperties({JwtProperties.class, OauthProperties.class})
@EnableAsync
@EnableJpaAuditing
@SpringBootApplication
public class AuthApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(AuthApplication.class);
		application.setWebApplicationType(WebApplicationType.SERVLET);
		application.run(args);
	}

	@PostConstruct
	void set_time_zone() {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
	}
}
