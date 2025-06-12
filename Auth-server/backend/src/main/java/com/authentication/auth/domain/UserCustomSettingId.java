package com.authentication.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCustomSettingId implements Serializable {
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "setting_id")
    private Integer settingId;
}
