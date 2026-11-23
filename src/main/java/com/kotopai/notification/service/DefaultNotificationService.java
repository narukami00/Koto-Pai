package com.kotopai.notification.service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kotopai.common.exception.ForbiddenException;
import com.kotopai.common.exception.ResourceNotFoundException;
import com.kotopai.notification.domain.Notification;
import com.kotopai.notification.domain.NotificationType;
import com.kotopai.notification.dto.NotificationResponse;
import com.kotopai.notification.repository.NotificationRepository;

@Service
@Transactional
public class DefaultNotificationService implements NotificationService {

    private final NotificationRepository notificationRepository;

    public DefaultNotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void notify(Long userId, NotificationType type, String message, String linkUrl) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }

        Notification notification = new Notification(userId, type, message.trim(), linkUrl != null ? linkUrl.trim() : null);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(Long userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        if (userId == null) {
            return 0L;
        }
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    public void markAsRead(Long userId, Long notificationId) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(notificationId, "notificationId must not be null");

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));

        if (!notification.getUserId().equals(userId)) {
            throw new ForbiddenException("Cannot mark another user's notification as read");
        }

        notification.markAsRead();
    }

    @Override
    public void markAllAsRead(Long userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        notificationRepository.markAllAsRead(userId, Instant.now());
    }
}

