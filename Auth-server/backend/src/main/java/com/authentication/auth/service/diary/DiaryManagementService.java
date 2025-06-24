package com.authentication.auth.service.diary;

import com.authentication.auth.diary.dto.DiaryResponseDto;
import com.authentication.auth.diary.repository.DiaryRepository;
import com.authentication.auth.domain.Diary;
import com.authentication.auth.domain.User;
import com.authentication.auth.dto.diary.DiaryCreateRequest;
import com.authentication.auth.dto.diary.DiaryUpdateRequest;
import com.authentication.auth.dto.diary.DiaryRequestDto;
import com.authentication.auth.repository.UserRepository;
import com.authentication.auth.service.diary.DiaryAnalysisService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDate;
import java.util.Optional;

@Service("diaryManagementService")
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DiaryManagementService {

    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;
    private final DiaryAnalysisService diaryAnalysisService;

        public DiaryResponseDto createDiaryPost(DiaryCreateRequest request, UserDetails userDetails) {
        User user = findUserByUsernameOrEmail(userDetails.getUsername());

        Diary diary = Diary.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .user(user)
                .date(LocalDate.parse(request.getDate()))
                .build();

        Diary savedDiary = diaryRepository.save(diary);

        // 비동기 AI 분석 요청
        try {
            log.info("일기 저장 완료. 비동기 분석을 요청합니다. Diary ID: {}", savedDiary.getId());
            DiaryRequestDto analysisRequest = new DiaryRequestDto(
                    savedDiary.getTitle(),
                    savedDiary.getContent(),
                    user.getLoginId(),
                    savedDiary.getDate() != null ? savedDiary.getDate().toString() : null
            );
            diaryAnalysisService.requestDiaryAnalysis(analysisRequest);
        } catch (Exception e) {
            log.error("비동기 분석 요청 중 오류 발생: {}", e.getMessage(), e);
            // AI 분석 실패가 일기 생성 자체에 영향을 주지 않도록 예외를 던지지 않음
        }

        return convertToResponseDto(savedDiary);
    }

    public DiaryResponseDto updateDiaryPost(Long diaryId, DiaryUpdateRequest request, UserDetails userDetails) {
        User user = findUserByUsernameOrEmail(userDetails.getUsername());

        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException("Diary not found"));

        if (!diary.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied");
        }

        diary.setTitle(request.getTitle());
        diary.setContent(request.getContent());
        diary.setDate(request.getDate());

        Diary savedDiary = diaryRepository.save(diary);
        return convertToResponseDto(savedDiary);
    }

    public void deleteDiaryPost(Long diaryId, UserDetails userDetails) {
        User user = findUserByUsernameOrEmail(userDetails.getUsername());

        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException("Diary not found"));

        if (!diary.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied");
        }

        diaryRepository.delete(diary);
    }

    @Transactional(readOnly = true)
    public Page<DiaryResponseDto> findDiariesByUser(UserDetails userDetails, Pageable pageable) {
        User user = findUserByUsernameOrEmail(userDetails.getUsername());
        Page<Diary> diaries = diaryRepository.findByUser(user, pageable);
        return diaries.map(this::convertToResponseDto);
    }

    @Transactional(readOnly = true)
    public DiaryResponseDto findDiaryById(Long diaryId) {
        User currentUser = getCurrentUser();
        Diary diary = diaryRepository.findByIdAndUser_Id(diaryId, currentUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("Diary not found"));

        return convertToResponseDto(diary);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return findUserByUsernameOrEmail(username);
    }

    private User findUserByUsernameOrEmail(String username) {
        // Try finding by loginId first (findByUserName calls findByLoginId)
        return userRepository.findByUserName(username)
                .or(() -> {
                    // If not found, try by email
                    log.warn("User not found with login_id: {}. Trying by email.", username);
                    return userRepository.findByEmail(username);
                })
                .orElseThrow(() -> {
                    log.error("User not found with login_id or email: {}", username);
                    return new EntityNotFoundException("User not found: " + username);
                });
    }

    private DiaryResponseDto convertToResponseDto(Diary diary) {
        return DiaryResponseDto.builder()
                .id(diary.getId())
                .title(diary.getTitle())
                .content(diary.getContent())
                .userName(diary.getUser().getUserName())
                .createdAt(diary.getCreatedAt())
                .updatedAt(diary.getUpdatedAt())
                .build();
    }
}
