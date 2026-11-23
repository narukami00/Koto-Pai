package com.kotopai.common.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.kotopai.auth.service.KotopaiUserDetails;
import com.kotopai.notification.service.NotificationService;

/**
 * Populates shared layout model attributes for Thymeleaf views without extra database queries.
 */
@ControllerAdvice
public class LayoutModelAdvice {

    private static final String DEFAULT_AVATAR_COLOR = "#18604B";

    private final NotificationService notificationService;

    public LayoutModelAdvice() {
        this(null);
    }

    public LayoutModelAdvice(@Autowired(required = false) NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @ModelAttribute("unreadNotificationCount")
    public Long unreadNotificationCount() {
        if (notificationService == null) {
            return 0L;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return 0L;
        }
        if (auth.getPrincipal() instanceof KotopaiUserDetails user) {
            try {
                return notificationService.getUnreadCount(user.getId());
            } catch (Exception ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    @ModelAttribute("navUser")
    public NavUserView currentNavUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        if (auth.getPrincipal() instanceof KotopaiUserDetails user) {
            return new NavUserView(
                    user.getDisplayName(),
                    user.getUserCode(),
                    extractInitial(user.getDisplayName(), user.getUserCode()),
                    DEFAULT_AVATAR_COLOR
            );
        }
        String name = auth.getName();
        return new NavUserView(name, name, extractInitial(name, "U"), DEFAULT_AVATAR_COLOR);
    }

    private static String extractInitial(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim().substring(0, 1).toUpperCase();
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback.trim().substring(0, 1).toUpperCase();
        }
        return "U";
    }
}

