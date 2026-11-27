package com.kotopai.outing.adapter;

import com.kotopai.auth.service.KotopaiUserDetails;
import com.kotopai.outing.port.CurrentUserProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Reads the id from the application principal. Numeric test principals remain supported for MockMvc integration tests.
 */
@Component
public class SecurityContextCurrentUser implements CurrentUserProvider {

    @Override
    public Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException("Not logged in");
        }
		if (authentication.getPrincipal() instanceof KotopaiUserDetails user) {
			return user.getId();
		}
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
			throw new BadCredentialsException(
					"Authenticated name is not a numeric user id: " + authentication.getName());
        }
    }
}
