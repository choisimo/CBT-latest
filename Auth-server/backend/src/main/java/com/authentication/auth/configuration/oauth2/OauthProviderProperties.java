package com.authentication.auth.configuration.oauth2;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oauth2.kakao")
public class OauthProviderProperties {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
}
