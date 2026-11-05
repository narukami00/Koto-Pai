package com.kotopai.ledger.event;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import com.kotopai.ledger.service.DebtService;

class OutingDebtEventListenerTest {

	private final DebtService debts = mock(DebtService.class);
	private final OutingDebtEventListener listener = new OutingDebtEventListener(debts);

	@Test
	void generatesDebtsWhenOutingActivates() {
		listener.onOutingActivated(new OutingActivatedEvent(42L, 7L));

		verify(debts).generateDebts(42L);
	}

	@Test
	void voidsDebtsWhenOutingIsCancelled() {
		listener.onOutingCancelled(new OutingCancelledEvent(42L));

		verify(debts).voidDebts(42L);
	}
}
