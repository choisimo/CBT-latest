package com.authentication.auth.domain;

@Entity
@Table(name = "User_custom_setting")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCustomSetting {
    @EmbeddedId
    private UserCustomSettingId id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("settingId")
    @JoinColumn(name = "setting_id")
    private SettingsOption settingsOption;
    
    @Column(name = "override_value", nullable = false)
    private String overrideValue;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

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
