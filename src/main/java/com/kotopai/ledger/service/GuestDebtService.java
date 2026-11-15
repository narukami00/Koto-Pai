package com.kotopai.ledger.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kotopai.ledger.domain.GuestDebt;
import com.kotopai.ledger.repository.GuestDebtRepository;
import com.kotopai.ledger.service.LedgerOperationException.Reason;

@Service
public class GuestDebtService {

	private final GuestDebtRepository guestDebts;

	public GuestDebtService(GuestDebtRepository guestDebts) {
		this.guestDebts = guestDebts;
	}

	@Transactional(readOnly = true)
	public List<GuestDebtResult> list(long ownerId) {
		return guestDebts.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
				.map(GuestDebtResult::from)
				.toList();
	}

	@Transactional
	public GuestDebtResult settle(long guestDebtId, long actorId) {
		GuestDebt debt = guestDebts.findById(guestDebtId)
				.orElseThrow(() -> new LedgerOperationException(Reason.NOT_FOUND, "Guest debt not found"));
		if (!debt.getOwnerId().equals(actorId)) {
			throw new LedgerOperationException(Reason.FORBIDDEN, "only the guest owner can settle this debt");
		}
		try {
			debt.settle();
		} catch (IllegalStateException exception) {
			throw new LedgerOperationException(Reason.CONFLICT, exception.getMessage());
		}
		guestDebts.flush();
		return GuestDebtResult.from(debt);
	}
}
