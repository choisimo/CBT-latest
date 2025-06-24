package com.authentication.auth.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponseDto {
    
    @JsonProperty("response")
    private String response;
    
    @JsonProperty("usage")
    private Map<String, Object> usage;
    
    @JsonProperty("model")
    private String model;
    
    @JsonProperty("timestamp")
    private String timestamp;
} 