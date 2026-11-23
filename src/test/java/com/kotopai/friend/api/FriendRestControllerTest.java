package com.kotopai.friend.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.kotopai.auth.config.SecurityConfig;
import com.kotopai.auth.service.KotopaiUserDetails;
import com.kotopai.auth.service.KotopaiUserDetailsService;
import com.kotopai.common.exception.GlobalExceptionHandler;
import com.kotopai.friend.dto.FriendRequestSummary;
import com.kotopai.friend.service.FriendService;
import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;
import com.kotopai.user.dto.UserSummary;

@WebMvcTest(controllers = FriendRestController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class FriendRestControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private FriendService friendService;

    @MockitoBean
    private KotopaiUserDetailsService userDetailsService;

    private KotopaiUserDetails userDetails;

    @BeforeEach
    void setUp() {
        User user = new User("rafsan10", "Rafsan", "rafsan@example.com", "hash", Role.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        userDetails = new KotopaiUserDetails(user);
    }

    @Test
    @DisplayName("GET /api/friends returns 200 with list of accepted friends")
    void listFriends_success() throws Exception {
        User friend = new User("tanvir01", "Tanvir", "tanvir@example.com", "hash", Role.USER);
        ReflectionTestUtils.setField(friend, "id", 2L);
        when(friendService.friendsOf(1L)).thenReturn(List.of(UserSummary.from(friend)));

        mvc.perform(get("/api/friends")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2L))
                .andExpect(jsonPath("$[0].userCode").value("tanvir01"))
                .andExpect(jsonPath("$[0].displayName").value("Tanvir"));
    }

    @Test
    @DisplayName("GET /api/friends/requests returns 200 with list of pending requests")
    void listPendingRequests_success() throws Exception {
        FriendRequestSummary req = new FriendRequestSummary(
                50L, 2L, "tanvir01", "Tanvir", "#18604B", Instant.now());
        when(friendService.pendingRequestsFor(1L)).thenReturn(List.of(req));

        mvc.perform(get("/api/friends/requests")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].friendshipId").value(50L))
                .andExpect(jsonPath("$[0].requesterId").value(2L))
                .andExpect(jsonPath("$[0].userCode").value("tanvir01"));
    }

    @Test
    @DisplayName("POST /api/friends/requests returns 201 Created when request is sent")
    void sendRequest_success() throws Exception {
        when(friendService.sendFriendRequest(eq(1L), eq("@tanvir01"))).thenReturn(100L);

        String json = """
                {
                    "userCode": "@tanvir01"
                }
                """;

        mvc.perform(post("/api/friends/requests")
                        .with(csrf())
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/friends/requests/100"))
                .andExpect(jsonPath("$.friendshipId").value(100L))
                .andExpect(jsonPath("$.message").value("Friend request sent successfully"));
    }

    @Test
    @DisplayName("POST /api/friends/requests returns 400 Bad Request when userCode is blank")
    void sendRequest_blankUserCode() throws Exception {
        String json = """
                {
                    "userCode": ""
                }
                """;

        mvc.perform(post("/api/friends/requests")
                        .with(csrf())
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("userCode")));
    }

    @Test
    @DisplayName("POST /api/friends/requests/{id}/accept returns 200 OK")
    void acceptRequest_success() throws Exception {
        mvc.perform(post("/api/friends/requests/100/accept")
                        .with(csrf())
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Friend request accepted"));

        verify(friendService).acceptFriendRequest(1L, 100L);
    }

    @Test
    @DisplayName("POST /api/friends/requests/{id}/reject returns 204 No Content")
    void rejectRequest_success() throws Exception {
        mvc.perform(post("/api/friends/requests/100/reject")
                        .with(csrf())
                        .with(user(userDetails)))
                .andExpect(status().isNoContent());

        verify(friendService).rejectFriendRequest(1L, 100L);
    }

    @Test
    @DisplayName("DELETE /api/friends/{friendId} returns 204 No Content")
    void removeFriend_success() throws Exception {
        mvc.perform(delete("/api/friends/2")
                        .with(csrf())
                        .with(user(userDetails)))
                .andExpect(status().isNoContent());

        verify(friendService).removeFriend(1L, 2L);
    }
}

