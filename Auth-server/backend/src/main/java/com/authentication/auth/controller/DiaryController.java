package com.authentication.auth.controller;

import com.authentication.auth.dto.diary.*;
import com.authentication.auth.dto.response.ApiResponse;
import com.authentication.auth.service.diary.DiaryService;
import com.authentication.auth.exception.ErrorType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController("diaryController")

@RequestMapping("/api/diaryposts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Diary API", description = "다이어리 관리 API")
public class DiaryController {

    private final DiaryService diaryService;

    /**
     * 다이어리 생성 (기존 /api/diaries POST 유지 또는 /api/diaryposts POST로 변경 협의 필요)
     * 여기서는 기존 엔드포인트를 유지한다고 가정하고, 새 엔드포인트만 추가합니다.
     * 만약 /api/diaryposts POST로 통합한다면 아래 메서드 주석 해제 및 경로 수정
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
     * 인증된 사용자의 일기 게시물 목록을 조건에 따라 조회 (신규)
     */
    @GetMapping
    @Operation(summary = "일기 게시물 목록 조회", description = "인증된 사용자의 일기 게시물 목록을 조건(검색, 날짜)에 따라 조회합니다.")
    public ResponseEntity<?> getDiaryPosts(
            @Parameter(description = "검색 키워드 (제목 및 내용)") @RequestParam(required = false) String q,
            @Parameter(description = "조회할 날짜 (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        if (q != null && date != null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ErrorType.INVALID_REQUEST_PARAMETER, Map.of("message", "검색 키워드(q)와 날짜(date)는 함께 사용할 수 없습니다.")));
        }

        String email = getCurrentUserEmail();
        Pageable pageable = PageRequest.of(page, size);

        if (date != null) {
            List<DiaryPostDetailDto> diaryDetails = diaryService.getDiariesByDate(email, date);
            return ResponseEntity.ok(ApiResponse.success(diaryDetails, "지정된 날짜의 일기 목록이 성공적으로 조회되었습니다."));
        } else {
            DiaryPostListResponseDto diaryListResponse = diaryService.getDiaryPosts(email, q, pageable);
            return ResponseEntity.ok(ApiResponse.success(diaryListResponse, "일기 게시물 목록이 성공적으로 조회되었습니다."));
        }
    }


    /**
     * 다이어리 상세 조회 (기존 /api/diaries/{diaryId} GET 유지 또는 /api/diaryposts/{postId} GET로 변경 협의 필요)
     * 여기서는 기존 엔드포인트를 유지한다고 가정합니다.
     */
    @GetMapping("/{diaryId}")
    @Operation(summary = "다이어리 상세 조회 (기존)", description = "특정 다이어리의 상세 정보를 조회합니다. /api/diaryposts/{postId} 와 동일 기능 제공 가능")
    public ResponseEntity<ApiResponse<DiaryDetailResponse>> getDiary(
            @Parameter(description = "다이어리 ID") @PathVariable Long diaryId) {
        String email = getCurrentUserEmail();
        // DiaryDetailResponse는 기존 DTO를 활용하거나, DiaryPostDetailDto로 대체 가능
        DiaryDetailResponse diary = diaryService.getDiary(email, diaryId);
        return ResponseEntity.ok(ApiResponse.success(diary, "다이어리 상세 정보가 성공적으로 조회되었습니다."));
    }

    /**
     * 다이어리 수정 (기존 /api/diaries/{diaryId} PUT 유지 또는 /api/diaryposts/{postId} PUT로 변경 협의 필요)
     */
    @PutMapping("/{diaryId}")
    @Operation(summary = "다이어리 수정 (기존)", description = "다이어리를 수정하고 AI 재분석을 수행합니다.")
    public ResponseEntity<ApiResponse<DiaryResponse>> updateDiary(
            @Parameter(description = "다이어리 ID") @PathVariable Long diaryId,
            @Valid @RequestBody DiaryUpdateRequest request) {
        String email = getCurrentUserEmail();
        DiaryResponse response = diaryService.updateDiary(email, diaryId, request);
        log.info("다이어리 수정 성공 - email: {}, diaryId: {}", email, diaryId);
        return ResponseEntity.ok(ApiResponse.success(response, "다이어리가 성공적으로 수정되었습니다."));
    }

    /**
     * 다이어리 삭제 (기존 /api/diaries/{diaryId} DELETE 유지 또는 /api/diaryposts/{postId} DELETE로 변경 협의 필요)
     */
    @DeleteMapping("/{diaryId}")
    @Operation(summary = "다이어리 삭제 (기존)", description = "특정 다이어리를 삭제합니다.")
    public ResponseEntity<ApiResponse<Void>> deleteDiary(
            @Parameter(description = "다이어리 ID") @PathVariable Long diaryId) {
        String email = getCurrentUserEmail();
        diaryService.deleteDiary(email, diaryId);
        log.info("다이어리 삭제 성공 - email: {}, diaryId: {}", email, diaryId);
        return ResponseEntity.ok(ApiResponse.success(null, "다이어리가 성공적으로 삭제되었습니다."));
    }

