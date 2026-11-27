package com.kotopai.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.kotopai.auth.service.KotopaiUserDetails;
import com.kotopai.ledger.domain.PaymentStatus;
import com.kotopai.ledger.event.PaymentRecordedEvent;
import com.kotopai.notification.domain.Notification;
import com.kotopai.notification.domain.NotificationType;
import com.kotopai.notification.repository.NotificationRepository;
import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;
import com.kotopai.user.repository.UserRepository;

@SpringBootTest
@Transactional
class NotificationIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mvc;
    private User creditor;
    private KotopaiUserDetails creditorDetails;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        creditor = userRepository.save(new User(
                "notif_creditor", "Creditor User", "creditor@kotopai.com", "hash", Role.USER));
        creditorDetails = new KotopaiUserDetails(creditor);
    }

    @Test
    @DisplayName("End-to-end event publication, notification persistence, and mark-read flow")
    void eventToNotificationFlow() throws Exception {
        // 1. Initially no notifications for creditor
        assertThat(notificationRepository.findAllByUserIdOrderByCreatedAtDesc(creditor.getId())).isEmpty();

        // 2. Publish domain event PaymentRecordedEvent
        PaymentRecordedEvent event = new PaymentRecordedEvent(
                100L, 50L, 10L, 999L, 999L, creditor.getId(), BigDecimal.valueOf(450), PaymentStatus.PENDING_CONFIRMATION);
        eventPublisher.publishEvent(event);

        // 3. Verify notification created in database
        List<Notification> notifications = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(creditor.getId());
        assertThat(notifications).hasSize(1);
        Notification notification = notifications.get(0);
        assertThat(notification.getType()).isEqualTo(NotificationType.PAYMENT_RECORDED);
        assertThat(notification.getMessage()).contains("450");
        assertThat(notification.isRead()).isFalse();

        // 4. REST API shows unread count = 1
        mvc.perform(get("/api/notifications/unread-count")
                        .with(user(creditorDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1L));

        // 5. Mark as read via REST API
        mvc.perform(post("/api/notifications/" + notification.getId() + "/read")
                        .with(csrf())
                        .with(user(creditorDetails)))
                .andExpect(status().isOk());

        // 6. Verify marked read in database
        Notification reloaded = notificationRepository.findById(notification.getId()).orElseThrow();
        assertThat(reloaded.isRead()).isTrue();
        assertThat(reloaded.getReadAt()).isNotNull();

        // 7. REST API unread count becomes 0
        mvc.perform(get("/api/notifications/unread-count")
                        .with(user(creditorDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0L));
    }
}

