package com.kotopai.common.web;

/**
 * Lightweight view model representing the currently signed-in user in the navigation shell.
 */
public record NavUserView(String displayName, String userCode, String initial, String avatarColor) {
}

