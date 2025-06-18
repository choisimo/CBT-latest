package com.authentication.auth.diary.service;  

import com.authentication.auth.diary.dto.DiaryResponseDto;
import com.authentication.auth.dto.diary.DiaryCreateRequest;
import com.authentication.auth.dto.diary.DiaryUpdateRequest;
import com.authentication.auth.diary.repository.DiaryRepository;
import com.authentication.auth.domain.Diary;
import com.authentication.auth.domain.User;
import com.authentication.auth.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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

    @Transactional
    public DiaryResponseDto createDiaryPost(DiaryCreateRequest request, UserDetails userDetails) {
        User user = userRepository.findByUserName(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User (owner) not found with username: " + userDetails.getUsername()));

        Diary diary = Diary.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .user(user)
                .build();
        diaryRepository.save(diary);

        return DiaryResponseDto.from(diary);
    }

    @Transactional
    public DiaryResponseDto updateDiaryPost(Long diaryId, DiaryUpdateRequest request, UserDetails userDetails) {
        User currentUser = userRepository.findByUserName(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User (current) not found with username: " + userDetails.getUsername()));

        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException("Diary not found with id: " + diaryId));

        if (!diary.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to update this diary.");
        }

        // Using setters as Diary.update(title, content) method is not present
        diary.setTitle(request.getTitle());
        diary.setContent(request.getContent());
        // The @PreUpdate annotation in Diary entity will update `updatedAt`
        diaryRepository.save(diary);

        return DiaryResponseDto.from(diary);
    }

    @Transactional
    public void deleteDiaryPost(Long diaryId, UserDetails userDetails) {
        User currentUser = userRepository.findByUserName(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User not found with username: " + userDetails.getUsername()));

        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException("Diary not found with id: " + diaryId));

        if (!diary.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to delete this diary.");
        }

        diaryRepository.delete(diary);
    }
}
