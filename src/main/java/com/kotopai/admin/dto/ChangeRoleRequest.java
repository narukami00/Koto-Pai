package com.kotopai.admin.dto;

import jakarta.validation.constraints.NotNull;
import com.kotopai.user.domain.Role;

/**
 * Request payload for changing a user's role.
 */
public record ChangeRoleRequest(
        @NotNull(message = "role must not be null")
        Role role
) {
}

