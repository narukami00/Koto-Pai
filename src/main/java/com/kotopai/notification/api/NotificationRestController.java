package com.kotopai.notification.api;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kotopai.auth.service.KotopaiUserDetails;
import com.kotopai.notification.dto.NotificationResponse;
import com.kotopai.notification.dto.UnreadCountResponse;
import com.kotopai.notification.service.NotificationService;

/**
 * REST API controller for user notifications.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationRestController {

    private final NotificationService notificationService;

    public NotificationRestController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> listNotifications(
            @AuthenticationPrincipal KotopaiUserDetails currentUser) {
        return ResponseEntity.ok(notificationService.getNotifications(currentUser.getId()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal KotopaiUserDetails currentUser) {
        return ResponseEntity.ok(new UnreadCountResponse(notificationService.getUnreadCount(currentUser.getId())));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> markAsRead(
            @AuthenticationPrincipal KotopaiUserDetails currentUser,
            @PathVariable("id") Long id) {
        notificationService.markAsRead(currentUser.getId(), id);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(
            @AuthenticationPrincipal KotopaiUserDetails currentUser) {
        notificationService.markAllAsRead(currentUser.getId());
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }
}

