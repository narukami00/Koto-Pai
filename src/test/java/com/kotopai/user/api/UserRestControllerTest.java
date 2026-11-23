package com.kotopai.user.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.kotopai.auth.config.SecurityConfig;
import com.kotopai.auth.service.KotopaiUserDetails;
import com.kotopai.auth.service.KotopaiUserDetailsService;
import com.kotopai.common.exception.GlobalExceptionHandler;
import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;
import com.kotopai.user.dto.UpdateProfileRequest;
import com.kotopai.user.dto.UserProfileResponse;
import com.kotopai.user.dto.UserSummary;
import com.kotopai.user.service.UserService;

@WebMvcTest(controllers = UserRestController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class UserRestControllerTest {

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
    @DisplayName("GET /api/me returns 200 with current user profile")
    void getCurrentProfile_success() throws Exception {
        UserProfileResponse profile = new UserProfileResponse(
                1L, "rafsan10", "Rafsan", "rafsan@example.com", "Test bio", "#18604B", "USER", Instant.now());
        when(userService.getProfile(1L)).thenReturn(profile);

        mvc.perform(get("/api/me")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.userCode").value("rafsan10"))
                .andExpect(jsonPath("$.displayName").value("Rafsan"))
                .andExpect(jsonPath("$.bio").value("Test bio"));
    }

    @Test
    @DisplayName("PUT /api/me updates profile and returns 200 with updated profile")
    void updateProfile_success() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("Rafsan Ahmed", "New bio", "#2B8C67");
        UserProfileResponse updated = new UserProfileResponse(
                1L, "rafsan10", "Rafsan Ahmed", "rafsan@example.com", "New bio", "#2B8C67", "USER", Instant.now());

        when(userService.updateProfile(eq(1L), eq(request))).thenReturn(updated);

        String json = """
                {
                    "displayName": "Rafsan Ahmed",
                    "bio": "New bio",
                    "avatarColor": "#2B8C67"
                }
                """;

        mvc.perform(put("/api/me")
                        .with(csrf())
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Rafsan Ahmed"))
                .andExpect(jsonPath("$.avatarColor").value("#2B8C67"));
    }

    @Test
    @DisplayName("PUT /api/me returns 400 Bad Request when displayName is blank")
    void updateProfile_blankDisplayName() throws Exception {
        String json = """
                {
                    "displayName": "",
                    "bio": "Bio"
                }
                """;

        mvc.perform(put("/api/me")
                        .with(csrf())
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("displayName")));
    }

    @Test
    @DisplayName("PUT /api/me returns 400 Bad Request when avatarColor is invalid hex format")
    void updateProfile_invalidAvatarColor() throws Exception {
        String json = """
                {
                    "displayName": "Valid Name",
                    "avatarColor": "not-a-color"
                }
                """;

        mvc.perform(put("/api/me")
                        .with(csrf())
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("avatarColor")));
    }

    @Test
    @DisplayName("GET /api/users/{userCode} returns 200 with UserSummary")
    void findByCode_success() throws Exception {
        UserSummary summary = new UserSummary(2L, "tanvir_h", "Tanvir", "#18604B");
        when(userService.findByCode("tanvir_h")).thenReturn(summary);

        mvc.perform(get("/api/users/tanvir_h")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.userCode").value("tanvir_h"))
                .andExpect(jsonPath("$.displayName").value("Tanvir"));
    }
}

