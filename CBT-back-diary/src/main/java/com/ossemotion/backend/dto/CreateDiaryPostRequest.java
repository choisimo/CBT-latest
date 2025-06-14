package com.ossemotion.backend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Data
public class CreateDiaryPostRequest {
    @NotNull(message = "Date cannot be null")
    private LocalDate date;

    @NotBlank(message = "Title cannot be blank")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    @NotBlank(message = "Content cannot be blank")
    private String content;
    
    // userId will be extracted from security context in the service layer later
}
