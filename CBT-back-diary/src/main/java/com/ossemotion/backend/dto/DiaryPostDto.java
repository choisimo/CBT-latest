package com.ossemotion.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryPostDto {
    private String id; // Diary ID (will be String representation of Long PK from Diary entity)
    private String userId; // User ID (String representation of Long PK from User entity)
    private LocalDate date; // Date of the diary entry itself
    private String title;
    private String content;
    private Boolean aiResponse; // True if AI alternative thoughts are present
    private String aiAlternativeThoughts; // Optional
    private Boolean isNegative; // From Diary.isNegative (BOOLEAN in DB)
    private LocalDateTime createdAt; // Timestamp of creation
    private LocalDateTime updatedAt; // Timestamp of last update
}
