package com.kotopai.notification.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.kotopai.auth.config.SecurityConfig;
import com.kotopai.auth.service.KotopaiUserDetails;
import com.kotopai.auth.service.KotopaiUserDetailsService;
import com.kotopai.common.web.LayoutModelAdvice;
import com.kotopai.notification.domain.NotificationType;
import com.kotopai.notification.dto.NotificationResponse;
import com.kotopai.notification.service.NotificationService;
import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;

@WebMvcTest(controllers = NotificationPageController.class)
@Import({SecurityConfig.class, LayoutModelAdvice.class})
class NotificationPageControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private KotopaiUserDetailsService userDetailsService;

    private KotopaiUserDetails userDetails;

    @BeforeEach
    void setUp() {
        User user = new User("rafsan10", "Rafsan", "rafsan@example.com", "hash", Role.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        userDetails = new KotopaiUserDetails(user);
    }

    @Test
    @DisplayName("GET /notifications renders notifications page with list and unread count")
    void notificationsPage_success() throws Exception {
        NotificationResponse n = new NotificationResponse(
                10L, 1L, NotificationType.PAYMENT_RECORDED, "Payment recorded", "/settle", false, Instant.now(), null);

        when(notificationService.getNotifications(1L)).thenReturn(List.of(n));
        when(notificationService.getUnreadCount(1L)).thenReturn(1L);

        mvc.perform(get("/notifications")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("notifications"))
                .andExpect(model().attributeExists("notifications", "unreadCount"));
    }

    @Test
    @DisplayName("POST /notifications/{id}/read marks read and redirects to targetUrl")
    void markAsRead_withTargetUrl() throws Exception {
        mvc.perform(post("/notifications/10/read")
                        .with(csrf())
                        .with(user(userDetails))
                        .param("targetUrl", "/settle"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/settle"));

        verify(notificationService).markAsRead(1L, 10L);
    }

    @Test
    @DisplayName("POST /notifications/read-all marks all read and redirects with flashSuccess")
    void markAllAsRead_success() throws Exception {
        mvc.perform(post("/notifications/read-all")
                        .with(csrf())
                        .with(user(userDetails)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"))
                .andExpect(flash().attribute("flashSuccess", "All notifications marked as read."));

        verify(notificationService).markAllAsRead(1L);
    }
}

