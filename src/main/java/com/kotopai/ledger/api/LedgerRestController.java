package com.kotopai.ledger.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kotopai.ledger.service.DebtHistoryItem;
import com.kotopai.ledger.service.LedgerService;
import com.kotopai.ledger.service.LedgerSummary;
import com.kotopai.outing.port.CurrentUserProvider;

@RestController
@RequestMapping("/api")
public class LedgerRestController {

	private final LedgerService ledger;
	private final CurrentUserProvider currentUser;

	public LedgerRestController(LedgerService ledger, CurrentUserProvider currentUser) {
		this.ledger = ledger;
		this.currentUser = currentUser;
	}

	@GetMapping("/ledger")
	public LedgerSummary ledger() {
		return ledger.summaryFor(currentUser.currentUserId());
	}

	@GetMapping("/debts")
	public List<DebtHistoryItem> debts() {
		return ledger.debtsFor(currentUser.currentUserId());
	}
}
