package com.ossemotion.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String userId;
    private String nickname;
    private String email;
    private boolean emailVerified;
    private String providerType;
    private String role;
}
