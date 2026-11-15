package com.kotopai.outing.adapter;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kotopai.common.exception.ResourceNotFoundException;
import com.kotopai.ledger.spi.OutingLedgerGateway;
import com.kotopai.outing.domain.Outing;
import com.kotopai.outing.domain.OutingStatus;
import com.kotopai.outing.repository.OutingRepository;

/** Keeps outing persistence behind a ledger-owned port instead of leaking its repository across packages. */
@Component
public class OutingLedgerAdapter implements OutingLedgerGateway {

	private final OutingRepository outings;

	public OutingLedgerAdapter(OutingRepository outings) {
		this.outings = outings;
	}

	@Override
	@Transactional(readOnly = true)
	public long creatorId(long outingId) {
		return load(outingId).getCreatorId();
	}

	@Override
	@Transactional
	public void markSettled(long outingId) {
		Outing outing = load(outingId);
		if (outing.getStatus() == OutingStatus.SETTLED) {
			return;
		}
		outing.markSettled();
	}

	private Outing load(long outingId) {
		return outings.findById(outingId)
				.orElseThrow(() -> new ResourceNotFoundException("Outing", outingId));
	}
}
