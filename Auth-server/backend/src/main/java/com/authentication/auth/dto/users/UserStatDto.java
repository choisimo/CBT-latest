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
    /** Domain: Not directly in User entity. */
    Date birthDate,
    /** Domain: Not directly in User entity. */
    String gender,
    /** Domain: Not directly in User entity. */
    boolean isPrivate,
    /** Domain: Not directly in User entity. */
    String profile,
    /** Domain: Not directly in User entity. */
    List<String> hashtags,
    /** Domain: Not directly in User entity. */
    List<String> certifications,
    /** Domain: Not directly in User entity. */
    List<String> groups,
    /** Domain: Not directly in User entity. Consider mapping from User.lastLogin or User.updatedAt if applicable. */
    LocalDateTime userActivities,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime lastLogin
) {}
