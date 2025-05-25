package com.authentication.auth.dto.settings;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SettingsUpdateRequest(
    @NotEmpty(message = "Settings to update cannot be empty")
    @Valid
    List<SettingsUpdateRequestItem> settingsToUpdate
) {}
