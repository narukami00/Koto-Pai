package com.kotopai.friend.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.kotopai.auth.service.KotopaiUserDetails;
import com.kotopai.friend.dto.FriendItemView;
import com.kotopai.friend.dto.FriendRequestSummary;
import com.kotopai.friend.service.FriendService;
import com.kotopai.ledger.service.CounterpartyType;
import com.kotopai.ledger.service.LedgerBalance;
import com.kotopai.ledger.service.LedgerService;
import com.kotopai.ledger.service.LedgerSummary;
import com.kotopai.user.dto.UserSummary;

/**
 * Controller for the server-rendered {@code /friends} Thymeleaf page.
 */
@Controller
public class FriendPageController {

    private final FriendService friendService;
    private final LedgerService ledgerService;

    public FriendPageController(FriendService friendService, LedgerService ledgerService) {
        this.friendService = friendService;
        this.ledgerService = ledgerService;
    }

    @GetMapping("/friends")
    public String friendsPage(
            @AuthenticationPrincipal KotopaiUserDetails currentUser,
            Model model) {
        Long userId = currentUser.getId();

        List<UserSummary> rawFriends = friendService.friendsOf(userId);
        List<FriendRequestSummary> pendingRequests = friendService.pendingRequestsFor(userId);

        Map<Long, BigDecimal> balanceByFriendId = Map.of();
        try {
            LedgerSummary summary = ledgerService.summaryFor(userId);
            balanceByFriendId = summary.balances().stream()
                    .filter(e -> e.type() == CounterpartyType.USER)
                    .collect(Collectors.toMap(
                            LedgerBalance::counterpartyId,
                            LedgerBalance::balance,
                            (a, b) -> a
                    ));
        } catch (Exception ignored) {
            // Defensive: ledger is optional/resilient
        }

        Map<Long, BigDecimal> finalBalances = balanceByFriendId;
        List<FriendItemView> friends = rawFriends.stream()
                .map(f -> FriendItemView.of(f, finalBalances.get(f.id())))
                .toList();

        model.addAttribute("friends", friends);
        model.addAttribute("pendingRequests", pendingRequests);
        model.addAttribute("currentUserId", userId);

        return "friends";
    }

    @PostMapping("/friends/request")
    public String sendFriendRequest(
            @AuthenticationPrincipal KotopaiUserDetails currentUser,
            @RequestParam("userCode") String userCode,
            RedirectAttributes redirectAttributes) {
        try {
            friendService.sendFriendRequest(currentUser.getId(), userCode);
            String displayCode = userCode != null ? userCode.trim() : "";
            if (!displayCode.startsWith("@")) {
                displayCode = "@" + displayCode;
            }
            redirectAttributes.addFlashAttribute("flashSuccess", "Friend request sent to " + displayCode + "!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/friends";
    }

    @PostMapping("/friends/{id}/accept")
    public String acceptFriendRequest(
            @AuthenticationPrincipal KotopaiUserDetails currentUser,
            @PathVariable("id") Long friendshipId,
            RedirectAttributes redirectAttributes) {
        try {
            friendService.acceptFriendRequest(currentUser.getId(), friendshipId);
            redirectAttributes.addFlashAttribute("flashSuccess", "Friend request accepted!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/friends";
    }

    @PostMapping("/friends/{id}/reject")
    public String rejectFriendRequest(
            @AuthenticationPrincipal KotopaiUserDetails currentUser,
            @PathVariable("id") Long friendshipId,
            RedirectAttributes redirectAttributes) {
        try {
            friendService.rejectFriendRequest(currentUser.getId(), friendshipId);
            redirectAttributes.addFlashAttribute("flashSuccess", "Friend request rejected.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/friends";
    }

    @PostMapping("/friends/{friendId}/remove")
    public String removeFriend(
            @AuthenticationPrincipal KotopaiUserDetails currentUser,
            @PathVariable("friendId") Long friendId,
            RedirectAttributes redirectAttributes) {
        try {
            friendService.removeFriend(currentUser.getId(), friendId);
            redirectAttributes.addFlashAttribute("flashSuccess", "Friend removed.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/friends";
    }
}
