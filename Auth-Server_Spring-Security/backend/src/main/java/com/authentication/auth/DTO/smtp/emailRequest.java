package com.career_block.auth.DTO.smtp;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class emailRequest {
    @Email
    @NotBlank
    private String email;
}
