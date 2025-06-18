package com.ossemotion.backend.service;

import com.ossemotion.backend.dto.CreateDiaryPostRequest;
import com.ossemotion.backend.dto.DiaryPostDto;
import com.ossemotion.backend.dto.UpdateDiaryPostRequest;
import com.ossemotion.backend.entity.Diary;
import com.ossemotion.backend.entity.User;
import com.ossemotion.backend.repository.DiaryRepository;
import com.ossemotion.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException; 
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository; 
    
    // Should be consistent with UserService's MOCK_USER_ID_LONG or obtained from security context
    private static final Long MOCK_USER_ID_LONG = 1L; 

    @Transactional
    public DiaryPostDto createDiaryPost(CreateDiaryPostRequest request) {
        // In real app, get actual User's Long ID from SecurityContext
        User user = userRepository.findById(MOCK_USER_ID_LONG)
                .orElseThrow(() -> new EntityNotFoundException("Mock user not found with ID: " + MOCK_USER_ID_LONG + ". Cannot create diary."));

        Diary diary = Diary.builder()
                .user(user)
                .diaryDate(request.getDate())
                .title(request.getTitle())
                .content(request.getContent())
                // aiAlternativeThoughts and isNegative will be null/default initially by Lombok's @Builder.Default or DB default
                .build();
        // createdAt and updatedAt are handled by @Builder.Default and @PrePersist if needed
        
        diary = diaryRepository.save(diary);
        return mapToDto(diary);
    }

    @Transactional(readOnly = true)
    public DiaryPostDto getDiaryPostById(Long diaryId) { // Changed to Long for consistency with Entity ID
        // TODO: Add user check: .findByIdAndUserId(diaryId, currentUserIdFromSecurityContext)
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException("Diary post not found with ID: " + diaryId));
        return mapToDto(diary);
    }

    @Transactional
    public DiaryPostDto updateDiaryPost(Long diaryId, UpdateDiaryPostRequest request) { // Changed to Long
        // TODO: Add user check
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException("Diary post not found with ID: " + diaryId));

        // User should match the diary's original user - check not implemented here yet
        // if (!diary.getUser().getId().equals(MOCK_USER_ID_LONG)) {
        //    throw new SecurityException("User not authorized to update this diary post.");
        // }

        if (request.getTitle() != null) {
            diary.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            diary.setContent(request.getContent());
        }
        // aiAlternativeThoughts and isNegative are typically not updated directly via this endpoint.
        // They would be updated by an AI analysis service/process.
        // updatedAt is handled by @PreUpdate
        diary = diaryRepository.save(diary);
        return mapToDto(diary);
    }

    private DiaryPostDto mapToDto(Diary diary) {
        return DiaryPostDto.builder()
                .id(diary.getId().toString()) // Convert Long ID to String for DTO
                .userId(diary.getUser().getId().toString()) // Convert User's Long ID to String
                .date(diary.getDiaryDate())
                .title(diary.getTitle())
                .content(diary.getContent())
                .aiResponse(diary.getAiAlternativeThoughts() != null && !diary.getAiAlternativeThoughts().isEmpty())
                .aiAlternativeThoughts(diary.getAiAlternativeThoughts())
                .isNegative(diary.getIsNegative())
                .createdAt(diary.getCreatedAt())
                .updatedAt(diary.getUpdatedAt())
                .build();
    }
}
