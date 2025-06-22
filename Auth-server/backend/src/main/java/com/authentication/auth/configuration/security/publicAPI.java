package com.authentication.auth.configuration.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Slf4j
@Data
@Service
public class publicAPI {

    private final List<String> apiEndPoints = Arrays.asList(
            "/login", "/public", "/api/public", "/errorPage", "/notExist", "/unauthorized", "/swagger-ui.html", "/v2/api-docs", "/swagger-resources", "/webjars", "/swagger-resources/configuration/ui", "/swagger-resources/configuration/security"
            ,"/oauth2/authorization/google", "/oauth2/authorization/google", "/oauth2/authorization/facebook", "/oauth2/authorization/github", "/oauth2/authorization/linkedin", "/oauth2/authorization/instagram", "/oauth2/authorization/twitter", "/oauth2/authorization/yahoo", "/oauth2/authorization/spotify", "/oauth2/authorization/amazon", "/oauth2/authorization/microsoft", "/oauth2/authorization/okta", "/oauth2/authorization/slack"
            ,"/oauth2/callback/google", "/oauth2/callback/google", "/oauth2/callback/facebook", "/oauth2/callback/github", "/oauth2/callback/linkedin", "/oauth2/callback/instagram", "/oauth2/callback/twitter", "/oauth2/callback/yahoo", "/oauth2/callback/spotify", "/oauth2/callback/amazon", "/oauth2/callback/microsoft", "/oauth2/callback/okta", "/oauth2/callback/slack",
            "/user/login", "/user/register", "/user/verify", "/user/forgetPassword", "/user/resetPassword", "/user/verifyResetPassword", "/user/verifyEmail", "/user/resendVerificationEmail", "/api/diary", "/api/diary/analyze", "/api/diary/responses", "/api/diary/health"
    );

    public boolean checkRequestAPI(HttpServletRequest request){

    String requestURI = request.getRequestURI();
     /** 
      * @TODO 실제 운영 환경에서는 제거할 것, 성능 최적화, 보안, 로깅 정책 등 고려 필요함
      *  @TODO-detail : log.info()는 성능에 영향을 줄 수 있으므로, 실제 운영 환경에서는 제거할 것
      */
        log.info("Request URI: {}", requestURI); 

        return (apiEndPoints.contains(requestURI));
    }
}
