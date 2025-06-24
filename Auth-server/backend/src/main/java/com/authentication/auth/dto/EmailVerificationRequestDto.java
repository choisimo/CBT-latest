package com.authentication.auth.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 이메일 인증 코드 확인 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationRequestDto {
    
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @NotBlank(message = "이메일은 필수입니다.")
    private String email;
    
    @NotBlank(message = "인증 코드는 필수입니다.")
    private String code;
}
