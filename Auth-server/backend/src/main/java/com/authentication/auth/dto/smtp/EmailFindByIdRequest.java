package com.authentication.auth.dto.smtp;

import jakarta.validation.constraints.NotBlank;

public record EmailFindByIdRequest(
    @NotBlank
    String email
) {}
