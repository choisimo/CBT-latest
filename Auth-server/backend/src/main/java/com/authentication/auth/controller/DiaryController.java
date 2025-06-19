package com.authentication.auth.controller;

import com.authentication.auth.dto.diary.*;
import com.authentication.auth.dto.response.ApiResponse;
import com.authentication.auth.service.diary.DiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController("diaryController")
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Diary API", description = "다이어리 관리 API")
public class DiaryController {

    private final DiaryService diaryService;

    /**
     * 다이어리 생성
     */
    @PostMapping
    @Operation(summary = "다이어리 생성", description = "새로운 다이어리를 생성하고 AI 분석을 수행합니다.")
    public ResponseEntity<ApiResponse<DiaryResponse>> createDiary(
            @Valid @RequestBody DiaryCreateRequest request) {
        String email = getCurrentUserEmail();
        DiaryResponse response = diaryService.createDiary(email, request);
        log.info("다이어리 생성 성공 - email: {}, diaryId: {}", email, response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "다이어리가 성공적으로 생성되었습니다."));
    }

    /**
     * 다이어리 목록 조회
     */
    @GetMapping
    @Operation(summary = "다이어리 목록 조회", description = "사용자의 다이어리 목록을 페이징으로 조회합니다.")
    public ResponseEntity<ApiResponse<Page<DiaryListItem>>> getDiaries(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {
        String email = getCurrentUserEmail();
        Pageable pageable = PageRequest.of(page, size);
        Page<DiaryListItem> diaries = diaryService.getDiaries(email, pageable);
        return ResponseEntity.ok(ApiResponse.success(diaries, "다이어리 목록이 성공적으로 조회되었습니다."));
    }

    /**
     * 다이어리 상세 조회
     */
    @GetMapping("/{diaryId}")
    @Operation(summary = "다이어리 상세 조회", description = "특정 다이어리의 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<DiaryDetailResponse>> getDiary(
            @Parameter(description = "다이어리 ID") @PathVariable Long diaryId) {
        String email = getCurrentUserEmail();
        DiaryDetailResponse diary = diaryService.getDiary(email, diaryId);
        return ResponseEntity.ok(ApiResponse.success(diary, "다이어리 상세 정보가 성공적으로 조회되었습니다."));
    }

    /**
     * 다이어리 수정
     */
    @PutMapping("/{diaryId}")
    @Operation(summary = "다이어리 수정", description = "다이어리를 수정하고 AI 재분석을 수행합니다.")
    public ResponseEntity<ApiResponse<DiaryResponse>> updateDiary(
            @Parameter(description = "다이어리 ID") @PathVariable Long diaryId,
            @Valid @RequestBody DiaryUpdateRequest request) {
        String email = getCurrentUserEmail();
        DiaryResponse response = diaryService.updateDiary(email, diaryId, request);
        log.info("다이어리 수정 성공 - email: {}, diaryId: {}", email, diaryId);
        return ResponseEntity.ok(ApiResponse.success(response, "다이어리가 성공적으로 수정되었습니다."));
    }

    /**
     * 다이어리 삭제
     */
    @DeleteMapping("/{diaryId}")
    @Operation(summary = "다이어리 삭제", description = "특정 다이어리를 삭제합니다.")
    public ResponseEntity<ApiResponse<Void>> deleteDiary(
            @Parameter(description = "다이어리 ID") @PathVariable Long diaryId) {
        String email = getCurrentUserEmail();
        diaryService.deleteDiary(email, diaryId);
        log.info("다이어리 삭제 성공 - email: {}, diaryId: {}", email, diaryId);
        return ResponseEntity.ok(ApiResponse.success(null, "다이어리가 성공적으로 삭제되었습니다."));
    }

    /**
     * 다이어리 통계 조회
     */
    @GetMapping("/stats")
    @Operation(summary = "다이어리 통계 조회", description = "사용자의 다이어리 통계를 조회합니다.")
    public ResponseEntity<ApiResponse<DiaryService.DiaryStatsResponse>> getDiaryStats() {
        String email = getCurrentUserEmail();
        DiaryService.DiaryStatsResponse stats = diaryService.getDiaryStats(email);
        return ResponseEntity.ok(ApiResponse.success(stats, "다이어리 통계가 성공적으로 조회되었습니다."));
    }

    /**
     * 다이어리 검색
     */
    @GetMapping("/search")
    @Operation(summary = "다이어리 검색", description = "키워드로 다이어리를 검색합니다.")
    public ResponseEntity<ApiResponse<Page<DiaryListItem>>> searchDiaries(
            @Parameter(description = "검색 키워드") @RequestParam String keyword,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {
        String email = getCurrentUserEmail();
        Pageable pageable = PageRequest.of(page, size);
        Page<DiaryListItem> diaries = diaryService.searchDiaries(email, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(diaries, "다이어리 검색 결과가 성공적으로 조회되었습니다."));
    }

    /**
     * 부정적인 다이어리 조회
     */
    @GetMapping("/negative")
    @Operation(summary = "부정적인 다이어리 조회", description = "부정적인 감정의 다이어리만 조회합니다.")
    public ResponseEntity<ApiResponse<Page<DiaryListItem>>> getNegativeDiaries(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {
        String email = getCurrentUserEmail();
        Pageable pageable = PageRequest.of(page, size);
        Page<DiaryListItem> diaries = diaryService.getNegativeDiaries(email, pageable);
        return ResponseEntity.ok(ApiResponse.success(diaries, "부정적인 다이어리 목록이 성공적으로 조회되었습니다."));
    }

    /**
     * 현재 로그인한 사용자 ID 추출
     */
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }
        // PrincipalDetails.username 은 email 이므로 그대로 반환
        return authentication.getName();
    }
}