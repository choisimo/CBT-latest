package com.authentication.auth.controller;

import com.authentication.auth.dto.token.PrincipalDetails;
import com.authentication.auth.service.sse.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
public class SseController {

    private final SseService sseService;

    @GetMapping(value = "/subscribe", produces = "text/event-stream")
    public SseEmitter subscribe(@AuthenticationPrincipal PrincipalDetails principalDetails) {
        String userId = principalDetails.getUsername();
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        sseService.saveSseEmitter(userId, emitter);

        // Send a confirmation event to the client
        try {
            emitter.send(SseEmitter.event().name("connect").data("Connection established."));
        } catch (java.io.IOException e) {
            sseService.removeEmitter(userId, emitter);
        }

        return emitter;
    }
}
