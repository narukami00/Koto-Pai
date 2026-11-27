package com.kotopai.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UserTest {

    @Test
    @DisplayName("Creates user with default avatar and valid fields")
    void createUser_success() {
        User user = new User("rafsan10", "Rafsan Riasat", "rafsan@example.com", "hash123", Role.USER);

        assertThat(user.getUserCode()).isEqualTo("rafsan10");
        assertThat(user.getDisplayName()).isEqualTo("Rafsan Riasat");
        assertThat(user.getEmail()).isEqualTo("rafsan@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("hash123");
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getBio()).isNull();
        assertThat(user.getAvatarColor()).isEqualTo("#18604B");
    }

    @Test
    @DisplayName("Normalizes userCode and email to lowercase and trims input")
    void normalizesUserCodeAndEmail() {
        User user = new User("  Tanvir_01  ", " Tanvir ", "  TANVIR@Example.COM  ", "hash", Role.USER);

        assertThat(user.getUserCode()).isEqualTo("tanvir_01");
        assertThat(user.getDisplayName()).isEqualTo("Tanvir");
        assertThat(user.getEmail()).isEqualTo("tanvir@example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "ab",
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", // 33 characters (max is 32)
        "user@code",
        "user-code",
        "user code",
        "",
        "   "
    })
    @DisplayName("Rejects invalid userCode format")
    void rejectsInvalidUserCode(String invalidCode) {
        assertThatThrownBy(() -> new User(invalidCode, "Name", "test@example.com", "hash", Role.USER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("Rejects blank displayName")
    void rejectsBlankDisplayName(String invalidName) {
        assertThatThrownBy(() -> new User("rafsan10", invalidName, "test@example.com", "hash", Role.USER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Rejects displayName exceeding 80 characters")
    void rejectsOverlongDisplayName() {
        String longName = "a".repeat(81);
        assertThatThrownBy(() -> new User("rafsan10", longName, "test@example.com", "hash", Role.USER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "notanemail", "plainaddress"})
    @DisplayName("Rejects invalid email format")
    void rejectsInvalidEmail(String invalidEmail) {
        assertThatThrownBy(() -> new User("rafsan10", "Name", invalidEmail, "hash", Role.USER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("Rejects blank password hash")
    void rejectsBlankPasswordHash(String invalidHash) {
        assertThatThrownBy(() -> new User("rafsan10", "Name", "test@example.com", invalidHash, Role.USER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("updateProfile updates name, bio, and avatarColor correctly")
    void updateProfile_success() {
        User user = new User("rafsan10", "Rafsan", "rafsan@example.com", "hash", Role.USER);

        user.updateProfile("Rafsan Updated", "Coding enthusiast", "#DB8428");

        assertThat(user.getDisplayName()).isEqualTo("Rafsan Updated");
        assertThat(user.getBio()).isEqualTo("Coding enthusiast");
        assertThat(user.getAvatarColor()).isEqualTo("#DB8428");
    }

    @Test
    @DisplayName("changePassword updates passwordHash")
    void changePassword_success() {
        User user = new User("rafsan10", "Rafsan", "rafsan@example.com", "hash1", Role.USER);

        user.changePassword("hash2_new");

        assertThat(user.getPasswordHash()).isEqualTo("hash2_new");
    }
}
