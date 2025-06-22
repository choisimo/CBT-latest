package com.authentication.auth.service.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiClientService {
    
    public String generateResponse(String input) {
        // Mock implementation for testing
        return "AI response for: " + input;
    }
}
