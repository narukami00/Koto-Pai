package com.kotopai.admin.dto;

import java.time.Instant;

import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;

/**
 * Administrative projection of a registered user.
 */
public record AdminUserView(
        Long id,
        String userCode,
        String displayName,
        String email,
        Role role,
        Instant createdAt
) {
    public static AdminUserView from(User user) {
        if (user == null) {
            return null;
        }
        return new AdminUserView(
                user.getId(),
                user.getUserCode(),
                user.getDisplayName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}

