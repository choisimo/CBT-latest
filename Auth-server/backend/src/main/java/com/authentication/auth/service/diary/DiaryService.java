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
        // TODO: 비동기 AI 분석 실패 시 사용자에게 알림을 주거나, 재시도 로직, 또는 실패 상태를 기록하는 등의 명확한 오류 처리 정책 필요.
        // 현재는 로그만 남기고 있으며, 이는 사용자 경험에 부정적일 수 있음.
        // 실패 시 CustomException(ErrorType.AI_ANALYSIS_FAILED, error.getMessage()) 발생을 고려할 수 있으나,
        // 비동기 흐름을 방해하지 않도록 주의해야 함. (예: 별도의 알림 채널, 상태 업데이트)
        aiClientService.analyzeDiary(request.content())
                .subscribe(
                    result -> {
                        // AI 분석 결과를 다이어리에 반영
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
        if (request.content() != null) {
            diary.setContent(request.content());
            
            // 내용이 변경된 경우 AI 재분석
            // TODO: 비동기 AI 분석 실패 시 오류 처리 정책 참고 (createDiary와 동일)
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