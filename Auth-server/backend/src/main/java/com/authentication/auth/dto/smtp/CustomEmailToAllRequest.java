package com.authentication.auth.dto.smtp;

public record CustomEmailToAllRequest(
    String title,
    String content
) {}
