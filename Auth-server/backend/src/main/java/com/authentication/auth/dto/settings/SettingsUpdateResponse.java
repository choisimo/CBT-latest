package com.authentication.auth.dto.settings;

import java.util.List;

public record SettingsUpdateResponse(
    String message,
    List<SettingItem> updatedSettings
) {}
