package com.authentication.auth.diary.service;  

import com.authentication.auth.diary.dto.DiaryResponseDto;
import com.authentication.auth.diary.repository.DiaryRepository;
import com.authentication.auth.domain.Diary;
import com.authentication.auth.domain.User;
import com.authentication.auth.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;

    /**
     * Reads a diary that belongs to the currently authenticated user and converts it to DTO.
     *
     * @param diaryId diary PK
     * @return DiaryResponseDto for API response
     */
    @Transactional(readOnly = true)
    public DiaryResponseDto findDiaryById(Long diaryId) {
        User currentUser = getCurrentUser();

        Diary diary = diaryRepository.findByIdAndUserId(diaryId, currentUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("Diary not found or access denied"));

        return DiaryResponseDto.from(diary);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        return userRepository.findByUserName(userName)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }
}
