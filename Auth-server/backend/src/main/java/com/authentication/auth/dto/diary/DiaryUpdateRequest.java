package com.authentication.auth.dto.diary;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DiaryUpdateRequest {
    private String title;
    private String content;
}
