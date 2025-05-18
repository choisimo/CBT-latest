package com.career_block.auth.DTO.users;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import com.authentication.auth.domain.Role;

@Data
@Builder
public class userStat {
    @NotBlank
    private String userId;
    @NotBlank
    private String nickname;
    private Role role;
    private Date birthDate;
    //private String gender;
    private boolean isPrivate;
    private String profile;
    private List<String> hashtags;
    private List<String> certifications;
    private List<String> groups;
    private LocalDateTime userActivites;
}
