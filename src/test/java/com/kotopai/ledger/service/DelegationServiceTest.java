package com.kotopai.ledger.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.kotopai.friend.service.FriendService;
import com.kotopai.ledger.domain.Debt;
import com.kotopai.ledger.domain.DebtStatus;
import com.kotopai.ledger.domain.Delegation;
import com.kotopai.ledger.event.DelegationExecutedEvent;
import com.kotopai.ledger.repository.DebtRepository;
import com.kotopai.ledger.repository.DelegationRepository;
import com.kotopai.ledger.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class DelegationServiceTest {

	@Mock DebtRepository debts;
	@Mock DelegationRepository delegations;
	@Mock PaymentRepository payments;
	@Mock FriendService friends;
	@Mock ApplicationEventPublisher events;

	private DelegationService service;

	@BeforeEach
	void setUp() {
		service = new DelegationService(debts, delegations, payments, friends, events);
	}

	@Test
	void middlemanDelegatesPartialAmountAndPublishesEvent() {
		Debt incoming = debt(10L, 1L, 2L, "100.00");
		Debt outgoing = debt(20L, 2L, 3L, "50.00");
		when(debts.findById(10L)).thenReturn(Optional.of(incoming));
		when(debts.findById(20L)).thenReturn(Optional.of(outgoing));
		when(friends.areFriends(1L, 3L)).thenReturn(true);
		when(debts.saveAndFlush(any(Debt.class))).thenAnswer(invocation -> {
			Debt delegated = invocation.getArgument(0);
			ReflectionTestUtils.setField(delegated, "id", 30L);
			return delegated;
		});
		when(delegations.saveAndFlush(any(Delegation.class))).thenAnswer(invocation -> {
			Delegation delegation = invocation.getArgument(0);
			ReflectionTestUtils.setField(delegation, "id", 40L);
			return delegation;
		});

		DelegationResult result = service.execute(2L, 10L, 20L, money("30.00"));

		assertThat(result.id()).isEqualTo(40L);
		assertThat(result.delegatedDebtId()).isEqualTo(30L);
		assertThat(incoming.getRemaining()).isEqualByComparingTo("70.00");
		assertThat(outgoing.getRemaining()).isEqualByComparingTo("20.00");
		assertThat(incoming.getStatus()).isEqualTo(DebtStatus.PARTIALLY_PAID);
		ArgumentCaptor<DelegationExecutedEvent> published =
				ArgumentCaptor.forClass(DelegationExecutedEvent.class);
		verify(events).publishEvent(published.capture());
		assertThat(published.getValue().amount()).isEqualByComparingTo("30.00");
	}

	@Test
	void userWhoIsNotTheMiddlemanCannotDelegate() {
		when(debts.findById(10L)).thenReturn(Optional.of(debt(10L, 1L, 2L, "100.00")));
		when(debts.findById(20L)).thenReturn(Optional.of(debt(20L, 2L, 3L, "50.00")));

		assertThatThrownBy(() -> service.execute(1L, 10L, 20L, money("30.00")))
				.isInstanceOf(LedgerOperationException.class)
				.hasMessageContaining("middleman");
	}

	private static Debt debt(long id, long debtorId, long creditorId, String amount) {
		Debt debt = Debt.forOuting(id, debtorId, creditorId, money(amount));
		ReflectionTestUtils.setField(debt, "id", id);
		return debt;
	}

	private static BigDecimal money(String amount) {
		return new BigDecimal(amount);
	}
}
