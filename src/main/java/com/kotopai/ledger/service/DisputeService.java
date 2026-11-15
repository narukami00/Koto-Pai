package com.kotopai.ledger.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kotopai.ledger.domain.Debt;
import com.kotopai.ledger.event.DebtDisputedEvent;
import com.kotopai.ledger.repository.DebtRepository;
import com.kotopai.ledger.spi.OutingLedgerGateway;
import com.kotopai.ledger.service.LedgerOperationException.Reason;

@Service
public class DisputeService {

	private final DebtRepository debts;
	private final OutingLedgerGateway outings;
	private final ApplicationEventPublisher events;

	public DisputeService(
			DebtRepository debts,
			OutingLedgerGateway outings,
			ApplicationEventPublisher events) {
		this.debts = debts;
		this.outings = outings;
		this.events = events;
	}

	@Transactional
	public DebtHistoryItem dispute(long debtId, long actorId, String reason) {
		Debt debt = debts.findById(debtId)
				.orElseThrow(() -> new LedgerOperationException(Reason.NOT_FOUND, "Debt not found"));
		if (!debt.getDebtorId().equals(actorId) && !debt.getCreditorId().equals(actorId)) {
			throw new LedgerOperationException(Reason.FORBIDDEN, "only the debtor or creditor can dispute this debt");
		}
		if (debt.getOutingId() == null) {
			throw new LedgerOperationException(Reason.CONFLICT, "delegated debt disputes are handled with delegation reversal");
		}
		try {
			debt.dispute(actorId, reason);
		} catch (IllegalStateException exception) {
			throw new LedgerOperationException(Reason.CONFLICT, exception.getMessage());
		}
		debts.flush();
		long creatorId = outings.creatorId(debt.getOutingId());
		events.publishEvent(new DebtDisputedEvent(
				debt.getId(), debt.getOutingId(), actorId, creatorId,
				debt.getDebtorId(), debt.getCreditorId(), debt.getDisputeReason()));
		return LedgerService.toHistoryItem(debt);
	}
}
