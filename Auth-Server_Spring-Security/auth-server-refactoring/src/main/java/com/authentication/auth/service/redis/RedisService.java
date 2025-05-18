package com.authentication.auth.service.redis;

import com.authentication.auth.constants.SecurityConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * Redis 서비스
 * 토큰 및 인증 관련 데이터를 Redis에 저장하고 관리
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class RedisService {

    private static final String TOKEN_PREFIX = "TOKEN:";
    private static final String REFRESH_TOKEN_PREFIX = "REFRESH:";
    private static final String ACCESS_TOKEN_PREFIX = "ACCESS:";
    private static final String EMAIL_CODE_PREFIX = "EMAIL_CODE:";
    
    @Value("${REFRESH_TOKEN_VALIDITY}")
    private long refreshExpire;

    @Value("${ACCESS_TOKEN_VALIDITY}")
    private long accessExpire;

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 리프레시 토큰을 Redis 키로 변환
     * @param userId 사용자 ID
     * @param provider 제공자
     * @return Redis 키
     */
    private String refreshTokenToRedisKey(String userId, String provider) {
        if (userId == null || provider == null || 
                userId.isEmpty() || provider.isEmpty()) {
            throw new IllegalArgumentException("Redis 키 생성에 필요한 파라미터가 누락되었습니다");
        }
        return REFRESH_TOKEN_PREFIX + provider + ":" + userId;
    }

    /**
     * 액세스 토큰을 Redis 키로 변환
     * @param refreshToken 리프레시 토큰
     * @return Redis 키
     */
    private String accessTokenToRedisKey(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()){
            throw new IllegalArgumentException("Redis 키 생성에 필요한 리프레시 토큰이 누락되었습니다");
        }
        return ACCESS_TOKEN_PREFIX + refreshToken.substring(0, Math.min(refreshToken.length(), 20));
    }

    /**
     * 이메일 코드를 Redis 키로 변환
     * @param email 이메일
     * @return Redis 키
     */
    private String emailCodeToRedisKey(String email) {
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Redis 키 생성에 필요한 이메일이 누락되었습니다");
        }
        return EMAIL_CODE_PREFIX + email;
    }

    /**
     * 리프레시 토큰 저장
     * @param userId 사용자 ID
     * @param provider 제공자
     * @param refreshToken 리프레시 토큰
     * @return 저장 성공 여부
     */
    @Transactional
    public boolean saveRToken(String userId, String provider, String refreshToken) {
        String redisKey = refreshTokenToRedisKey(userId, provider);
        try {
            redisTemplate.opsForValue().set(redisKey, refreshToken, refreshExpire, TimeUnit.SECONDS);
            log.info("Redis에 리프레시 토큰 저장 성공: 제공자={}, 사용자={}", provider, userId);
            return true;
        } catch (Exception e) {
            log.error("Redis에 리프레시 토큰 저장 실패: 제공자={}, 사용자={}", provider, userId, e);
            return false;
        }
    }

    /**
     * 액세스 토큰 저장
     * @param refreshToken 리프레시 토큰
     * @param accessToken 액세스 토큰
     * @param userId 사용자 ID
     * @return 저장 성공 여부
     */
    @Transactional
    public boolean saveAccessToken(String refreshToken, String accessToken, String userId) {
        String redisKey = accessTokenToRedisKey(refreshToken);
        try {
            redisTemplate.opsForValue().set(redisKey, accessToken, accessExpire, TimeUnit.SECONDS);
            log.info("Redis에 액세스 토큰 저장 성공: 사용자={}", userId);
            return true;
        } catch (Exception e) {
            log.error("Redis에 액세스 토큰 저장 실패: 사용자={}", userId, e);
            return false;
        }
    }

    /**
     * 액세스 토큰 조회
     * @param refreshToken 리프레시 토큰
     * @return 액세스 토큰
     */
    @Transactional(readOnly = true)
    public String getAccessToken(String refreshToken) {
        String redisKey = accessTokenToRedisKey(refreshToken);
        try {
            String accessToken = redisTemplate.opsForValue().get(redisKey);

            if (accessToken == null) {
                log.warn("Redis에서 액세스 토큰을 찾을 수 없습니다");
                return null;
            }

            return accessToken;
        } catch (Exception e) {
            log.error("Redis에서 액세스 토큰 조회 실패", e);
            return null;
        }
    }

    /**
     * 리프레시 토큰 존재 여부 확인
     * @param userId 사용자 ID
     * @param provider 제공자
     * @param refreshToken 리프레시 토큰
     * @return 존재 여부
     */
    @Transactional(readOnly = true)
    public boolean isRTokenExist(String userId, String provider, String refreshToken) {
        String redisKey = refreshTokenToRedisKey(userId, provider);
        try {
            String storedToken = redisTemplate.opsForValue().get(redisKey);
            return storedToken != null && storedToken.equals(refreshToken);
        } catch (Exception e) {
            log.error("Redis에서 리프레시 토큰 확인 실패: 제공자={}", provider, e);
            return false;
        }
    }

    /**
     * 리프레시 토큰 삭제
     * @param userId 사용자 ID
     * @param provider 제공자
     * @return 삭제 성공 여부
     */
    @Transactional
    public boolean deleteRToken(String userId, String provider) {
        String redisKey = refreshTokenToRedisKey(userId, provider);
        try {
            Boolean removed = redisTemplate.delete(redisKey);
            if (removed != null && removed) {
                log.info("Redis에서 리프레시 토큰 삭제 성공: 제공자={}, 사용자={}", provider, userId);
            }
            return removed != null && removed;
        } catch (Exception e) {
            log.error("Redis에서 리프레시 토큰 삭제 실패: 제공자={}", provider, e);
            return false;
        }
    }

    /**
     * 리프레시 토큰 변경
     * @param userId 사용자 ID
     * @param provider 제공자
     * @param oldRefreshToken 기존 리프레시 토큰
     * @param newRefreshToken 새 리프레시 토큰
     * @return 변경 성공 여부
     */
    @Transactional
    public boolean changeRToken(String userId, String provider, String oldRefreshToken, String newRefreshToken) {
        try {
            // 기존 토큰이 유효한지 확인
            if (!isRTokenExist(userId, provider, oldRefreshToken)) {
                log.warn("기존 리프레시 토큰이 유효하지 않습니다: 제공자={}, 사용자={}", provider, userId);
                return false;
            }
            
            // 기존 토큰 삭제 후 새 토큰 저장
            deleteRToken(userId, provider);
            saveRToken(userId, provider, newRefreshToken);
            
            log.info("리프레시 토큰 변경 성공: 제공자={}, 사용자={}", provider, userId);
            return true;
        } catch (Exception e) {
            log.error("리프레시 토큰 변경 실패: 제공자={}", provider, e);
            return false;
        }
    }

    /**
     * 이메일 인증 코드 저장
     * @param email 이메일
     * @param code 인증 코드
     * @return 저장 성공 여부
     */
    @Transactional
    public boolean saveEmailCode(String email, String code) {
        if (email == null || email.isEmpty() || code == null || code.isEmpty()){
            log.error("이메일 또는 인증 코드가 누락되었습니다");
            return false;
        }
        try {
            String redisKey = emailCodeToRedisKey(email);
            redisTemplate.opsForValue().set(redisKey, code, 1800, TimeUnit.SECONDS); // 30분 유효
            log.info("이메일 인증 코드 저장 성공: 이메일={}", email);
            return true;
        } catch (Exception e) {
            log.error("이메일 인증 코드 저장 실패: 이메일={}", email, e);
            return false;
        }
    }

    /**
     * 이메일 인증 코드 확인
     * @param email 이메일
     * @param code 인증 코드
     * @return 일치 여부
     */
    @Transactional(readOnly = true)
    public boolean checkEmailCode(String email, String code) {
        if (email == null || email.isEmpty() || code == null || code.isEmpty()){
            log.error("이메일 또는 인증 코드가 누락되었습니다");
            return false;
        }
        try {
            String redisKey = emailCodeToRedisKey(email);
            String storedCode = redisTemplate.opsForValue().get(redisKey);
            return code.equals(storedCode);
        } catch (Exception e) {
            log.error("이메일 인증 코드 확인 실패: 이메일={}", email, e);
            return false;
        }
    }

    /**
     * 리프레시 토큰 찾기
     * @param userId 사용자 ID
     * @param provider 제공자
     * @param refreshToken 리프레시 토큰
     * @return 존재 여부
     */
    @Transactional(readOnly = true)
    public boolean findRToken(String userId, String provider, String refreshToken) {
        return isRTokenExist(userId, provider, refreshToken);
    }
}
