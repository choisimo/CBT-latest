package com.authentication.auth.service.diary;

import com.authentication.auth.domain.Diary;
import com.authentication.auth.domain.User;
import com.authentication.auth.dto.diary.*;
import com.authentication.auth.exception.CustomException;
import com.authentication.auth.exception.ErrorType;
import com.authentication.auth.repository.DiaryRepository;
import com.authentication.auth.repository.UserRepository;
import com.authentication.auth.service.ai.AiClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;
    private final AiClientService aiClientService;

    /**
     * 다이어리 생성 (AI 분석 포함)
     */
    @Transactional
    public DiaryResponse createDiary(String email, DiaryCreateRequest request) {
        log.info("다이어리 생성 요청 - email: {}, title: {}", email, request.title());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorType.USER_NOT_FOUND));

        // 다이어리 엔터티 생성
        Diary diary = Diary.builder()
                .user(user)
                .title(request.title())
                .content(request.content())
                .isNegative(false) // 기본값
                .build();

        // 다이어리 저장
        Diary savedDiary = diaryRepository.save(diary);

        // AI 분석 비동기 처리
        aiClientService.analyzeDiary(request.content())
                .subscribe(
                    result -> {
                        savedDiary.setIsNegative(result.isNegative());
                        if (result.alternativeThought() != null && !result.alternativeThought().isEmpty()) {
                            savedDiary.setAlternativeThought(result.alternativeThought());
                        }
                        diaryRepository.save(savedDiary);
                        log.info("AI 분석 완료 및 다이어리 업데이트 - diaryId: {}", savedDiary.getId());
                    },
                    error -> log.error("AI 분석 실패 - diaryId: {}, error: {}", savedDiary.getId(), error.getMessage())
                );

        return DiaryResponse.fromEntity(savedDiary);
    }

    /**
     *  일반 다이어리 목록 조회 (페이징) - 기존 getDiaries 와 유사하나, DiaryPostItemDto 사용 고려
     *  이 메소드는 현재 /api/diaryposts 엔드포인트에서 직접 사용되지 않음.
     *  만약 기존 /api/diaries GET 이 유지된다면 해당 컨트롤러에서 이 메소드를 호출.
     */
    public Page<DiaryListItem> getDiariesLegacy(String email, Pageable pageable) {
        log.info("레거시 다이어리 목록 조회 - email: {}, page: {}", email, pageable.getPageNumber());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorType.USER_NOT_FOUND));
        return diaryRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(DiaryListItem::fromEntity); // 기존 DTO 사용
    }


    /**
     * 일기 게시물 목록 조회 (검색 또는 전체, 페이지네이션) - 신규 /api/diaryposts GET
     */
    public DiaryPostListResponseDto getDiaryPosts(String email, String keyword, Pageable pageable) {
        log.info("일기 게시물 목록 조회 - email: {}, keyword: {}, page: {}", email, keyword, pageable.getPageNumber());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorType.USER_NOT_FOUND));

        Page<Diary> diariesPage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            diariesPage = diaryRepository.findByUserAndTitleContainingOrUserAndContentContainingOrderByCreatedAtDesc(user, keyword, user, keyword, pageable);
        } else {
            diariesPage = diaryRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        }

        List<DiaryPostItemDto> diaryPostItems = diariesPage.getContent().stream()
                .map(DiaryPostItemDto::fromEntity)
                .collect(Collectors.toList());

        PageInfoDto pageInfo = PageInfoDto.builder()
                .page(diariesPage.getNumber())
                .size(diariesPage.getSize())
                .totalElements(diariesPage.getTotalElements())
                .totalPages(diariesPage.getTotalPages())
                .build();

        return DiaryPostListResponseDto.builder()
                .data(diaryPostItems)
                .pageInfo(pageInfo)
                .build();
    }

    /**
     * 특정 날짜의 모든 일기 조회 - 신규 /api/diaryposts GET (date 파라미터)
     */
    public List<DiaryPostDetailDto> getDiariesByDate(String email, LocalDate date) {
        log.info("특정 날짜 일기 조회 - email: {}, date: {}", email, date);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorType.USER_NOT_FOUND));

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<Diary> diaries = diaryRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(user, startOfDay, endOfDay);

        return diaries.stream()
                .map(DiaryPostDetailDto::fromEntity)
                .collect(Collectors.toList());
    }


    /**
     * 다이어리 상세 조회
     */
    public DiaryDetailResponse getDiary(String email, Long diaryId) {
        log.info("다이어리 상세 조회 - email: {}, diaryId: {}", email, diaryId);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorType.USER_NOT_FOUND));

        Diary diary = diaryRepository.findByIdAndUser(diaryId, user)
                .orElseThrow(() -> new CustomException(ErrorType.DIARY_NOT_FOUND));

        return DiaryDetailResponse.fromEntity(diary);
    }

    /**
     * 다이어리 수정
     */
    @Transactional
    public DiaryResponse updateDiary(String email, Long diaryId, DiaryUpdateRequest request) {
        log.info("다이어리 수정 - email: {}, diaryId: {}", email, diaryId);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorType.USER_NOT_FOUND));

        Diary diary = diaryRepository.findByIdAndUser(diaryId, user)
                .orElseThrow(() -> new CustomException(ErrorType.DIARY_NOT_FOUND));

        if (request.title() != null) {
            diary.setTitle(request.title());
        }
        if (request.content() != null) {
            diary.setContent(request.content());
            
            aiClientService.analyzeDiary(request.content())
                    .subscribe(
                        result -> {
                            diary.setIsNegative(result.isNegative());
                            if (result.alternativeThought() != null && !result.alternativeThought().isEmpty()) {
                                diary.setAlternativeThought(result.alternativeThought());
                            }
                            diaryRepository.save(diary);
                            log.info("다이어리 수정 후 AI 재분석 완료 - diaryId: {}", diary.getId());
                        },
                        error -> log.error("다이어리 수정 후 AI 재분석 실패 - diaryId: {}, error: {}", diary.getId(), error.getMessage())
                    );
        }

        return DiaryResponse.fromEntity(diaryRepository.save(diary));
    }

    /**
     * 다이어리 삭제
     */
    @Transactional
    public void deleteDiary(String email, Long diaryId) {
        log.info("다이어리 삭제 - email: {}, diaryId: {}", email, diaryId);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorType.USER_NOT_FOUND));

        Diary diary = diaryRepository.findByIdAndUser(diaryId, user)
                .orElseThrow(() -> new CustomException(ErrorType.DIARY_NOT_FOUND));

        diaryRepository.delete(diary);
        log.info("다이어리 삭제 완료 - diaryId: {}", diaryId);
    }

    /**
     * 사용자별 다이어리 통계
     */
    public DiaryStatsResponse getDiaryStats(String email) {
        log.info("다이어리 통계 조회 - email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorType.USER_NOT_FOUND));

        long totalCount = diaryRepository.countByUser(user);
        long negativeCount = diaryRepository.countByUserAndIsNegativeTrue(user); // countByUserAndIsNegativeTrue 사용
        long positiveCount = totalCount - negativeCount;

        return new DiaryStatsResponse(totalCount, positiveCount, negativeCount);
    }

    /**
     * 키워드로 다이어리 검색 (기존 searchDiaries 와 유사, 현재는 getDiaryPosts 에서 통합 처리)
     * 이 메소드는 현재 /api/diaryposts 엔드포인트에서 직접 사용되지 않음.
     */
    public Page<DiaryListItem> searchDiariesLegacy(String email, String keyword, Pageable pageable) {
        log.info("레거시 다이어리 검색 - email: {}, keyword: {}", email, keyword);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorType.USER_NOT_FOUND));
        // 기존에는 title만 검색했으나, content도 포함하도록 변경 가능 (요구사항에 따라)
        return diaryRepository.findByUserAndTitleContainingOrderByCreatedAtDesc(user, keyword, pageable)
                .map(DiaryListItem::fromEntity);
    }

    /**
     * 부정적인 감정의 다이어리만 조회 (기존 getNegativeDiaries 와 유사)
     * 이 메소드는 현재 /api/diaryposts 엔드포인트에서 직접 사용되지 않음. 필터링은 getDiaryPosts 에서 처리 가능.
     */
    public Page<DiaryListItem> getNegativeDiariesLegacy(String email, Pageable pageable) {
        log.info("레거시 부정적 다이어리 조회 - email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorType.USER_NOT_FOUND));
        return diaryRepository.findByUserAndIsNegativeTrueOrderByCreatedAtDesc(user, pageable)
                .map(DiaryListItem::fromEntity);
    }

    /**
     * 월별 일기 작성일 조회 (캘린더용)
     */
    public DiaryCalendarResponseDto getDiaryCalendar(String email, int year, int month) {
        log.info("캘린더 데이터 조회 - email: {}, year: {}, month: {}", email, year, month);
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("월(month)은 1에서 12 사이의 값이어야 합니다.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorType.USER_NOT_FOUND));

        List<Integer> daysWithDiary = diaryRepository.findDistinctDaysWithDiaryByUserAndMonth(user, year, month);

        return DiaryCalendarResponseDto.builder()
                .year(year)
                .month(month)
                .daysWithDiary(daysWithDiary)
                .build();
    }

    /**
     * 다이어리 통계 응답 DTO
     */
    public record DiaryStatsResponse(
        long totalCount,
        long positiveCount,
        long negativeCount
    ) {}
}