package com.kotopai.ledger.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.kotopai.activity.service.ActivityService;
import com.kotopai.ledger.service.LedgerService;
import com.kotopai.outing.port.CurrentUserProvider;

@Controller
public class LedgerPageController {

	private final LedgerService ledger;
	private final ActivityService activities;
	private final CurrentUserProvider currentUser;

	public LedgerPageController(
			LedgerService ledger,
			ActivityService activities,
			CurrentUserProvider currentUser) {
		this.ledger = ledger;
		this.activities = activities;
		this.currentUser = currentUser;
	}

	@GetMapping("/ledger")
	public String ledger(Model model) {
		long userId = currentUser.currentUserId();
		model.addAttribute("summary", ledger.summaryFor(userId));
		return "ledger/index";
	}

	@GetMapping("/ledger/friends/{friendId}")
	public String friendHistory(
			@PathVariable long friendId,
			Model model) {
		long userId = currentUser.currentUserId();
		model.addAttribute("userId", userId);
		model.addAttribute("friendId", friendId);
		model.addAttribute("history", ledger.historyBetween(userId, friendId));
		model.addAttribute("activities", activities.between(userId, friendId));
		return "ledger/friend-history";
	}
}
