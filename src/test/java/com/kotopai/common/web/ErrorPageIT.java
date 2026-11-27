package com.kotopai.common.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * The servlet container forwards failed requests to /error. Visitors must see a friendly, branded page (status
 * code, plain-language explanation, a way back) instead of Spring's raw "Whitelabel Error Page", and the page
 * must be reachable without a login.
 */
@SpringBootTest
class ErrorPageIT {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static RequestPostProcessor failedWith(int statusCode) {
        return request -> {
            request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, statusCode);
            request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/somewhere");
            return request;
        };
    }

    @Test
    @DisplayName("404 shows a friendly 'Page not found' page for a signed-out visitor")
    void notFoundPage() throws Exception {
        mvc.perform(get("/error").accept(MediaType.TEXT_HTML).with(failedWith(404)))
            .andExpect(status().isNotFound())
            .andExpect(content().string(containsString("Page not found")))
            .andExpect(content().string(containsString("Back to home")))
            .andExpect(content().string(not(containsString("Whitelabel"))));
    }

    @Test
    @DisplayName("403 explains that the account has no access instead of showing a raw error")
    void forbiddenPage() throws Exception {
        mvc.perform(get("/error").accept(MediaType.TEXT_HTML).with(failedWith(403)))
            .andExpect(status().isForbidden())
            .andExpect(content().string(containsString("You can’t open this page")))
            .andExpect(content().string(containsString("does not have access")))
            .andExpect(content().string(not(containsString("Whitelabel"))));
    }

    @Test
    @DisplayName("500 shows a generic apology and never leaks internal details")
    void serverErrorPage() throws Exception {
        mvc.perform(get("/error").accept(MediaType.TEXT_HTML).with(failedWith(500)))
            .andExpect(status().isInternalServerError())
            .andExpect(content().string(containsString("Something went wrong")))
            .andExpect(content().string(not(containsString("Whitelabel"))))
            .andExpect(content().string(not(containsString("Exception"))))
            .andExpect(content().string(not(containsString("at com."))));
    }

    @Test
    @DisplayName("Signed-out visitors get a Log in button on the error page")
    void anonymousSeesLoginButton() throws Exception {
        mvc.perform(get("/error").accept(MediaType.TEXT_HTML).with(failedWith(404)))
            .andExpect(content().string(containsString("href=\"/login\"")));
    }
}
