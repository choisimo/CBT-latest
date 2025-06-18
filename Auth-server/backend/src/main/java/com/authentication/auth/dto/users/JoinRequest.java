package com.authentication.auth.dto.users;

import com.authentication.auth.domain.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Date;

/**
 * 회원 가입 요청 정보를 담는 불변 레코드
 */
public record JoinRequest (
    @Schema(description = "사용자 ID (로그인 시 사용)", example = "newuser123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "사용자 ID는 필수입니다") 
    @Size(min = 4, max = 20, message = "사용자 ID는 4~20자 사이여야 합니다")
    String userId,
    
    @Schema(description = "사용자 비밀번호", example = "P@sswOrd123!", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
    String userPw,
    
    @Schema(description = "사용자 이메일 주소", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @Email(message = "이메일 형식이 올바르지 않습니다")
    String email,
    
    @Schema(description = "사용자 역할", example = "USER", defaultValue = "USER")
    String role
) {
    /**
     * 기본 프로필 이미지를 사용하는 팩토리 메서드
     */
    public static JoinRequest of(
            String userId, String userPw,
            String email, String role) {
        return new JoinRequest(
                userId, userPw, email, role);
    }

    /**
     * 사용자 엔티티로 변환
     */
    public User toEntity(String encodedPassword) {
        return User.builder()
                .userName(this.userId)
                .password(encodedPassword)
                .email(this.email)
                .userRole(this.role != null ? this.role : "USER")
                .isPremium(false)
                .isActive("WAITING")
                .build();
    }
    
    /**
     * 유효성 검사
     */
    public boolean isValid() {
        return userId != null && !userId.isBlank() &&
               userPw != null && !userPw.isBlank();
    }
}
