package com.authentication.auth.dto.users;

import com.authentication.auth.domain.User;

import java.time.LocalDateTime;

/**
 * 사용자 프로필 응답 정보를 담는 불변 레코드
 */
public record UserProfileResponse(
    String userName,
    String email,
    String role,
    LocalDateTime createdAt,
    LocalDateTime lastLogin
) {
    /**
     * 사용자 엔티티에서 응답 객체 생성
     */
    public static UserProfileResponse fromEntity(User user) {
        return new UserProfileResponse(
            user.getUserName(),
            user.getEmail(),
            user.getUserRole(),
            user.getCreatedAt(),
            user.getLastLogin()
        );
    }
}
