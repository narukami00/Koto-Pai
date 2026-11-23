package com.kotopai.friend.dto;

import java.time.Instant;

import com.kotopai.friend.domain.Friendship;
import com.kotopai.user.domain.User;

/**
 * Summary projection of an incoming pending friend request.
 */
public record FriendRequestSummary(
        Long friendshipId,
        Long requesterId,
        String userCode,
        String displayName,
        String avatarColor,
        Instant createdAt
) {
    public static FriendRequestSummary from(Friendship friendship) {
        if (friendship == null) {
            return null;
        }
        User requester = friendship.getRequester();
        return new FriendRequestSummary(
                friendship.getId(),
                requester.getId(),
                requester.getUserCode(),
                requester.getDisplayName(),
                requester.getAvatarColor(),
                friendship.getCreatedAt()
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
