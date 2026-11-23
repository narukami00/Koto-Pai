package com.kotopai.notification.service;

import java.util.List;

import com.kotopai.notification.domain.NotificationType;
import com.kotopai.notification.dto.NotificationResponse;

/**
 * Service for issuing notifications, querying inbox, and updating read status.
 */
public interface NotificationService {

    /**
     * Creates and persists a notification for a user.
     *
     * @param userId recipient user ID
     * @param type notification type
     * @param message notification message
     * @param linkUrl optional resource URL for one-click navigation
     */
    void notify(Long userId, NotificationType type, String message, String linkUrl);

    /**
     * Retrieves all notifications for a user, newest first.
     *
     * @param userId recipient user ID
     * @return list of notification responses
     */
    List<NotificationResponse> getNotifications(Long userId);

    /**
     * Counts unread notifications for a user.
     *
     * @param userId recipient user ID
     * @return count of unread notifications
     */
    long getUnreadCount(Long userId);

    /**
     * Marks a specific notification as read.
     *
     * @param userId user attempting to mark as read (must be owner)
     * @param notificationId ID of notification
     */
    void markAsRead(Long userId, Long notificationId);

    /**
     * Marks all unread notifications as read for a user.
     *
     * @param userId user ID
     */
    void markAllAsRead(Long userId);
}

