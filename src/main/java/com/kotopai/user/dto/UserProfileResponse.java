package com.kotopai.user.dto;

import java.time.Instant;

import com.kotopai.user.domain.User;

/**
 * Public and authenticated profile response object.
 */
public record UserProfileResponse(
        Long id,
        String userCode,
        String displayName,
        String email,
        String bio,
        String avatarColor,
        String role,
        Instant createdAt
) {
    public static UserProfileResponse from(User user) {
        if (user == null) {
            return null;
        }
        return new UserProfileResponse(
                user.getId(),
                user.getUserCode(),
                user.getDisplayName(),
                user.getEmail(),
                user.getBio(),
                user.getAvatarColor(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getCreatedAt()
        );
    }

    public String getInitial() {
        if (displayName != null && !displayName.isBlank()) {
            return displayName.substring(0, 1).toUpperCase();
        }
        if (userCode != null && !userCode.isBlank()) {
            return userCode.substring(0, 1).toUpperCase();
        }
        return "?";
    }
}

