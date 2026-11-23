package com.kotopai.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.kotopai.auth.service.KotopaiUserDetails;
import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;
import com.kotopai.user.repository.UserRepository;

@SpringBootTest
@Transactional
class UserProfileIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mvc;
    private User testUser;
    private KotopaiUserDetails userDetails;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        testUser = userRepository.save(new User(
                "profile_tester", "Tester User", "tester@kotopai.com", "hash", Role.USER));
        userDetails = new KotopaiUserDetails(testUser);
    }

    @Test
    @DisplayName("End-to-end profile view, update, and database verification")
    void fullProfileWorkflow() throws Exception {
        // 1. Fetch current profile
        mvc.perform(get("/api/me")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userCode").value("profile_tester"))
                .andExpect(jsonPath("$.displayName").value("Tester User"));

        // 2. Update profile via PUT /api/me
        String updateJson = """
                {
                    "displayName": "Updated Tester",
                    "bio": "A passionate bill splitter",
                    "avatarColor": "#D95D39"
                }
                """;

        mvc.perform(put("/api/me")
                        .with(csrf())
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Updated Tester"))
                .andExpect(jsonPath("$.bio").value("A passionate bill splitter"))
                .andExpect(jsonPath("$.avatarColor").value("#D95D39"));

        // 3. Verify changes persisted in database
        User reloaded = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(reloaded.getDisplayName()).isEqualTo("Updated Tester");
        assertThat(reloaded.getBio()).isEqualTo("A passionate bill splitter");
        assertThat(reloaded.getAvatarColor()).isEqualTo("#D95D39");
    }
}

