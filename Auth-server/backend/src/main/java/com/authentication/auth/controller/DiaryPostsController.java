package com.authentication.auth.controller;

import com.authentication.auth.diary.dto.DiaryResponseDto;
import com.authentication.auth.service.diary.DiaryManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Legacy Diary Posts API Controller
 * 클라이언트가 /api/diaryposts 경로로 요청하는 경우를 위한 호환성 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/diaryposts")
@RequiredArgsConstructor
public class DiaryPostsController {

    private final DiaryManagementService diaryManagementService;

    /**
     * 일기 조회 (READ) - Legacy API 호환성 지원
     * @param diaryPostId 조회할 일기의 ID
     * @return 일기 정보
     */
    @GetMapping("/{diaryPostId}")
    public ResponseEntity<DiaryResponseDto> getDiaryPost(@PathVariable Long diaryPostId) {
        log.info("Legacy 일기 조회 요청 - diaryPostId: {}", diaryPostId);
        DiaryResponseDto response = diaryManagementService.findDiaryById(diaryPostId);
        return ResponseEntity.ok(response);
    }
}
