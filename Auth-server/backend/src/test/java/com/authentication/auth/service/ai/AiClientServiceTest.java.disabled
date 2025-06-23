package com.authentication.auth.service.ai;

import com.authentication.auth.dto.diary.AiChatRequest;
import com.authentication.auth.dto.diary.AiChatResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * AiClientService 유닛 테스트.
 */
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class AiClientServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @SuppressWarnings("rawtypes")
    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AiClientService aiClientService;

    @BeforeEach
    void setUp() {
        // AiClientService의 aiServerUrl 필드에 기본값 설정 (리플렉션 또는 생성자 주입 방식 고려 가능)
        // 여기서는 테스트의 단순화를 위해 직접 주입하지 않음. 실제 환경에서는 @Value 주입 필요.
        // private 필드 직접 설정은 좋은 방법은 아니지만, 테스트를 위해 임시 사용 가능
        // ReflectionTestUtils.setField(aiClientService, "aiServerUrl", "http://localhost:8000");
    }

    @Test
    @DisplayName("다이어리 분석 성공 - 긍정적 내용")
    void analyzeDiary_Success_PositiveContent() throws JsonProcessingException {
        // Given
        String diaryContent = "오늘 날씨가 정말 좋아서 기분이 상쾌했다.";
        AiChatResponse aiResponse = new AiChatResponse("{\"isNegative\": false, \"alternativeThought\": null}");
        JsonNode jsonNode = new ObjectMapper().readTree(aiResponse.response());

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any(AiChatRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(AiChatResponse.class)).thenReturn(Mono.just(aiResponse));
        when(objectMapper.readTree(aiResponse.response())).thenReturn(jsonNode);

        // When
        Mono<AiClientService.DiaryAnalysisResult> resultMono = aiClientService.analyzeDiary(diaryContent);

        // Then
        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertThat(result.isNegative()).isFalse();
                    assertThat(result.alternativeThought()).isNull();
                    assertThat(result.error()).isNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("다이어리 분석 성공 - 부정적 내용 및 대안적 사고 포함")
    void analyzeDiary_Success_NegativeContentWithAlternative() throws JsonProcessingException {
        // Given
        String diaryContent = "오늘 너무 우울해서 아무것도 하기 싫었다.";
        AiChatResponse aiResponse = new AiChatResponse("{\"isNegative\": true, \"alternativeThought\": \"하지만 내일은 기분이 나아질 수 있다고 생각해보자.\"}");
        JsonNode jsonNode = new ObjectMapper().readTree(aiResponse.response());

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any(AiChatRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(AiChatResponse.class)).thenReturn(Mono.just(aiResponse));
        when(objectMapper.readTree(aiResponse.response())).thenReturn(jsonNode);

        // When
        Mono<AiClientService.DiaryAnalysisResult> resultMono = aiClientService.analyzeDiary(diaryContent);

        // Then
        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertThat(result.isNegative()).isTrue();
                    assertThat(result.alternativeThought()).isEqualTo("하지만 내일은 기분이 나아질 수 있다고 생각해보자.");
                    assertThat(result.error()).isNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("다이어리 분석 실패 - AI 서버 오류")
    void analyzeDiary_Failure_AiServerError() {
        // Given
        String diaryContent = "AI 서버 테스트 중 오류 발생 예상.";
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any(AiChatRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(AiChatResponse.class)).thenReturn(Mono.error(new RuntimeException("AI 서버 통신 오류")));

        // When
        Mono<AiClientService.DiaryAnalysisResult> resultMono = aiClientService.analyzeDiary(diaryContent);

        // Then
        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertThat(result.isNegative()).isFalse(); // onErrorReturn 기본값
                    assertThat(result.alternativeThought()).isNull(); // onErrorReturn 기본값
                    assertThat(result.error()).isEqualTo("AI 분석 서비스 오류");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("다이어리 분석 실패 - 응답 파싱 오류")
    void analyzeDiary_Failure_ResponseParsingError() throws JsonProcessingException {
        // Given
        String diaryContent = "AI 응답이 이상하게 올 경우.";
        AiChatResponse aiResponse = new AiChatResponse("이것은 유효한 JSON이 아닙니다.");

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any(AiChatRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(AiChatResponse.class)).thenReturn(Mono.just(aiResponse));
        when(objectMapper.readTree(aiResponse.response())).thenThrow(new JsonProcessingException("파싱 오류"){});

        // When
        Mono<AiClientService.DiaryAnalysisResult> resultMono = aiClientService.analyzeDiary(diaryContent);

        // Then
        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertThat(result.isNegative()).isFalse(); // 파싱 실패 시 isNegative 기본값
                    assertThat(result.alternativeThought()).isEqualTo("이것은 유효한 JSON이 아닙니다."); // 원본 응답을 대안 사고로 사용
                    assertThat(result.error()).isNull();
                })
                .verifyComplete();
    }
}
