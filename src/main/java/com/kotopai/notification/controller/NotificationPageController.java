package com.kotopai.notification.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.kotopai.auth.service.KotopaiUserDetails;
import com.kotopai.notification.dto.NotificationResponse;
import com.kotopai.notification.service.NotificationService;

/**
 * Controller for the server-rendered {@code /notifications} Thymeleaf page.
 */
@Controller
public class NotificationPageController {

    private final NotificationService notificationService;

    public NotificationPageController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/notifications")
    public String notificationsPage(
            @AuthenticationPrincipal KotopaiUserDetails currentUser,
            Model model) {
        Long userId = currentUser.getId();
        List<NotificationResponse> notifications = notificationService.getNotifications(userId);
        long unreadCount = notificationService.getUnreadCount(userId);

        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        return "notifications";
    }

    @PostMapping("/notifications/{id}/read")
    public String markAsRead(
            @AuthenticationPrincipal KotopaiUserDetails currentUser,
            @PathVariable("id") Long id,
            @RequestParam(value = "targetUrl", required = false) String targetUrl,
            RedirectAttributes redirectAttributes) {
        try {
            notificationService.markAsRead(currentUser.getId(), id);
            if (targetUrl != null && !targetUrl.isBlank() && targetUrl.startsWith("/")) {
                return "redirect:" + targetUrl;
            }
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/read-all")
    public String markAllAsRead(
            @AuthenticationPrincipal KotopaiUserDetails currentUser,
            RedirectAttributes redirectAttributes) {
        try {
            notificationService.markAllAsRead(currentUser.getId());
            redirectAttributes.addFlashAttribute("flashSuccess", "All notifications marked as read.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/notifications";
    }
}

