package com.kotopai.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for custom exception constructors and messages.
 */
class ExceptionTest {

    // --- ResourceNotFoundException ---

    @Test
    void resourceNotFound_simpleMessage() {
        var ex = new ResourceNotFoundException("User not found");
        assertThat(ex.getMessage()).isEqualTo("User not found");
    }

    @Test
    void resourceNotFound_byId() {
        var ex = new ResourceNotFoundException("User", 42L);
        assertThat(ex.getMessage()).isEqualTo("User not found with id: 42");
    }

    @Test
    void resourceNotFound_byFieldValue() {
        var ex = new ResourceNotFoundException("User", "userCode", "rafsan10");
        assertThat(ex.getMessage()).isEqualTo("User not found with userCode: rafsan10");
    }

    // --- ForbiddenException ---

    @Test
    void forbidden_message() {
        var ex = new ForbiddenException("You do not own this resource");
        assertThat(ex.getMessage()).isEqualTo("You do not own this resource");
    }

    // --- DuplicateResourceException ---

    @Test
    void duplicate_message() {
        var ex = new DuplicateResourceException("Email already registered");
        assertThat(ex.getMessage()).isEqualTo("Email already registered");
    }
}

