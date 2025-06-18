package com.authentication.auth.dto.response;

import com.authentication.auth.domain.User;
import com.authentication.auth.dto.token.TokenDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 로그인 성공 시 반환되는 응답 DTO.
 * access_token 과 사용자 기본 정보를 포함한다.
 */
@Builder
public record LoginResponseDto(
        @JsonProperty("access_token")
        String accessToken,
        @JsonProperty("refresh_token")
        String refreshToken,
        UserInfo user
) {

    /**
     * 사용자 정보 서브 DTO.
     */
    @Builder
    public record UserInfo(
            String userId,
            String username,
            String email,
            List<String> roles
    ) {}

    /**
     * 정적 팩토리 메서드로 User, TokenDto, 권한 리스트를 받아 LoginResponseDto 를 생성한다.
     */
    public static LoginResponseDto of(User user, TokenDto tokenDto, List<String> roles) {
        UserInfo userInfo = UserInfo.builder()
                .userId(String.valueOf(user.getId()))
                .username(user.getUserName())
                .email(user.getEmail())
                .roles(roles)
                .build();
        return LoginResponseDto.builder()
                .accessToken(tokenDto.accessToken())
                .refreshToken(tokenDto.refreshToken())
                .user(userInfo)
                .build();
    }
}
