package com.authentication.auth.DTO.users;

import com.authentication.auth.domain.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.util.Date;

/**
 * @author: nodove
 * 기존 class -> record 으로 변경 
 * record 매개변수에 직접 @NotBlank 추가
 * 
 * */
public record JoinRequest(
    @NotBlank String userId,
    @NotBlank String userPw,
    @NotBlank String userName,
    @NotBlank String nickname,
    @NotBlank String phone,
    String email,
    User.UserRole role,
    Date birthDate,
    @NotBlank String gender,
    @JsonProperty("isPrivate") boolean isPrivate,
    String profile,
    String code
) {
    // default 프로필 이미지를 위한 정적 팩토리 메서드 -> 불필요한 객체 생성 방지(생성자 활용, immutable) 목적
    public static JoinRequest of(String userId, String userPw, String userName, String nickname, 
                              String phone, String email, User.UserRole role, Date birthDate, 
                              String gender, boolean isPrivate, String code) {
        return new JoinRequest(userId, userPw, userName, nickname, phone, email, role, 
                            birthDate, gender, isPrivate, "https://zrr.kr/iPHf", code);
    }

    // 사용자 엔티티로 변환하는 메서드
    public User toEntity() {
        return User.builder()
                .userId(this.userId)
                .userPw(this.userPw)
                .userName(this.userName)
                .nickname(this.nickname)
                .phone(this.phone)
                .email(this.email)
                .role(this.role)
                .birthDate(this.birthDate)
                .gender(this.gender)
                .isPrivate(this.isPrivate)
                .profile(this.profile)
                .build();
    }
}
