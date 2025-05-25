package com.authentication.auth.controller;

import com.authentication.auth.api.docs.SseApi;
import com.authentication.auth.dto.token.PrincipalDetails;
import com.authentication.auth.service.sse.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SseController implements SseApi {

    private final SseService sseService;

    /**
     * 클라이언트가 SSE 스트림에 구독하기 위한 엔드포인트
     * 예: GET /sse/subscribe/{userId}
     *
     * @param principalDetails 인증된 사용자 정보 (from SecurityContextHolder)
     * @param lastEventId      마지막으로 수신한 이벤트 ID
     * @return SseEmitter 객체
     */
    @Override
    public ResponseEntity<SseEmitter> subscribe(@AuthenticationPrincipal PrincipalDetails principalDetails,
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
     * @param userId  대상 사용자의 ID
     * @param payload 전송할 데이터
     * @return ResponseEntity
     */
    @Override
    public ResponseEntity<Void> sendDummyData(@PathVariable("user_id") String userId,
            @RequestBody Map<String, String> payload) {
        String message = payload.get("message");
        if (message == null || message.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        sseService.sendEventToUser(userId, message);
        return ResponseEntity.ok().build();
    }
}
