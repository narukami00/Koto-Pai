package com.kotopai.user.domain;

import java.util.Objects;
import java.util.regex.Pattern;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import com.kotopai.common.BaseEntity;

/**
 * A registered user of Koto Pai.
 *
 * <p>Extends {@link BaseEntity} to inherit the primary key and creation timestamp.
 * Invariant checks and normalization (e.g. lowercase {@code userCode} and {@code email})
 * are enforced at construction and mutation boundaries.
 */
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    private static final Pattern USER_CODE_PATTERN = Pattern.compile("^[a-z0-9_]{3,32}$");

    @Column(name = "user_code", nullable = false, unique = true, length = 32)
    private String userCode;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(length = 255)
    private String bio;

    @Column(name = "avatar_color", length = 16)
    private String avatarColor;

    protected User() {
    }

    public User(String userCode, String displayName, String email, String passwordHash, Role role) {
        this(userCode, displayName, email, passwordHash, role, null, "#18604B");
    }

    public User(
            String userCode,
            String displayName,
            String email,
            String passwordHash,
            Role role,
            String bio,
            String avatarColor) {
        this.userCode = validateUserCode(userCode);
        this.displayName = validateDisplayName(displayName);
        this.email = validateEmail(email);
        this.passwordHash = validatePasswordHash(passwordHash);
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.bio = bio != null ? bio.trim() : null;
        this.avatarColor = (avatarColor != null && !avatarColor.isBlank()) ? avatarColor.trim() : "#18604B";
    }

    public void updateProfile(String displayName, String bio, String avatarColor) {
        this.displayName = validateDisplayName(displayName);
        this.bio = bio != null ? bio.trim() : null;
        if (avatarColor != null && !avatarColor.isBlank()) {
            this.avatarColor = avatarColor.trim();
        }
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = validatePasswordHash(newPasswordHash);
    }

    public void changeRole(Role newRole) {
        this.role = Objects.requireNonNull(newRole, "role must not be null");
    }

    private static String validateUserCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("userCode must not be blank");
        }
        String normalized = code.trim().toLowerCase();
        if (!USER_CODE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "userCode must be 3-32 characters of lowercase letters, digits, or underscores: " + code);
        }
        return normalized;
    }

    private static String validateDisplayName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 80) {
            throw new IllegalArgumentException("displayName cannot exceed 80 characters");
        }
        return trimmed;
    }

    private static String validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        String normalized = email.trim().toLowerCase();
        if (!normalized.contains("@") || normalized.length() > 120) {
            throw new IllegalArgumentException("invalid email format: " + email);
        }
        return normalized;
    }

    private static String validatePasswordHash(String hash) {
        if (hash == null || hash.isBlank()) {
            throw new IllegalArgumentException("passwordHash must not be blank");
        }
        return hash;
    }

    public String getUserCode() {
        return userCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public String getBio() {
        return bio;
    }

    public String getAvatarColor() {
        return avatarColor;
    }
}

