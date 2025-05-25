package com.authentication.auth.service.oauth2;

import com.authentication.auth.domain.UserAuthentication;
import com.authentication.auth.domain.UserAuthenticationId;
import com.authentication.auth.domain.AuthProvider;
import com.authentication.auth.repository.AuthProviderRepository;
import com.authentication.auth.repository.UserAuthenticationRepository;
import com.authentication.auth.configuration.oauth2.OauthProperties;
import com.authentication.auth.configuration.token.JwtUtility;
import com.authentication.auth.dto.token.TokenDto;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.authentication.auth.domain.User;
import com.authentication.auth.repository.UserRepository;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class Oauth2Service {

    @Value("${server.cookie.domain}")
    private String domain; // For cookie domain

    private final OauthProperties oauthProperties;
    private final UserRepository userRepository;
    private final RedisService redisService;
    private final JwtUtility jwtUtility;
    private final AuthProviderRepository authProviderRepository;
    private final UserAuthenticationRepository userAuthenticationRepository;
    private final RestTemplate restTemplate;

    public Map<String, String> getKakaoTokens(String tempCode) {
        log.info("current : getKakaoTokens");

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
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.POST, entity, new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response.getBody() != null && response.getBody().containsKey("access_token")
                    && response.getBody().containsKey("refresh_token")) {
                return Map.of(
                        "access_token", response.getBody().get("access_token").toString(),
                        "refresh_token", response.getBody().get("refresh_token").toString());
            } else {
                throw new RuntimeException("액세스 토큰을 가져오지 못했습니다.");
            }
        } catch (Exception e) {
            log.error("Error while requesting tokens: ", e);
            throw e;
        }
    }

    public Map<String, Object> getKakaoUserProfile(String accessToken) {
        String url = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.POST, entity, new ParameterizedTypeReference<Map<String, Object>>() {});
            if (response.getBody() == null) {
                throw new RuntimeException("카카오 사용자 정보를 가져오지 못했습니다.");
            }
            // Extract 'properties' and 'kakao_account' maps
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
                // Kakao provides birthday and birthyear separately if available
                String birthday = (String) kakaoAccount.get("birthday"); // MMDD
                String birthyear = (String) kakaoAccount.get("birthyear"); // YYYY
                if (birthyear != null && birthday != null) {
                    userProfile.put("birthdate", birthyear + "-" + birthday.substring(0, 2) + "-" + birthday.substring(2));
                } else if (birthday != null) {
                     userProfile.put("birthdate", birthday); // Store as MMDD if year not available
                }
            }
            return userProfile;
        } catch (Exception e) {
            log.error("Error while fetching Kakao user profile: ", e);
            throw e;
        }
    }

    public Map<String, String> getNaverTokens(String tempCode, String state) {
        log.info("current : getNaverTokens");

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
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.POST, entity, new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response.getBody() != null && response.getBody().containsKey("access_token") && response.getBody().containsKey("refresh_token")) {
                return Map.of(
                        "access_token", response.getBody().get("access_token").toString(),
                        "refresh_token", response.getBody().get("refresh_token").toString());
            } else {
                throw new RuntimeException("액세스 토큰을 가져오지 못했습니다.");
            }
        } catch (Exception e) {
            log.error("Error while requesting tokens: ", e);
            throw e;
        }
    }

    public Map<String, Object> getNaverUserProfile(String accessToken) {
        String url = "https://openapi.naver.com/v1/nid/me";

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);

        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<Map<String, Object>>() {});
            if (response.getBody() != null && response.getBody().containsKey("response")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> naverResponseData = (Map<String, Object>) response.getBody().get("response");
                return naverResponseData;
            }
            throw new RuntimeException("네이버 사용자 정보를 가져오지 못했습니다.");
        } catch (Exception e) {
            log.error("Error while fetching Naver user profile: ", e);
            throw e;
        }
    }

    public Map<String, String> getGoogleTokens(String tempCode) {
        log.info("current : getGoogleTokens");

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
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.POST, entity, new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response.getBody() != null && response.getBody().containsKey("access_token") && response.getBody().containsKey("refresh_token")) {
                return Map.of(
                        "access_token", response.getBody().get("access_token").toString(),
                        "refresh_token", response.getBody().get("refresh_token").toString());
            } else {
                throw new RuntimeException("액세스 토큰을 가져오지 못했습니다.");
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                log.error("Oauth 2.0 compliance error: {}", e.getResponseBodyAsString());
            } else {
                log.error("Error while requesting tokens : ", e);
            }
            throw e;
        }
    }

    public Map<String, Object> getGoogleUserProfile(String accessToken) {
        String url = "https://www.googleapis.com/oauth2/v3/userinfo";

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);

        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<Map<String, Object>>() {});
            if (response.getBody() == null) {
                throw new RuntimeException("구글 사용자 정보를 가져오지 못했습니다.");
            }
            return response.getBody();
        } catch (Exception e) {
            log.error("Error while fetching Google user profile: ", e);
            throw e;
        }
    }

    @Transactional
    public User saveOrUpdateOauth2User(String provider, String oauthId, Map<String, Object> userProfile) {
        Optional<UserAuthentication> userAuthOptional = userAuthenticationRepository.findByAuthProvider_ProviderNameAndSocialId(provider.toUpperCase(), oauthId);

        if (userAuthOptional.isPresent()) {
            User user = userAuthOptional.get().getUser();
            log.info("user found for {} with userName: {}", provider, user.getUserName());
            return updateUserDetails(user, userProfile, provider);
        } else {
            return createUser(provider, userProfile);
        }
    }

    private User createUser(String provider, Map<String, Object> userProfile) {
        String email = (String) userProfile.getOrDefault("email", null);
        String oauthId = switch (provider.toLowerCase()) {
            case "naver" -> {
                Object naverResponseObj = userProfile.get("response");
                if (naverResponseObj instanceof Map) {
                    @SuppressWarnings("unchecked") // Suppress only for this known safe cast after instanceof
                    Map<String, Object> naverResponse = (Map<String, Object>) naverResponseObj;
                    Object naverIdObj = naverResponse.get("id");
                    if (naverIdObj != null) {
                        yield naverIdObj.toString();
                    }
                }
                throw new IllegalArgumentException("Invalid Naver user profile structure");
            }
            case "kakao" -> userProfile.get("id").toString();
            case "google" -> userProfile.get("sub").toString();
            default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
        };

        // Use email as userName if available and unique, otherwise use oauthId
        // This logic might need adjustment based on how unique userName (login ID) should be generated.
        // For now, keeping it as it was, but noting User.userName is now the login ID.
        String loginId = email != null && !userRepository.existsByUserName(email) ? email : oauthId;
        if (userRepository.existsByUserName(loginId)) {
            // If email was taken, and oauthId is also taken (e.g. user signed up normally then tried OAuth with same ID source)
            // Or if email wasn't available and oauthId is taken.
            // A more robust solution might involve appending a suffix or asking user for a unique ID.
            // For now, if the preferred loginId is taken, append provider to oauthId to attempt uniqueness.
            loginId = provider + "_" + oauthId;
            if (userRepository.existsByUserName(loginId)) {
                // If still taken, append a random component. This is getting complex and suggests a design review for unique ID generation.
                loginId = loginId + "_" + UUID.randomUUID().toString().substring(0, 8);
            }
        }

        User newUser = User.builder()
                .userName(loginId) // Changed: loginId (derived from OAuth) maps to User.userName
                .email(email)
                .userRole("USER") // Default role
                .isActive("ACTIVE") // OAuth users are active by default
                .isPremium(false)
                .build();
        userRepository.save(newUser);

        // Create and save AuthProvider if it doesn't exist
        AuthProvider authProvider = authProviderRepository.findByProviderName(provider)
                .orElseGet(() -> {
                    AuthProvider newProvider = AuthProvider.builder()
                            .providerName(provider)
                            .description(provider + " authentication")
                            .isActive(true)
                            .build();
                    return authProviderRepository.save(newProvider);
                });

        // Create UserAuthentication link
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
        String nameFromProfile = (String) userProfile.get("name");

        boolean updated = false;
        if (emailFromProfile != null && !emailFromProfile.equals(user.getEmail())) {
            user.setEmail(emailFromProfile);
            updated = true;
        }
        if (nameFromProfile != null && !nameFromProfile.equals(user.getUserName())) {
            // This assumes userProfile.name is the display name. If User.userName is the login ID,
            // this line might need to be removed or adjusted if display name is stored elsewhere or not at all.
            // For now, keeping it as it was, but noting User.userName is now the login ID.
            // If this 'nameFromProfile' is meant to be a display name, it has no field in User entity.
            // Let's assume for now this was an attempt to update a display name, which is no longer in User.
            // So, we should probably remove this update to userName if it's not the login ID.
            // Given User.userName is now login ID, we should NOT update it with a display name from profile.
            // user.setUserName(nameFromProfile); // Commenting out: userName is login ID, not display name.
            // updated = true; // No update to userName here.
        }

        // Ensure user is active
        if (!"ACTIVE".equals(user.getIsActive())) { // New String check
            user.setIsActive("ACTIVE");
            updated = true;
        }

        if (updated) {
            userRepository.save(user);
        }

        // Update UserAuthentication if necessary (e.g., socialId if it can change, though unlikely)
        // This part might be more complex depending on what needs to be updated in UserAuthentication
        AuthProvider authProvider = authProviderRepository.findByProviderName(provider)
                .orElseThrow(() -> new RuntimeException("AuthProvider not found: " + provider));

        UserAuthenticationId userAuthId = new UserAuthenticationId(user.getId(), authProvider.getId());
        Optional<UserAuthentication> existingAuth = userAuthenticationRepository.findById(userAuthId);

        String currentSocialId = switch (provider.toLowerCase()) {
            case "naver" -> {
                Object naverResponseObj = userProfile.get("response");
                if (naverResponseObj instanceof Map) {
                    @SuppressWarnings("unchecked") // Suppress only for this known safe cast after instanceof
                    Map<String, Object> naverResponse = (Map<String, Object>) naverResponseObj;
                    Object naverIdObj = naverResponse.get("id");
                    if (naverIdObj != null) {
                        yield naverIdObj.toString();
                    }
                }
                throw new IllegalArgumentException("Invalid Naver user profile structure");
            }
            case "kakao" -> userProfile.get("id").toString();
            case "google" -> userProfile.get("sub").toString();
            default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
        };

        if (existingAuth.isPresent()) {
            UserAuthentication auth = existingAuth.get();
            boolean authUpdated = false; // Flag to check if UserAuthentication needs saving
            if (!currentSocialId.equals(auth.getSocialId())) {
                auth.setSocialId(currentSocialId);
                authUpdated = true;
            }
            if (authUpdated) {
                userAuthenticationRepository.save(auth);
            }
        } else {
            // This case should ideally not happen if saveOrUpdateOauth2User ensures UserAuthentication exists
            UserAuthentication newUserAuth = UserAuthentication.builder()
                    .id(userAuthId)
                    .user(user)
                    .authProvider(authProvider)
                    .socialId(currentSocialId)
                    .build();
            userAuthenticationRepository.save(newUserAuth);
        }
        return user; // Added return statement
    }

    @Transactional
    public ResponseEntity<?> handleOauth2Login(Map<String, String> requestBody, HttpServletResponse response,
            String provider) {
        String tempCode = requestBody.get("tempCode");
        String state = requestBody.get("state"); // Naver uses state

        String accessTokenOauth;
        String refreshTokenOauth = null; // Not all providers might return this or be used immediately
        Map<String, Object> userProfile;

        try {
            switch (provider.toLowerCase()) {
                case "kakao":
                    Map<String, String> kakaoTokens = getKakaoTokens(tempCode);
                    accessTokenOauth = kakaoTokens.get("access_token");
                    refreshTokenOauth = kakaoTokens.get("refresh_token");
                    userProfile = getKakaoUserProfile(accessTokenOauth);
                    break;
                case "naver":
                    Map<String, String> naverTokens = getNaverTokens(tempCode, state);
                    accessTokenOauth = naverTokens.get("access_token");
                    refreshTokenOauth = naverTokens.get("refresh_token");
                    userProfile = getNaverUserProfile(accessTokenOauth);
                    break;
                case "google":
                    Map<String, String> googleTokens = getGoogleTokens(tempCode);
                    accessTokenOauth = googleTokens.get("access_token");
                    refreshTokenOauth = googleTokens.get("refresh_token");
                    userProfile = getGoogleUserProfile(accessTokenOauth);
                    break;
                default:
                    return ResponseEntity.badRequest().body("Unsupported provider: " + provider);
            }

            if (accessTokenOauth == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("OAuth Access Token_이 없습니다.");
            }
            if (userProfile == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("OAuth User Profile_이 없습니다.");
            }

            String oauthId = switch (provider.toLowerCase()) {
                case "naver" -> {
                    Object naverResponseObj = userProfile.get("response");
                    if (naverResponseObj instanceof Map) {
                        @SuppressWarnings("unchecked") // Suppress only for this known safe cast after instanceof
                        Map<String, Object> naverResponse = (Map<String, Object>) naverResponseObj;
                        Object naverIdObj = naverResponse.get("id");
                        if (naverIdObj != null) {
                            yield naverIdObj.toString(); // Naver nests profile in 'response'
                        }
                    }
                    throw new IllegalArgumentException("Invalid Naver user profile structure");
                }
                case "kakao" -> userProfile.get("id").toString();
                case "google" -> userProfile.get("sub").toString();
                default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
            };

            if (oauthId == null) {
                log.error("id를 파싱하는데 문제가 생긴 것 같습니다 ㅠㅠ");
                 return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("OAuth ID를 파싱할 수 없습니다.");
            }

            if (refreshTokenOauth != null) {
                 redisService.saveRToken(oauthId, provider, refreshTokenOauth);
            }

            User user = saveOrUpdateOauth2User(provider, oauthId, userProfile);

            TokenDto appAccessToken = jwtUtility.buildToken(user.getUserName(), // Changed from user.getUserId()
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getUserRole()))); // Role as String

            loginResponse(response, appAccessToken, refreshTokenOauth, provider);

            return ResponseEntity.ok(Map.of(
                    "access_token", appAccessToken.accessToken(), 
                    "userProfile", userProfile));

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST && e.getResponseBodyAsString().contains("invalid_grant")) {
                log.error("OAuth 2.0 Error (invalid_grant) for provider {}: {}", provider, e.getResponseBodyAsString());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("OAuth 인증 실패: 유효하지 않은 인증 코드 또는 요청입니다.");
            } else if (e.getStatusCode().is4xxClientError()) {
                log.error("Client Error during OAuth process for provider {}: Status {}, Body {}", provider, e.getStatusCode(), e.getResponseBodyAsString());
                return ResponseEntity.status(e.getStatusCode()).body("OAuth 처리 중 클라이언트 오류 발생: " + e.getResponseBodyAsString());
            } else {
                log.error("Error during OAuth process for provider {}: ", provider, e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("OAuth 처리 중 서버 오류 발생");
            }
        } catch (Exception e) {
            log.error("Unexpected error during OAuth process for provider {}: ", provider, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("OAuth 처리 중 예기치 않은 오류 발생");
        }
    }

    private void loginResponse(HttpServletResponse response, TokenDto accessToken, String refreshToken, String provider) {
        response.addHeader("Authorization", "Bearer " + accessToken.accessToken());
        if (refreshToken != null) {
            Cookie newCookie = new Cookie(provider + "_refreshToken", refreshToken);
            newCookie.setHttpOnly(true);
            newCookie.setDomain(domain);
            newCookie.setPath("/");
            newCookie.setSecure(true); // Recommended for production
            newCookie.setMaxAge(60 * 60 * 24 * 7); // 7 days
            response.addCookie(newCookie);
        }
        log.info("{} login successful, access token issued.", provider);
    }
}
