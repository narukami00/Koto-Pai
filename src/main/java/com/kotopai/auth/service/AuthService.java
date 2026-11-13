package com.kotopai.auth.service;

import com.kotopai.auth.dto.RegisterRequest;
import com.kotopai.user.dto.UserSummary;

/**
 * Service handling registration and authentication operations.
 */
public interface AuthService {

    /**
     * Registers a new user account with BCrypt password hashing.
     *
     * @param request validated registration details
     * @return UserSummary of the newly created user
     */
    UserSummary register(RegisterRequest request);
}

