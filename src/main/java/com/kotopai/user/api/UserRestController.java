package com.kotopai.user.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kotopai.auth.service.KotopaiUserDetails;
import com.kotopai.user.dto.UpdateProfileRequest;
import com.kotopai.user.dto.UserProfileResponse;
import com.kotopai.user.dto.UserSummary;
import com.kotopai.user.service.UserService;

import jakarta.validation.Valid;

/**
 * REST API controller for user profile management and public user lookups.
 */
@RestController
@RequestMapping("/api")
public class UserRestController {

    private final UserService userService;

    public UserRestController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentProfile(
            @AuthenticationPrincipal KotopaiUserDetails currentUser) {
        return ResponseEntity.ok(userService.getProfile(currentUser.getId()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal KotopaiUserDetails currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(currentUser.getId(), request));
    }

    @GetMapping("/users/{userCode}")
    public ResponseEntity<UserSummary> findByCode(@PathVariable("userCode") String userCode) {
        return ResponseEntity.ok(userService.findByCode(userCode));
    }
}

