package com.authentication.auth.DTO.users;

import lombok.Data;

public record LoginRequest(String userId, String password) {}
