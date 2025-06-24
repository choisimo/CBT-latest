package com.authentication.auth.dto.users;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
    private Long id;
    private String nickname;
    private String email;
    private String loginId;
    private Boolean isPremium;
    private String userRole;
}
