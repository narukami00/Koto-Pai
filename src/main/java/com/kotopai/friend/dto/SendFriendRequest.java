package com.kotopai.friend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for sending a friend request.
 */
public record SendFriendRequest(
        @NotBlank(message = "userCode must not be blank")
        String userCode
) {
}

