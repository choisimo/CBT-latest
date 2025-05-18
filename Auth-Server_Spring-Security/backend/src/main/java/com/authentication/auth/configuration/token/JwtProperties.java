package com.authentication.auth.configuration.token;
import io.jsonwebtoken.security.Keys;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.security.Key;

/**
* ConfigurationProperties 를 사용하여 application.yml 파일의 jwt 설정을 읽어옴.
*/

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties (
    private String siteDomain,
    private String cookieDomain,
    private String secretKey,
    private String secretKey2,
    private Long accessTokenValidity,
    private Long refreshTokenValidity
) implements Record {
    /**
    * return access token key
    */
    public Key key() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }
    /**
    * return refresh token key
    */
    public Key key2() {
        return Keys.hmacShaKeyFor(secretKey2.getBytes());
    }
}

