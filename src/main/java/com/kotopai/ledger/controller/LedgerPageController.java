package com.kotopai.ledger.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.kotopai.ledger.service.LedgerService;

@Controller
public class LedgerPageController {

	private final LedgerService ledger;

	public LedgerPageController(LedgerService ledger) {
		this.ledger = ledger;
	}

	@GetMapping("/ledger")
	public String ledger(@RequestParam long userId, Model model) {
		model.addAttribute("summary", ledger.summaryFor(userId));
		return "ledger/index";
	}

	@GetMapping("/ledger/friends/{friendId}")
	public String friendHistory(
			@PathVariable long friendId,
			@RequestParam long userId,
			Model model) {
		model.addAttribute("userId", userId);
		model.addAttribute("friendId", friendId);
		model.addAttribute("history", ledger.historyBetween(userId, friendId));
		return "ledger/friend-history";
	}
}
