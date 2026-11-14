package com.kotopai.outing.it;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Who may see and change what, checked through the real filter chain and the whole application. */
class OutingSecurityIT extends OutingItSupport {

    private static final long CREATOR = 201;
    private static final long PARTICIPANT = 202;
    private static final long STRANGER = 203;

    @Test
    @DisplayName("Without a login the API answers 401, not a login page")
    void unauthenticatedIsRejected() throws Exception {
        mvc.perform(get("/api/outings")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/guests")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/outings/preview").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A write without a CSRF token is refused even for a logged-in user")
    void csrfIsEnforced() throws Exception {
        mvc.perform(post("/api/outings").with(user(String.valueOf(CREATOR))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"x\",\"category\":\"EATING\",\"date\":\"2026-11-14\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A login name that is not a user id is rejected as unauthenticated")
    void nonNumericLoginIsRejected() throws Exception {
        mvc.perform(get("/api/outings").with(user("rafsan"))).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Drafts are invisible to everyone but the creator; active outings are visible to participants only")
    void visibilityFollowsStatusAndRole() throws Exception {
        long outingId = createDraft(CREATOR, "Visibility");
        putAs(CREATOR, "/api/outings/" + outingId, contents(
            userRef(CREATOR) + "," + userRef(PARTICIPANT),
            personalItem("Tea", "30.00", userRef(PARTICIPANT)),
            payment(userRef(CREATOR), "30.00"))).andExpect(status().isOk());

        // draft: only the creator sees it; for anybody else it does not exist
        getAs(CREATOR, "/api/outings/" + outingId).andExpect(status().isOk());
        getAs(PARTICIPANT, "/api/outings/" + outingId).andExpect(status().isNotFound());
        getAs(STRANGER, "/api/outings/" + outingId).andExpect(status().isNotFound());

        postAs(CREATOR, "/api/outings/" + outingId + "/submit", "").andExpect(status().isOk());

        // active: the participant now sees it, the stranger still does not
        getAs(PARTICIPANT, "/api/outings/" + outingId).andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Visibility"));
        getAs(STRANGER, "/api/outings/" + outingId).andExpect(status().isNotFound());
        getAs(PARTICIPANT, "/api/outings").andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id == " + outingId + ")].title").value("Visibility"));
        getAs(STRANGER, "/api/outings").andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id == " + outingId + ")]").isEmpty());

        // a participant can look but not touch
        postAs(PARTICIPANT, "/api/outings/" + outingId + "/cancel", "").andExpect(status().isForbidden());
        putAs(PARTICIPANT, "/api/outings/" + outingId, contents(userRef(PARTICIPANT), "", "")).andExpect(status().isForbidden());
        postAs(STRANGER, "/api/outings/" + outingId + "/cancel", "").andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Guests belong to their owner: another user cannot list, use or delete them")
    void guestsArePrivate() throws Exception {
        long guestId = createGuest(CREATOR, "Private Rahim");

        getAs(STRANGER, "/api/guests").andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == " + guestId + ")]").isEmpty());
        mvc.perform(delete("/api/guests/" + guestId).with(user(String.valueOf(STRANGER))).with(csrf()))
            .andExpect(status().isNotFound());

        long outingId = createDraft(STRANGER, "Stolen guest");
        putAs(STRANGER, "/api/outings/" + outingId, contents(userRef(STRANGER) + "," + guestRef(guestId), "", ""))
            .andExpect(status().isUnprocessableEntity());
    }
}
