package com.kotopai.ledger.controller;

import java.math.BigDecimal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.kotopai.ledger.service.LedgerService;
import com.kotopai.ledger.service.SettlementFacade;
import com.kotopai.outing.port.CurrentUserProvider;

@Controller
public class SettlePageController {

	private final SettlementFacade settlement;
	private final LedgerService ledger;
	private final CurrentUserProvider currentUser;

	public SettlePageController(
			SettlementFacade settlement,
			LedgerService ledger,
			CurrentUserProvider currentUser) {
		this.settlement = settlement;
		this.ledger = ledger;
		this.currentUser = currentUser;
	}

	@GetMapping("/settle")
	public String settle(Model model) {
		long actorId = currentUser.currentUserId();
		model.addAttribute("userId", actorId);
		model.addAttribute("debts", ledger.debtsFor(actorId).stream()
				.filter(debt -> debt.status().contributesToBalance()).toList());
		model.addAttribute("suggestions", settlement.delegationSuggestions(actorId));
		return "ledger/settle";
	}

	@PostMapping("/settle/delegations")
	public String delegate(
			@RequestParam long incomingDebtId,
			@RequestParam long outgoingDebtId,
			@RequestParam BigDecimal amount) {
		settlement.delegate(currentUser.currentUserId(), incomingDebtId, outgoingDebtId, amount);
		return "redirect:/settle?delegated";
	}
}
