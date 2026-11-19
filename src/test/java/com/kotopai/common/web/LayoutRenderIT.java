package com.kotopai.common.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.kotopai.auth.service.KotopaiUserDetails;
import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;

/**
 * Integration tests verifying the shared Thymeleaf layout shell renders correctly
 * for both anonymous and authenticated users.
 */
@SpringBootTest
class LayoutRenderIT {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("Anonymous visitor sees navigation shell, login/signup buttons, and home content")
    void anonymousHomeRendersLayoutShell() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"shell\"")))
                .andExpect(content().string(containsString("class=\"side\"")))
                .andExpect(content().string(containsString("class=\"tabbar\"")))
                .andExpect(content().string(containsString("href=\"/friends\"")))
                .andExpect(content().string(containsString("href=\"/outings/new\"")))
                .andExpect(content().string(containsString("href=\"/settle\"")))
                .andExpect(content().string(containsString("href=\"/activity\"")))
                .andExpect(content().string(containsString("href=\"/profile\"")))
                .andExpect(content().string(containsString("href=\"/login\"")))
                .andExpect(content().string(containsString("href=\"/register\"")))
                .andExpect(content().string(containsString("Hello, world!")))
                .andExpect(content().string(not(containsString("href=\"/notifications\""))));
    }

    @Test
    @DisplayName("Authenticated user sees display name, @userCode, notification bell, and CSRF logout form")
    void authenticatedHomeRendersUserCardAndBell() throws Exception {
        User domainUser = new User("rafsan10", "Rafsan Riasat", "rafsan@example.com", "hash", Role.USER);
        ReflectionTestUtils.setField(domainUser, "id", 1L);
        KotopaiUserDetails principal = new KotopaiUserDetails(domainUser);

        mvc.perform(get("/").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Rafsan Riasat")))
                .andExpect(content().string(containsString("@rafsan10")))
                .andExpect(content().string(containsString("href=\"/notifications\"")))
                .andExpect(content().string(containsString("action=\"/logout\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    @DisplayName("Flash success and error attributes render in layout banners")
    void flashMessagesRenderInLayout() throws Exception {
        mvc.perform(get("/")
                        .flashAttr("flashSuccess", "Friend request sent!")
                        .flashAttr("flashError", "Could not complete action."))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"flash ok\"")))
                .andExpect(content().string(containsString("Friend request sent!")))
                .andExpect(content().string(containsString("class=\"flash err\"")))
                .andExpect(content().string(containsString("Could not complete action.")));
    }
}

