package com.authentication.auth.filter;

import com.authentication.auth.DTO.token.tokenDto;
import com.authentication.auth.configuration.token.jwtUtility;
import com.authentication.auth.domain.users;
import com.authentication.auth.others.constants.SecurityConstants;
import com.authentication.auth.service.oauth2.oauth2Service;
import com.authentication.auth.service.oauth2.snsTokenValidator;
import com.authentication.auth.service.redis.redisService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Random;

@Slf4j
@Component
public class snsRequestFilter implements PluggableFilter {

    private final JwtUtility jwtUtility;
    private final RedisService redisService;


    @Value("${site.domain}")
    private String domain;

    public snsRequestFilter(jwtUtility jwtUtility, redisService redisService, snsTokenValidator snsTokenValidator, oauth2Service oauth2Service) {
        this.jwtUtility = jwtUtility;
        this.redisService = redisService;
        this.snsTokenValidator = snsTokenValidator;
        this.oauth2Service = oauth2Service;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String providerHeader = request.getHeader("provider"); // SNS 정보 제공자 추가하기
        String authorizationHolder = request.getHeader(SecurityConstants.TOKEN_HEADER);
        log.info("authorizationHolder logging for test {}", authorizationHolder);

        log.info("current SNS providerHeader is {} ", providerHeader);

        if (providerHeader == null || "server".equals(providerHeader)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!isBearerToken(authorizationHolder)) {
            log.error("authorization is null");
            filterChain.doFilter(request, response);
            return;
        }

        String JWT = extractToken(authorizationHolder);

        if (JWT == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (jwtUtility.validateJWT(JWT)) {
            setAuthentication(JWT);
        } else {
            handleRefreshToken(request, response, JWT, providerHeader);
        }

        filterChain.doFilter(request, response);
    }


    private void handleRefreshToken(HttpServletRequest request, HttpServletResponse response, String JWT, String providerHeader) throws IOException {
        String snsRefreshToken = jwtUtility.checkSnsCookie(request, response);
        if (snsRefreshToken == null) {
            log.warn("no refresh token exists in cookies .. ");
            return;
        }

        String userId = getUserIdFromJWT(JWT);
        if (RedisMatchSnsRToken(userId, snsRefreshToken)) {
            refreshTokenAndAuthenticate(request, response, snsRefreshToken, providerHeader, userId);
        } else {
            log.warn("sns refreshToken does not exist in redis .. ");
        }
    }

    private void refreshTokenAndAuthenticate(HttpServletRequest request, HttpServletResponse response, String snsRefreshToken, String providerHeader, String userId) throws IOException {
        Map<String, String> newTokens = snsTokenValidator.getNewTokenByRefreshToken(snsRefreshToken, providerHeader);
        String newSnsAccessToken = newTokens.get("access_token");

        if (newSnsAccessToken == null) {
            log.error("access token does not exist");
            return;
        }

        Map<String, Object> snsUserProfile = getSnsUserProfile(newSnsAccessToken, providerHeader);
        if (snsUserProfile == null) {
            log.error("cannot get any sns userProfiles");
            return;
        }

        String oauthId = extractOauthId(snsUserProfile, providerHeader);
        redisService.saveRToken(oauthId, providerHeader, snsRefreshToken);

        users user = oauth2Service.saveOrUpdateOauth2User(providerHeader, oauthId, snsUserProfile);
        tokenDto accessToken = jwtUtility.createToken(user.getUserId(), user.getNickname(), Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name())));
        loginResponse(response, accessToken.getAccessToken(), snsRefreshToken, providerHeader);

        setAuthentication(accessToken.getAccessToken());
    }

    private String extractOauthId(Map<String, Object> snsUserProfile, String providerHeader) {
        return switch (providerHeader) {
            case "naver" -> ((Map<String, Object>) snsUserProfile.get("response")).get("id").toString();
            case "kakao" -> snsUserProfile.get("id").toString();
            default -> snsUserProfile.get("sub").toString();
        };
    }


    private String getUserIdFromJWT(String JWT) {
        return (String) jwtUtility.getClaimsFromAccessToken(JWT).get("userId");
    }



    private boolean RedisMatchSnsRToken(String userId, String RToken) {
        return redisService.findRToken(userId, "server", RToken);
    }

    private Map<String, Object> getSnsUserProfile(String access_token, String provider) {
        Map<String, Object> userProfile = null;
        switch (provider) {
            case "kakao" :
                userProfile = oauth2Service.getKakaoUserProfile(access_token);
                break;
            case "naver" :
                userProfile = oauth2Service.getNaverUserProfile(access_token);
                break;
            case "google" :
                userProfile = oauth2Service.getGoogleUserProfile(access_token);
                break;
            default:
                throw new IllegalArgumentException("Unsupported provider: " + provider);
        }
        return userProfile;
    }

    private void loginResponse(HttpServletResponse response, String accessToken, String refreshToken, String provider) {
        Cookie newCookie = new Cookie(provider + "_refreshToken", refreshToken);
        newCookie.setHttpOnly(true);
        newCookie.setDomain(domain);
        newCookie.setPath("/");
        response.addCookie(newCookie);

        response.addHeader(SecurityConstants.TOKEN_HEADER, SecurityConstants.TOKEN_PREFIX + accessToken);
    }


    private Date parseBirthday(String birthday) {
        if (birthday != null && birthday.length() == 5) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("MM-dd");
                Date date = sdf.parse(birthday);
                return date;
            } catch (ParseException e) {
                log.error("Error parsing birthday: ", e);
            }
        }
        return null; // Implement proper parsing if different format
    }


    private String generateRandomString(int length) {
        final String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            sb.append(characters.charAt(random.nextInt(characters.length())));
        }

        return sb.toString();
    }

    private String extractToken(String authorizationHolder) {
        return authorizationHolder != null ? authorizationHolder.split(" ")[1] : null;
    }


    private boolean isBearerToken(String authorization) {
        return authorization != null && authorization.startsWith("Bearer ");
    }

    private void setAuthentication(String JWT) {
        try {
            Authentication authentication = jwtUtility.getAuthentication(JWT);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.info("유효한 SNS authorization access_token");
        } catch (Exception e) {
            log.error("인증 실패!!");
        }
    }
        
        @Override
        public void configure(HttpSecurity http) throws Exception {
            http.addFilterBefore(this, JwtVerificationFilter.class);
        }
    
        @Override
        public int getOrder() {
            return 150; // AuthenticationFilter와 JwtVerificationFilter 사이 순서
        }
    
        @Override
        public Class<? extends Filter> getBeforeFilter() {
            return AuthenticationFilter.class;
        }
    
        @Override
        public Class<? extends Filter> getAfterFilter() {
            return JwtVerificationFilter.class;
        }
}
