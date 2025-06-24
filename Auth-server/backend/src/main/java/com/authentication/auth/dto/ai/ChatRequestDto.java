package com.authentication.auth.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequestDto {
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("conversation_history")
    private List<Map<String, String>> conversationHistory;
    
    @JsonProperty("system_prompt")
    private String systemPrompt;
    
    @JsonProperty("model")
    private String model;
    
    @JsonProperty("temperature")
    private Double temperature;
    
    @JsonProperty("max_tokens")
    private Integer maxTokens;
    
    @JsonProperty("stream")
    private Boolean stream;
} 