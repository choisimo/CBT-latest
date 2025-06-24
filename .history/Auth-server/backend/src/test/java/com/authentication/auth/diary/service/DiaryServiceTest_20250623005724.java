package com.authentication.auth.diary.service;

import com.authentication.auth.diary.dto.DiaryResponseDto;
import com.authentication.auth.diary.repository.DiaryRepository;
import com.authentication.auth.domain.Diary;
import com.authentication.auth.domain.User;
import com.authentication.auth.dto.diary.DiaryCreateRequest;
import com.authentication.auth.dto.diary.DiaryUpdateRequest;
import com.authentication.auth.repository.UserRepository;
import com.authentication.auth.service.diary.DiaryManagementService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiaryServiceTest {

    @Mock
    private DiaryRepository diaryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DiaryManagementService diaryService;

    @Mock
    private UserDetails userDetails;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    private User testUser;
    private User otherUser;
    private Diary testDiary;
    private DiaryCreateRequest diaryCreateRequest;
    private DiaryUpdateRequest diaryUpdateRequest;

    @BeforeEach
    void setUp() {
        // Authentication mock for getCurrentUser() used by findDiaryById
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        testUser = User.builder()
                .id(1L)
                .loginId("testUser")
                .nickname("testUser")
                .password("password")
                .email("test@example.com")
                .build();

        otherUser = User.builder()
                .id(2L)
                .loginId("otherUser")
                .nickname("otherUser")
                .password("password")
                .email("other@example.com")
                .build();

        testDiary = Diary.builder()
                .id(1L)
                .user(testUser)
                .title("Test Title")
                .content("Test Content")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        diaryCreateRequest = new DiaryCreateRequest();
        diaryCreateRequest.setTitle("New Title");
        diaryCreateRequest.setContent("New Content");

        diaryUpdateRequest = new DiaryUpdateRequest();
        diaryUpdateRequest.setTitle("Updated Title");
        diaryUpdateRequest.setContent("Updated Content");
    }

    // Test Cases will be implemented here
    
    // --- createDiaryPost Tests ---
    @Test
    @DisplayName("createDiaryPost_success")
    void createDiaryPost_success() {
        when(userDetails.getUsername()).thenReturn(testUser.getUserName());
        when(userRepository.findByUserName(testUser.getUserName())).thenReturn(Optional.of(testUser));
        
        Diary savedDiary = Diary.builder()
                                .id(2L)
                                .user(testUser)
                                .title(diaryCreateRequest.getTitle())
                                .content(diaryCreateRequest.getContent())
                                .build();
        when(diaryRepository.save(any(Diary.class))).thenReturn(savedDiary);

        DiaryResponseDto responseDto = diaryService.createDiaryPost(diaryCreateRequest, userDetails);

        assertNotNull(responseDto);
        assertEquals(savedDiary.getId(), responseDto.getId());
        assertEquals(diaryCreateRequest.getTitle(), responseDto.getTitle());
        assertEquals(diaryCreateRequest.getContent(), responseDto.getContent());
        assertEquals(testUser.getUserName(), responseDto.getUserName());

        verify(userRepository, times(1)).findByUserName(testUser.getUserName());
        verify(diaryRepository, times(1)).save(any(Diary.class));
    }

    @Test
    @DisplayName("createDiaryPost_userNotFound")
    void createDiaryPost_userNotFound() {
        when(userDetails.getUsername()).thenReturn("unknownUser");
        when(userRepository.findByUserName("unknownUser")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            diaryService.createDiaryPost(diaryCreateRequest, userDetails);
        });

        verify(userRepository, times(1)).findByUserName("unknownUser");
        verify(diaryRepository, never()).save(any(Diary.class));
    }

    // --- updateDiaryPost Tests ---
    @Test
    @DisplayName("updateDiaryPost_success")
    void updateDiaryPost_success() {
        when(userDetails.getUsername()).thenReturn(testUser.getUserName());
        when(userRepository.findByUserName(testUser.getUserName())).thenReturn(Optional.of(testUser));
        when(diaryRepository.findById(testDiary.getId())).thenReturn(Optional.of(testDiary));
        
        Diary updatedDiary = Diary.builder()
                                .id(testDiary.getId())
                                .user(testUser)
                                .title(diaryUpdateRequest.getTitle())
                                .content(diaryUpdateRequest.getContent())
                                .build();
        when(diaryRepository.save(any(Diary.class))).thenReturn(updatedDiary);

        DiaryResponseDto responseDto = diaryService.updateDiaryPost(testDiary.getId(), diaryUpdateRequest, userDetails);

        assertNotNull(responseDto);
        assertEquals(testDiary.getId(), responseDto.getId());
        assertEquals(diaryUpdateRequest.getTitle(), responseDto.getTitle());
        assertEquals(diaryUpdateRequest.getContent(), responseDto.getContent());

        verify(userRepository, times(1)).findByUserName(testUser.getUserName());
        verify(diaryRepository, times(1)).findById(testDiary.getId());
        verify(diaryRepository, times(1)).save(any(Diary.class));
    }

    @Test
    @DisplayName("updateDiaryPost_diaryNotFound")
    void updateDiaryPost_diaryNotFound() {
        when(userDetails.getUsername()).thenReturn(testUser.getUserName());
        when(userRepository.findByUserName(testUser.getUserName())).thenReturn(Optional.of(testUser));
        when(diaryRepository.findById(testDiary.getId())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            diaryService.updateDiaryPost(testDiary.getId(), diaryUpdateRequest, userDetails);
        });

        verify(userRepository, times(1)).findByUserName(testUser.getUserName());
        verify(diaryRepository, times(1)).findById(testDiary.getId());
        verify(diaryRepository, never()).save(any(Diary.class));
    }

    @Test
    @DisplayName("updateDiaryPost_accessDenied")
    void updateDiaryPost_accessDenied() {
        when(userDetails.getUsername()).thenReturn(otherUser.getUserName()); // Current user is otherUser
        when(userRepository.findByUserName(otherUser.getUserName())).thenReturn(Optional.of(otherUser));
        // testDiary is owned by testUser
        when(diaryRepository.findById(testDiary.getId())).thenReturn(Optional.of(testDiary)); 

        assertThrows(AccessDeniedException.class, () -> {
            diaryService.updateDiaryPost(testDiary.getId(), diaryUpdateRequest, userDetails);
        });

        verify(userRepository, times(1)).findByUserName(otherUser.getUserName());
        verify(diaryRepository, times(1)).findById(testDiary.getId());
        verify(diaryRepository, never()).save(any(Diary.class));
    }

    // --- deleteDiaryPost Tests ---
    @Test
    @DisplayName("deleteDiaryPost_success")
    void deleteDiaryPost_success() {
        when(userDetails.getUsername()).thenReturn(testUser.getUserName());
        when(userRepository.findByUserName(testUser.getUserName())).thenReturn(Optional.of(testUser));
        when(diaryRepository.findById(testDiary.getId())).thenReturn(Optional.of(testDiary));

        diaryService.deleteDiaryPost(testDiary.getId(), userDetails);

        verify(userRepository, times(1)).findByUserName(testUser.getUserName());
        verify(diaryRepository, times(1)).findById(testDiary.getId());
        verify(diaryRepository, times(1)).delete(testDiary);
    }

    @Test
    @DisplayName("deleteDiaryPost_diaryNotFound")
    void deleteDiaryPost_diaryNotFound() {
        when(userDetails.getUsername()).thenReturn(testUser.getUserName());
        when(userRepository.findByUserName(testUser.getUserName())).thenReturn(Optional.of(testUser));
        when(diaryRepository.findById(testDiary.getId())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            diaryService.deleteDiaryPost(testDiary.getId(), userDetails);
        });

        verify(userRepository, times(1)).findByUserName(testUser.getUserName());
        verify(diaryRepository, times(1)).findById(testDiary.getId());
        verify(diaryRepository, never()).delete(any(Diary.class));
    }

    @Test
    @DisplayName("deleteDiaryPost_accessDenied")
    void deleteDiaryPost_accessDenied() {
        when(userDetails.getUsername()).thenReturn(otherUser.getUserName()); // Current user is otherUser
        when(userRepository.findByUserName(otherUser.getUserName())).thenReturn(Optional.of(otherUser));
        // testDiary is owned by testUser
        when(diaryRepository.findById(testDiary.getId())).thenReturn(Optional.of(testDiary));

        assertThrows(AccessDeniedException.class, () -> {
            diaryService.deleteDiaryPost(testDiary.getId(), userDetails);
        });

        verify(userRepository, times(1)).findByUserName(otherUser.getUserName());
        verify(diaryRepository, times(1)).findById(testDiary.getId());
        verify(diaryRepository, never()).delete(any(Diary.class));
    }

    // --- findDiaryById Tests ---
    @Test
    @DisplayName("findDiaryById_success")
    void findDiaryById_success() {
        // For getCurrentUser() in findDiaryById
        when(authentication.getName()).thenReturn(testUser.getUserName());
        when(userRepository.findByUserName(testUser.getUserName())).thenReturn(Optional.of(testUser));
        
        // findDiaryById uses findByIdAndUserId
        when(diaryRepository.findByIdAndUserId(testDiary.getId(), testUser.getId())).thenReturn(Optional.of(testDiary));

        DiaryResponseDto responseDto = diaryService.findDiaryById(testDiary.getId());

        assertNotNull(responseDto);
        assertEquals(testDiary.getId(), responseDto.getId());
        assertEquals(testDiary.getTitle(), responseDto.getTitle());
        assertEquals(testDiary.getContent(), responseDto.getContent());
        assertEquals(testUser.getUserName(), responseDto.getUserName());

        verify(authentication, times(1)).getName();
        verify(userRepository, times(1)).findByUserName(testUser.getUserName());
        verify(diaryRepository, times(1)).findByIdAndUserId(testDiary.getId(), testUser.getId());
    }

    @Test
    @DisplayName("findDiaryById_notFoundOrAccessDenied")
    void findDiaryById_notFoundOrAccessDenied() {
        // For getCurrentUser() in findDiaryById
        when(authentication.getName()).thenReturn(testUser.getUserName());
        when(userRepository.findByUserName(testUser.getUserName())).thenReturn(Optional.of(testUser));
        
        // findDiaryById uses findByIdAndUserId, returns empty if not found or user mismatch
        when(diaryRepository.findByIdAndUserId(testDiary.getId(), testUser.getId())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            diaryService.findDiaryById(testDiary.getId());
        });
        
        verify(authentication, times(1)).getName();
        verify(userRepository, times(1)).findByUserName(testUser.getUserName());
        verify(diaryRepository, times(1)).findByIdAndUserId(testDiary.getId(), testUser.getId());
    }
}
