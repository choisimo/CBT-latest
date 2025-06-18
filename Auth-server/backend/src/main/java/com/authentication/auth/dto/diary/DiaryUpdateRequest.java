package com.authentication.auth.dto.diary;

import jakarta.validation.constraints.Size;

public record DiaryUpdateRequest(
    @Size(max = 255, message = "제목은 255자를 넘을 수 없습니다.")
    String title,

    @Size(max = 5000, message = "내용은 5000자를 넘을 수 없습니다.")
    String content
) {} 