package com.kotopai.notification.domain;

/**
 * Types of asynchronous system and domain notifications.
 */
public enum NotificationType {
    OUTING_ACTIVATED,
    OUTING_UPDATED,
    OUTING_CANCELLED,
    PAYMENT_RECORDED,
    PAYMENT_CONFIRMED,
    DEBT_DISPUTED,
    DEBT_DELEGATED,
    FRIEND_REQUEST,
    SYSTEM
}

