package com.kotopai.notification.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.kotopai.common.exception.GlobalExceptionHandler;
import com.kotopai.notification.domain.NotificationType;
import com.kotopai.notification.dto.NotificationResponse;
import com.kotopai.notification.service.NotificationService;
import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;

@WebMvcTest(controllers = NotificationRestController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class NotificationRestControllerTest {

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
    @DisplayName("GET /api/notifications returns 200 with list of notifications")
    void listNotifications_success() throws Exception {
        NotificationResponse n = new NotificationResponse(
                10L, 1L, NotificationType.PAYMENT_RECORDED, "Payment recorded", "/settle", false, Instant.now(), null);
        when(notificationService.getNotifications(1L)).thenReturn(List.of(n));

        mvc.perform(get("/api/notifications")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L))
                .andExpect(jsonPath("$[0].type").value("PAYMENT_RECORDED"))
                .andExpect(jsonPath("$[0].message").value("Payment recorded"));
    }

    @Test
    @DisplayName("GET /api/notifications/unread-count returns 200 with count")
    void getUnreadCount_success() throws Exception {
        when(notificationService.getUnreadCount(1L)).thenReturn(3L);

        mvc.perform(get("/api/notifications/unread-count")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(3L));
    }

    @Test
    @DisplayName("POST /api/notifications/{id}/read marks notification read")
    void markAsRead_success() throws Exception {
        mvc.perform(post("/api/notifications/10/read")
                        .with(csrf())
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notification marked as read"));

        verify(notificationService).markAsRead(1L, 10L);
    }

    @Test
    @DisplayName("POST /api/notifications/read-all marks all read")
    void markAllAsRead_success() throws Exception {
        mvc.perform(post("/api/notifications/read-all")
                        .with(csrf())
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("All notifications marked as read"));

        verify(notificationService).markAllAsRead(1L);
    }
}

