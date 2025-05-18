package com.career_block.auth.service.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class sseService {

    // 사용자 ID를 키로, SseEmitter 목록을 값으로 저장
    private final ConcurrentHashMap<String, List<SseEmitter>> emittersMap = new ConcurrentHashMap<>();


    // SseEmitter 저장
    public boolean saveSseEmitter(String userId, SseEmitter emitter) {
        if (userId == null || userId.isEmpty() || emitter == null) {
            log.error("userId or emitter is null");
            return false;
        }

        emittersMap.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // 연결 종료 시 제거
        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError((e) -> removeEmitter(userId, emitter));

        log.info("SseEmitter saved for userId: {}", userId);
        return true;
    }


    // SseEmitter 제거
    public void removeEmitter(String userId, SseEmitter emitter){
        List<SseEmitter> emitters = emittersMap.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                emittersMap.remove(userId);
            }
        }
    }


    // 특정 사용자에게 이벤트 전송
    public void sendEventToUser(String userId, Object data) {
        List<SseEmitter> emitters = emittersMap.get(userId);
        if (emitters != null) {
            emitters.forEach(emitter -> {
                try {
                    emitter.send(SseEmitter.event().name("message").data(data));
                } catch (IOException e) {
                    log.error("Error sending event to userId: {}", userId, e);
                    removeEmitter(userId, emitter);
                }
            });
        }
    }


    // 모든 사용자에게 이벤트 전송
    public void sendEventToAll(Object data) {
        emittersMap.forEach((userId, emitters) -> {
            sendEventToUser(userId, data);
        });
    }


}
