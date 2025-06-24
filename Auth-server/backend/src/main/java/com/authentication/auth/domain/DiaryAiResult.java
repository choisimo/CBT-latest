package com.authentication.auth.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * MongoDB 문서로 저장되는 일기 AI 분석 결과
 * @Date : 2025-06-21
 * @Detail : Diary 엔티티에서 AI 관련 필드들을 MongoDB로 분리
 */
@Document(collection = "diary_ai_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaryAiResult {
    
    @Id
    private String id;
    
    /**
     * MariaDB의 Diary 엔티티 ID와 연결
     */
    @Field("diary_id")
    private Long diaryId;
    
    /**
     * 사용자 ID (성능 최적화를 위한 중복 저장)
     */
    @Field("user_id")
    private Long userId;
    
    /**
     * AI가 생성한 대안적 사고
     */
    @Field("alternative_thought")
    private String alternativeThought;
    
    /**
     * 부정적 감정 여부
     */
    @Field("is_negative")
    private Boolean isNegative;
    
    /**
     * 감정 분석 점수 (0-100, 추후 확장용)
     */
    @Field("emotion_score")
    private Integer emotionScore;
    
    /**
     * AI 모델 버전 (추후 모델 업데이트 추적용)
     */
    @Field("ai_model_version")
    private String aiModelVersion;
    
    /**
     * 생성 시간
     */
    @Field("created_at")
    private LocalDateTime createdAt;
    
    /**
     * 수정 시간
     */
    @Field("updated_at")
    private LocalDateTime updatedAt;
} 