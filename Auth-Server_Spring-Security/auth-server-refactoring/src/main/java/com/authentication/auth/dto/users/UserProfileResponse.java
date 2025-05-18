package com.authentication.auth.dto.users;

import com.authentication.auth.domain.User;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 사용자 프로필 응답 정보를 담는 불변 레코드
 */
public record UserProfileResponse(
    String userId,
    String userName,
    String nickname,
    String email,
    String phone,
    String role,
    Date birthDate,
    String gender,
    @JsonProperty("isPrivate") boolean isPrivate,
    String profile,
    LocalDateTime createdAt,
    LocalDateTime lastLogin
) {
    /**
     * 사용자 엔티티에서 응답 객체 생성
     */
    public static UserProfileResponse fromEntity(User user) {
        return new UserProfileResponse(
            user.getId().toString(),
            user.getUserName(),
            user.getUserName(), // nickname 필드가 없어서 userName으로 대체
            "", // email 필드가 없어서 빈 문자열로 대체
            "", // phone 필드가 없어서 빈 문자열로 대체
            user.getUserRole().name(),
            null, // birthDate 필드가 없어서 null로 대체
            "", // gender 필드가 없어서 빈 문자열로 대체
            false, // isPrivate 필드가 없어서 false로 대체
            "", // profile 필드가 없어서 빈 문자열로 대체
            user.getCreatedAt(),
            user.getLastLogin()
        );
    }
}
