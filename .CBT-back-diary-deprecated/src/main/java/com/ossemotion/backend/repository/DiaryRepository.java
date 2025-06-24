package com.ossemotion.backend.repository;

import com.ossemotion.backend.entity.Diary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DiaryRepository extends JpaRepository<Diary, Long> { // ID type is Long

    // Find a diary by its ID (Diary.id) and User ID (User.id)
    Optional<Diary> findByIdAndUserId(Long diaryId, Long userId);

    // Find all diaries for a given user, with pagination
    Page<Diary> findAllByUserId(Long userId, Pageable pageable);
    
    // Example: Find all diaries for a given user and diaryDate
    // Page<Diary> findAllByUserIdAndDiaryDate(Long userId, LocalDate diaryDate, Pageable pageable);
}
