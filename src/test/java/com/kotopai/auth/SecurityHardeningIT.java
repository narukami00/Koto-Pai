package com.kotopai.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.kotopai.admin.service.AdminService;
import com.kotopai.auth.service.KotopaiUserDetails;
import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;
import com.kotopai.user.repository.UserRepository;

@SpringBootTest
@Transactional
class SecurityHardeningIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AdminService adminService;

    private MockMvc mvc;
    private User admin;
    private User regularUser;
    private KotopaiUserDetails adminDetails;
    private KotopaiUserDetails regularDetails;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        admin = userRepository.save(new User(
                "sec_admin",
                "Sec Admin",
                "sec_admin@kotopai.com",
                passwordEncoder.encode("secret123"),
                Role.ADMIN
        ));
        adminDetails = new KotopaiUserDetails(admin);

        regularUser = userRepository.save(new User(
                "sec_user",
                "Sec User",
                "sec_user@kotopai.com",
                passwordEncoder.encode("secret123"),
                Role.USER
        ));
        regularDetails = new KotopaiUserDetails(regularUser);
    }

    @Test
    @DisplayName("Anonymous user cannot access /admin (redirects to /login)")
    void anonymous_cannotAccessAdminPage() throws Exception {
        mvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Anonymous user cannot access /api/admin/stats (redirects to /login)")
    void anonymous_cannotAccessAdminRest() throws Exception {
        mvc.perform(get("/api/admin/stats"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("ROLE_USER cannot access /admin (returns 403 Forbidden)")
    void regularUser_cannotAccessAdminPage() throws Exception {
        mvc.perform(get("/admin").with(user(regularDetails)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_USER cannot access /api/admin/** (returns 403 Forbidden)")
    void regularUser_cannotAccessAdminRest() throws Exception {
        mvc.perform(get("/api/admin/stats").with(user(regularDetails)))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/admin/users").with(user(regularDetails)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_ADMIN can access /admin and /api/admin/**")
    void admin_canAccessAdminEndpoints() throws Exception {
        mvc.perform(get("/admin").with(user(adminDetails)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/admin/stats").with(user(adminDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").isNumber());

        mvc.perform(get("/api/admin/users").with(user(adminDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("POST to admin endpoint without CSRF token returns 403 Forbidden")
    void postWithoutCsrf_returnsForbidden() throws Exception {
        mvc.perform(post("/admin/users/" + regularUser.getId() + "/role")
                        .with(user(adminDetails))
                        .param("role", "ADMIN"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/admin/users/" + regularUser.getId() + "/role")
                        .with(user(adminDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": \"ADMIN\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Direct method call to AdminService without authentication throws AuthenticationCredentialsNotFoundException")
    void methodSecurity_unauthenticated_throwsException() {
        assertThatThrownBy(() -> adminService.getSystemStats())
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Direct method call to AdminService with ROLE_USER throws AuthorizationDeniedException")
    void methodSecurity_unauthorizedUser_throwsException() {
        assertThatThrownBy(() -> adminService.getSystemStats())
                .isInstanceOf(AuthorizationDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Direct method call to AdminService with ROLE_ADMIN succeeds")
    void methodSecurity_admin_succeeds() {
        var stats = adminService.getSystemStats();
        assertThat(stats).isNotNull();
    }

    @Test
    @DisplayName("End-to-end admin role management promotes user to ADMIN and manages permissions")
    void adminRoleManagement_endToEnd() throws Exception {
        // Promote regularUser to ADMIN via REST
        mvc.perform(post("/api/admin/users/" + regularUser.getId() + "/role")
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": \"ADMIN\"}"))
                .andExpect(status().isOk());

        User updatedUser = userRepository.findById(regularUser.getId()).orElseThrow();
        assertThat(updatedUser.getRole()).isEqualTo(Role.ADMIN);

        // Now updatedUser has ADMIN role, demote sec_admin to USER via REST
        mvc.perform(post("/api/admin/users/" + admin.getId() + "/role")
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": \"USER\"}"))
                .andExpect(status().isOk());

        User demotedAdmin = userRepository.findById(admin.getId()).orElseThrow();
        assertThat(demotedAdmin.getRole()).isEqualTo(Role.USER);

        // Attempting to demote updatedUser (now the sole remaining admin) via REST must return 400 Bad Request
        KotopaiUserDetails newAdminDetails = new KotopaiUserDetails(updatedUser);
        mvc.perform(post("/api/admin/users/" + updatedUser.getId() + "/role")
                        .with(user(newAdminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": \"USER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("sole remaining administrator")));
    }
}

