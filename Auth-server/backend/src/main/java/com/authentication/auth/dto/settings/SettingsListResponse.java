package com.authentication.auth.dto.settings;

import java.util.List;

public record SettingsListResponse(
    List<SettingItem> settings
) {}
