package com.kotopai.user.dto;

import com.kotopai.user.domain.User;

/**
 * Public summary projection of a user.
 *
 * <p>Used across modules (e.g. friend lists, participant selection, chip displays)
 * to safely share basic user details without exposing sensitive fields like password hashes.
 */
public record UserSummary(Long id, String userCode, String displayName, String avatarColor) {

    public static UserSummary from(User user) {
        if (user == null) {
            return null;
        }
        return new UserSummary(user.getId(), user.getUserCode(), user.getDisplayName(), user.getAvatarColor());
    }
}

