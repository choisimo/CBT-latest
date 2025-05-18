package com.career_block.auth.service.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Component
public class redisService {

    @Value("${REFRESH_TOKEN_VALIDITY}")
    private long refreshExpire;

    @Value("${ACCESS_TOKEN_VALIDITY}")
    private long accessExpire;

    private final RedisTemplate<String, String> redisTemplate;


    private String RefreshTokenToRedisKey(String userId, String provider, String RToken) {
        if (userId == null || provider == null || RToken == null ||
                userId.isEmpty() || provider.isEmpty() || RToken.isEmpty()) {
            throw new RuntimeException("error while convert Refresh Token into REDIS_KEY..");
        }
        return provider + "_RToken_" + userId;
    }

    private String AccessTokenToRedisKey(String RToken) {
        if (RToken == null || RToken.isEmpty()){
            throw new RuntimeException("error while convert access Token into REDIS_KEY..");
        }
        return "_accessToken_" + RToken.substring(10);
    }

    @Transactional
    public boolean saveRToken(String userId, String provider, String RToken) {
        String REDIS_KEY = RefreshTokenToRedisKey(userId, provider, RToken);
        try {
            redisTemplate.opsForValue().set(REDIS_KEY, RToken, refreshExpire, TimeUnit.SECONDS);
            log.info("Redis RToken save success for provider: {}", provider);
            return true;
        } catch (Exception e) {
            log.error("Redis has failed to save Refresh Token for provider {} and for userId {}", provider, userId, e);
            return false;
        }
    }

    @Transactional
    public boolean saveAccessToken(String RToken, String accessToken, String userId) {
        String REDIS_KEY = AccessTokenToRedisKey(RToken);
        try {
            redisTemplate.opsForValue().set(REDIS_KEY, accessToken, accessExpire, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            log.error("Redis has failed to save Access Token for userId {}", userId);
            return false;
        }
    }

    @Transactional
    public String getAccessToken(String RToken) {
        String REDIS_KEY = AccessTokenToRedisKey(RToken);
        try {
            String accessToken = redisTemplate.opsForValue().get(REDIS_KEY);

            if (accessToken == null) {
                log.warn("No access Token INFO");
                return null;
            }

            return accessToken;
        } catch (Exception e) {
            log.error("Failed to get AccessToken INFO from REDIS");
            return null;
        }
    }

    @Transactional
    public boolean isRTokenExist(String userId, String provider, String RToken) {
        String REDIS_KEY = RefreshTokenToRedisKey(userId, provider, RToken);
        try {
            Boolean exists = redisTemplate.hasKey(REDIS_KEY);
            return exists != null && exists;
        } catch (Exception e) {
            log.error("REDIS key searching failed for provider: {}", provider, e);
            return false;
        }
    }


    @Transactional
    public boolean deleteRToken(String userId, String provider, String RToken) {
        String REDIS_KEY = RefreshTokenToRedisKey(userId, provider, RToken);
        try {
            Boolean removed = redisTemplate.delete(REDIS_KEY);
            return removed != null && removed;
        } catch (Exception e) {
            log.error("Failed to delete RToken for provider: {}", provider, e);
            return false;
        }
    }


    @Transactional
    public List<String> getAllRTokens(String userId, String provider) {
        String pattern = provider + "_RToken_" + userId + "_";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()){
            return Collections.emptyList();
        }
        return redisTemplate.opsForValue().multiGet(keys);
    }


    @Transactional
    public boolean changeRToken(String userId, String provider, String RToken, String newRToken) {
        try {
            deleteRToken(userId, provider, RToken);
            saveRToken(userId, provider, newRToken);
            log.info("{}'s RToken has been changed for provider: {}", userId, provider);
            return true;
        } catch (Exception e) {
            log.error("Failed to change RToken for provider: {}", provider, e);
            return false;
        }
    }


    @Transactional
    public boolean saveEmailCode(String email, String code) {
        if (email == null || email.isEmpty() || code == null || code.isEmpty()){
            log.error("essential parameter or parameters is null");
            return false;
        }
        try {
            redisTemplate.opsForValue().set(email, code, 1800, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            log.error("Failed to save email code for email: {}", email);
            return false;
        }
    }


    @Transactional(readOnly = true)
    public boolean checkEmailCode(String email, String code) {
        if (email == null || email.isEmpty() || code == null || code.isEmpty()){
            log.error("essential parameter or parameters is null");
            return false;
        }
        try {
            return (Objects.equals(code, redisTemplate.opsForValue().get(email)));
        } catch (Exception e) {
            log.error("Failed to check Email Code for email : {}", email);
            return false;
        }
    }

    @Transactional
    public boolean findRToken(String userId, String provider, String RToken) {
        try {
            String key = RefreshTokenToRedisKey(userId, provider, RToken);
            return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, RToken));
        } catch (Exception e) {
            log.error("redis key search failed", e);
            return false;
        }
    }


}
