package com.authentication.auth.controller;

import com.authentication.auth.dto.users.JoinRequest; // Adjusted DTO import
import com.authentication.auth.service.redis.RedisService;
import com.authentication.auth.service.users.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional // Each test will roll back transactions
class UsersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Mock dependent services to isolate controller validation logic for this test
    @MockBean
    private UserService userService;

    @MockBean
    private RedisService redisService;
    // EmailService is not directly used by the join method's validation part but might be if other methods were tested.

    @DisplayName("회원가입 시 잘못된 입력값에 대해 400 Bad Request를 반환한다.")
    @ParameterizedTest(name = "[{index}] {0}") // Name the tests for better readability
    @MethodSource("provideInvalidJoinRequests")
    void join_with_invalid_input_returns_bad_request(String testName, JoinRequest joinRequest) throws Exception {
        // given
        String requestBody = objectMapper.writeValueAsString(joinRequest);

        // Mocking redisService to always return true for email code check,
        // as we are testing DTO validation here, not the email code logic.
        when(redisService.checkEmailCode(anyString(), anyString())).thenReturn(true);

        // when & then
        mockMvc.perform(post("/api/public/join") // Corrected endpoint
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    private static Stream<Arguments> provideInvalidJoinRequests() {
        // Based on JoinRequest(userPw, email, emailAuthCode)
        // Validations:
        // userPw: @NotBlank, @Size(min = 8)
        // email: @Email (implies @NotBlank essentially)
        // emailAuthCode: @NotBlank
        return Stream.of(
                Arguments.of("비밀번호 null", new JoinRequest(null, "test@example.com", "123456")),
                Arguments.of("비밀번호 공백", new JoinRequest("", "test@example.com", "123456")),
                Arguments.of("비밀번호 너무 짧음 (7자)", new JoinRequest("pass123", "test@example.com", "123456")),
                Arguments.of("이메일 null", new JoinRequest("password123", null, "123456")),
                Arguments.of("이메일 공백", new JoinRequest("password123", "", "123456")),
                Arguments.of("이메일 형식 아님", new JoinRequest("password123", "invalid-email", "123456")),
                Arguments.of("이메일 인증 코드 null", new JoinRequest("password123", "test@example.com", null)),
                Arguments.of("이메일 인증 코드 공백", new JoinRequest("password123", "test@example.com", ""))
        );
    }

    // Example of a test for a successful case (requires more mocking or actual service logic)
    // @Test
    // @DisplayName("올바른 입력값으로 회원가입 시 200 OK를 반환한다.")
    // void join_with_valid_input_returns_ok() throws Exception {
    //     // given
    //     JoinRequest validRequest = new JoinRequest("password123Valid", "valid@example.com", "validCode");
    //     String requestBody = objectMapper.writeValueAsString(validRequest);

    //     when(redisService.checkEmailCode("valid@example.com", "validCode")).thenReturn(true);
    //     when(userService.join(any(JoinRequest.class))).thenReturn(new com.authentication.auth.domain.User()); // Mock user creation
    //     // Mock activateUser if necessary
    //     doNothing().when(userService).activateUser(anyString());


    //     // when & then
    //     mockMvc.perform(post("/api/public/join")
    //                     .contentType(MediaType.APPLICATION_JSON)
    //                     .content(requestBody))
    //             .andExpect(status().isOk())
    //             .andDo(print());
    // }
}
