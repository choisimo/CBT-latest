package com.authentication.auth.service.diary;

import com.authentication.auth.domain.Diary;
import com.authentication.auth.domain.User;
import com.authentication.auth.service.DiaryService;
import com.authentication.auth.diary.repository.DiaryRepository;
import com.authentication.auth.repository.UserRepository;
import com.authentication.auth.service.ai.AiClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DiaryService 유닛 테스트.
 */
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class DiaryServiceTest {

    @Mock
    private DiaryRepository diaryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AiClientService aiClientService;

    @InjectMocks
    private DiaryService diaryService;

    private User testUser;
    private Diary testDiary;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("test@example.com").nickname("testUser").build(); // Assuming nickname is part of User
        testDiary = Diary.builder()
                .id(1L)
                .user(testUser)
                .title("Test Diary")
                .content("This is a test diary.")
                .createdAt(LocalDateTime.now())
                .isNegative(false)
                .alternativeThought(null)
                .build();
    }

    @Test
    @DisplayName("다이어리 생성 성공")
    void createDiary_Success() {
        // Given
        DiaryCreateRequest request = new DiaryCreateRequest("New Diary", "Content of new diary.");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        // Mocking the first save (before AI analysis)
        when(diaryRepository.save(any(Diary.class))).thenAnswer(invocation -> {
            Diary diaryToSave = invocation.getArgument(0);
            // Simulate ID generation and return the diary with ID
            return Diary.builder()
                        .id(2L) // new ID
                        .user(diaryToSave.getUser())
                        .title(diaryToSave.getTitle())
                        .content(diaryToSave.getContent())
                        .isNegative(diaryToSave.isNegative())
                        .alternativeThought(diaryToSave.getAlternativeThought())
                        .createdAt(diaryToSave.getCreatedAt() != null ? diaryToSave.getCreatedAt() : LocalDateTime.now())
                        .build();
        });
        
        AiClientService.DiaryAnalysisResult analysisResult = new AiClientService.DiaryAnalysisResult(false, null, null);
        when(aiClientService.analyzeDiary(request.content())).thenReturn(Mono.just(analysisResult));

        // When
        DiaryResponse response = diaryService.createDiary(1L, request);

        // Then
        assertThat(response.id()).isEqualTo(2L); // ID from mocked save
        assertThat(response.title()).isEqualTo(request.title());
        assertThat(response.content()).isEqualTo(request.content());
        
        // Verify save is called twice: once for initial creation, once after AI analysis (if successful)
        verify(diaryRepository, times(2)).save(any(Diary.class));
        verify(aiClientService).analyzeDiary(request.content());
    }
    
    @Test
    @DisplayName("다이어리 생성 실패 - 사용자를 찾을 수 없음")
    void createDiary_Failure_UserNotFound() {
        // Given
        DiaryCreateRequest request = new DiaryCreateRequest("New Diary", "Content");
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            diaryService.createDiary(1L, request);
        });
        assertThat(exception.getMessage()).isEqualTo("사용자를 찾을 수 없습니다.");
        verify(diaryRepository, never()).save(any(Diary.class));
        verify(aiClientService, never()).analyzeDiary(anyString());
    }


    @Test
    @DisplayName("다이어리 목록 조회 성공")
    void getDiaries_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Diary> diaryPage = new PageImpl<>(Collections.singletonList(testDiary), pageable, 1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(diaryRepository.findByUserOrderByCreatedAtDesc(testUser, pageable)).thenReturn(diaryPage);

        // When
        Page<DiaryListItem> result = diaryService.getDiaries(1L, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).title()).isEqualTo(testDiary.getTitle());
    }
    
    @Test
    @DisplayName("다이어리 목록 조회 실패 - 사용자를 찾을 수 없음")
    void getDiaries_Failure_UserNotFound() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            diaryService.getDiaries(1L, pageable);
        });
        assertThat(exception.getMessage()).isEqualTo("사용자를 찾을 수 없습니다.");
        verify(diaryRepository, never()).findByUserOrderByCreatedAtDesc(any(User.class), any(Pageable.class));
    }

    @Test
    @DisplayName("다이어리 상세 조회 성공")
    void getDiary_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(diaryRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testDiary));

        // When
        DiaryDetailResponse response = diaryService.getDiary(1L, 1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(testDiary.getId());
        assertThat(response.title()).isEqualTo(testDiary.getTitle());
    }
    
    @Test
    @DisplayName("다이어리 상세 조회 실패 - 다이어리를 찾을 수 없음")
    void getDiary_Failure_DiaryNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(diaryRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            diaryService.getDiary(1L, 1L);
        });
        assertThat(exception.getMessage()).isEqualTo("다이어리를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("다이어리 수정 성공 - 내용 변경")
    void updateDiary_Success_ContentChanged() {
        // Given
        DiaryUpdateRequest request = new DiaryUpdateRequest("Updated Title", "Updated Content");
        Diary existingDiary = Diary.builder().id(1L).user(testUser).title("Old Title").content("Old Content").isNegative(false).build();
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(diaryRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(existingDiary));
        
        // Mocking the save operation for the update itself
        when(diaryRepository.save(any(Diary.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        AiClientService.DiaryAnalysisResult analysisResult = new AiClientService.DiaryAnalysisResult(true, "Alternative thought", null);
        when(aiClientService.analyzeDiary(request.content())).thenReturn(Mono.just(analysisResult));

        // When
        DiaryResponse response = diaryService.updateDiary(1L, 1L, request);

        // Then
        assertThat(response.title()).isEqualTo(request.title());
        assertThat(response.content()).isEqualTo(request.content());
        // Verify save is called twice: once for the main update, once after AI analysis
        verify(diaryRepository, times(2)).save(any(Diary.class)); 
        verify(aiClientService).analyzeDiary(request.content());
    }
    
    @Test
    @DisplayName("다이어리 수정 성공 - 내용 변경 없을 시 AI 분석 호출 안함")
    void updateDiary_Success_NoContentChange() {
        // Given
        DiaryUpdateRequest request = new DiaryUpdateRequest("Updated Title Only", null); // Content is null
        Diary existingDiary = Diary.builder().id(1L).user(testUser).title("Old Title").content("Old Content").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(diaryRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(existingDiary));
        when(diaryRepository.save(any(Diary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        DiaryResponse response = diaryService.updateDiary(1L, 1L, request);

        // Then
        assertThat(response.title()).isEqualTo(request.title());
        assertThat(response.content()).isEqualTo("Old Content"); // Content should not change
        verify(diaryRepository, times(1)).save(any(Diary.class));
        verify(aiClientService, never()).analyzeDiary(anyString()); 
    }


    @Test
    @DisplayName("다이어리 삭제 성공")
    void deleteDiary_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(diaryRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testDiary));
        doNothing().when(diaryRepository).delete(testDiary);

        // When
        diaryService.deleteDiary(1L, 1L);

        // Then
        verify(diaryRepository).delete(testDiary);
    }

    @Test
    @DisplayName("다이어리 통계 조회 성공")
    void getDiaryStats_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(diaryRepository.countByUser(testUser)).thenReturn(5L);
        
        // Mock for negative count
        Page<Diary> negativeDiariesPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0,1), 0); // 0 negative diaries
        when(diaryRepository.findByUserAndIsNegativeTrueOrderByCreatedAtDesc(eq(testUser), any(Pageable.class)))
                .thenReturn(negativeDiariesPage);

        // When
        DiaryService.DiaryStatsResponse stats = diaryService.getDiaryStats(1L);

        // Then
        assertThat(stats.totalCount()).isEqualTo(5);
        assertThat(stats.negativeCount()).isEqualTo(0); // Based on mocked negativeDiariesPage
        assertThat(stats.positiveCount()).isEqualTo(5); // 5 total - 0 negative
    }

    @Test
    @DisplayName("키워드로 다이어리 검색 성공")
    void searchDiaries_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Diary> diaryPage = new PageImpl<>(Collections.singletonList(testDiary), pageable, 1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(diaryRepository.findByUserAndTitleContaining(testUser, "Test", pageable)).thenReturn(diaryPage);

        // When
        Page<DiaryListItem> result = diaryService.searchDiaries(1L, "Test", pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).title()).isEqualTo(testDiary.getTitle());
    }

    @Test
    @DisplayName("부정적인 감정의 다이어리만 조회 성공")
    void getNegativeDiaries_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Diary negativeDiary = Diary.builder() // Create a specifically negative diary for this test
                                .id(2L)
                                .user(testUser)
                                .title("Negative Diary")
                                .content("This is a negative test diary.")
                                .createdAt(LocalDateTime.now())
                                .isNegative(true) // Set diary to negative
                                .build();
        Page<Diary> diaryPage = new PageImpl<>(Collections.singletonList(negativeDiary), pageable, 1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(diaryRepository.findByUserAndIsNegativeTrueOrderByCreatedAtDesc(testUser, pageable)).thenReturn(diaryPage);

        // When
        Page<DiaryListItem> result = diaryService.getNegativeDiaries(1L, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        DiaryListItem item = result.getContent().get(0);
        assertThat(item.title()).isEqualTo(negativeDiary.getTitle());
        assertThat(item.isNegative()).isTrue();
    }
}
