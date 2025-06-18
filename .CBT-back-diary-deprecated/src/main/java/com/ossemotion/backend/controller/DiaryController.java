package com.ossemotion.backend.controller;

import com.ossemotion.backend.dto.CreateDiaryPostRequest;
import com.ossemotion.backend.dto.DiaryPostDto;
import com.ossemotion.backend.dto.UpdateDiaryPostRequest;
import com.ossemotion.backend.service.DiaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/diaryposts")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    @PostMapping
    public ResponseEntity<DiaryPostDto> createDiaryPost(@Valid @RequestBody CreateDiaryPostRequest request) {
        DiaryPostDto createdDiary = diaryService.createDiaryPost(request);
        return new ResponseEntity<>(createdDiary, HttpStatus.CREATED);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<DiaryPostDto> getDiaryPostById(@PathVariable String postId) {
        // Consider error handling for non-numeric postId if direct conversion is used.
        // Or change service method to accept String and parse there.
        // For now, assuming postId can be parsed to Long.
        try {
            Long diaryId = Long.parseLong(postId);
            DiaryPostDto diaryPostDto = diaryService.getDiaryPostById(diaryId);
            return ResponseEntity.ok(diaryPostDto);
        } catch (NumberFormatException e) {
            // Or let GlobalExceptionHandler handle a custom InvalidIdFormatException
            return ResponseEntity.badRequest().body(null); // Simplified error response
        }
    }

    @PutMapping("/{postId}")
    public ResponseEntity<DiaryPostDto> updateDiaryPost(@PathVariable String postId, 
                                                          @Valid @RequestBody UpdateDiaryPostRequest request) {
        try {
            Long diaryId = Long.parseLong(postId);
            DiaryPostDto updatedDiary = diaryService.updateDiaryPost(diaryId, request);
            return ResponseEntity.ok(updatedDiary);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(null); // Simplified error response
        }
    }
}

