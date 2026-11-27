package com.kotopai.auth.service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;

/**
 * Spring Security {@link UserDetails} adapter for Koto Pai {@link User} entities.
 */
public class KotopaiUserDetails implements UserDetails {

    private final Long id;
    private final String userCode;
    private final String displayName;
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    public KotopaiUserDetails(User user) {
        Objects.requireNonNull(user, "user must not be null");
        this.id = user.getId();
        this.userCode = user.getUserCode();
        this.displayName = user.getDisplayName();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    public Long getId() {
        return id;
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

    @Override
    public String getUsername() {
        return userCode;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

