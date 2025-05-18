package com.career_block.auth.DTO.smtp;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class emailCheckDto {
    @Email
    @NotBlank
    private String email;
    @NotBlank
    private String code;
}
