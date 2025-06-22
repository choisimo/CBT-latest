package com.authentication.auth.diary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryResponseDto {
    private Long id;
    private String title;
    private String content;
    private String userName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
