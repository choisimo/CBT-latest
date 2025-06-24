package com.authentication.auth.controller;

import com.authentication.auth.dto.AIResponseDto;
import com.authentication.auth.dto.DiaryRequestDto;
import com.authentication.auth.dto.diary.DiaryCreateRequest;
import com.authentication.auth.dto.diary.DiaryUpdateRequest;
import com.authentication.auth.diary.dto.DiaryResponseDto;
import com.authentication.auth.service.DiaryService;
import com.authentication.auth.service.diary.DiaryManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;
    private final DiaryManagementService diaryManagementService;

    /**
     * 일기 생성 (CREATE) - 프론트엔드가 사용하는 기본 CRUD 엔드포인트
     */
    @PostMapping
    public ResponseEntity<DiaryResponseDto> createDiary(
            @Valid @RequestBody DiaryCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("일기 생성 요청 - 사용자: {}, 제목: {}", userDetails.getUsername(), request.getTitle());
        DiaryResponseDto response = diaryManagementService.createDiaryPost(request, userDetails);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * 일기 목록 조회 (READ) - 페이징 처리
     */
    @GetMapping
    public ResponseEntity<Page<DiaryResponseDto>> getDiaries(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("사용자 {}의 일기 목록 조회 요청, 페이지 정보: {}", userDetails.getUsername(), pageable);
        Page<DiaryResponseDto> diaries = diaryManagementService.findDiariesByUser(userDetails, pageable);
        return ResponseEntity.ok(diaries);
    }

    /**
     * 일기 조회 (READ) - 단일 일기 조회
     */
    @GetMapping("/{diaryId}")
    public ResponseEntity<DiaryResponseDto> getDiary(@PathVariable Long diaryId) {
        log.info("일기 조회 요청 - ID: {}", diaryId);
        DiaryResponseDto response = diaryManagementService.findDiaryById(diaryId);
        return ResponseEntity.ok(response);
    }

    /**
     * 일기 수정 (UPDATE)
     */
    @PutMapping("/{diaryId}")
    public ResponseEntity<DiaryResponseDto> updateDiary(
            @PathVariable Long diaryId,
            @Valid @RequestBody DiaryUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("일기 수정 요청 - ID: {}, 사용자: {}", diaryId, userDetails.getUsername());
        DiaryResponseDto response = diaryManagementService.updateDiaryPost(diaryId, request, userDetails);
        return ResponseEntity.ok(response);
    }

    /**
     * 일기 삭제 (DELETE)
     */
    @DeleteMapping("/{diaryId}")
    public ResponseEntity<Void> deleteDiary(
            @PathVariable Long diaryId,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("일기 삭제 요청 - ID: {}, 사용자: {}", diaryId, userDetails.getUsername());
        diaryManagementService.deleteDiaryPost(diaryId, userDetails);
        return ResponseEntity.noContent().build();
    }

    /**
     * 일기 분석 요청 (CREATE/READ)
     */
    @PostMapping("/{diaryId}/analysis")
    public ResponseEntity<AIResponseDto> getDiaryAnalysis(@PathVariable Long diaryId) {
        log.info("일기 분석 요청 - diaryId: {}", diaryId);
        AIResponseDto response = diaryService.getAnalysisByDiaryId(diaryId);
        return ResponseEntity.ok(response);
    }

    /**
     * 일기 분석 (CREATE)
     */
    @PostMapping("/analyze")
    public ResponseEntity<AIResponseDto> analyzeDiary(@Valid @RequestBody DiaryRequestDto request) {
        log.info("일기 분석 요청 - 사용자: {}, 제목: {}", request.getUserId(), request.getTitle());
        AIResponseDto response = diaryService.analyzeAndSaveDiary(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 전체 조회 (READ ALL)
     */
    @GetMapping("/responses/{userId}")
    public ResponseEntity<List<AIResponseDto>> getAllResponses(@PathVariable String userId) {
        log.info("전체 응답 조회 - 사용자: {}", userId);
        List<AIResponseDto> responses = diaryService.getAllAIResponses(userId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 단건 조회 (READ ONE)
     */
    @GetMapping("/response/{id}")
    public ResponseEntity<AIResponseDto> getResponse(@PathVariable String id) {
        log.info("응답 조회 - ID: {}", id);
        Optional<AIResponseDto> response = diaryService.getAIResponseById(id);
        return response.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 날짜별 조회 (READ BY DATE)
     */
    @GetMapping("/responses/{userId}/range")
    public ResponseEntity<List<AIResponseDto>> getResponsesByDate(
            @PathVariable String userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        log.info("날짜별 조회 - 사용자: {}, 기간: {} ~ {}", userId, startDate, endDate);
        List<AIResponseDto> responses = diaryService.getAIResponsesByDateRange(userId, startDate, endDate);
        return ResponseEntity.ok(responses);
    }

    /**
     * 수정 (UPDATE)
     */
    @PutMapping("/response/{id}")
    public ResponseEntity<AIResponseDto> updateResponse(
            @PathVariable String id,
            @RequestBody AIResponseDto updateDto) {
        log.info("응답 수정 - ID: {}", id);
        AIResponseDto response = diaryService.updateAIResponse(id, updateDto);
        return ResponseEntity.ok(response);
    }

    /**
     * 삭제 (DELETE)
     */
    @DeleteMapping("/response/{id}")
    public ResponseEntity<String> deleteResponse(@PathVariable String id) {
        log.info("응답 삭제 - ID: {}", id);
        diaryService.deleteAIResponse(id);
        return ResponseEntity.ok("삭제 완료");
    }

    /**
     * 상태 확인
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Diary API 정상 동작");
    }



}