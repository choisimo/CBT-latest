package com.authentication.auth.diary.repository;

import com.authentication.auth.domain.Diary;
import com.authentication.auth.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DiaryRepository extends JpaRepository<Diary, Long> {
    Optional<Diary> findByIdAndUserId(Long diaryId, Long userId);
    Optional<Diary> findByIdAndUser_Id(Long id, Long userId);

    Page<Diary> findByUser(User user, Pageable pageable);
}
