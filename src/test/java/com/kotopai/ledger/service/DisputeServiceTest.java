package com.kotopai.ledger.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import com.kotopai.ledger.domain.Debt;
import com.kotopai.ledger.domain.DebtStatus;
import com.kotopai.ledger.event.DebtDisputedEvent;
import com.kotopai.ledger.repository.DebtRepository;
import com.kotopai.ledger.spi.OutingLedgerGateway;

@ExtendWith(MockitoExtension.class)
class DisputeServiceTest {

	@Mock DebtRepository debts;
	@Mock OutingLedgerGateway outings;
	@Mock ApplicationEventPublisher events;
	@Mock DelegationService delegations;

	private DisputeService service;

	@BeforeEach
	void setUp() {
		service = new DisputeService(debts, outings, events, delegations);
	}

	@Test
	void participantDisputeStoresReasonAndTargetsOutingCreator() {
		Debt debt = debt();
		when(debts.findById(10L)).thenReturn(Optional.of(debt));
		when(outings.creatorId(50L)).thenReturn(7L);

		DebtHistoryItem result = service.dispute(10L, 2L, "  Nabila was not on this item  ");

		assertThat(result.status()).isEqualTo(DebtStatus.DISPUTED);
		assertThat(result.disputeReason()).isEqualTo("Nabila was not on this item");
		ArgumentCaptor<DebtDisputedEvent> event = ArgumentCaptor.forClass(DebtDisputedEvent.class);
		verify(events).publishEvent(event.capture());
		assertThat(event.getValue().outingCreatorId()).isEqualTo(7L);
		assertThat(event.getValue().disputedById()).isEqualTo(2L);
	}

	@Test
	void unrelatedUserCannotDispute() {
		when(debts.findById(10L)).thenReturn(Optional.of(debt()));

		assertThatThrownBy(() -> service.dispute(10L, 9L, "Not mine"))
				.isInstanceOf(LedgerOperationException.class)
				.hasMessageContaining("debtor or creditor");
	}

	private static Debt debt() {
		Debt debt = Debt.forOuting(50L, 2L, 1L, new BigDecimal("100.00"));
		ReflectionTestUtils.setField(debt, "id", 10L);
		return debt;
	}
}
