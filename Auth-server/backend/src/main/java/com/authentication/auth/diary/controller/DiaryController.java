package main.java.com.authentication.auth.diary.controller;

import com.authentication.auth.diary.dto.DiaryResponseDto;
import com.authentication.auth.diary.service.DiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/diaries")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<DiaryResponseDto> getDiary(@PathVariable Long id) {
        DiaryResponseDto dto = diaryService.findDiaryById(id);
        return ResponseEntity.ok(dto);
    }
}
