package com.authentication.auth.dto.settings;

import jakarta.validation.constraints.NotBlank;

public record SettingsUpdateRequestItem(
    @NotBlank(message = "Setting key cannot be blank")
    String settingKey,
    Object newValue
) {}
