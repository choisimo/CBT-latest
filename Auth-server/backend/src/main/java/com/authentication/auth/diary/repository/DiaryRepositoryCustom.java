package main.java.com.authentication.auth.diary.repository;

import com.authentication.auth.domain.Diary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface DiaryRepositoryCustom {

    /**
     * 특정 사용자의 특정 일기를 조회합니다. (본인 확인)
     * @param diaryId 일기 ID
     * @param userId 사용자 ID
     * @return Optional<Diary>
     */
    Optional<Diary> findByIdAndUserId(Long diaryId, Long userId);

    /**
     * 특정 사용자의 모든 일기를 페이징 처리하여 최신순으로 조회합니다.
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return Page<Diary>
     */
    Page<Diary> findAllByUserId(Long userId, Pageable pageable);
}
