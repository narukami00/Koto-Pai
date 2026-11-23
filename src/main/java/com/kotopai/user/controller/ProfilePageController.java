package com.kotopai.user.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.kotopai.auth.service.KotopaiUserDetails;
import com.kotopai.user.dto.ProfileStatsView;
import com.kotopai.user.dto.UpdateProfileRequest;
import com.kotopai.user.dto.UserProfileResponse;
import com.kotopai.user.service.UserService;

/**
 * Controller for the server-rendered {@code /profile} Thymeleaf page.
 */
@Controller
public class ProfilePageController {

    private final UserService userService;

    public ProfilePageController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String profilePage(
            @AuthenticationPrincipal KotopaiUserDetails currentUser,
            Model model) {
        Long userId = currentUser.getId();
        UserProfileResponse profile = userService.getProfile(userId);
        ProfileStatsView stats = userService.getStats(userId);

        model.addAttribute("profile", profile);
        model.addAttribute("stats", stats);
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @AuthenticationPrincipal KotopaiUserDetails currentUser,
            @RequestParam("displayName") String displayName,
            @RequestParam(value = "bio", required = false) String bio,
            @RequestParam(value = "avatarColor", required = false) String avatarColor,
            RedirectAttributes redirectAttributes) {
        try {
            userService.updateProfile(currentUser.getId(), new UpdateProfileRequest(displayName, bio, avatarColor));
            redirectAttributes.addFlashAttribute("flashSuccess", "Profile updated successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/profile";
    }
}

