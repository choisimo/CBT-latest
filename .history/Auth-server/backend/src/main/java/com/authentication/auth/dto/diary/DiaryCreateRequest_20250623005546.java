package com.authentication.auth.dto.diary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiaryCreateRequest {
    private String title;
    private String content;
    
    // 두 개 파라미터 생성자 추가
    public DiaryCreateRequest(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
