package com.authentication.auth.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "ai_responses")
public class AIResponse {
    
    @Id
    private String id;

    @Field("diary_id")
    private Long diaryId;
    
    @Field("user_id")
    private String userId;
    
    @Field("diary_title")
    private String diaryTitle;
    
    @Field("diary_content")
    private String diaryContent;
    
    @Field("emotions")
    private List<Emotion> emotions;
    
    @Field("summary")
    private String summary;
    
    @Field("coaching")
    private String coaching;
    
    @Field("created_at")
    private LocalDateTime createdAt;
    
    @Field("updated_at")
    private LocalDateTime updatedAt;
    
    @Field("status")
    @Builder.Default
    private AnalysisStatus status = AnalysisStatus.PENDING;
    
    @Field("error_message")
    private String errorMessage;
    
    public enum AnalysisStatus {
        PENDING,     // 대기중
        PROCESSING,  // 분석중  
        COMPLETED,   // 완료
        FAILED       // 실패
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Emotion {
        @Field("category")
        private String category;
        
        @Field("intensity")
        private Integer intensity;
    }
} 