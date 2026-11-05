package com.kotopai.ledger.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kotopai.ledger.service.DebtHistoryItem;
import com.kotopai.ledger.service.LedgerService;
import com.kotopai.ledger.service.LedgerSummary;

@RestController
@RequestMapping("/api")
public class LedgerRestController {

	private final LedgerService ledger;

	public LedgerRestController(LedgerService ledger) {
		this.ledger = ledger;
	}

	@GetMapping("/ledger")
	public LedgerSummary ledger(@RequestParam long userId) {
		return ledger.summaryFor(userId);
	}

	@GetMapping("/debts")
	public List<DebtHistoryItem> debts(@RequestParam long userId) {
		return ledger.debtsFor(userId);
	}
}
