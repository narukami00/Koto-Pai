package com.kotopai.notification.dto;

import java.time.Instant;

import com.kotopai.notification.domain.Notification;
import com.kotopai.notification.domain.NotificationType;

/**
 * Response model representing a notification.
 */
public record NotificationResponse(
        Long id,
        Long userId,
        NotificationType type,
        String message,
        String linkUrl,
        boolean read,
        Instant createdAt,
        Instant readAt
) {
    public static NotificationResponse from(Notification n) {
        if (n == null) {
            return null;
        }
        return new NotificationResponse(
                n.getId(),
                n.getUserId(),
                n.getType(),
                n.getMessage(),
                n.getLinkUrl(),
                n.isRead(),
                n.getCreatedAt(),
                n.getReadAt()
        );
    }
}

