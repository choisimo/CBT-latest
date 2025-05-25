package com.authentication.auth.dto.settings;

import com.authentication.auth.domain.SettingsOption;

public record SettingItem(
    String settingKey,
    Object value,
    String dataType,
    String description,
    boolean isUserEditable
) {
    public static SettingItem fromSettingsOption(SettingsOption option, Object currentValue) {
        if (option == null) return null;
        return new SettingItem(
            option.getSettingKey(),
            currentValue,
            option.getDataType(),
            option.getDescription(),
            option.getIsUserEditable()
        );
    }
}
