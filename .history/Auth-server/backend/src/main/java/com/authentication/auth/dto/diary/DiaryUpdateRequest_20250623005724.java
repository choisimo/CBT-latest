package com.authentication.auth.dto.diary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiaryUpdateRequest {
    private String title;
    private String content;
}
