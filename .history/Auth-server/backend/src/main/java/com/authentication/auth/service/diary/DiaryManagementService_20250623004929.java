package com.authentication.auth.service.diary;

import com.authentication.auth.diary.dto.DiaryResponseDto;
import com.authentication.auth.diary.repository.DiaryRepository;
import com.authentication.auth.domain.Diary;
import com.authentication.auth.domain.User;
import com.authentication.auth.dto.diary.DiaryCreateRequest;
import com.authentication.auth.dto.diary.DiaryUpdateRequest;
import com.authentication.auth.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("diaryManagementService")
@RequiredArgsConstructor
@Transactional
public class DiaryManagementService {

    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;

    public DiaryResponseDto createDiaryPost(DiaryCreateRequest request, UserDetails userDetails) {
        User user = userRepository.findByUserName(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Diary diary = Diary.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .user(user)
                .build();

        Diary savedDiary = diaryRepository.save(diary);
        return convertToResponseDto(savedDiary);
    }

    public DiaryResponseDto updateDiaryPost(Long diaryId, DiaryUpdateRequest request, UserDetails userDetails) {
        User user = userRepository.findByUserName(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException("Diary not found"));

        if (!diary.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied");
        }

        diary.setTitle(request.getTitle());
        diary.setContent(request.getContent());

        Diary savedDiary = diaryRepository.save(diary);
        return convertToResponseDto(savedDiary);
    }

    public void deleteDiaryPost(Long diaryId, UserDetails userDetails) {
        User user = userRepository.findByUserName(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException("Diary not found"));

        if (!diary.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied");
        }

        diaryRepository.delete(diary);
    }

    @Transactional(readOnly = true)
    public DiaryResponseDto findDiaryById(Long diaryId) {
        User currentUser = getCurrentUser();
        Diary diary = diaryRepository.findByIdAndUserId(diaryId, currentUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("Diary not found"));

        return convertToResponseDto(diary);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUserName(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
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
