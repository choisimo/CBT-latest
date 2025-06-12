package com.authentication.auth.dto.users;

public record OAuth2CallbackRequest(
    String tempCode,
    String state
) {}
