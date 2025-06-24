package com.authentication.auth.controller;

import com.authentication.auth.dto.AIResponseDto;
import com.authentication.auth.dto.diary.DiaryRequestDto;
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
     * 일기 분석 조회 (GET) - 분석 결과만 조회
     */
    @GetMapping("/{diaryId}/analysis")
    public ResponseEntity<?> getAnalysisResult(@PathVariable Long diaryId) {
        log.info("일기 분석 결과 조회 - diaryId: {}", diaryId);
        AIResponseDto response = diaryService.getAnalysisByDiaryId(diaryId);
        if (response == null) {
            return ResponseEntity.ok(java.util.Collections.emptyMap());
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 일기 분석 요청 또는 조회 (POST) - 분석 요청 또는 기존 결과 반환
     */
    @PostMapping("/{diaryId}/analysis")
    public ResponseEntity<?> requestOrGetDiaryAnalysis(
            @PathVariable Long diaryId,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("일기 분석 요청/조회 - diaryId: {}, 사용자: {}", diaryId, userDetails != null ? userDetails.getUsername() : "anonymous");
        
        try {
            // 먼저 기존 분석 결과가 있는지 확인
            AIResponseDto existingResponse = diaryService.getAnalysisByDiaryId(diaryId);
            
            if (existingResponse != null && "COMPLETED".equals(existingResponse.getStatus())) {
                // 완료된 분석 결과가 있으면 반환
                log.info("기존 완료된 분석 결과 반환 - diaryId: {}", diaryId);
                return ResponseEntity.ok(existingResponse);
            } else {
                // 분석 결과가 없거나 완료되지 않았으면 새로 분석 요청
                log.info("새로운 분석 요청 시작 - diaryId: {}", diaryId);
                String username = userDetails != null ? userDetails.getUsername() : "anonymous";
                diaryService.requestAnalysis(diaryId, username);
                
                // 즉시 현재 상태 반환 (PENDING 또는 PROCESSING)
                AIResponseDto currentResponse = diaryService.getAnalysisByDiaryId(diaryId);
                if (currentResponse != null) {
                    return ResponseEntity.ok(currentResponse);
                } else {
                    return ResponseEntity.ok(java.util.Map.of(
                        "status", "PENDING",
                        "message", "분석이 시작되었습니다. 잠시 후 결과를 확인해주세요."
                    ));
                }
            }
        } catch (Exception e) {
            log.error("일기 분석 요청 중 오류 발생 - diaryId: {}, 오류: {}", diaryId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of(
                        "status", "FAILED",
                        "message", "분석 요청 중 오류가 발생했습니다: " + e.getMessage()
                    ));
        }
    }

    /**
     * 일기 분석 수동 트리거 (FORCE ANALYSIS)
     * 기존 분석 결과가 없거나 재분석이 필요한 경우 사용
     */
    @PostMapping("/{diaryId}/analysis/trigger")
    public ResponseEntity<String> triggerDiaryAnalysis(
            @PathVariable Long diaryId, 
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("일기 분석 수동 트리거 요청 - diaryId: {}, 사용자: {}", diaryId, userDetails.getUsername());
        
        try {
            // 현재 사용자의 일기인지 확인 후 분석 요청
            DiaryResponseDto diary = diaryManagementService.findDiaryById(diaryId);
            diaryService.requestAnalysis(diaryId, userDetails.getUsername());
            
            log.info("일기 분석이 백그라운드에서 시작되었습니다 - diaryId: {}", diaryId);
            return ResponseEntity.ok("분석이 시작되었습니다. 잠시 후 결과를 확인해주세요.");
            
        } catch (Exception e) {
            log.error("일기 분석 트리거 중 오류 발생 - diaryId: {}, 오류: {}", diaryId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("분석 요청 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 개발/디버깅용 임시 엔드포인트 - 인증 없이 분석 트리거
     * TODO: 프로덕션에서는 제거해야 함
     */
    @PostMapping("/{diaryId}/analysis/debug-trigger")
    public ResponseEntity<String> debugTriggerDiaryAnalysis(@PathVariable Long diaryId) {
        log.info("디버그 일기 분석 트리거 요청 - diaryId: {}", diaryId);
        
        try {
            // 인증 없이 바로 분석 요청 (디버깅용)
            diaryService.requestAnalysis(diaryId, "debug-user");
            
            log.info("디버그 일기 분석이 백그라운드에서 시작되었습니다 - diaryId: {}", diaryId);
            return ResponseEntity.ok("디버그 분석이 시작되었습니다. 잠시 후 결과를 확인해주세요.");
            
        } catch (Exception e) {
            log.error("디버그 일기 분석 트리거 중 오류 발생 - diaryId: {}, 오류: {}", diaryId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("디버그 분석 요청 중 오류가 발생했습니다: " + e.getMessage());
        }
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