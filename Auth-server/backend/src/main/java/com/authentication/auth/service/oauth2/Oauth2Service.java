package com.authentication.auth.service.oauth2;

import com.authentication.auth.domain.UserAuthentication;
import com.authentication.auth.domain.UserAuthenticationId;
import com.authentication.auth.domain.AuthProvider;
import com.authentication.auth.repository.AuthProviderRepository;
import com.authentication.auth.repository.UserAuthenticationRepository;
import com.authentication.auth.configuration.oauth2.OauthProperties;
import com.authentication.auth.configuration.token.JwtUtility;
import com.authentication.auth.dto.token.TokenDto;
import com.authentication.auth.dto.users.LoginResponse;
import com.authentication.auth.exception.CustomException;
import com.authentication.auth.exception.ErrorType;
import com.authentication.auth.service.redis.RedisService;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.authentication.auth.domain.User;
import com.authentication.auth.repository.UserRepository;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class Oauth2Service {

    @Value("${server.cookie.domain}")
    private String domain; // For cookie domain - Will be used in P2 for HttpOnly cookie

    private final OauthProperties oauthProperties;
    private final UserRepository userRepository;
    private final RedisService redisService;
    private final JwtUtility jwtUtility;
    private final AuthProviderRepository authProviderRepository;
    private final UserAuthenticationRepository userAuthenticationRepository;
    private final RestTemplate restTemplate;

    public Map<String, String> getKakaoTokens(String tempCode) {
        log.info("Requesting Kakao tokens with tempCode: {}", tempCode != null ? "present" : "null");
        String url = "https://kauth.kakao.com/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", oauthProperties.kakao().clientId());
        params.add("redirect_uri", oauthProperties.kakao().redirectUri());
        params.add("code", tempCode);
        params.add("client_secret", oauthProperties.kakao().clientSecret());

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        try {
            org.springframework.http.ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.POST, entity, new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response.getBody() != null && response.getBody().containsKey("access_token")
                    && response.getBody().containsKey("refresh_token")) {
                return Map.of(
                        "access_token", response.getBody().get("access_token").toString(),
                        "refresh_token", response.getBody().get("refresh_token").toString());
            } else {
                log.warn("Kakao getKakaoTokens response does not contain access_token or refresh_token. Response: {}", response.getBody());
                throw new CustomException(ErrorType.OAUTH2_INVALID_RESPONSE, "카카오로부터 유효한 토큰 응답을 받지 못했습니다.");
            }
        } catch (HttpClientErrorException e) {
            log.error("HttpClientErrorException while requesting Kakao tokens: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new CustomException(ErrorType.OAUTH2_PROVIDER_ERROR, "카카오 토큰 요청 실패: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Exception while requesting Kakao tokens: ", e);
            throw new CustomException(ErrorType.OAUTH2_PROVIDER_ERROR, "카카오 토큰 요청 중 알 수 없는 오류 발생: " + e.getMessage());
        }
    }

    public Map<String, Object> getKakaoUserProfile(String accessToken) {
        String url = "https://kapi.kakao.com/v2/user/me";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        try {
            org.springframework.http.ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.POST, entity, new ParameterizedTypeReference<Map<String, Object>>() {});
            if (response.getBody() == null) {
                log.warn("Kakao getKakaoUserProfile response body is null.");
                throw new CustomException(ErrorType.OAUTH2_INVALID_RESPONSE, "카카오로부터 사용자 프로필 정보를 받지 못했습니다.");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) response.getBody().get("properties");
            @SuppressWarnings("unchecked")
            Map<String, Object> kakaoAccount = (Map<String, Object>) response.getBody().get("kakao_account");

            Map<String, Object> userProfile = new HashMap<>();
            userProfile.put("id", response.getBody().get("id").toString());
            if (properties != null) {
                userProfile.put("nickname", properties.get("nickname"));
            }
            if (kakaoAccount != null) {
                userProfile.put("email", kakaoAccount.get("email"));
                userProfile.put("gender", kakaoAccount.get("gender"));
                userProfile.put("phone_number", kakaoAccount.get("phone_number"));
                String birthday = (String) kakaoAccount.get("birthday");
                String birthyear = (String) kakaoAccount.get("birthyear");
                if (birthyear != null && birthday != null) {
                    userProfile.put("birthdate", birthyear + "-" + birthday.substring(0, 2) + "-" + birthday.substring(2));
                } else if (birthday != null) {
                     userProfile.put("birthdate", birthday);
                }
            }
            return userProfile;
        } catch (HttpClientErrorException e) {
            log.error("HttpClientErrorException while fetching Kakao user profile: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new CustomException(ErrorType.OAUTH2_PROVIDER_ERROR, "카카오 사용자 프로필 요청 실패: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Exception while fetching Kakao user profile: ", e);
            throw new CustomException(ErrorType.OAUTH2_PROVIDER_ERROR, "카카오 사용자 프로필 요청 중 알 수 없는 오류 발생: " + e.getMessage());
        }
    }

    public Map<String, String> getNaverTokens(String tempCode, String state) {
        log.info("Requesting Naver tokens with tempCode: {}, state: {}", tempCode != null ? "present" : "null", state != null ? "present" : "null");
        String url = "https://nid.naver.com/oauth2.0/token";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", oauthProperties.naver().clientId());
        params.add("client_secret", oauthProperties.naver().clientSecret());
        params.add("code", tempCode);
        params.add("state", state);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        try {
            org.springframework.http.ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.POST, entity, new ParameterizedTypeReference<Map<String, Object>>() {});
            if (response.getBody() != null && response.getBody().containsKey("access_token") && response.getBody().containsKey("refresh_token")) {
                return Map.of(
                        "access_token", response.getBody().get("access_token").toString(),
                        "refresh_token", response.getBody().get("refresh_token").toString());
            } else {
                log.warn("Naver getNaverTokens response does not contain access_token or refresh_token. Response: {}", response.getBody());
                throw new CustomException(ErrorType.OAUTH2_INVALID_RESPONSE, "네이버로부터 유효한 토큰 응답을 받지 못했습니다.");
            }
        } catch (HttpClientErrorException e) {
            log.error("HttpClientErrorException while requesting Naver tokens: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new CustomException(ErrorType.OAUTH2_PROVIDER_ERROR, "네이버 토큰 요청 실패: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Exception while requesting Naver tokens: ", e);
            throw new CustomException(ErrorType.OAUTH2_PROVIDER_ERROR, "네이버 토큰 요청 중 알 수 없는 오류 발생: " + e.getMessage());
        }
    }

    public Map<String, Object> getNaverUserProfile(String accessToken) {
        String url = "https://openapi.naver.com/v1/nid/me";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        try {
            org.springframework.http.ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<Map<String, Object>>() {});
            if (response.getBody() != null && response.getBody().containsKey("response")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> naverResponseData = (Map<String, Object>) response.getBody().get("response");
                if (naverResponseData == null) {
                    log.warn("Naver getNaverUserProfile 'response' field is null. Full response: {}", response.getBody());
                    throw new CustomException(ErrorType.OAUTH2_INVALID_RESPONSE, "네이버 사용자 프로필 'response' 필드가 비어있습니다.");
                }
                return naverResponseData;
            }
            log.warn("Naver getNaverUserProfile response body is null or does not contain 'response' field. Response: {}", response.getBody());
            throw new CustomException(ErrorType.OAUTH2_INVALID_RESPONSE, "네이버로부터 유효한 사용자 프로필 정보를 받지 못했습니다.");
        } catch (HttpClientErrorException e) {
            log.error("HttpClientErrorException while fetching Naver user profile: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new CustomException(ErrorType.OAUTH2_PROVIDER_ERROR, "네이버 사용자 프로필 요청 실패: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Exception while fetching Naver user profile: ", e);
            throw new CustomException(ErrorType.OAUTH2_PROVIDER_ERROR, "네이버 사용자 프로필 요청 중 알 수 없는 오류 발생: " + e.getMessage());
        }
    }

    public Map<String, String> getGoogleTokens(String tempCode) {
        log.info("Requesting Google tokens with tempCode: {}", tempCode != null ? "present" : "null");
        String url = "https://oauth2.googleapis.com/token";
        String decodedTempCode = URLDecoder.decode(tempCode, StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", oauthProperties.google().clientId());
        params.add("client_secret", oauthProperties.google().clientSecret());
        params.add("redirect_uri", oauthProperties.google().redirectUri());
        params.add("code", decodedTempCode);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        try {
            org.springframework.http.ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.POST, entity, new ParameterizedTypeReference<Map<String, Object>>() {});
            if (response.getBody() != null && response.getBody().containsKey("access_token") && response.getBody().containsKey("refresh_token")) {
                return Map.of(
                        "access_token", response.getBody().get("access_token").toString(),
                        "refresh_token", response.getBody().get("refresh_token").toString());
            } else {
                log.warn("Google getGoogleTokens response does not contain access_token or refresh_token. Response: {}", response.getBody());
                throw new CustomException(ErrorType.OAUTH2_INVALID_RESPONSE, "구글로부터 유효한 토큰 응답을 받지 못했습니다.");
            }
        } catch (HttpClientErrorException e) {
            log.error("HttpClientErrorException while requesting Google tokens: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            if (e.getResponseBodyAsString().contains("invalid_grant")) {
                 throw new CustomException(ErrorType.OAUTH2_INVALID_GRANT, "구글 인증 실패 (invalid_grant): " + e.getResponseBodyAsString());
            }
            throw new CustomException(ErrorType.OAUTH2_PROVIDER_ERROR, "구글 토큰 요청 실패: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Exception while requesting Google tokens: ", e);
            throw new CustomException(ErrorType.OAUTH2_PROVIDER_ERROR, "구글 토큰 요청 중 알 수 없는 오류 발생: " + e.getMessage());
        }
    }

    public Map<String, Object> getGoogleUserProfile(String accessToken) {
        String url = "https://www.googleapis.com/oauth2/v3/userinfo";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        try {
            org.springframework.http.ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<Map<String, Object>>() {});
            if (response.getBody() == null) {
                log.warn("Google getGoogleUserProfile response body is null.");
                throw new CustomException(ErrorType.OAUTH2_INVALID_RESPONSE, "구글로부터 사용자 프로필 정보를 받지 못했습니다.");
            }
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("HttpClientErrorException while fetching Google user profile: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new CustomException(ErrorType.OAUTH2_PROVIDER_ERROR, "구글 사용자 프로필 요청 실패: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Exception while fetching Google user profile: ", e);
            throw new CustomException(ErrorType.OAUTH2_PROVIDER_ERROR, "구글 사용자 프로필 요청 중 알 수 없는 오류 발생: " + e.getMessage());
        }
    }

    @Transactional
    public User saveOrUpdateOauth2User(String provider, String oauthId, Map<String, Object> userProfile) {
        Optional<UserAuthentication> userAuthOptional = userAuthenticationRepository.findByAuthProvider_ProviderNameAndSocialId(provider.toUpperCase(), oauthId);

        if (userAuthOptional.isPresent()) {
            User user = userAuthOptional.get().getUser();
            log.info("User found for {} with userName (login ID): {}", provider, user.getUserName());
            return updateUserDetails(user, userProfile, provider);
        } else {
            log.info("Creating new user for {} with oauthId: {}", provider, oauthId);
            return createUser(provider, oauthId, userProfile);
        }
    }

    private User createUser(String provider, String oauthId, Map<String, Object> userProfile) {
        String email = (String) userProfile.getOrDefault("email", null);
        
        // Login ID (userName) 결정 로직
        String loginId = email; // 기본적으로 이메일을 loginId로 사용 시도
        if (email == null || email.isBlank() || userRepository.existsByUserName(email)) {
            // 이메일이 없거나, 비어있거나, 이미 사용 중인 경우 provider_oauthId를 loginId로 사용
            loginId = provider.toLowerCase() + "_" + oauthId;
            if (userRepository.existsByUserName(loginId)) {
                // provider_oauthId도 이미 사용 중인 경우, 랜덤 UUID 일부를 추가하여 고유성 확보
                loginId = loginId + "_" + UUID.randomUUID().toString().substring(0, 8);
            }
        }
        
        User newUser = User.builder()
                .userName(loginId)
                .email(email) // 이메일은 중복될 수 있으나, userName(loginId)는 고유해야 함
                .userRole("USER")
                .isActive("ACTIVE")
                .isPremium(false)
                .build();
        userRepository.save(newUser);
        log.info("New user created with userName (login ID): {}", loginId);

        AuthProvider authProvider = authProviderRepository.findByProviderName(provider.toUpperCase())
                .orElseGet(() -> {
                    AuthProvider newProvider = AuthProvider.builder()
                            .providerName(provider.toUpperCase())
                            .description(provider + " authentication")
                            .isActive(true)
                            .build();
                    return authProviderRepository.save(newProvider);
                });

        UserAuthenticationId userAuthId = new UserAuthenticationId(newUser.getId(), authProvider.getId());
        UserAuthentication userAuthentication = UserAuthentication.builder()
                .id(userAuthId)
                .user(newUser)
                .authProvider(authProvider)
                .socialId(oauthId)
                .build();
        userAuthenticationRepository.save(userAuthentication);

        return newUser;
    }

    private User updateUserDetails(User user, Map<String, Object> userProfile, String provider) {
        String emailFromProfile = (String) userProfile.get("email");
        // String nameFromProfile = (String) userProfile.get("name"); // User 엔티티에 name 필드 없음. userName은 loginId임.

        boolean updated = false;
        if (emailFromProfile != null && !emailFromProfile.equals(user.getEmail())) {
            // 이메일 변경 시, 새 이메일이 다른 사용자의 userName(loginId)으로 사용되고 있는지 확인 필요할 수 있음 (정책에 따라)
            // 여기서는 단순 업데이트
            user.setEmail(emailFromProfile);
            updated = true;
            log.info("User {} email updated to {}", user.getUserName(), emailFromProfile);
        }

        if (!"ACTIVE".equals(user.getIsActive())) {
            user.setIsActive("ACTIVE");
            updated = true;
            log.info("User {} status updated to ACTIVE", user.getUserName());
        }

        if (updated) {
            userRepository.save(user);
        }
        // UserAuthentication의 socialId는 일반적으로 변경되지 않으므로 업데이트 로직 불필요.
        return user;
    }

    @Transactional
    public LoginResponse handleOauth2Login(Map<String, String> requestBody, HttpServletResponse httpServletResponse, String provider) {
        String tempCode = requestBody.get("tempCode");
        if (tempCode == null || tempCode.isBlank()) {
            throw new CustomException(ErrorType.INVALID_REQUEST_PARAMETER, "tempCode가 필요합니다.");
        }
        String state = requestBody.get("state"); // Naver는 state 사용

        String accessTokenOauth;
        String refreshTokenOauth = null; // OAuth 제공자로부터 받는 리프레시 토큰
        Map<String, Object> userProfile;

        switch (provider.toLowerCase()) {
            case "kakao":
                Map<String, String> kakaoTokens = getKakaoTokens(tempCode);
                accessTokenOauth = kakaoTokens.get("access_token");
                refreshTokenOauth = kakaoTokens.get("refresh_token"); // 카카오는 리프레시 토큰 제공
                userProfile = getKakaoUserProfile(accessTokenOauth);
                break;
            case "naver":
                 if (state == null || state.isBlank()) { // Naver는 state 검증 필요 (P2에서)
                    throw new CustomException(ErrorType.INVALID_REQUEST_PARAMETER, "Naver 로그인 시 state 파라미터가 필요합니다.");
                }
                Map<String, String> naverTokens = getNaverTokens(tempCode, state);
                accessTokenOauth = naverTokens.get("access_token");
                refreshTokenOauth = naverTokens.get("refresh_token"); // 네이버도 리프레시 토큰 제공
                userProfile = getNaverUserProfile(accessTokenOauth);
                break;
            case "google":
                Map<String, String> googleTokens = getGoogleTokens(tempCode);
                accessTokenOauth = googleTokens.get("access_token");
                refreshTokenOauth = googleTokens.get("refresh_token"); // 구글도 리프레시 토큰 제공
                userProfile = getGoogleUserProfile(accessTokenOauth);
                break;
            default:
                throw new CustomException(ErrorType.UNSUPPORTED_OAUTH_PROVIDER, "지원하지 않는 OAuth 제공자입니다: " + provider);
        }

        // accessTokenOauth, userProfile null 체크는 각 getXXX 메소드에서 CustomException으로 처리됨.

        String oauthId = switch (provider.toLowerCase()) {
            case "naver" -> {
                Object resp = userProfile.get("response"); // Naver UserProfile 구조
                if (!(resp instanceof Map)) throw new CustomException(ErrorType.OAUTH2_INVALID_RESPONSE, "네이버 사용자 프로필 'response' 필드가 Map이 아닙니다.");
                @SuppressWarnings("unchecked") Map<String, Object> navUser = (Map<String, Object>) resp;
                Object idObj = navUser.get("id");
                if (idObj == null) throw new CustomException(ErrorType.OAUTH2_INVALID_RESPONSE, "네이버 사용자 프로필에 id가 없습니다.");
                yield idObj.toString();
            }
            case "kakao" -> {
                Object idObj = userProfile.get("id");
                if (idObj == null) throw new CustomException(ErrorType.OAUTH2_INVALID_RESPONSE, "카카오 사용자 프로필에 id가 없습니다.");
                yield idObj.toString();
            }
            case "google" -> {
                Object subObj = userProfile.get("sub"); // Google UserProfile 구조 (sub가 ID)
                if (subObj == null) throw new CustomException(ErrorType.OAUTH2_INVALID_RESPONSE, "구글 사용자 프로필에 sub (id)가 없습니다.");
                yield subObj.toString();
            }
            default -> throw new CustomException(ErrorType.UNSUPPORTED_OAUTH_PROVIDER, "지원하지 않는 OAuth 제공자 (ID 추출): " + provider);
        };
        
        log.info("OAuth login successful for provider: {}, oauthId: {}", provider, oauthId);

        // OAuth 제공자가 반환한 Refresh Token을 Redis에 저장 (선택적, 앱의 Refresh Token과 다름)
        if (refreshTokenOauth != null && !refreshTokenOauth.isBlank()) {
            // 이 refreshTokenOauth는 앱의 로그인 세션용이 아니라, OAuth Provider의 Access Token을 갱신하기 위한 용도.
            // 필요시 저장하여 활용 (예: Provider의 Access Token 만료 시 서버 단에서 갱신)
            // 현재 로직에서는 이 refreshTokenOauth를 직접 사용하지 않음.
            redisService.saveRToken(oauthId, provider, refreshTokenOauth); // provider_socialId를 키로 저장
            log.info("Saved OAuth provider's refresh token to Redis for provider: {}, oauthId: {}", provider, oauthId);
        }

        User user = saveOrUpdateOauth2User(provider, oauthId, userProfile);

        // 애플리케이션의 Access Token 생성
        TokenDto appAccessTokenDto = jwtUtility.buildToken(user.getUserName(), // userName (loginId) 사용
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getUserRole())));

        // 애플리케이션의 Refresh Token은 P2에서 HttpOnly 쿠키로 전달 예정.
        // 여기서는 LoginResponse에 앱의 AccessToken만 포함.
        // String appRefreshToken = tokenProvider.createAppRefreshToken(user.getUserName(), provider); // TokenProvider에 이런 메소드가 있다고 가정
        // redisService.saveAppRefreshToken(user.getUserName(), appRefreshToken); // 앱의 리프레시 토큰 저장
        // tokenProvider.setHttpOnlyCookie(httpServletResponse, appRefreshToken); // P2에서 처리

        log.info("Application access token generated for user: {}", user.getUserName());
        return new LoginResponse(appAccessTokenDto.accessToken(), null); // 앱의 RefreshToken은 쿠키로 전달 예정
    }
}
