package com.kotopai.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for user registration.
 */
public record RegisterRequest(
        @NotBlank(message = "userCode is required")
        @Pattern(
                regexp = "^[a-z0-9_]{3,32}$",
                message = "userCode must be 3-32 characters of lowercase letters, numbers, or underscores"
        )
        String userCode,

        @NotBlank(message = "displayName is required")
        @Size(max = 80, message = "displayName cannot exceed 80 characters")
        String displayName,

        @NotBlank(message = "email is required")
        @Email(message = "invalid email address")
        @Size(max = 120, message = "email cannot exceed 120 characters")
        String email,

        @NotBlank(message = "password is required")
        @Size(min = 6, max = 100, message = "password must be at least 6 characters")
        String password
) {
}

