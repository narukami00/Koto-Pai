package com.kotopai.outing.adapter;

import com.kotopai.outing.port.CurrentUserProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Temporary adapter until Member 1 authentication is on develop: the authenticated name must be the numeric user
 * id. When real users exist, replace this class with one that reads the id from the real principal.
 */
@Component
public class SecurityContextCurrentUser implements CurrentUserProvider {

    @Override
    public Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException("Not logged in");
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new BadCredentialsException("Authenticated name is not a numeric user id: " + authentication.getName());
        }
    }
}
