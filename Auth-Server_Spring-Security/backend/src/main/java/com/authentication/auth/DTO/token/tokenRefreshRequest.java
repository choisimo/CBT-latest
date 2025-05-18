package com.career_block.auth.DTO.token;

import lombok.Data;

@Data
public class tokenRefreshRequest {
    private String expiredToken;
    private String provider;
}
