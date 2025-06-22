package com.authentication.auth.diary.repository;

import com.authentication.auth.domain.Diary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DiaryRepository extends JpaRepository<Diary, Long> {
    Optional<Diary> findByIdAndUserId(Long id, Long userId);
}
