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
import com.authentication.auth.service.DiaryService;
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
import java.util.List;
import java.util.Optional;

@Service("diaryManagementService")
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DiaryManagementService {

    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;
    private final DiaryAnalysisService diaryAnalysisService;
    private final DiaryService diaryService;

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
            // DiaryService를 통해 분석 요청 (더 확실한 방법)
            diaryService.requestAnalysis(savedDiary.getId(), userDetails.getUsername());
            log.info("일기 생성 후 분석 요청 완료 - Diary ID: {}", savedDiary.getId());
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

        // 기존 내용과 새로운 내용 비교
        boolean contentChanged = !diary.getContent().equals(request.getContent()) || 
                                !diary.getTitle().equals(request.getTitle());

        diary.setTitle(request.getTitle());
        diary.setContent(request.getContent());
        diary.setDate(request.getDate());

        Diary savedDiary = diaryRepository.save(diary);

        // 내용이 변경된 경우에만 재분석 요청
        if (contentChanged) {
            try {
                log.info("일기 수정 완료. 내용 변경으로 인한 재분석을 요청합니다. Diary ID: {}", savedDiary.getId());
                // DiaryService를 통해 분석 요청 (더 확실한 방법)
                diaryService.requestAnalysis(savedDiary.getId(), userDetails.getUsername());
                log.info("일기 수정 후 재분석 요청 완료 - Diary ID: {}", savedDiary.getId());
            } catch (Exception e) {
                log.error("수정된 일기 재분석 요청 중 오류 발생: {}", e.getMessage(), e);
                // AI 분석 실패가 일기 수정 자체에 영향을 주지 않도록 예외를 던지지 않음
            }
        } else {
            log.info("일기 수정 완료. 내용 변경이 없어 재분석을 건너뜁니다. Diary ID: {}", savedDiary.getId());
        }

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

    /**
     * 검색어로 일기 조회
     */
    @Transactional(readOnly = true)
    public Page<DiaryResponseDto> searchDiaries(UserDetails userDetails, String searchQuery, Pageable pageable) {
        User user = findUserByUsernameOrEmail(userDetails.getUsername());
        
        // 제목이나 내용에 검색어가 포함된 일기 조회
        Page<Diary> diaries = diaryRepository.findByUserAndTitleContainingOrUserAndContentContaining(
                user, searchQuery, user, searchQuery, pageable);
        
        return diaries.map(this::convertToResponseDto);
    }

    /**
     * 특정 날짜의 일기 조회
     */
    @Transactional(readOnly = true)
    public Page<DiaryResponseDto> findDiariesByDate(UserDetails userDetails, String dateString, Pageable pageable) {
        User user = findUserByUsernameOrEmail(userDetails.getUsername());
        LocalDate date = LocalDate.parse(dateString);
        
        Page<Diary> diaries = diaryRepository.findByUserAndDate(user, date, pageable);
        return diaries.map(this::convertToResponseDto);
    }

    /**
     * 월별 일기 작성 날짜 목록 조회 (달력 표시용)
     */
    @Transactional(readOnly = true)
    public List<String> findDiaryDatesByMonth(UserDetails userDetails, String monthString) {
        User user = findUserByUsernameOrEmail(userDetails.getUsername());
        
        // monthString format: "YYYY-MM"
        String[] parts = monthString.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        
        return diaryRepository.findDistinctDatesByUserAndDateBetween(user, startDate, endDate)
                .stream()
                .map(LocalDate::toString)
                .sorted()
                .toList();
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
                .userName(diary.getUser().getNickname())
                .createdAt(diary.getCreatedAt())
                .updatedAt(diary.getUpdatedAt())
                .build();
    }
}
