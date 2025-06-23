package com.authentication.auth.service.oauth2;

import com.authentication.auth.configuration.oauth2.OauthProperties;
import com.authentication.auth.configuration.token.JwtUtility;
import com.authentication.auth.domain.AuthProvider;
import com.authentication.auth.domain.User;
import com.authentication.auth.domain.UserAuthentication;
import com.authentication.auth.dto.token.TokenDto;
import com.authentication.auth.dto.users.LoginResponse;
import com.authentication.auth.repository.AuthProviderRepository;
import com.authentication.auth.repository.UserAuthenticationRepository;
import com.authentication.auth.repository.UserRepository;
import com.authentication.auth.service.redis.RedisService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Oauth2ServiceTest {

    @Mock
    private OauthProperties oauthProperties;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RedisService redisService;
    @Mock
    private JwtUtility jwtUtility;
    @Mock
    private AuthProviderRepository authProviderRepository;
    @Mock
    private UserAuthenticationRepository userAuthenticationRepository;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private PasswordEncoder passwordEncoder;

    private Oauth2Service oauth2Service;

    private User testUser;
    private AuthProvider kakaoAuthProvider;
    private OauthProperties.Client kakaoProps;

    @BeforeEach
    void setUp() {
        Oauth2Service realOauth2Service = new Oauth2Service(
                oauthProperties,
                userRepository,
                redisService,
                jwtUtility,
                authProviderRepository,
                userAuthenticationRepository,
                restTemplate,
                passwordEncoder
        );
        oauth2Service = spy(realOauth2Service);

        testUser = User.builder().id(1L).nickname("testuser").email("test@example.com").userRole("USER").isActive("ACTIVE").build();
        kakaoAuthProvider = AuthProvider.builder().id(1).providerName("KAKAO").build();
        kakaoProps = new OauthProperties.Client("kakao-client-id", "kakao-client-secret", "http://localhost/kakao-redirect");
    }

    @Test
    @DisplayName("saveOrUpdateOauth2User - 새로운 사용자 생성")
    void saveOrUpdateOauth2User_NewUser() {
        // Given
        String provider = "KAKAO";
        String oauthId = "kakao123";
        Map<String, Object> userProfile = Map.of("email", "newuser@example.com", "nickname", "new_user_nickname");

        when(userAuthenticationRepository.findByAuthProvider_ProviderNameAndSocialId(provider.toUpperCase(), oauthId))
                .thenReturn(Optional.empty());
        when(userRepository.existsByNickname("new_user_nickname")).thenReturn(false);
        when(authProviderRepository.findByProviderName(provider.toUpperCase())).thenReturn(Optional.of(kakaoAuthProvider));

        User savedUser = User.builder().id(2L).nickname("new_user_nickname").email("newuser@example.com").build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        User resultUser = oauth2Service.saveOrUpdateOauth2User(provider, oauthId, userProfile);

        // Then
        assertNotNull(resultUser);
        assertEquals(savedUser.getId(), resultUser.getId());
        assertEquals(savedUser.getNickname(), resultUser.getNickname());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("new_user_nickname", userCaptor.getValue().getNickname());

        ArgumentCaptor<UserAuthentication> userAuthCaptor = ArgumentCaptor.forClass(UserAuthentication.class);
        verify(userAuthenticationRepository).save(userAuthCaptor.capture());
        assertEquals(oauthId, userAuthCaptor.getValue().getSocialId());
        assertEquals(savedUser.getId(), userAuthCaptor.getValue().getUser().getId());
    }

    @Test
    @DisplayName("saveOrUpdateOauth2User - 기존 사용자 업데이트")
    void saveOrUpdateOauth2User_ExistingUser() {
        // Given
        String provider = "KAKAO";
        String oauthId = "existing_kakao123";
        String newEmail = "updated_email@example.com";
        Map<String, Object> userProfile = Map.of("email", newEmail);

        UserAuthentication existingAuth = UserAuthentication.builder()
                .user(testUser)
                .authProvider(kakaoAuthProvider)
                .socialId(oauthId)
                .build();

        when(userAuthenticationRepository.findByAuthProvider_ProviderNameAndSocialId(provider.toUpperCase(), oauthId))
                .thenReturn(Optional.of(existingAuth));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User resultUser = oauth2Service.saveOrUpdateOauth2User(provider, oauthId, userProfile);

        // Then
        assertNotNull(resultUser);
        assertEquals(testUser.getId(), resultUser.getId());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(newEmail, userCaptor.getValue().getEmail());
    }

    @Test
    @DisplayName("handleOauth2Login - 카카오 로그인 성공")
    void handleOauth2Login_Kakao_Success() {
        // Given
        String provider = "KAKAO";
        String tempCode = "kakaoTempCode";
        Map<String, String> requestBody = Map.of("tempCode", tempCode);
        HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);

        Map<String, String> kakaoTokens = Map.of("access_token", "kakao_access_token", "refresh_token", "kakao_refresh_token");
        Map<String, Object> userProfile = Map.of("id", "kakao123", "email", "kakao@example.com");
        TokenDto appTokenDto = new TokenDto("app_access_token", null);

        doReturn(kakaoTokens).when(oauth2Service).getKakaoTokens(tempCode);
        doReturn(userProfile).when(oauth2Service).getKakaoUserProfile("kakao_access_token");
        doReturn(testUser).when(oauth2Service).saveOrUpdateOauth2User(anyString(), anyString(), anyMap());
        Collection<GrantedAuthority> expectedAuthorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + testUser.getUserRole()));
        doReturn(appTokenDto).when(jwtUtility).buildToken(eq(testUser.getNickname()), eq(expectedAuthorities));
        doNothing().when(redisService).saveRToken(anyString(), anyString(), anyString());

        // When
        LoginResponse loginResponse = oauth2Service.handleOauth2Login(requestBody, httpServletResponse, provider);

        // Then
        assertNotNull(loginResponse);
        assertEquals(appTokenDto.accessToken(), loginResponse.accessToken());
        assertEquals(testUser.getNickname(), loginResponse.user().nickname());

        verify(oauth2Service).saveOrUpdateOauth2User(eq(provider), eq("kakao123"), eq(userProfile));
        verify(redisService).saveRToken(eq("kakao123"), eq(provider), eq("kakao_refresh_token"));

        verify(jwtUtility).buildToken(eq(testUser.getNickname()), eq(expectedAuthorities));
    }

    @Test
    @DisplayName("getKakaoTokens - 성공")
    void getKakaoTokens_Success() {
        // Given
        when(oauthProperties.kakao()).thenReturn(kakaoProps);
        String tempCode = "testCode";
        Map<String, Object> mockApiResponse = new HashMap<>();
        mockApiResponse.put("access_token", "mock_access_token");
        mockApiResponse.put("refresh_token", "mock_refresh_token");
        ResponseEntity<Map<String, Object>> mockResponse = ResponseEntity.ok(mockApiResponse);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(mockResponse);

        // When
        Map<String, String> tokens = oauth2Service.getKakaoTokens(tempCode);

        // Then
        assertNotNull(tokens);
        assertEquals("mock_access_token", tokens.get("access_token"));
        assertEquals("mock_refresh_token", tokens.get("refresh_token"));
    }

    @Test
    @DisplayName("getKakaoUserProfile - 성공")
    void getKakaoUserProfile_Success() {
        // Given
        String accessToken = "test_access_token";
        Map<String, Object> mockApiResponse = new HashMap<>();
        mockApiResponse.put("id", "12345");
        Map<String, Object> properties = new HashMap<>();
        properties.put("nickname", "test_nickname");
        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("email", "test@example.com");
        mockApiResponse.put("properties", properties);
        mockApiResponse.put("kakao_account", kakaoAccount);
        ResponseEntity<Map<String, Object>> mockResponse = ResponseEntity.ok(mockApiResponse);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(mockResponse);

        // When
        Map<String, Object> userProfile = oauth2Service.getKakaoUserProfile(accessToken);

        // Then
        assertNotNull(userProfile);
        assertEquals("12345", userProfile.get("id"));
        assertEquals("test_nickname", userProfile.get("nickname"));
        assertEquals("test@example.com", userProfile.get("email"));
    }
}
