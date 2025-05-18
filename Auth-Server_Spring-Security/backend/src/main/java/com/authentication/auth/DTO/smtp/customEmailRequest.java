package com.career_block.auth.DTO.smtp;

import lombok.Data;

@Data
public class customEmailRequest {
    private String email;
    private String content;
    private String title;
}
