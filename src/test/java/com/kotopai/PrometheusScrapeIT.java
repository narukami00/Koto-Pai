package com.kotopai;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Prometheus scrapes /actuator/prometheus with HTTP Basic. The endpoint must refuse everyone else, including
 * a logged-in app user, and must serve real metrics to the scrape account.
 */
@AutoConfigureMetrics // tests switch metrics export off by default; this turns the Prometheus registry back on
@SpringBootTest(properties = {
    "management.endpoints.web.exposure.include=health,prometheus",
    "management.metrics.tags.application=kotopai",
    "kotopai.metrics.username=prometheus",
    "kotopai.metrics.password=scrape-test-pw"
})
class PrometheusScrapeIT {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("Anonymous requests to the scrape endpoint are rejected with 401")
    void anonymousRejected() throws Exception {
        mvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A wrong scrape password is rejected")
    void wrongPasswordRejected() throws Exception {
        mvc.perform(get("/actuator/prometheus").with(httpBasic("prometheus", "nope")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("The scrape account gets the Prometheus text format with JVM metrics")
    void scrapeAccountGetsMetrics() throws Exception {
        mvc.perform(get("/actuator/prometheus").with(httpBasic("prometheus", "scrape-test-pw")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("jvm_memory_used_bytes")))
            .andExpect(content().string(containsString("application=\"kotopai\"")));
    }

    @Test
    @DisplayName("The scrape account cannot use the rest of the app")
    void scrapeAccountIsNotAnAppUser() throws Exception {
        mvc.perform(get("/api/outings").with(httpBasic("prometheus", "scrape-test-pw")))
            .andExpect(status().is(not(200)));
    }
}
