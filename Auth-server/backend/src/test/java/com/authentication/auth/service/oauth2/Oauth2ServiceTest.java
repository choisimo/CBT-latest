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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List; 

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
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

    @Spy 
    @InjectMocks
    private Oauth2Service oauth2Service;

    private User testUser;
    private AuthProvider kakaoAuthProvider;
        private OauthProperties.Client kakaoProps;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(oauth2Service, "domain", "test.com");

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
        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("id", oauthId); 
        Map<String, Object> kakaoAccountMap = new HashMap<>();
        kakaoAccountMap.put("email", "newuser@example.com");
        userProfile.put("kakao_account", kakaoAccountMap);
        Map<String, Object> propertiesMap = new HashMap<>();
        propertiesMap.put("nickname", "NewKakaoUser");
        userProfile.put("properties", propertiesMap);


        when(userAuthenticationRepository.findByAuthProvider_ProviderNameAndSocialId(provider, oauthId))
                .thenReturn(Optional.empty());
                when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L); 
            return user;
        });
        when(authProviderRepository.findByProviderName(provider)).thenReturn(Optional.of(kakaoAuthProvider));
        when(userAuthenticationRepository.save(any(UserAuthentication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User resultUser = oauth2Service.saveOrUpdateOauth2User(provider, oauthId, userProfile);

        // Then
        assertThat(resultUser).isNotNull();
        assertThat(resultUser.getEmail()).isEqualTo("newuser@example.com");
        assertThat(resultUser.getUserName()).isEqualTo("newuser@example.com"); 
        verify(userRepository).save(any(User.class));
        verify(userAuthenticationRepository).save(any(UserAuthentication.class));
    }

    @Test
    @DisplayName("saveOrUpdateOauth2User - 기존 사용자 업데이트")
    void saveOrUpdateOauth2User_ExistingUser() {
        // Given
        String provider = "KAKAO";
        String oauthId = "kakao123"; 
        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("id", oauthId); 
        Map<String, Object> kakaoAccountMap = new HashMap<>();
        kakaoAccountMap.put("email", "updated@example.com");
        userProfile.put("kakao_account", kakaoAccountMap);


        UserAuthentication existingAuth = UserAuthentication.builder()
                .id(new UserAuthenticationId(testUser.getId(), kakaoAuthProvider.getId()))
                .user(testUser)
                .authProvider(kakaoAuthProvider)
                .socialId(oauthId) 
                .build();

        when(userAuthenticationRepository.findByAuthProvider_ProviderNameAndSocialId(provider, oauthId))
                .thenReturn(Optional.of(existingAuth));
        when(authProviderRepository.findByProviderName(provider)).thenReturn(Optional.of(kakaoAuthProvider));
        when(userAuthenticationRepository.findById(any(UserAuthenticationId.class))).thenReturn(Optional.of(existingAuth));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));


        // When
        User resultUser = oauth2Service.saveOrUpdateOauth2User(provider, oauthId, userProfile);

        // Then
        assertThat(resultUser).isNotNull();
        assertThat(resultUser.getEmail()).isEqualTo("updated@example.com");
        verify(userRepository).save(testUser); 
        verify(userAuthenticationRepository, never()).save(any(UserAuthentication.class)); 
    }
    
    @Test
    @DisplayName("handleOauth2Login - Kakao 성공")
    void handleOauth2Login_Kakao_Success() {
        // Given
        when(oauthProperties.kakao()).thenReturn(kakaoProps);
        String provider = "kakao";
        String tempCode = "kakaoTempCode";
        Map<String, String> requestBody = Map.of("tempCode", tempCode);
        HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);

        Map<String, String> oauthTokens = Map.of("access_token", "kakao_access_token", "refresh_token", "kakao_refresh_token");
        doReturn(oauthTokens).when(oauth2Service).getKakaoTokens(tempCode);
        
        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("id", "kakao123"); 
        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("email", "kakao@example.com");
        userProfile.put("kakao_account", kakaoAccount);
        doReturn(userProfile).when(oauth2Service).getKakaoUserProfile("kakao_access_token");

                User user = User.builder().id(1L).nickname("kakao@example.com").userRole("USER").isActive("ACTIVE").build();
        doReturn(user).when(oauth2Service).saveOrUpdateOauth2User(eq(provider), eq("kakao123"), anyMap());
        
        TokenDto appTokenDto = new TokenDto("app_access_token", "app_refresh_token_unused");
        when(jwtUtility.buildToken(eq("kakao@example.com"), any(List.class))).thenReturn(appTokenDto);
        
        doNothing().when(redisService).saveRToken(eq("kakao123"), eq(provider), eq("kakao_refresh_token"));

        // When
        LoginResponse response = oauth2Service.handleOauth2Login(requestBody, httpServletResponse, provider);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("app_access_token");
        
        verify(redisService).saveRToken("kakao123", provider, "kakao_refresh_token");
        verify(httpServletResponse).addHeader("Authorization", "Bearer app_access_token");
        verify(httpServletResponse).addCookie(any()); 
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
        ResponseEntity<Map<String, Object>> responseEntity = new ResponseEntity<>(mockApiResponse, HttpStatus.OK);

        when(restTemplate.exchange(
            eq("https://kauth.kakao.com/oauth/token"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class))
        ).thenReturn(responseEntity);

        // When
        Map<String, String> tokens = oauth2Service.getKakaoTokens(tempCode);

        // Then
        assertThat(tokens).isNotNull();
        assertThat(tokens.get("access_token")).isEqualTo("mock_access_token");
        assertThat(tokens.get("refresh_token")).isEqualTo("mock_refresh_token");
    }

    @Test
    @DisplayName("getKakaoUserProfile - 성공")
    void getKakaoUserProfile_Success() {
        // Given
        String accessToken = "testAccessToken";
        Map<String, Object> mockKakaoResponse = new HashMap<>();
        mockKakaoResponse.put("id", 12345L); 
        Map<String, Object> properties = new HashMap<>();
        properties.put("nickname", "Test User");
        mockKakaoResponse.put("properties", properties);
        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("email", "test@example.com");
        mockKakaoResponse.put("kakao_account", kakaoAccount);
        
        ResponseEntity<Map<String, Object>> responseEntity = new ResponseEntity<>(mockKakaoResponse, HttpStatus.OK);

        when(restTemplate.exchange(
            eq("https://kapi.kakao.com/v2/user/me"),
            eq(HttpMethod.POST), 
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class))
        ).thenReturn(responseEntity);
        
        // When
        Map<String, Object> userProfile = oauth2Service.getKakaoUserProfile(accessToken);

        // Then
        assertThat(userProfile).isNotNull();
        assertThat(userProfile.get("id")).isEqualTo("12345"); 
        assertThat(userProfile.get("nickname")).isEqualTo("Test User");
        assertThat(userProfile.get("email")).isEqualTo("test@example.com");
    }
}
