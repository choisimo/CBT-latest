package com.authentication.auth.diary.controller;

import com.authentication.auth.diary.dto.DiaryResponseDto;
import com.authentication.auth.diary.service.DiaryService;
import com.authentication.auth.dto.diary.DiaryCreateRequest;
import com.authentication.auth.dto.diary.DiaryUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/diaries") // Sticking to existing base path
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    // Existing method - signature seems fine as auth is handled in service.
    @GetMapping("/{diaryId}") // Changed {id} to {diaryId} for consistency
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<DiaryResponseDto> findDiaryById(@PathVariable Long diaryId) {
        DiaryResponseDto dto = diaryService.findDiaryById(diaryId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<DiaryResponseDto> createDiary(
            @RequestBody DiaryCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        DiaryResponseDto diaryResponseDto = diaryService.createDiaryPost(request, userDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(diaryResponseDto);
    }

    @PutMapping("/{diaryId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<DiaryResponseDto> updateDiary(
            @PathVariable Long diaryId,
            @RequestBody DiaryUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        DiaryResponseDto diaryResponseDto = diaryService.updateDiaryPost(diaryId, request, userDetails);
        return ResponseEntity.ok(diaryResponseDto);
    }

    @DeleteMapping("/{diaryId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteDiary(
            @PathVariable Long diaryId,
            @AuthenticationPrincipal UserDetails userDetails) {
        diaryService.deleteDiaryPost(diaryId, userDetails);
        return ResponseEntity.noContent().build();
    }
}
