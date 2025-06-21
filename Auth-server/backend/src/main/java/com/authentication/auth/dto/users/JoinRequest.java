package com.authentication.auth.dto.users;

import com.authentication.auth.domain.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @Schema(description = "사용자 로그인 ID", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "사용자 로그인 ID는 필수입니다")
    String loginId,

    @Schema(description = "사용자 비밀번호", example = "P@sswOrd123!", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
    String userPw,
    
    @Schema(description = "사용자 이메일 주소", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "이메일 형식이 올바르지 않습니다")
    String email,

    @Schema(description = "닉네임", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "닉네임은 필수입니다")
    String nickname
) {
    /**
     * 기본 프로필 이미지를 사용하는 팩토리 메서드
     */
    public static JoinRequest of(
            String loginId,
            String userPw,
            String email,
            String nickname,
            ) {
        return new JoinRequest(
                loginId, userPw, email, nickname);
    }

    /**
     * 사용자 엔티티로 변환
     */
    public User toEntity(String encodedPassword) {
        return User.builder()
                .userName(this.email.split("@")[0])
                .password(encodedPassword)
                .email(this.email)
                .isPremium(false)
                .isActive("WAITING")
                .build();
    }
    
    /**
     * 유효성 검사
     */
    @JsonIgnore
    public boolean isValid() {
        return userPw != null && !userPw.isBlank() &&
               email != null && !email.isBlank();
    }
}
