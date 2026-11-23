package com.kotopai.friend.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.kotopai.auth.config.SecurityConfig;
import com.kotopai.auth.service.KotopaiUserDetails;
import com.kotopai.auth.service.KotopaiUserDetailsService;
import com.kotopai.common.web.LayoutModelAdvice;
import com.kotopai.friend.dto.FriendRequestSummary;
import com.kotopai.friend.service.FriendService;
import com.kotopai.ledger.service.CounterpartyType;
import com.kotopai.ledger.service.LedgerBalance;
import com.kotopai.ledger.service.LedgerService;
import com.kotopai.ledger.service.LedgerSummary;
import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;
import com.kotopai.user.dto.UserSummary;

@WebMvcTest(controllers = FriendPageController.class)
@Import({SecurityConfig.class, LayoutModelAdvice.class})
class FriendPageControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private FriendService friendService;

    @MockitoBean
    private LedgerService ledgerService;

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
    @DisplayName("GET /friends renders friends template with friends and pending requests")
    void friendsPage_success() throws Exception {
        User friend = new User("tanvir01", "Tanvir", "tanvir@example.com", "hash", Role.USER);
        ReflectionTestUtils.setField(friend, "id", 2L);

        when(friendService.friendsOf(1L)).thenReturn(List.of(UserSummary.from(friend)));
        when(friendService.pendingRequestsFor(1L)).thenReturn(List.of(
                new FriendRequestSummary(10L, 3L, "nabila02", "Nabila", "#18604B", Instant.now())));

        LedgerSummary ledgerSummary = new LedgerSummary(
                1L, BigDecimal.valueOf(300), BigDecimal.ZERO, BigDecimal.valueOf(300),
                List.of(new LedgerBalance(2L, CounterpartyType.USER, BigDecimal.valueOf(300))));
        when(ledgerService.summaryFor(1L)).thenReturn(ledgerSummary);

        mvc.perform(get("/friends")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("friends"))
                .andExpect(model().attributeExists("friends", "pendingRequests", "currentUserId"));
    }

    @Test
    @DisplayName("POST /friends/request redirects with flashSuccess on successful request")
    void sendFriendRequest_success() throws Exception {
        when(friendService.sendFriendRequest(eq(1L), eq("tanvir01"))).thenReturn(50L);

        mvc.perform(post("/friends/request")
                        .with(csrf())
                        .with(user(userDetails))
                        .param("userCode", "tanvir01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/friends"))
                .andExpect(flash().attribute("flashSuccess", "Friend request sent to @tanvir01!"));
    }

    @Test
    @DisplayName("POST /friends/request redirects with flashError on failure")
    void sendFriendRequest_failure() throws Exception {
        doThrow(new IllegalArgumentException("User not found"))
                .when(friendService).sendFriendRequest(eq(1L), eq("unknown_user"));

        mvc.perform(post("/friends/request")
                        .with(csrf())
                        .with(user(userDetails))
                        .param("userCode", "unknown_user"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/friends"))
                .andExpect(flash().attribute("flashError", "User not found"));
    }

    @Test
    @DisplayName("POST /friends/{id}/accept accepts request and redirects with flashSuccess")
    void acceptFriendRequest_success() throws Exception {
        mvc.perform(post("/friends/10/accept")
                        .with(csrf())
                        .with(user(userDetails)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/friends"))
                .andExpect(flash().attribute("flashSuccess", "Friend request accepted!"));

        verify(friendService).acceptFriendRequest(1L, 10L);
    }

    @Test
    @DisplayName("POST /friends/{id}/reject rejects request and redirects with flashSuccess")
    void rejectFriendRequest_success() throws Exception {
        mvc.perform(post("/friends/10/reject")
                        .with(csrf())
                        .with(user(userDetails)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/friends"))
                .andExpect(flash().attribute("flashSuccess", "Friend request rejected."));

        verify(friendService).rejectFriendRequest(1L, 10L);
    }

    @Test
    @DisplayName("POST /friends/{friendId}/remove removes friend and redirects with flashSuccess")
    void removeFriend_success() throws Exception {
        mvc.perform(post("/friends/2/remove")
                        .with(csrf())
                        .with(user(userDetails)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/friends"))
                .andExpect(flash().attribute("flashSuccess", "Friend removed."));

        verify(friendService).removeFriend(1L, 2L);
    }
}

