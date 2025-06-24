package com.authentication.auth.diary.repository;

import com.authentication.auth.domain.Diary;
import com.authentication.auth.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiaryRepository extends JpaRepository<Diary, Long> {
    Optional<Diary> findByIdAndUserId(Long diaryId, Long userId);
    Optional<Diary> findByIdAndUser_Id(Long id, Long userId);

    Page<Diary> findByUser(User user, Pageable pageable);

    // 검색 기능: 제목이나 내용에 검색어 포함
    Page<Diary> findByUserAndTitleContainingOrUserAndContentContaining(
            User user1, String titleKeyword,
            User user2, String contentKeyword,
            Pageable pageable);

    // 특정 날짜의 일기 조회
    Page<Diary> findByUserAndDate(User user, LocalDate date, Pageable pageable);

    // 월별 일기 작성 날짜 목록 조회
    List<LocalDate> findDistinctDatesByUserAndDateBetween(User user, LocalDate startDate, LocalDate endDate);
}
