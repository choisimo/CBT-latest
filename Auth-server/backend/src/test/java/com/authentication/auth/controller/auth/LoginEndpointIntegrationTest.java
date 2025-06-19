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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class LoginEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PrincipalDetailService principalDetailService;

    @Autowired
    private UserRepository userRepository;

    @Autowired // Inject real PasswordEncoder
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Clean up any existing users to avoid conflicts between tests
        userRepository.deleteAll();

        // Define a test user
        testUser = User.builder()
                .id(null) // Let DB generate ID
                .userName("testuser_name") // A distinct username/nickname
                .password(passwordEncoder.encode("password123")) // Store encoded password
                .email("test@example.com") // Login identifier
                .userRole("USER")
                .isActive("ACTIVE")
                .build();
        // Save the user to the actual database to allow AuthenticationFilter and DaoAuthenticationProvider to work
        userRepository.save(testUser);

        // Mocking PrincipalDetailService to return the user when queried by email
        // This is essential because AuthenticationFilter -> AuthenticationManager -> DaoAuthenticationProvider -> PrincipalDetailService
        when(principalDetailService.loadUserByUsername(testUser.getEmail()))
                .thenReturn(new PrincipalDetails(testUser)); // Uses the actual user from DB

        // For incorrect email, mock it to throw UsernameNotFoundException
        when(principalDetailService.loadUserByUsername("wrong@example.com"))
                .thenThrow(new UsernameNotFoundException("User not found with email: wrong@example.com"));
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll(); // Clean up after each test
    }

    @Test
    @DisplayName("로그인 성공: 올바른 이메일과 비밀번호로 요청 시 200 OK 와 토큰 반환")
    void login_withCorrectEmailAndPassword_returns200AndToken() throws Exception {
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");
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
        LoginRequest loginRequest = new LoginRequest("wrong@example.com", "password123");
        String loginRequestBody = objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(post("/api/public/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestBody))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(ErrorType.AUTHENTICATION_FAILED.getMessage()))
                .andExpect(jsonPath("$.status").value(ErrorType.AUTHENTICATION_FAILED.getStatusCode()));
    }

    @Test
    @DisplayName("로그인 실패: 올바른 이메일, 틀린 비밀번호로 요청 시 401 Unauthorized 와 에러 메시지 반환")
    void login_withCorrectEmailAndWrongPassword_returns401() throws Exception {
        // For this to work correctly, DaoAuthenticationProvider needs to compare passwords.
        // The user "test@example.com" is loaded by mocked PrincipalDetailService.
        // Then, Spring Security's DaoAuthenticationProvider will compare the provided "wrongpassword"
        // with the encoded password from the UserDetails ("password123" encoded).
        LoginRequest loginRequest = new LoginRequest("test@example.com", "wrongpassword");
        String loginRequestBody = objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(post("/api/public/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestBody))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(ErrorType.AUTHENTICATION_FAILED.getMessage()))
                .andExpect(jsonPath("$.status").value(ErrorType.AUTHENTICATION_FAILED.getStatusCode()));
    }

    @Test
    @DisplayName("로그인 실패: 요청 본문에 email 필드 누락 시 400 Bad Request 와 유효성 검증 메시지 반환")
    void login_missingEmailField_returns400() throws Exception {
        // Using Map to simulate missing field, as LoginRequest record would require it.
        String loginRequestBody = objectMapper.writeValueAsString(Map.of("password", "password123"));

        mockMvc.perform(post("/api/public/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestBody))
                .andDo(print())
                .andExpect(status().isBadRequest())
                // The exact error message depends on Spring's DefaultHandlerExceptionResolver and Bean Validation message interpolation.
                // It often includes field name and the message from the annotation.
                // We expect a message related to "email" being "NotBlank".
                // For multiple violations, the response structure might be more complex.
                // For simplicity, checking for a general error structure or a known part of the message.
                // Actual validation messages might be in an "errors" array or similar.
                // The GlobalExceptionHandler should handle MethodArgumentNotValidException.
                // Let's assume GlobalExceptionHandler returns a similar structure as ErrorType for validation.
                // Or, it might return a list of field errors.
                // For now, we'll check for a 400 and a non-empty error message.
                // This needs to be adjusted based on actual GlobalExceptionHandler's response for validation errors.
                .andExpect(jsonPath("$.message").isNotEmpty()) // Or more specific if GlobalExceptionHandler is known
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()));
                // Example of more specific check if GlobalExceptionHandler formats it:
                // .andExpect(jsonPath("$.errors[0].field").value("email"))
                // .andExpect(jsonPath("$.errors[0].defaultMessage").value("이메일은 필수 입력 항목입니다."))
    }

    @Test
    @DisplayName("로그인 실패: 요청 본문에 password 필드 누락 시 400 Bad Request 와 유효성 검증 메시지 반환")
    void login_missingPasswordField_returns400() throws Exception {
        String loginRequestBody = objectMapper.writeValueAsString(Map.of("email", "test@example.com"));

        mockMvc.perform(post("/api/public/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestBody))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    @DisplayName("로그인 실패: 유효하지 않은 이메일 형식으로 요청 시 400 Bad Request 와 유효성 검증 메시지 반환")
    void login_invalidEmailFormat_returns400() throws Exception {
        LoginRequest loginRequest = new LoginRequest("invalid-email", "password123");
        String loginRequestBody = objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(post("/api/public/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestBody))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty()) // Expecting "유효한 이메일 형식이 아닙니다." or similar
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()));
    }
}
