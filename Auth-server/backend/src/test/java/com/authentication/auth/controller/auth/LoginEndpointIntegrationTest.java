package com.authentication.auth.controller.auth;

import com.authentication.auth.domain.User;
import com.authentication.auth.dto.token.PrincipalDetails;
import com.authentication.auth.service.security.PrincipalDetailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // To ensure test isolation and rollback
public class LoginEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PrincipalDetailService principalDetailService;
    
    // We need a real PasswordEncoder bean for the AuthenticationManager to work correctly
    // If not already configured as a bean in a test configuration, it might be needed.
    // However, SpringBootTest should load the main application context which includes it.

    private User testUser;
    private PrincipalDetails principalDetails;
    private final PasswordEncoder passwordEncoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        // Define a test user
        testUser = User.builder()
                .id(1L)
                .userName("testuser") // This is the login ID
                .password(passwordEncoder.encode("password"))
                .email("test@example.com") // User's email
                // .userName("Test User") // This was ambiguous, userName is the login ID. Let's assume display name is same as login ID for now or not explicitly set in User for this test.
                .userRole("USER")
                .build();

        // Define PrincipalDetails for the test user
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + testUser.getUserRole()));
        principalDetails = new PrincipalDetails(testUser);

        // Mock PrincipalDetailService
        // When loadUserByUsername is called with "testuser", return our PrincipalDetails
        when(principalDetailService.loadUserByUsername("testuser")).thenReturn(principalDetails);
    }

    @Test
    @DisplayName("로그인 성공 시 사용자 정보와 토큰을 포함하여 응답한다")
    void login_success_returnsUserInfoAndToken() throws Exception {
        // Given: Correct login credentials
        // The password in the request body should be the raw password.
        // Spring Security's DaoAuthenticationProvider will encode it and match with the stored encoded password.
        // For this test, AuthenticationManager uses the UserDetailsService (mocked principalDetailService).
        // The DaoAuthenticationProvider will compare the provided password (after encoding) with the one from UserDetails.
        // So, the mock for principalDetailService.loadUserByUsername("testuser") should return PrincipalDetails
        // which has the "encodedPassword". The password encoder bean will be used.
        // For simplicity of test, we ensure the username matches and mock UserDetailsService.
        // The actual password "password" will be handled by the AuthenticationManager.
        String loginRequestBody = objectMapper.writeValueAsString(
            Map.of("username", "testuser", "password", "password") // Raw password
        );

        // When & Then: Perform login request and verify response
        mockMvc.perform(post("/api/public/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("로그인 성공"))
                .andExpect(jsonPath("$.data.access_token").exists())
                .andExpect(jsonPath("$.data.access_token").isString())
                .andExpect(jsonPath("$.data.user.userId").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.data.user.username").value(testUser.getUserName())) // This should be the login ID "testuser"
                .andExpect(jsonPath("$.data.user.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.data.user.roles[0]").value("ROLE_" + testUser.getUserRole()))
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true));
    }
}
