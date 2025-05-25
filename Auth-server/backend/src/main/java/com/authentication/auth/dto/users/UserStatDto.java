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
    Date birthDate,
    String gender,
    boolean isPrivate,
    String profile,
    List<String> hashtags,
    List<String> certifications,
    List<String> groups,
    LocalDateTime userActivites,
    Date createdAt,
    Date updatedAt,
    LocalDateTime lastLogin
) {}
