package com.kotopai.friend.dto;

import java.math.BigDecimal;

import com.kotopai.user.dto.UserSummary;

/**
 * View model projection for displaying an accepted friend with optional net balance.
 */
public record FriendItemView(
        Long id,
        String userCode,
        String displayName,
        String avatarColor,
        BigDecimal balance
) {
    public static FriendItemView of(UserSummary summary, BigDecimal balance) {
        return new FriendItemView(
                summary.id(),
                summary.userCode(),
                summary.displayName(),
                summary.avatarColor(),
                balance
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

