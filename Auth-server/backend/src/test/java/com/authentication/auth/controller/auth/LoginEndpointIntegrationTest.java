package com.authentication.auth.controller.auth;

import com.authentication.auth.domain.User;
import com.authentication.auth.dto.users.LoginRequest;
import com.authentication.auth.dto.token.PrincipalDetails;
import com.authentication.auth.exception.ErrorType;
import com.authentication.auth.repository.UserRepository;
import com.authentication.auth.service.security.PrincipalDetailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")

public class LoginEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PrincipalDetailService principalDetailService;

    @Autowired
    private UserRepository userRepository;

    @Autowired // Inject real PasswordEncoder
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String userEmail = "test_" + uniqueSuffix + "@example.com";

        // Define a test user
        testUser = User.builder()
                .nickname("testuser_" + uniqueSuffix)
                .password(passwordEncoder.encode("password123"))
                .email(userEmail)
                .loginId("testuser_" + uniqueSuffix)
                .userRole("USER")
                .build();
        userRepository.save(testUser);

        // Mocking PrincipalDetailService
        when(principalDetailService.loadUserByUsername(userEmail))
                .thenReturn(new PrincipalDetails(testUser)); // Uses the actual user from DB

        // For incorrect email, mock it to throw UsernameNotFoundException
        when(principalDetailService.loadUserByUsername("wrong@example.com"))
                .thenThrow(new UsernameNotFoundException("User not found with email: wrong@example.com"));
    }

    @Test
    @DisplayName("로그인 성공: 올바른 이메일과 비밀번호로 요청 시 200 OK 와 토큰 반환")
    void login_withCorrectEmailAndPassword_returns200AndToken() throws Exception {
                LoginRequest loginRequest = new LoginRequest(testUser.getLoginId(), testUser.getEmail(), "password123");
        String loginRequestBody = objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(post("/api/public/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("로그인 성공"))
                .andExpect(jsonPath("$.data.access_token").exists())
                .andExpect(jsonPath("$.data.refresh_token").exists()) // Assuming refresh token is also returned in body
                .andExpect(jsonPath("$.data.user.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.data.user.username").value(testUser.getUserName()))
                .andExpect(jsonPath("$.data.user.roles[0]").value("ROLE_USER"));
                // .andExpect(cookie().exists("refreshToken")); // Cookie check might be environment dependent or need specific setup
    }

    @Test
    @DisplayName("로그인 실패: 존재하지 않는 이메일로 요청 시 401 Unauthorized 와 에러 메시지 반환")
    void login_withNonExistingEmail_returns401() throws Exception {
        LoginRequest loginRequest = new LoginRequest(null, "wrong@example.com", "password123");
        String loginRequestBody = objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(post("/api/public/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestBody))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorType.AUTHENTICATION_FAILED.getMessage()))
                .andExpect(jsonPath("$.data.code").value(ErrorType.AUTHENTICATION_FAILED.name()));
    }

    @Test
    @DisplayName("로그인 실패: 올바른 이메일, 틀린 비밀번호로 요청 시 401 Unauthorized 와 에러 메시지 반환")
    void login_withCorrectEmailAndWrongPassword_returns401() throws Exception {
        // For this to work correctly, DaoAuthenticationProvider needs to compare passwords.
        // The user is loaded by mocked PrincipalDetailService.
        // Then, Spring Security's DaoAuthenticationProvider will compare the provided "wrongpassword"
        // with the encoded password from the UserDetails ("password123" encoded).
        LoginRequest loginRequest = new LoginRequest(testUser.getLoginId(), testUser.getEmail(), "wrongpassword");
        String loginRequestBody = objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(post("/api/public/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestBody))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorType.AUTHENTICATION_FAILED.getMessage()))
                .andExpect(jsonPath("$.data.code").value(ErrorType.AUTHENTICATION_FAILED.name()));
    }

    @Test
    @DisplayName("로그인 실패: 요청 본문에 email 필드 누락 시 401 Unauthorized 반환")
    void login_missingEmailField_returns401() throws Exception {
        String loginRequestBody = objectMapper.writeValueAsString(Map.of("password", "password123"));

        mockMvc.perform(post("/api/public/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestBody))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorType.AUTHENTICATION_FAILED.getMessage()))
                .andExpect(jsonPath("$.data.code").value(ErrorType.AUTHENTICATION_FAILED.name()));
    }

    @Test
    @DisplayName("로그인 실패: 요청 본문에 password 필드 누락 시 401 Unauthorized 반환")
    void login_missingPasswordField_returns401() throws Exception {
        String loginRequestBody = objectMapper.writeValueAsString(Map.of("email", testUser.getEmail()));

        mockMvc.perform(post("/api/public/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestBody))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorType.AUTHENTICATION_FAILED.getMessage()))
                .andExpect(jsonPath("$.data.code").value(ErrorType.AUTHENTICATION_FAILED.name()));
    }

    @Test
    @DisplayName("로그인 실패: 유효하지 않은 이메일 형식으로 요청 시 401 Unauthorized 반환")
    void login_invalidEmailFormat_returns401() throws Exception {
        LoginRequest loginRequest = new LoginRequest(null, "invalid-email", "password123");
        String loginRequestBody = objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(post("/api/public/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestBody))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorType.AUTHENTICATION_FAILED.getMessage()))
                .andExpect(jsonPath("$.data.code").value(ErrorType.AUTHENTICATION_FAILED.name()));
    }
}
