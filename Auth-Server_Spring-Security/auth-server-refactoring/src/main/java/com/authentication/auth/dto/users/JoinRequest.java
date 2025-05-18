package com.authentication.auth.dto.users;

import com.authentication.auth.domain.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Date;

/**
 * 회원 가입 요청 정보를 담는 불변 레코드
 */
public record JoinRequest(
    @NotBlank(message = "사용자 ID는 필수입니다") 
    @Size(min = 4, max = 20, message = "사용자 ID는 4~20자 사이여야 합니다")
    String userId,
    
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
    String userPw,
    
    @NotBlank(message = "이름은 필수입니다")
    String userName,
    
    @NotBlank(message = "닉네임은 필수입니다")
    @Size(min = 2, max = 20, message = "닉네임은 2~20자 사이여야 합니다")
    String nickname,
    
    @NotBlank(message = "전화번호는 필수입니다")
    @Pattern(regexp = "^\\d{3}-\\d{3,4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다")
    String phone,
    
    @Email(message = "이메일 형식이 올바르지 않습니다")
    String email,
    
    User.UserRole role,
    
    Date birthDate,
    
    @NotBlank(message = "성별은 필수입니다")
    String gender,
    
    @JsonProperty("isPrivate") 
    boolean isPrivate,
    
    String profile,
    
    @NotBlank(message = "인증 코드는 필수입니다")
    String code
) {
    /**
     * 기본 프로필 이미지를 사용하는 팩토리 메서드
     */
    public static JoinRequest of(
            String userId, String userPw, String userName, String nickname, 
            String phone, String email, User.UserRole role, Date birthDate, 
            String gender, boolean isPrivate, String code) {
        return new JoinRequest(
                userId, userPw, userName, nickname, phone, email, role, 
                birthDate, gender, isPrivate, "https://zrr.kr/iPHf", code);
    }

    /**
     * 사용자 엔티티로 변환
     */
    public User toEntity(String encodedPassword) {
        return User.builder()
                .password(encodedPassword)
                .userName(this.userName)
                .userRole(this.role != null ? this.role : User.UserRole.USER)
                .isPremium(false)
                .isActive(User.UserStatus.WAITING)
                .build();
    }
    
    /**
     * 유효성 검사
     */
    public boolean isValid() {
        return userId != null && !userId.isBlank() &&
               userPw != null && !userPw.isBlank() &&
               userName != null && !userName.isBlank() &&
               nickname != null && !nickname.isBlank() &&
               phone != null && !phone.isBlank() &&
               code != null && !code.isBlank();
    }
}