    /**
     * 다이어리 통계 조회 (기존 /api/diaries/stats GET 유지 또는 /api/diaryposts/stats GET로 변경 협의 필요)
     */
    @GetMapping("/stats")
    @Operation(summary = "다이어리 통계 조회 (기존)", description = "사용자의 다이어리 통계를 조회합니다.")
    public ResponseEntity<ApiResponse<DiaryService.DiaryStatsResponse>> getDiaryStats() {
        String email = getCurrentUserEmail();
        DiaryService.DiaryStatsResponse stats = diaryService.getDiaryStats(email);
        return ResponseEntity.ok(ApiResponse.success(stats, "다이어리 통계가 성공적으로 조회되었습니다."));
    }


    // 기존 /api/diaries/search 와 /api/diaries/negative 는 새로운 GET /api/diaryposts 로 통합됨.
    // 필요시 기존 엔드포인트들은 @Deprecated 처리하거나 삭제. 여기서는 주석 처리.
    /*
    @GetMapping("/search")
    @Operation(summary = "다이어리 검색 (Deprecated)", description = "키워드로 다이어리를 검색합니다. GET /api/diaryposts?q={keyword}를 사용하세요.")
    @Deprecated
    public ResponseEntity<ApiResponse<Page<DiaryListItem>>> searchDiaries(
            @Parameter(description = "검색 키워드") @RequestParam String keyword,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {
        String email = getCurrentUserEmail();
        Pageable pageable = PageRequest.of(page, size);
        Page<DiaryListItem> diaries = diaryService.searchDiaries(email, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(diaries, "다이어리 검색 결과가 성공적으로 조회되었습니다."));
    }

    @GetMapping("/negative")
    @Operation(summary = "부정적인 다이어리 조회 (Deprecated)", description = "부정적인 감정의 다이어리만 조회합니다. GET /api/diaryposts?isNegative=true (미구현) 또는 다른 방식으로 필터링하세요.")
    @Deprecated
    public ResponseEntity<ApiResponse<Page<DiaryListItem>>> getNegativeDiaries(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {
        String email = getCurrentUserEmail();
        Pageable pageable = PageRequest.of(page, size);
        Page<DiaryListItem> diaries = diaryService.getNegativeDiaries(email, pageable);
        return ResponseEntity.ok(ApiResponse.success(diaries, "부정적인 다이어리 목록이 성공적으로 조회되었습니다."));
    }
    */

    /**
     * 현재 로그인한 사용자 ID 추출
     */
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            // 실제 프로덕션에서는 ErrorType 등을 사용하여 좀 더 구조화된 예외 처리가 필요할 수 있습니다.
            // 여기서는 간단히 IllegalArgumentException을 사용합니다.
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }
        return authentication.getName(); // PrincipalDetails.username은 email
    }

    /**
     * 월별로 일기가 있는 날짜 목록을 조회 (신규)
     */
    @GetMapping("/calendar")
    @Operation(summary = "월별 일기 작성일 조회 (캘린더용)", description = "지정된 연월에 일기가 작성된 날짜 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<?>> getDiaryCalendar(
            @Parameter(description = "조회할 연월 (YYYY-MM)") @RequestParam String month) {

        if (month == null || !month.matches("\\d{4}-\\d{2}")) {
            // ErrorType 등을 사용한 표준화된 오류 응답이 더 좋을 수 있습니다.
            return ResponseEntity.badRequest().body(ApiResponse.error(ErrorType.INVALID_REQUEST_PARAMETER, Map.of("message", "month 파라미터는 YYYY-MM 형식이어야 합니다.")));
        }

        String email = getCurrentUserEmail();
        try {
            String[] parts = month.split("-");
            int year = Integer.parseInt(parts[0]);
            int monthValue = Integer.parseInt(parts[1]);

            DiaryCalendarResponseDto calendarResponse = diaryService.getDiaryCalendar(email, year, monthValue);
            return ResponseEntity.ok(ApiResponse.success(calendarResponse, "캘린더 데이터가 성공적으로 조회되었습니다."));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ErrorType.INVALID_REQUEST_PARAMETER, Map.of("message", "month 파라미터의 연도 또는 월 형식이 잘못되었습니다.")));
        } catch (IllegalArgumentException e) { // DiaryService에서 발생 가능 (예: 잘못된 월)
            return ResponseEntity.badRequest().body(ApiResponse.error(ErrorType.INVALID_REQUEST_PARAMETER, Map.of("message", e.getMessage())));
        }
    }
}