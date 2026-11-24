package com.kotopai.admin.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.kotopai.admin.dto.AdminSystemStats;
import com.kotopai.admin.dto.AdminUserView;
import com.kotopai.admin.service.AdminService;
import com.kotopai.auth.service.KotopaiUserDetails;
import com.kotopai.user.domain.Role;

/**
 * Controller for the server-rendered {@code /admin} Thymeleaf dashboard.
 */
@Controller
@RequestMapping("/admin")
public class AdminPageController {

    private final AdminService adminService;

    public AdminPageController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public String adminDashboard(
            @AuthenticationPrincipal KotopaiUserDetails currentUser,
            Model model) {
        AdminSystemStats stats = adminService.getSystemStats();
        List<AdminUserView> users = adminService.listUsers();

        model.addAttribute("stats", stats);
        model.addAttribute("users", users);
        model.addAttribute("currentAdminId", currentUser != null ? currentUser.getId() : null);
        return "admin";
    }

    @PostMapping("/users/{id}/role")
    public String changeUserRole(
            @AuthenticationPrincipal KotopaiUserDetails currentUser,
            @PathVariable("id") Long id,
            @RequestParam("role") Role role,
            RedirectAttributes redirectAttributes) {
        try {
            Long adminId = currentUser != null ? currentUser.getId() : null;
            adminService.changeUserRole(adminId, id, role);
            redirectAttributes.addFlashAttribute("flashSuccess", "User role successfully updated to " + role);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin";
    }
}

