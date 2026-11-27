package com.kotopai.auth;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;
import com.kotopai.user.repository.UserRepository;

@SpringBootTest
@Transactional
class SecurityIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        userRepository.save(new User(
                "testuser",
                "Test User",
                "test@kotopai.com",
                passwordEncoder.encode("secret123"),
                Role.USER
        ));
    }

    @Test
    @DisplayName("Public endpoints are accessible without authentication")
    void publicEndpointsAccessible() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk());

        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Log in to track outings")));

        mvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Create an account")));
    }

    @Test
    @DisplayName("Protected endpoint redirects unauthenticated request to /login")
    void protectedEndpointRedirectsToLogin() throws Exception {
        mvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login")));
    }

    @Test
    @DisplayName("Form login succeeds with valid userCode")
    void formLoginSuccessByUserCode() throws Exception {
        mvc.perform(formLogin().user("testuser").password("secret123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("testuser"));
    }

    @Test
    @DisplayName("Form login succeeds with valid email")
    void formLoginSuccessByEmail() throws Exception {
        mvc.perform(formLogin().user("test@kotopai.com").password("secret123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("testuser"));
    }

    @Test
    @DisplayName("Form login fails with wrong password")
    void formLoginFailure() throws Exception {
        mvc.perform(formLogin().user("testuser").password("wrongpassword"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    @DisplayName("POST /register via form redirects to /login?registered")
    void webFormRegistrationSuccess() throws Exception {
        mvc.perform(post("/register")
                        .with(csrf())
                        .param("userCode", "webuser01")
                        .param("displayName", "Web User")
                        .param("email", "webuser@example.com")
                        .param("password", "webpass123")
                        .param("confirmPassword", "webpass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));
    }

    @Test
    @DisplayName("POST /register re-shows the form with an error when the passwords differ")
    void webFormRegistrationPasswordMismatch() throws Exception {
        mvc.perform(post("/register")
                        .with(csrf())
                        .param("userCode", "mismatch01")
                        .param("displayName", "Mismatch User")
                        .param("email", "mismatch@example.com")
                        .param("password", "webpass123")
                        .param("confirmPassword", "different456"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Passwords do not match")));
    }

    @Test
    @DisplayName("POST /register without a confirmation is rejected and creates no account")
    void webFormRegistrationMissingConfirmation() throws Exception {
        mvc.perform(post("/register")
                        .with(csrf())
                        .param("userCode", "noconfirm01")
                        .param("displayName", "No Confirm")
                        .param("email", "noconfirm@example.com")
                        .param("password", "webpass123"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Passwords do not match")));

        // The account must not exist: logging in as it fails.
        mvc.perform(formLogin().user("noconfirm01").password("webpass123"))
                .andExpect(unauthenticated());
    }

    @Test
    @DisplayName("The register page has a confirm password field")
    void registerPageHasConfirmField() throws Exception {
        mvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"confirmPassword\"")));
    }
}

