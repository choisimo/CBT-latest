package com.authentication.auth.domain;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Settings_option")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettingsOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "setting_key", nullable = false, unique = true, length = 50)
    private String settingKey;
    
    @Column(name = "default_value", nullable = false)
    private String defaultValue;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false)
    private DataType dataType;
    
    private String description;
    
    @Column(name = "is_user_editable")
    private Boolean isUserEditable = true;
    
    @OneToMany(mappedBy = "settingsOption", cascade = CascadeType.ALL)
    private List<UserCustomSetting> userCustomSettings = new ArrayList<>();
    
    public enum DataType {
        STRING, NUMBER, BOOLEAN, JSON
    }
}
