package com.kotopai.friend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.kotopai.auth.service.KotopaiUserDetails;
import com.kotopai.friend.domain.Friendship;
import com.kotopai.friend.domain.FriendshipStatus;
import com.kotopai.friend.repository.FriendshipRepository;
import com.kotopai.friend.service.FriendService;
import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;
import com.kotopai.user.dto.UserSummary;
import com.kotopai.user.repository.UserRepository;

@SpringBootTest
@Transactional
class FriendLifecycleIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private FriendService friendService;

    private MockMvc mvc;
    private User alice;
    private User bob;
    private KotopaiUserDetails aliceDetails;
    private KotopaiUserDetails bobDetails;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        alice = userRepository.save(new User("alice_f", "Alice", "alice_f@example.com", "hash", Role.USER));
        bob = userRepository.save(new User("bob_f", "Bob", "bob_f@example.com", "hash", Role.USER));

        aliceDetails = new KotopaiUserDetails(alice);
        bobDetails = new KotopaiUserDetails(bob);
    }

    @Test
    @DisplayName("Complete friendship lifecycle: send -> pending -> accept -> list friends -> remove")
    void fullFriendshipLifecycle() throws Exception {
        // 1. Initial state: not friends
        assertThat(friendService.areFriends(alice.getId(), bob.getId())).isFalse();

        // 2. Alice sends friend request to @bob_f
        String sendJson = """
                {
                    "userCode": "@bob_f"
                }
                """;

        String responseStr = mvc.perform(post("/api/friends/requests")
                        .with(csrf())
                        .with(user(aliceDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sendJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.friendshipId").exists())
                .andReturn().getResponse().getContentAsString();

        Long friendshipId = friendshipRepository.findBetween(alice.getId(), bob.getId())
                .map(Friendship::getId)
                .orElseThrow();

        // 3. Bob checks pending requests via REST API
        mvc.perform(get("/api/friends/requests")
                        .with(user(bobDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].friendshipId").value(friendshipId))
                .andExpect(jsonPath("$[0].requesterId").value(alice.getId()))
                .andExpect(jsonPath("$[0].userCode").value("alice_f"));

        // 4. Bob accepts Alice's friend request
        mvc.perform(post("/api/friends/requests/" + friendshipId + "/accept")
                        .with(csrf())
                        .with(user(bobDetails)))
                .andExpect(status().isOk());

        // 5. Verify friendship is accepted in DB & service
        assertThat(friendService.areFriends(alice.getId(), bob.getId())).isTrue();
        assertThat(friendService.areFriends(bob.getId(), alice.getId())).isTrue();

        // 6. Alice sees Bob in her friend list
        mvc.perform(get("/api/friends")
                        .with(user(aliceDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(bob.getId()))
                .andExpect(jsonPath("$[0].userCode").value("bob_f"));

        // 7. Bob sees Alice in his friend list
        List<UserSummary> bobsFriends = friendService.friendsOf(bob.getId());
        assertThat(bobsFriends).extracting(UserSummary::userCode).containsExactly("alice_f");

        // 8. Alice removes Bob as a friend
        mvc.perform(delete("/api/friends/" + bob.getId())
                        .with(csrf())
                        .with(user(aliceDetails)))
                .andExpect(status().isNoContent());

        // 9. Verified: no longer friends
        assertThat(friendService.areFriends(alice.getId(), bob.getId())).isFalse();
        assertThat(friendService.friendsOf(alice.getId())).isEmpty();
        assertThat(friendService.friendsOf(bob.getId())).isEmpty();
    }
}

