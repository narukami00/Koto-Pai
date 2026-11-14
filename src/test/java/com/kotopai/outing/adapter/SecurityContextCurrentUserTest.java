package com.kotopai.outing.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityContextCurrentUserTest {

    private final SecurityContextCurrentUser provider = new SecurityContextCurrentUser();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static UsernamePasswordAuthenticationToken login(String name) {
        return UsernamePasswordAuthenticationToken.authenticated(name, "pw", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    @DisplayName("The numeric login name is the user id")
    void numericNameIsTheId() {
        SecurityContextHolder.getContext().setAuthentication(login("42"));

        assertThat(provider.currentUserId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("Nobody logged in, or an anonymous visitor, is refused")
    void notLoggedIn() {
        assertThatThrownBy(provider::currentUserId).isInstanceOf(AuthenticationCredentialsNotFoundException.class);

        SecurityContextHolder.getContext().setAuthentication(
            new AnonymousAuthenticationToken("key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        assertThatThrownBy(provider::currentUserId).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    @DisplayName("A login name that is not a number is a clear authentication error")
    void nonNumericName() {
        SecurityContextHolder.getContext().setAuthentication(login("rafsan"));

        assertThatThrownBy(provider::currentUserId)
            .isInstanceOf(BadCredentialsException.class).hasMessageContaining("rafsan");
    }
}
