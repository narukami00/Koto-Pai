package com.kotopai.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating a user profile.
 */
public record UpdateProfileRequest(
        @NotBlank(message = "displayName must not be blank")
        @Size(max = 80, message = "displayName cannot exceed 80 characters")
        String displayName,

        @Size(max = 255, message = "bio cannot exceed 255 characters")
        String bio,

        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "avatarColor must be a valid 6-character hex code (e.g. #18604B)")
        String avatarColor
) {
}

