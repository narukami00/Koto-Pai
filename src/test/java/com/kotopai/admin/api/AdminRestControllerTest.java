package com.kotopai.admin.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

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

import com.kotopai.admin.dto.AdminSystemStats;
import com.kotopai.admin.dto.AdminUserView;
import com.kotopai.admin.service.AdminService;
import com.kotopai.auth.config.SecurityConfig;
import com.kotopai.auth.service.KotopaiUserDetails;
import com.kotopai.auth.service.KotopaiUserDetailsService;
import com.kotopai.common.exception.GlobalExceptionHandler;
import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;

@WebMvcTest(controllers = AdminRestController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AdminRestControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AdminService adminService;

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
    @DisplayName("GET /api/admin/stats returns 200 with stats for ADMIN")
    void getStats_asAdmin_returns200() throws Exception {
        AdminSystemStats stats = new AdminSystemStats(10L, 5L, 8L, 20L, BigDecimal.valueOf(1500));
        when(adminService.getSystemStats()).thenReturn(stats);

        mvc.perform(get("/api/admin/stats").with(user(adminDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(10))
                .andExpect(jsonPath("$.totalOutings").value(5))
                .andExpect(jsonPath("$.totalFriendships").value(8))
                .andExpect(jsonPath("$.totalNotifications").value(20))
                .andExpect(jsonPath("$.totalSettledVolume").value(1500));
    }

    @Test
    @DisplayName("GET /api/admin/users returns 200 with user list for ADMIN")
    void getUsers_asAdmin_returns200() throws Exception {
        AdminUserView u1 = new AdminUserView(1L, "admin", "Admin", "admin@kotopai.com", Role.ADMIN, Instant.now());
        AdminUserView u2 = new AdminUserView(2L, "user1", "User One", "user1@kotopai.com", Role.USER, Instant.now());
        when(adminService.listUsers(null)).thenReturn(List.of(u1, u2));

        mvc.perform(get("/api/admin/users").with(user(adminDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userCode").value("admin"))
                .andExpect(jsonPath("$[0].role").value("ADMIN"))
                .andExpect(jsonPath("$[1].userCode").value("user1"))
                .andExpect(jsonPath("$[1].role").value("USER"));
    }

    @Test
    @DisplayName("GET /api/admin/users?q=... returns only the matching users")
    void getUsers_withQuery_filters() throws Exception {
        AdminUserView match = new AdminUserView(2L, "user1", "User One", "user1@kotopai.com", Role.USER, Instant.now());
        when(adminService.listUsers("user")).thenReturn(List.of(match));

        mvc.perform(get("/api/admin/users").param("q", "user").with(user(adminDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userCode").value("user1"));
    }

    @Test
    @DisplayName("GET /api/admin/users?q=... is forbidden for a regular user")
    void getUsers_withQuery_forbiddenForUser() throws Exception {
        mvc.perform(get("/api/admin/users").param("q", "user").with(user(regularUserDetails)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/admin/users/{id}/role updates user role")
    void changeRole_asAdmin_success() throws Exception {
        mvc.perform(post("/api/admin/users/2/role")
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": \"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User role updated successfully to ADMIN"));

        verify(adminService).changeUserRole(1L, 2L, Role.ADMIN);
    }

    @Test
    @DisplayName("GET /api/admin/stats returns 403 Forbidden for non-admin user")
    void getStats_asUser_returnsForbidden() throws Exception {
        mvc.perform(get("/api/admin/stats").with(user(regularUserDetails)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/admin/users/{id}/role without CSRF returns 403 Forbidden")
    void changeRole_withoutCsrf_returnsForbidden() throws Exception {
        mvc.perform(post("/api/admin/users/2/role")
                        .with(user(adminDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": \"ADMIN\"}"))
                .andExpect(status().isForbidden());
    }
}

