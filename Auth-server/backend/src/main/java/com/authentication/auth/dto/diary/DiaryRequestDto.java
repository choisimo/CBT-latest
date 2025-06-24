package com.authentication.auth.dto.diary;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaryRequestDto {
    
    @NotBlank(message = "일기 제목은 필수입니다.")
    @JsonProperty("title")
    private String title;
    
    @NotBlank(message = "일기 내용은 필수입니다.")
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("userId")
    private String userId;

    @JsonProperty("date")
    private String date;
} 