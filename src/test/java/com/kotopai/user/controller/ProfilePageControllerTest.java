package com.kotopai.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.kotopai.auth.config.SecurityConfig;
import com.kotopai.auth.service.KotopaiUserDetails;
import com.kotopai.auth.service.KotopaiUserDetailsService;
import com.kotopai.common.web.LayoutModelAdvice;
import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;
import com.kotopai.user.dto.ProfileStatsView;
import com.kotopai.user.dto.UpdateProfileRequest;
import com.kotopai.user.dto.UserProfileResponse;
import com.kotopai.user.service.UserService;

@WebMvcTest(controllers = ProfilePageController.class)
@Import({SecurityConfig.class, LayoutModelAdvice.class})
class ProfilePageControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private KotopaiUserDetailsService userDetailsService;

    private KotopaiUserDetails userDetails;

    @BeforeEach
    void setUp() {
        User user = new User("rafsan10", "Rafsan", "rafsan@example.com", "hash", Role.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        userDetails = new KotopaiUserDetails(user);
    }

    @Test
    @DisplayName("GET /profile renders profile view with profile and stats attributes")
    void profilePage_success() throws Exception {
        UserProfileResponse profile = new UserProfileResponse(
                1L, "rafsan10", "Rafsan", "rafsan@example.com", "Bio", "#18604B", "USER", Instant.now());
        ProfileStatsView stats = new ProfileStatsView(5, 3, BigDecimal.valueOf(1200));

        when(userService.getProfile(1L)).thenReturn(profile);
        when(userService.getStats(1L)).thenReturn(stats);

        mvc.perform(get("/profile")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attributeExists("profile", "stats"));
    }

    @Test
    @DisplayName("POST /profile redirects with flashSuccess on successful update")
    void updateProfile_success() throws Exception {
        UpdateProfileRequest expectedRequest = new UpdateProfileRequest("New Name", "New Bio", "#2B8C67");

        mvc.perform(post("/profile")
                        .with(csrf())
                        .with(user(userDetails))
                        .param("displayName", "New Name")
                        .param("bio", "New Bio")
                        .param("avatarColor", "#2B8C67"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("flashSuccess", "Profile updated successfully!"));

        verify(userService).updateProfile(eq(1L), eq(expectedRequest));
    }

    @Test
    @DisplayName("POST /profile redirects with flashError on failure")
    void updateProfile_failure() throws Exception {
        doThrow(new IllegalArgumentException("displayName cannot exceed 80 characters"))
                .when(userService).updateProfile(eq(1L), any(UpdateProfileRequest.class));

        mvc.perform(post("/profile")
                        .with(csrf())
                        .with(user(userDetails))
                        .param("displayName", "Overly Long Name...")
                        .param("bio", "Bio"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("flashError", "displayName cannot exceed 80 characters"));
    }
}

