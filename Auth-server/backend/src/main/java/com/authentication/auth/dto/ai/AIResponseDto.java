package com.authentication.auth.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIResponseDto {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("diaryTitle")
    private String diaryTitle;
    
    @JsonProperty("diaryContent")
    private String diaryContent;
    
    @JsonProperty("emotions")
    private List<EmotionDto> emotions;
    
    @JsonProperty("summary")
    private String summary;
    
    @JsonProperty("coaching")
    private String coaching;
    
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
    
    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EmotionDto {
        @JsonProperty("category")
        private String category;
        
        @JsonProperty("intensity")
        private Integer intensity;
    }
} 