package com.career_block.auth.service.oauth2;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.authentication.auth.DTO.token.tokenDto;
import com.authentication.auth.configuration.token.jwtUtility;
import com.authentication.auth.domain.Role;
import com.authentication.auth.domain.users;
import com.authentication.auth.others.nickNameGenerator;
import com.authentication.auth.others.constants.SecurityConstants;
import com.authentication.auth.repository.usersRepository;
import com.authentication.auth.service.redis.redisService;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class oauth2Service {

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String kakaoClientSecret;

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String naverClientId;

    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String naverClientSecret;

    @Value("${spring.security.oauth2.client.registration.naver.redirect-uri}")
    private String naverRedirectUri;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String googleRedirectUri;

    @Value("${site.domain}")
    private String domain;

    private final usersRepository usersRepository;
    private final redisService redisService;
    private final jwtUtility jwtUtility;
    private final nickNameGenerator nickNameGenerator;


    public Map<String, String> getKakaoTokens(String tempCode) {
        log.info("current : getKakaoTokens");

        String url = "https://kauth.kakao.com/oauth/token";

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoClientId);
        params.add("redirect_uri", kakaoRedirectUri);
        params.add("code", tempCode);
        params.add("client_secret", kakaoClientSecret);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("access_token") && response.getBody().containsKey("refresh_token")) {
                return Map.of(
                        "access_token", response.getBody().get("access_token").toString(),
                        "refresh_token", response.getBody().get("refresh_token").toString()
                );
            } else {
                throw new RuntimeException("액세스 토큰을 가져오지 못했습니다.");
            }
        } catch (Exception e) {
            log.error("Error while requesting tokens: ", e);
            throw e;
        }
    }

    public Map<String, Object> getKakaoUserProfile(String accessToken) {
        log.info("current : getKakaoUserProfile");

        String url = "https://kapi.kakao.com/v2/user/me";

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);

        log.info("Requesting user profile with accessToken: {}", accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("사용자 프로필을 가져오지 못했습니다.");
            }
        } catch (Exception e) {
            log.error("Error while requesting user profile: ", e);
            throw e;
        }
    }

    public Map<String, String> getNaverTokens(String tempCode, String state) {
        log.info("current : getNaverTokens");

        String url = "https://nid.naver.com/oauth2.0/token";

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", naverClientId);
        params.add("redirect_uri", naverRedirectUri);
        params.add("code", tempCode);
        params.add("client_secret", naverClientSecret);
        params.add("state", state);

        log.info("Requesting tokens with params: {}", params);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            log.info("Response from Naver: {}", response);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                log.info("responseBody info : {}", responseBody.toString());

                if (responseBody != null && responseBody.containsKey("access_token") && responseBody.containsKey("refresh_token")) {
                    log.info("Received tokens: {}", responseBody);
                    return Map.of(
                            "access_token", responseBody.get("access_token").toString(),
                            "refresh_token", responseBody.get("refresh_token").toString()
                    );
                } else {
                    log.error("Response body is missing required tokens: {}", responseBody);
                    throw new RuntimeException("액세스 토큰을 가져오지 못했습니다.");
                }
            } else {
                log.error("Failed to get tokens, status code: {}, response: {}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("액세스 토큰을 가져오지 못했습니다.");
            }
        } catch (Exception e) {
            log.error("Error while requesting tokens: ", e);
            throw e;
        }
    }


    public Map<String, Object> getNaverUserProfile(String accessToken) {
        log.info("current : getNaverUserProfile");

        String url = "https://openapi.naver.com/v1/nid/me";

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);

        log.info("Requesting user profile with accessToken: {}", accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("사용자 프로필을 가져오지 못했습니다.");
            }
        } catch (Exception e) {
            log.error("Error while requesting user profile: ", e);
            throw e;
        }
    }

    public Map<String, String> getGoogleTokens(String tempCode) {
        log.info("current : getGoogleTokens");

        String url = "https://oauth2.googleapis.com/token";

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded");

        String decode =URLDecoder.decode(tempCode, StandardCharsets.UTF_8);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", googleClientId);
        params.add("redirect_uri", googleRedirectUri);
        params.add("code", decode);
        params.add("client_secret", googleClientSecret);
        params.add("access_type", "offline");
        params.add("prompt", "consent");

        log.info("Requesting tokens with params: {}", params);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("access_token") && response.getBody().containsKey("refresh_token")) {
                log.info("Received tokens: {}", response.getBody());
                return Map.of(
                        "access_token", response.getBody().get("access_token").toString(),
                        "refresh_token", response.getBody().get("refresh_token").toString()
                );
            } else {
                log.info("response.getBody() :  {}", response.getBody());
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
        log.info("current : getGoogleUserProfile");

        String url = "https://www.googleapis.com/oauth2/v3/userinfo";

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);

        log.info("Requesting user profile with accessToken: {}", accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("사용자 프로필을 가져오지 못했습니다.");
            }
        } catch (Exception e) {
            log.error("Error while requesting user profile: ", e);
            throw e;
        }
    }

    @Transactional
    public users saveOrUpdateOauth2User(String provider, String oauthId, Map<String, Object> userProfile) {
        Optional<users> optionalUser = usersRepository.findByProviderAndProviderId(provider, oauthId);
        users user;
        if (optionalUser.isPresent()) {
            user = optionalUser.get();
            log.info("user found for {}", optionalUser.get().getUserId());
            updateUserDetails(user, userProfile, provider);
        } else {
            log.info("user not found, creating new user");
            user = createUser(provider, userProfile);
        }
        return usersRepository.save(user);
    }


    private void updateUserDetails(users user, Map<String, Object> userProfile, String provider) {
        log.info("updateUserDetails 호출");

        String newName = null;
        String newNickname = null;
        String newProfileImage = null;
        String newGender = null;
        String newPhone = null;
        String newBirthDate = null;
        String newEmail = null;

        String defaultString = "";
        String defaultGender = "N/A";
        String defaultPhone = "000-0000-0000";

        switch (provider) {
            case "kakao":
                Map<String, Object> properties = (Map<String, Object>) userProfile.get("properties");
                newNickname = (String) properties.get("nickname");
                newProfileImage = (String) properties.get("profile_image");
                Map<String, Object> kakaoAccount = (Map<String, Object>) userProfile.get("kakao_account");
                newEmail = (String) kakaoAccount.get("email");
                break;
            case "naver":
                Map<String, Object> response = (Map<String, Object>) userProfile.get("response");
                newName = (String) response.get("name");
                newNickname = (String) response.get("nickname");
                newProfileImage = (String) response.get("profile_image");
                newEmail = (String) response.get("email");
                newGender = (String) response.get("gender");
                newPhone = (String) response.get("mobile_e164");
                newBirthDate = (String) response.get("birthday");
                break;
            case "google":
                newName = (String) userProfile.get("name");
                newProfileImage = (String) userProfile.get("picture");
                newEmail = (String) userProfile.get("email");
                newGender = defaultGender;
                newPhone = defaultPhone;
                newBirthDate = defaultString;
                break;
        }

        if (newName != null && !newName.equals(user.getUserName())) {
            user.setUserName(newName);
        }

        if (newNickname != null && !newNickname.equals(user.getNickname())) {
            user.setNickname(newNickname);
        }

        if (newProfileImage != null && !newProfileImage.equals(user.getProfile())) {
            user.setProfile(newProfileImage);
        }

        if (newGender != null && !newGender.equals(user.getGender())) {
            user.setGender(newGender);
        }

        if (newPhone != null && !newPhone.equals(user.getPhone())) {
            user.setPhone(newPhone);
        }

        if (newBirthDate != null && !newBirthDate.equals(user.getBirthDate())) {
            user.setBirthDate(parseBirthday(newBirthDate));
        }

        if (newEmail != null && !newEmail.equals(user.getEmail())) {
            user.setEmail(newEmail);
        }
    }

    private users createUser(String provider, Map<String, Object> userProfile) {

        log.info("createUser 호출");

        String randomPassword = generateRandomString(13);
        String randomUserId = generateRandomString(13);

        String name = null;
        String nickname = null;
        String profileImage = null;
        String email = null;
        String providerId = null;
        String gender = null;
        String phone = null;
        String birthDate = null;

        String defaultString = "";
        String defaultGender = "N/A"; // Example default gender
        String defaultPhone = "000-0000-0000"; // Example default phone number


        log.info("provider [{}] 의 userProfile : {}", provider, userProfile);

        switch (provider) {
            case "kakao":
                Map<String, Object> properties = (Map<String, Object>) userProfile.get("properties");
                log.info("createUser [kakao] properties : {}", properties);
                providerId = String.valueOf(userProfile.get("id"));
                nickname = (String) properties.get("nickname");
                profileImage = (String) properties.get("profile_image");
                Map<String, Object> kakaoAccount = (Map<String, Object>) userProfile.get("kakao_account");
                email = (String) kakaoAccount.get("email");
                break;
            case "naver":
                Map<String, Object> response = (Map<String, Object>) userProfile.get("response");
                log.info("createUser [naver] response : {}", response);
                providerId = (String) response.get("id");
                name = (String) response.get("name");
                nickname = (String) response.get("nickname");
                profileImage = (String) response.get("profile_image");
                email = (String) response.get("email");
                gender = (String) response.get("gender");
                phone = (String) response.get("mobile_e164");
                birthDate = (String) response.get("birthday");
                break;
            case "google":
                log.info("createUser [google] userProfile : {}", userProfile);
                providerId = (String) userProfile.get("sub"); // Google uses "sub" for user ID
                name = (String) userProfile.get("name");
                profileImage = (String) userProfile.get("picture");
                email = (String) userProfile.get("email");
                gender = defaultGender; // Google does not provide gender directly
                phone = defaultPhone; // Google does not provide phone directly
                birthDate = defaultString; // Google does not provide birthdate directly
                break;
        }

        return users.builder()
                .userId(randomUserId)
                .userPw(randomPassword)
                .userName(name != null ? name : defaultString)
                .nickname(nickname != null ? nickname : defaultString)
                .email(email != null ? email : defaultString)
                .profile(profileImage != null ? profileImage : defaultString)
                .gender(gender != null ? gender : defaultGender)
                .phone(phone != null ? phone : defaultPhone)
                .birthDate(birthDate != null ? parseBirthday(birthDate) : null)
                .provider(provider)
                .providerId(providerId != null ? providerId : "")
                .role(Role.USER)
                .build();
    }

    @Transactional
    public ResponseEntity<?> handleOauth2Login(Map<String, String> requestBody, HttpServletResponse response, String provider) {
        log.info("current : handleOauth2Login{}", provider);

        String tempCode = requestBody.get("tempCode");

        if (tempCode == null) {
            log.error("there is no tempCode needed for {} login", provider);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("there is no tempCode needed for " + provider + " login");
        }

        try {
            Map<String, String> tokens;
            Map<String, Object> userProfile;

            switch (provider) {
                case "kakao":
                    tokens = getKakaoTokens(tempCode);
                    userProfile = getKakaoUserProfile(tokens.get("access_token"));
                    break;
                case "naver":
                    String state = requestBody.get("state");
                    if (state == null) {
                        log.error("there is no state needed for naver login");
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("there is no state for naver login");
                    }
                    tokens = getNaverTokens(tempCode, state);
                    userProfile = getNaverUserProfile(tokens.get("access_token"));
                    log.info("Naver user profile: {}", userProfile); // Log the entire user profile
                    break;
                case "google":
                    tokens = getGoogleTokens(tempCode);
                    userProfile = getGoogleUserProfile(tokens.get("access_token"));
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported provider: " + provider);
            }

            String snsAccessToken = tokens.get("access_token");
            String refreshToken = tokens.get("refresh_token");

            if (snsAccessToken == null || refreshToken == null) {
                log.error("access token or refresh token does not exist");
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body("access token or refresh token does not exist");
            }

            if (userProfile == null) {
                log.error("userProfile does not exist");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("user info does not exist");
            }

            // extract userId based on provider
            String oauthId = switch (provider) {
                case "naver" -> ((Map<String, Object>) userProfile.get("response")).get("id").toString();
                case "kakao" -> userProfile.get("id").toString();
                case "google" -> userProfile.get("sub").toString();
                default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
            };

            if (oauthId == null) {
                log.error("id를 파싱하는데 문제가 생긴 것 같습니다 ㅠㅠ");
            }

            // Redis에 refresh token 저장
            redisService.saveRToken(oauthId, provider, refreshToken);

            // Oauth2 사용자 확인 후 저장 또는 업데이트 하기
            users user = saveOrUpdateOauth2User(provider, oauthId, userProfile);

            // 서버 자체 access_token 생성
            tokenDto accessToken = jwtUtility.buildToken(user.getUserId(), user.getNickname(), Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name())));

            loginResponse(response, accessToken.getAccessToken(), refreshToken, provider);

            //return ResponseEntity.ok(userProfile);
            return ResponseEntity.ok(Map.of(
                    "access_token", accessToken.getAccessToken(),
                    "userProfile", userProfile
            ));

        } catch (HttpClientErrorException e){
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST && e.getResponseBodyAsString().contains("invalid_grant")) {
                log.error("{} 로그인 실패 - 재 사용된 인증 코드 입니다. ", provider);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("재사용된 인증 코드입니다.");
            }
            log.error("{} 로그인 실패!", provider, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(provider + "로그인 실패!");
        } catch (Exception e) {
            log.error("{} 로그인 실패! ", provider, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(provider + " 로그인 실패!");
        }
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
}
