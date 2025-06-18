package com.authentication.auth.repository;

import com.authentication.auth.domain.Diary;
import com.authentication.auth.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiaryRepository extends JpaRepository<Diary, Long> {
    
    // 사용자별 다이어리 조회 (페이징)
    Page<Diary> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    // 사용자별 다이어리 조회 (리스트)
    List<Diary> findByUserOrderByCreatedAtDesc(User user);
    
    // 사용자 ID로 다이어리 조회
    @Query("SELECT d FROM Diary d WHERE d.user.id = :userId ORDER BY d.createdAt DESC")
    Page<Diary> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
    
    // 특정 사용자의 다이어리 개수
    long countByUser(User user);
    
    // 사용자별 부정적인 감정의 다이어리만 조회
    Page<Diary> findByUserAndIsNegativeTrueOrderByCreatedAtDesc(User user, Pageable pageable);
    
    // 사용자별 긍정적인 감정의 다이어리만 조회
    Page<Diary> findByUserAndIsNegativeFalseOrderByCreatedAtDesc(User user, Pageable pageable);
    
    // 특정 기간 내 사용자 다이어리 조회
    @Query("SELECT d FROM Diary d WHERE d.user = :user AND d.createdAt BETWEEN :startDate AND :endDate ORDER BY d.createdAt DESC")
    List<Diary> findByUserAndDateRange(@Param("user") User user, 
                                      @Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate);
    
    // 사용자와 다이어리 ID로 조회 (권한 확인용)
    Optional<Diary> findByIdAndUser(Long id, User user);
    
    // 제목으로 검색
    @Query("SELECT d FROM Diary d WHERE d.user = :user AND d.title LIKE %:keyword% ORDER BY d.createdAt DESC")
    Page<Diary> findByUserAndTitleContaining(@Param("user") User user, 
                                           @Param("keyword") String keyword, 
                                           Pageable pageable);
    
    // 내용으로 검색
    @Query("SELECT d FROM Diary d WHERE d.user = :user AND d.content LIKE %:keyword% ORDER BY d.createdAt DESC")
    Page<Diary> findByUserAndContentContaining(@Param("user") User user, 
                                             @Param("keyword") String keyword, 
                                             Pageable pageable);
} 