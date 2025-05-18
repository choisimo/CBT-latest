package com.career_block.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.authentication.auth.DTO.token.principalDetails;
import com.authentication.auth.service.sse.sseService;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class sseController {

    private final sseService sseService;


    /**
     * 클라이언트가 SSE 스트림에 구독하기 위한 엔드포인트
     * 예: GET /sse/subscribe/{userId}
     *
     * @param userId 사용자의 고유 ID
     * @return SseEmitter 객체
     */
    @Operation(summary = "SSE 구독", description = "서버에서 SSE 이벤트를 구독합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "구독 성공", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json"))
    })
    @GetMapping(value = "/protected/sse/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> subscribeSse(@AuthenticationPrincipal principalDetails principalDetails,
                                                   @RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "") String lastEventId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        String userId = principalDetails.getUserId();
        boolean saved = sseService.saveSseEmitter(userId, emitter);
        if (!saved) {
            log.error("Failed to save SseEmitter for userId : {}", userId);
            emitter.completeWithError(new IllegalStateException("Failed to save SseEmitter for userId : " + userId));
        }

        // Initial Data 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("INIT")
                    .data("Subscription successful"));
        } catch (Exception e) {
            log.error("Error sending initial event to userId : {}", userId, e);
            sseService.removeEmitter(userId, emitter);
            emitter.completeWithError(e);
        }

        return ResponseEntity.status(HttpStatus.OK).body(emitter);
    }



    /**
     * 더미 데이터를 특정 사용자에게 전송하는 엔드포인트
     * 예: POST /sse/dummyData/{user_id}
     *
     * @param principalDetails 인증된 사용자 정보
     * @param userId 대상 사용자의 ID
     * @param payload 전송할 데이터
     * @return ResponseEntity
     */
    @Operation(summary = "더미 데이터 전송", description = "특정 사용자에게 더미 데이터를 전송합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "데이터 전송 성공", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/public/dummyData/{user_id}")
    public ResponseEntity<?> sseDummyData(@AuthenticationPrincipal principalDetails principalDetails,
                                          @PathVariable("user_id") String userId,
                                          @RequestBody Map<String, String> payload){
        String message = payload.get("message");
        if (message == null || message.isEmpty()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Message is required");
        }

        sseService.sendEventToUser(userId, message);
        return ResponseEntity.ok().build();
    }


}
