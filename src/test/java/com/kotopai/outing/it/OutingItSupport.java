package com.kotopai.outing.it;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Shared helpers for the outing integration tests. They start the whole application (H2) and call the JSON API
 * through the real security filter chain, so what is exercised is the same path a browser takes: filters,
 * controllers, services, database and the ledger reacting to events.
 */
@SpringBootTest
abstract class OutingItSupport {

    @Autowired
    private WebApplicationContext context;

    protected MockMvc mvc;

    @BeforeEach
    void setUpMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    protected ResultActions getAs(long userId, String url) throws Exception {
        return mvc.perform(get(url).with(user(String.valueOf(userId))));
    }

    protected ResultActions postAs(long userId, String url, String json) throws Exception {
        return mvc.perform(post(url).with(user(String.valueOf(userId))).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions putAs(long userId, String url, String json) throws Exception {
        return mvc.perform(put(url).with(user(String.valueOf(userId))).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected static long idOf(ResultActions result) throws Exception {
        MvcResult mvcResult = result.andReturn();
        return ((Number) JsonPath.read(mvcResult.getResponse().getContentAsString(), "$.id")).longValue();
    }

    protected long createDraft(long creator, String title) throws Exception {
        return idOf(postAs(creator, "/api/outings",
            "{\"title\":\"" + title + "\",\"category\":\"EATING\",\"date\":\"2026-11-14\"}"));
    }

    protected long createGuest(long owner, String name) throws Exception {
        return idOf(postAs(owner, "/api/guests", "{\"name\":\"" + name + "\"}"));
    }

    protected static String userRef(long id) {
        return "{\"userId\":" + id + "}";
    }

    protected static String guestRef(long id) {
        return "{\"guestId\":" + id + "}";
    }

    protected static String personalItem(String name, String amount, String participant) {
        return "{\"name\":\"" + name + "\",\"amount\":" + amount + ",\"splitType\":\"PERSONAL\",\"shares\":[{\"participant\":"
            + participant + "}]}";
    }

    protected static String contents(String participants, String items, String contributions) {
        return "{\"participants\":[" + participants + "],\"items\":[" + items + "],\"contributions\":[" + contributions + "]}";
    }

    protected static String payment(String participant, String amount) {
        return "{\"participant\":" + participant + ",\"amount\":" + amount + "}";
    }
}
