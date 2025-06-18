package com.authentication.auth.dto.users;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public record UserStatDto(
    @NotBlank
    String userId,
    String userName,
    String email,
    String role,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime lastLogin
) {}
