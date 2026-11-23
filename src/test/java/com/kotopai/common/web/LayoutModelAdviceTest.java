package com.kotopai.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import com.kotopai.auth.service.KotopaiUserDetails;
import com.kotopai.notification.service.NotificationService;
import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;

class LayoutModelAdviceTest {

    private final LayoutModelAdvice advice = new LayoutModelAdvice();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Returns null when SecurityContext has no authentication")
    void returnsNullWhenUnauthenticated() {
        assertThat(advice.currentNavUser()).isNull();
    }

    @Test
    @DisplayName("Returns null for anonymous authentication")
    void returnsNullForAnonymous() {
        var anon = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anon);

        assertThat(advice.currentNavUser()).isNull();
    }

    @Test
    @DisplayName("Builds NavUserView from KotopaiUserDetails principal")
    void extractsKotopaiUserDetails() {
        User user = new User("rafsan10", "Rafsan Riasat", "rafsan@example.com", "hash", Role.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        KotopaiUserDetails principal = new KotopaiUserDetails(user);

        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        NavUserView navUser = advice.currentNavUser();
        assertThat(navUser).isNotNull();
        assertThat(navUser.displayName()).isEqualTo("Rafsan Riasat");
        assertThat(navUser.userCode()).isEqualTo("rafsan10");
        assertThat(navUser.initial()).isEqualTo("R");
    }

    @Test
    @DisplayName("Falls back gracefully for non-Kotopai principals such as mock users")
    void fallsBackForGenericPrincipal() {
        var auth = new UsernamePasswordAuthenticationToken(
                "tanvir", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        NavUserView navUser = advice.currentNavUser();
        assertThat(navUser).isNotNull();
        assertThat(navUser.displayName()).isEqualTo("tanvir");
        assertThat(navUser.userCode()).isEqualTo("tanvir");
        assertThat(navUser.initial()).isEqualTo("T");
    }

    @Test
    @DisplayName("unreadNotificationCount returns 0 when unauthenticated or service absent")
    void unreadNotificationCount_unauthenticatedOrAbsent() {
        assertThat(advice.unreadNotificationCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("unreadNotificationCount returns service count when authenticated")
    void unreadNotificationCount_authenticated() {
        NotificationService mockService = mock(NotificationService.class);
        LayoutModelAdvice customAdvice = new LayoutModelAdvice(mockService);

        User user = new User("rafsan10", "Rafsan", "rafsan@example.com", "hash", Role.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        KotopaiUserDetails principal = new KotopaiUserDetails(user);

        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(mockService.getUnreadCount(1L)).thenReturn(4L);

        assertThat(customAdvice.unreadNotificationCount()).isEqualTo(4L);
    }
}

