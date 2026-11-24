package com.kotopai.admin.controller;

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
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.kotopai.admin.dto.AdminSystemStats;
import com.kotopai.admin.dto.AdminUserView;
import com.kotopai.admin.service.AdminService;
import com.kotopai.auth.config.SecurityConfig;
import com.kotopai.auth.service.KotopaiUserDetails;
import com.kotopai.auth.service.KotopaiUserDetailsService;
import com.kotopai.common.web.LayoutModelAdvice;
import com.kotopai.notification.service.NotificationService;
import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;

@WebMvcTest(controllers = AdminPageController.class)
@Import({SecurityConfig.class, LayoutModelAdvice.class})
class AdminPageControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private KotopaiUserDetailsService userDetailsService;

    private KotopaiUserDetails adminDetails;
    private KotopaiUserDetails regularUserDetails;

    @BeforeEach
    void setUp() {
        User admin = new User("admin", "Admin", "admin@kotopai.com", "hash", Role.ADMIN);
        ReflectionTestUtils.setField(admin, "id", 1L);
        adminDetails = new KotopaiUserDetails(admin);

        User regular = new User("user1", "User One", "user1@kotopai.com", "hash", Role.USER);
        ReflectionTestUtils.setField(regular, "id", 2L);
        regularUserDetails = new KotopaiUserDetails(regular);
    }

    @Test
    @DisplayName("GET /admin returns 200 and renders admin view for ADMIN")
    void adminDashboard_asAdmin_returns200() throws Exception {
        AdminSystemStats stats = new AdminSystemStats(10L, 5L, 8L, 20L, BigDecimal.valueOf(1500));
        AdminUserView u1 = new AdminUserView(1L, "admin", "Admin", "admin@kotopai.com", Role.ADMIN, Instant.now());
        when(adminService.getSystemStats()).thenReturn(stats);
        when(adminService.listUsers()).thenReturn(List.of(u1));

        mvc.perform(get("/admin").with(user(adminDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(model().attributeExists("stats", "users", "currentAdminId"));
    }

    @Test
    @DisplayName("POST /admin/users/{id}/role changes role and redirects with flashSuccess")
    void changeUserRole_success_redirectsWithFlash() throws Exception {
        mvc.perform(post("/admin/users/2/role")
                        .with(user(adminDetails))
                        .with(csrf())
                        .param("role", "ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("flashSuccess", "User role successfully updated to ADMIN"));

        verify(adminService).changeUserRole(1L, 2L, Role.ADMIN);
    }

    @Test
    @DisplayName("POST /admin/users/{id}/role handles error and redirects with flashError")
    void changeUserRole_error_redirectsWithFlashError() throws Exception {
        doThrow(new IllegalStateException("Cannot demote the sole remaining administrator"))
                .when(adminService).changeUserRole(1L, 1L, Role.USER);

        mvc.perform(post("/admin/users/1/role")
                        .with(user(adminDetails))
                        .with(csrf())
                        .param("role", "USER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("flashError", "Cannot demote the sole remaining administrator"));
    }

    @Test
    @DisplayName("GET /admin returns 403 Forbidden for regular user")
    void adminDashboard_asUser_returnsForbidden() throws Exception {
        mvc.perform(get("/admin").with(user(regularUserDetails)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /admin redirects anonymous user to login")
    void adminDashboard_anonymous_redirectsToLogin() throws Exception {
        mvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}
