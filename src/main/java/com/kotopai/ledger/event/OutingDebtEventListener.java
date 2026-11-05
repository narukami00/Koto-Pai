package com.kotopai.ledger.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.kotopai.ledger.service.DebtService;

@Component
public class OutingDebtEventListener {

	private final DebtService debts;

	public OutingDebtEventListener(DebtService debts) {
		this.debts = debts;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onOutingActivated(OutingActivatedEvent event) {
		debts.generateDebts(event.outingId());
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onOutingCancelled(OutingCancelledEvent event) {
		debts.voidDebts(event.outingId());
	}
}
