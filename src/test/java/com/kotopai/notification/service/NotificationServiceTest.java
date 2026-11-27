package com.kotopai.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.kotopai.common.exception.ForbiddenException;
import com.kotopai.common.exception.ResourceNotFoundException;
import com.kotopai.notification.domain.Notification;
import com.kotopai.notification.domain.NotificationType;
import com.kotopai.notification.dto.NotificationResponse;
import com.kotopai.notification.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private DefaultNotificationService notificationService;

    @Test
    @DisplayName("notify successfully persists notification")
    void notify_success() {
        notificationService.notify(1L, NotificationType.PAYMENT_RECORDED, "Payment recorded", "/settle");

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("notify throws when message is blank")
    void notify_blankMessage() {
        assertThatThrownBy(() -> notificationService.notify(1L, NotificationType.SYSTEM, "  ", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getNotifications returns mapped responses")
    void getNotifications_success() {
        Notification n = new Notification(1L, NotificationType.OUTING_ACTIVATED, "Outing activated", "/outings/1");
        ReflectionTestUtils.setField(n, "id", 10L);

        when(notificationRepository.findAllByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(n));

        List<NotificationResponse> list = notificationService.getNotifications(1L);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).id()).isEqualTo(10L);
        assertThat(list.get(0).type()).isEqualTo(NotificationType.OUTING_ACTIVATED);
        assertThat(list.get(0).linkUrl()).isEqualTo("/outings/1");
    }

    @Test
    @DisplayName("getUnreadCount returns repository count")
    void getUnreadCount_success() {
        when(notificationRepository.countByUserIdAndReadFalse(1L)).thenReturn(5L);

        assertThat(notificationService.getUnreadCount(1L)).isEqualTo(5L);
        assertThat(notificationService.getUnreadCount(null)).isEqualTo(0L);
    }

    @Test
    @DisplayName("markAsRead marks unread notification as read")
    void markAsRead_success() {
        Notification n = new Notification(1L, NotificationType.PAYMENT_CONFIRMED, "Payment confirmed", "/ledger");
        ReflectionTestUtils.setField(n, "id", 20L);

        when(notificationRepository.findById(20L)).thenReturn(Optional.of(n));

        notificationService.markAsRead(1L, 20L);

        assertThat(n.isRead()).isTrue();
        assertThat(n.getReadAt()).isNotNull();
    }

    @Test
    @DisplayName("markAsRead throws ForbiddenException if user is not owner")
    void markAsRead_forbidden() {
        Notification n = new Notification(2L, NotificationType.PAYMENT_CONFIRMED, "Payment confirmed", "/ledger");
        ReflectionTestUtils.setField(n, "id", 20L);

        when(notificationRepository.findById(20L)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> notificationService.markAsRead(1L, 20L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("markAllAsRead calls repository update")
    void markAllAsRead_success() {
        notificationService.markAllAsRead(1L);

        verify(notificationRepository).markAllAsRead(eq(1L), any());
    }
}

