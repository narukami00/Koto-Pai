package com.kotopai.friend.api;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kotopai.auth.service.KotopaiUserDetails;
import com.kotopai.friend.dto.FriendRequestSummary;
import com.kotopai.friend.dto.SendFriendRequest;
import com.kotopai.friend.service.FriendService;
import com.kotopai.user.dto.UserSummary;

import jakarta.validation.Valid;

/**
 * REST API controller for friendship management, friend requests, and queries.
 */
@RestController
@RequestMapping("/api/friends")
public class FriendRestController {

    private final FriendService friendService;

    public FriendRestController(FriendService friendService) {
        this.friendService = friendService;
    }

    @GetMapping
    public ResponseEntity<List<UserSummary>> listFriends(
            @AuthenticationPrincipal KotopaiUserDetails currentUser) {
        return ResponseEntity.ok(friendService.friendsOf(currentUser.getId()));
    }

    @GetMapping("/requests")
    public ResponseEntity<List<FriendRequestSummary>> listPendingRequests(
            @AuthenticationPrincipal KotopaiUserDetails currentUser) {
        return ResponseEntity.ok(friendService.pendingRequestsFor(currentUser.getId()));
    }

    @PostMapping("/requests")
    public ResponseEntity<Map<String, Object>> sendRequest(
            @AuthenticationPrincipal KotopaiUserDetails currentUser,
            @Valid @RequestBody SendFriendRequest request) {
        Long friendshipId = friendService.sendFriendRequest(currentUser.getId(), request.userCode());
        return ResponseEntity
                .created(URI.create("/api/friends/requests/" + friendshipId))
                .body(Map.of(
                        "friendshipId", friendshipId,
                        "message", "Friend request sent successfully"
                ));
    }

    @PostMapping("/requests/{id}/accept")
    public ResponseEntity<Map<String, String>> acceptRequest(
            @AuthenticationPrincipal KotopaiUserDetails currentUser,
            @PathVariable("id") Long friendshipId) {
        friendService.acceptFriendRequest(currentUser.getId(), friendshipId);
        return ResponseEntity.ok(Map.of("message", "Friend request accepted"));
    }

    @PostMapping("/requests/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectRequest(
            @AuthenticationPrincipal KotopaiUserDetails currentUser,
            @PathVariable("id") Long friendshipId) {
        friendService.rejectFriendRequest(currentUser.getId(), friendshipId);
    }

    @DeleteMapping("/{friendId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFriend(
            @AuthenticationPrincipal KotopaiUserDetails currentUser,
            @PathVariable("friendId") Long friendId) {
        friendService.removeFriend(currentUser.getId(), friendId);
    }
}

