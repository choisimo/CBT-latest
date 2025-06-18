package com.authentication.auth.dto.diary;

import java.util.Map;

public record AiChatResponse(
    String response,
    Map<String, Object> usage,
    String model,
    String timestamp
) {} 