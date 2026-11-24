package com.kotopai.outing.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.kotopai.auth.config.SecurityConfig;
import com.kotopai.auth.service.KotopaiUserDetails;
import com.kotopai.auth.service.KotopaiUserDetailsService;
import com.kotopai.common.web.LayoutModelAdvice;
import com.kotopai.outing.service.OutingAccessPolicy;
import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = OutingWizardController.class)
@Import({SecurityConfig.class, LayoutModelAdvice.class})
class OutingWizardControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private OutingAccessPolicy access;

    @MockitoBean
    private KotopaiUserDetailsService userDetailsService;

    private KotopaiUserDetails me;

    @BeforeEach
    void setUp() {
        User user = new User("rafsan10", "Rafsan", "rafsan@example.com", "hash", Role.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        me = new KotopaiUserDetails(user);
    }

    @Test
    @DisplayName("The new-outing page renders the wizard shell for the logged-in user")
    void newOutingPage() throws Exception {
        mvc.perform(get("/outings/new").with(user(me)))
            .andExpect(status().isOk())
            .andExpect(view().name("outings/wizard"))
            .andExpect(content().string(containsString("id=\"wizard\"")))
            .andExpect(content().string(containsString("data-me-id=\"1\"")))
            .andExpect(content().string(containsString("data-me-name=\"Rafsan\"")))
            .andExpect(content().string(containsString("/js/outing-wizard.js")))
            .andExpect(content().string(containsString("id=\"csrf-token\"")))
            .andExpect(content().string(containsString("New outing")));
    }

    @Test
    @DisplayName("Editing an outing the user may edit renders the wizard with its id")
    void editPage() throws Exception {
        when(access.canEdit(7L, 1L)).thenReturn(true);

        mvc.perform(get("/outings/7/edit").with(user(me)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("data-outing-id=\"7\"")))
            .andExpect(content().string(containsString("Edit outing")));
    }

    @Test
    @DisplayName("Editing an outing the user may not edit goes back to the list with a message")
    void editRefused() throws Exception {
        when(access.canEdit(9L, 1L)).thenReturn(false);

        mvc.perform(get("/outings/9/edit").with(user(me)))
            .andExpect(redirectedUrl("/outings"))
            .andExpect(flash().attribute("flashError", "You cannot edit this outing."));
    }

    @Test
    @DisplayName("The wizard needs a login")
    void loginRequired() throws Exception {
        mvc.perform(get("/outings/new")).andExpect(status().is3xxRedirection());
    }
}
