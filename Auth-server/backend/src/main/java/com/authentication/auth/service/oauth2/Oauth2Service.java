package com.authentication.auth.service.oauth2;

import com.authentication.auth.configuration.oauth2.OauthProperties;
import com.authentication.auth.configuration.token.JwtUtility;
import com.authentication.auth.domain.AuthProvider;
import com.authentication.auth.domain.User;
import com.authentication.auth.domain.UserAuthentication;
import com.authentication.auth.domain.UserAuthenticationId;
import com.authentication.auth.dto.token.TokenDto;
import com.authentication.auth.dto.users.LoginResponse;
import com.authentication.auth.repository.AuthProviderRepository;
import com.authentication.auth.repository.UserAuthenticationRepository;
import com.authentication.auth.repository.UserRepository;
import com.authentication.auth.service.redis.RedisService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Service
@RequiredArgsConstructor
@Slf4j
public class Oauth2Service {

    private final OauthProperties oauthProperties;
    private final UserRepository userRepository;
    private final RedisService redisService;
    private final JwtUtility jwtUtility;
    private final AuthProviderRepository authProviderRepository;
    private final UserAuthenticationRepository userAuthenticationRepository;
    private final RestTemplate restTemplate;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public LoginResponse handleOauth2Login(Map<String, String> requestBody, HttpServletResponse httpServletResponse, String provider) {
        String tempCode = requestBody.get("tempCode");
        Map<String, String> tokens = getKakaoTokens(tempCode);
        String accessToken = tokens.get("access_token");
        String refreshToken = tokens.get("refresh_token");

        Map<String, Object> userProfile = getKakaoUserProfile(accessToken);
        String oauthId = String.valueOf(userProfile.get("id"));

        User user = saveOrUpdateOauth2User(provider, oauthId, userProfile);

        TokenDto appTokenDto = jwtUtility.buildToken(user.getNickname(), Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getUserRole())));

        if (refreshToken != null) {
            redisService.saveRToken(oauthId, provider, refreshToken);
        }

        return new LoginResponse(appTokenDto.accessToken(), new LoginResponse.UserInfo(user.getId(), user.getNickname(), user.getEmail()));
    }

    @Transactional
    public User saveOrUpdateOauth2User(String provider, String oauthId, Map<String, Object> userProfile) {
        Optional<UserAuthentication> userAuthOptional = userAuthenticationRepository.findByAuthProvider_ProviderNameAndSocialId(provider.toUpperCase(), oauthId);

        User user;
        if (userAuthOptional.isPresent()) {
            user = userAuthOptional.get().getUser();
            String email = (String) userProfile.get("email");
            user.updateFromOauthProfile(email, user.getNickname());
            user = userRepository.save(user);
        } else {
            String email = (String) userProfile.get("email");
            String nickname = (String) userProfile.get("nickname");

            if (nickname == null || userRepository.existsByNickname(nickname)) {
                nickname = provider.toLowerCase() + "_" + oauthId;
            }

            user = User.builder()
                    .nickname(nickname)
                    .email(email)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .userRole("USER")
                    .isActive("ACTIVE")
                    .isPremium(false)
                    .build();
            user = userRepository.save(user);

            AuthProvider authProvider = authProviderRepository.findByProviderName(provider.toUpperCase())
                    .orElseThrow(() -> new IllegalStateException(provider + " provider not found"));

            UserAuthentication newUserAuth = UserAuthentication.builder()
                    .id(new UserAuthenticationId(user.getId(), authProvider.getId()))
                    .user(user)
                    .authProvider(authProvider)
                    .socialId(oauthId)
                    .build();
            userAuthenticationRepository.save(newUserAuth);
        }
        return user;
    }

    public Map<String, String> getKakaoTokens(String tempCode) {
        OauthProperties.Client kakao = oauthProperties.kakao();
        String tokenUrl = "https://kauth.kakao.com/oauth/token";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", kakao.clientId());
        body.add("redirect_uri", kakao.redirectUri());
        body.add("code", tempCode);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, request, new ParameterizedTypeReference<>() {});

        Map<String, Object> responseBody = response.getBody();
        return Map.of(
                "access_token", (String) responseBody.get("access_token"),
                "refresh_token", (String) responseBody.get("refresh_token")
        );
    }

    public Map<String, Object> getKakaoUserProfile(String accessToken) {
        String userUrl = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(headers);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(userUrl, HttpMethod.POST, request, new ParameterizedTypeReference<>() {});

        Map<String, Object> responseBody = response.getBody();
        Map<String, Object> kakaoAccount = (Map<String, Object>) responseBody.get("kakao_account");
        Map<String, Object> properties = (Map<String, Object>) responseBody.get("properties");

        return Map.of(
                "id", String.valueOf(responseBody.get("id")),
                "email", (String) kakaoAccount.get("email"),
                "nickname", (String) properties.get("nickname")
        );
    }
}
