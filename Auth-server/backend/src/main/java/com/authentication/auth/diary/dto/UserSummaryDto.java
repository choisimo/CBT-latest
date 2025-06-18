package com.authentication.auth.diary.dto;
import com.authentication.auth.domain.User;

/**
 * Lightweight projection of User information for Diary responses.
 */
public record UserSummaryDto(Long userId, String nickname) {

    public static UserSummaryDto from(User user) {
        return new UserSummaryDto(user.getId(), user.getUserName());
    }
}
