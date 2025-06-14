package com.ossemotion.backend.dto;

import lombok.Data;
import jakarta.validation.constraints.Size;
// No need for Optional from java.util here as per the prompt's example,
// null fields will indicate no update for that field.
// However, if explicit nulls vs. absent fields were needed, Optional could be used.

@Data
public class UpdateDiaryPostRequest {
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title; // Optional update

    private String content; // Optional update
}
