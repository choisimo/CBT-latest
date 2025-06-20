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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;
    private final AiClientService aiClientService;
    private final DiaryService self; // Self-injection for calling @Transactional methods from within the same class proxy

    // Constructor updated for self-injection
    public DiaryService(DiaryRepository diaryRepository, UserRepository userRepository, AiClientService aiClientService, DiaryService self) {
        this.diaryRepository = diaryRepository;
        this.userRepository = userRepository;
        this.aiClientService = aiClientService;
        this.self = self;
    }

    /**
     * 다이어리 생성 (초기 저장)
     * AI 분석은 비동기적으로 별도 처리됩니다.
     */
    @Transactional
    public DiaryResponse createDiary(String email, DiaryCreateRequest request) {
        log.info("다이어리 생성 요청 - email: {}, title: {}", email, request.title());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorType.USER_NOT_FOUND));

        Diary diary = Diary.builder()
                .user(user)
                .title(request.title())
                .content(request.content())
                .isNegative(false) // 기본값, AI 분석 후 업데이트될 수 있음
                .build();

        Diary savedDiary = diaryRepository.save(diary);
        log.info("다이어리 초기 저장 완료 - diaryId: {}", savedDiary.getId());

        // AI 분석 및 업데이트를 비동기적으로 트리거 (별도 트랜잭션으로 처리됨)
        // self.processAiAnalysisAndUpdate(savedDiary.getId(), request.content());
        // Using a direct call for now, if processAiAnalysisAndUpdate is @Async, self-injection is needed.
        // For non-@Async but separate transaction for update part, self-injection is also good practice.
        try {
             self.processAiAnalysisAndUpdate(savedDiary.getId(), request.content());
        } catch (Exception e) {
            // Log error if submitting AI task fails, but don't let it fail the main diary creation.
            log.error("AI 분석 작업 제출 실패 - diaryId: {}. 다이어리는 생성되었으나 AI 분석은 수행되지 않을 수 있습니다.", savedDiary.getId(), e);
        }


        // 응답은 초기 저장된 다이어리 기준으로 생성
        return DiaryResponse.fromEntity(savedDiary);
    }

    /**
     * AI 분석을 수행하고 결과를 다이어리에 업데이트합니다. (비동기 호출용)
     * AI 분석 자체는 비동기(WebFlux Mono)이며, 결과 업데이트는 새 트랜잭션에서 수행됩니다.
     */
    // Consider making this @Async if AI client call is blocking or if you want it on a separate thread pool.
    // If aiClientService.analyzeDiary is already fully non-blocking (returns Mono/Flux and doesn't block internally),
    // then @Async here is primarily for offloading the subscribe part from the caller thread.
    public void processAiAnalysisAndUpdate(Long diaryId, String contentToAnalyze) {
        log.info("AI 분석 및 업데이트 시작 - diaryId: {}", diaryId);
        aiClientService.analyzeDiary(contentToAnalyze)
            .subscribe(
                aiResult -> {
                    log.info("AI 분석 결과 수신 - diaryId: {}. isNegative: {}", diaryId, aiResult.isNegative());
                    // AI 결과를 별도 트랜잭션으로 업데이트
                    try {
                        self.updateDiaryWithAiResultsInternal(diaryId, aiResult);
                    } catch (Exception e) {
                        log.error("AI 결과로 다이어리 업데이트 중 오류 발생 - diaryId: {}: {}", diaryId, e.getMessage(), e);
                        // TODO: 실패 알림 또는 재시도 로직 고려
                    }
                },
                error -> {
                    log.error("AI 분석 서비스 호출 실패 - diaryId: {}: {}", diaryId, error.getMessage());
                    // TODO: 실패 알림 또는 재시도 로직 고려
                }
            );
    }

    /**
     * AI 분석 결과를 받아 다이어리를 업데이트하는 내부 트랜잭션 메소드.
     */
    @Transactional
    public void updateDiaryWithAiResultsInternal(Long diaryId, AiClientService.DiaryAnalysisResult aiResult) {
        log.info("AI 결과 업데이트 트랜잭션 시작 - diaryId: {}", diaryId);
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> {
                    log.warn("AI 결과 업데이트 시 다이어리 찾기 실패 - diaryId: {}", diaryId);
                    return new CustomException(ErrorType.DIARY_NOT_FOUND_FOR_AI_UPDATE);
                });

        diary.setIsNegative(aiResult.isNegative());
        if (aiResult.alternativeThought() != null && !aiResult.alternativeThought().isEmpty()) {
            diary.setAlternativeThought(aiResult.alternativeThought());
        }
        // Ensure updatedAt is modified if you have an @PreUpdate hook or manual update
        // diary.setUpdatedAt(LocalDateTime.now()); 
        diaryRepository.save(diary);
        log.info("AI 결과로 다이어리 업데이트 완료 - diaryId: {}", diary.getId());
    }

    /**
     * 다이어리 목록 조회 (페이징)
     */
    public Page<DiaryListItem> getDiaries(String email, Pageable pageable) {
        log.info("다이어리 목록 조회 - email: {}, page: {}", email, pageable.getPageNumber());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorType.USER_NOT_FOUND));

        return diaryRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(DiaryListItem::fromEntity);
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

        // 다이어리 정보 업데이트
        if (request.title() != null) {
            diary.setTitle(request.title());
        }
        boolean contentChanged = false;
        if (request.title() != null) {
            diary.setTitle(request.title());
        }
        if (request.content() != null && !request.content().equals(diary.getContent())) {
            diary.setContent(request.content());
            contentChanged = true;
        }

        Diary updatedDiary = diaryRepository.save(diary); // Save basic updates first
        log.info("다이어리 기본 정보 수정 완료 - diaryId: {}", updatedDiary.getId());

        if (contentChanged) {
            log.info("다이어리 내용 변경됨, AI 재분석 트리거 - diaryId: {}", updatedDiary.getId());
            // AI 분석 및 업데이트를 비동기적으로 트리거
            try {
                self.processAiAnalysisAndUpdate(updatedDiary.getId(), request.content());
            } catch (Exception e) {
                log.error("AI 재분석 작업 제출 실패 - diaryId: {}. 다이어리는 수정되었으나 AI 재분석은 수행되지 않을 수 있습니다.", updatedDiary.getId(), e);
            }
        }

        return DiaryResponse.fromEntity(updatedDiary);
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
        long negativeCount = diaryRepository.findByUserAndIsNegativeTrueOrderByCreatedAtDesc(user, Pageable.unpaged()).getTotalElements();
        long positiveCount = totalCount - negativeCount;

        return new DiaryStatsResponse(totalCount, positiveCount, negativeCount);
    }

    /**
     * 키워드로 다이어리 검색
     */
    public Page<DiaryListItem> searchDiaries(String email, String keyword, Pageable pageable) {
        log.info("다이어리 검색 - email: {}, keyword: {}", email, keyword);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorType.USER_NOT_FOUND));

        return diaryRepository.findByUserAndTitleContaining(user, keyword, pageable)
                .map(DiaryListItem::fromEntity);
    }

    /**
     * 부정적인 감정의 다이어리만 조회
     */
    public Page<DiaryListItem> getNegativeDiaries(String email, Pageable pageable) {
        log.info("부정적 다이어리 조회 - email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorType.USER_NOT_FOUND));

        return diaryRepository.findByUserAndIsNegativeTrueOrderByCreatedAtDesc(user, pageable)
                .map(DiaryListItem::fromEntity);
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