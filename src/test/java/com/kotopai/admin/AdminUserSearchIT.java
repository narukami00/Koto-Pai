package com.kotopai.admin;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.kotopai.auth.service.KotopaiUserDetails;
import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;
import com.kotopai.user.repository.UserRepository;

/**
 * The Admin Console user search through the whole stack: security filter, controller, service, JPQL query,
 * Thymeleaf template.
 */
@SpringBootTest
@Transactional
class AdminUserSearchIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mvc;
    private KotopaiUserDetails admin;
    private KotopaiUserDetails regular;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        admin = new KotopaiUserDetails(userRepository.saveAndFlush(
                new User("zqx_admin", "Zqxel Admin", "admin@zqx.example", "hash", Role.ADMIN)));
        regular = new KotopaiUserDetails(userRepository.saveAndFlush(
                new User("zqx_beta", "Zqxel Beta", "beta@zqx.example", "hash", Role.USER)));
        userRepository.saveAndFlush(new User("other_person", "Someone Else", "else@example.com", "hash", Role.USER));
    }

    @Test
    @DisplayName("Searching by display name, ignoring case, lists only the matching users on the page")
    void pageSearchByName() throws Exception {
        mvc.perform(get("/admin").param("q", "ZQXEL BETA").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Zqxel Beta")))
                .andExpect(content().string(not(containsString("Someone Else"))))
                .andExpect(content().string(containsString("1 found")));
    }

    @Test
    @DisplayName("Searching by e-mail finds the user")
    void pageSearchByEmail() throws Exception {
        mvc.perform(get("/admin").param("q", "else@example").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Someone Else")))
                .andExpect(content().string(not(containsString("Zqxel Beta"))));
    }

    @Test
    @DisplayName("A search that matches nobody shows the empty state")
    void pageSearchWithNoMatch() throws Exception {
        mvc.perform(get("/admin").param("q", "no-such-person-xyz").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No users match")))
                .andExpect(content().string(containsString("0 found")));
    }

    @Test
    @DisplayName("Without a query the page lists everyone and shows the total")
    void pageWithoutQueryListsEveryone() throws Exception {
        mvc.perform(get("/admin").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Zqxel Beta")))
                .andExpect(content().string(containsString("Someone Else")))
                .andExpect(content().string(containsString("total")));
    }

    @Test
    @DisplayName("The REST endpoint filters by the same rule and matches the handle too")
    void restSearchByHandle() throws Exception {
        mvc.perform(get("/api/admin/users").param("q", "zqx_").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].userCode", hasItems("zqx_admin", "zqx_beta")))
                .andExpect(jsonPath("$[*].userCode", not(hasItem("other_person"))));
    }

    @Test
    @DisplayName("Wildcard characters in the search are matched literally, not as SQL wildcards")
    void wildcardsAreLiteral() throws Exception {
        mvc.perform(get("/api/admin/users").param("q", "%").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("A regular user cannot search or list users: the admin area stays forbidden")
    void regularUserIsForbidden() throws Exception {
        mvc.perform(get("/admin").param("q", "zqx").with(user(regular)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users").param("q", "zqx").with(user(regular)))
                .andExpect(status().isForbidden());
    }
}
