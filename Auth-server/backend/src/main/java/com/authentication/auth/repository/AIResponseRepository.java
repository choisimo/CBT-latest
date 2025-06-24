package com.authentication.auth.repository;

import com.authentication.auth.domain.AIResponse;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AIResponseRepository extends MongoRepository<AIResponse, String> {
    
    /**
     * 특정 사용자의 AI 응답 목록 조회
     */
    List<AIResponse> findByUserIdOrderByCreatedAtDesc(String userId);
    
    /**
     * 특정 사용자의 기간별 AI 응답 조회
     */
    @Query("{'userId': ?0, 'createdAt': {'$gte': ?1, '$lte': ?2}}")
    List<AIResponse> findByUserIdAndCreatedAtBetween(String userId, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 특정 사용자의 최신 AI 응답 조회
     */
    Optional<AIResponse> findTopByUserIdOrderByCreatedAtDesc(String userId);
    
    /**
     * 특정 일기 제목으로 AI 응답 조회
     */
    Optional<AIResponse> findByUserIdAndDiaryTitle(String userId, String diaryTitle);
    
    /**
     * 특정 사용자의 응답 개수 조회
     */
    long countByUserId(String userId);
} 