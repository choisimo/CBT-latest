package com.authentication.auth.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaryAnalysisRequestDto {
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("userId")
    private String userId;
} 