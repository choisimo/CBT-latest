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
    
    @JsonProperty("text")
    private String text;
    
    // 필요시 추가 필드들을 위해 남겨둠 (AI 서버에서 사용하지 않으면 무시됨)
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("userId")
    private String userId;
} 