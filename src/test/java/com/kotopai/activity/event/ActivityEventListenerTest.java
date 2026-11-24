package com.kotopai.activity.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kotopai.activity.domain.ActivityEvent;
import com.kotopai.activity.domain.ActivityType;
import com.kotopai.activity.repository.ActivityEventRepository;
import com.kotopai.common.events.OutingCreatedEvent;
import com.kotopai.ledger.domain.PaymentStatus;
import com.kotopai.ledger.event.DebtAddedEvent;
import com.kotopai.ledger.event.DebtDisputedEvent;
import com.kotopai.ledger.event.DelegationExecutedEvent;
import com.kotopai.ledger.event.DelegationReversedEvent;
import com.kotopai.ledger.event.GuestDebtSettledEvent;
import com.kotopai.ledger.event.PaymentConfirmedEvent;
import com.kotopai.ledger.event.PaymentRecordedEvent;

@ExtendWith(MockitoExtension.class)
class ActivityEventListenerTest {

	@Mock ActivityEventRepository activities;

	@Test
	void recordsEveryRequiredTimelineEvent() {
		ActivityEventListener listener = new ActivityEventListener(activities);
		BigDecimal amount = new BigDecimal("25.00");

		listener.onOutingCreated(new OutingCreatedEvent(10L, 1L, "Burger night"));
		listener.onDebtAdded(new DebtAddedEvent(20L, 10L, 2L, 1L, amount));
		listener.onPaymentRecorded(new PaymentRecordedEvent(
				30L, 20L, 10L, 2L, 2L, 1L, amount, PaymentStatus.PENDING_CONFIRMATION));
		listener.onPaymentConfirmed(new PaymentConfirmedEvent(30L, 20L, 10L, 1L, 2L, 1L, amount));
		listener.onDelegationExecuted(new DelegationExecutedEvent(40L, 20L, 21L, 22L, 2L, 3L, 1L, amount));
		listener.onDelegationReversed(new DelegationReversedEvent(
				40L, 22L, 2L, 2L, 3L, 1L, amount, "Keep original debts"));
		listener.onDebtDisputed(new DebtDisputedEvent(20L, 10L, 2L, 1L, 2L, 1L, "Wrong share"));
		listener.onGuestDebtSettled(new GuestDebtSettledEvent(50L, 10L, 60L, 1L, amount));

		ArgumentCaptor<ActivityEvent> captured = ArgumentCaptor.forClass(ActivityEvent.class);
		verify(activities, times(ActivityType.values().length)).save(captured.capture());
		assertThat(captured.getAllValues())
				.extracting(ActivityEvent::getType)
				.containsExactlyInAnyOrder(ActivityType.values());
	}
}
