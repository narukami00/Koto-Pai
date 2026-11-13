package com.kotopai.auth.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@Transactional
class AuthRestControllerIT {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("POST /api/auth/register creates user and returns 201 Created")
    void register_success() throws Exception {
        String json = """
                {
                    "userCode": "newuser01",
                    "displayName": "New User",
                    "email": "newuser@example.com",
                    "password": "strongpassword123"
                }
                """;

        mvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userCode").value("newuser01"))
                .andExpect(jsonPath("$.displayName").value("New User"))
                .andExpect(jsonPath("$.avatarColor").exists());
    }

    @Test
    @DisplayName("POST /api/auth/register rejects invalid email format with 400 Bad Request")
    void register_invalidEmail() throws Exception {
        String json = """
                {
                    "userCode": "validuser",
                    "displayName": "Valid User",
                    "email": "notanemail",
                    "password": "strongpassword123"
                }
                """;

        mvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/auth/register rejects duplicate registration with 409 Conflict")
    void register_duplicate() throws Exception {
        String json = """
                {
                    "userCode": "dupuser",
                    "displayName": "User",
                    "email": "dup@example.com",
                    "password": "password123"
                }
                """;

        mvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}

