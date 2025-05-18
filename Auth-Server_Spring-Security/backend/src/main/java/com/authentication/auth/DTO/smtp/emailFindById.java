package com.career_block.auth.DTO.smtp;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class emailFindById {
    @NotBlank
    private String userId;
}
