package com.kotopai.activity.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.kotopai.activity.domain.ActivityEvent;
import com.kotopai.activity.domain.ActivityType;
import com.kotopai.activity.repository.ActivityEventRepository;
import com.kotopai.common.events.OutingCreatedEvent;
import com.kotopai.ledger.event.DebtAddedEvent;
import com.kotopai.ledger.event.DebtDisputedEvent;
import com.kotopai.ledger.event.DelegationExecutedEvent;
import com.kotopai.ledger.event.DelegationReversedEvent;
import com.kotopai.ledger.event.GuestDebtSettledEvent;
import com.kotopai.ledger.event.PaymentConfirmedEvent;
import com.kotopai.ledger.event.PaymentRecordedEvent;

/** Observer that turns domain events into durable, user-visible timeline entries. */
@Component
public class ActivityEventListener {

	private final ActivityEventRepository activities;

	public ActivityEventListener(ActivityEventRepository activities) {
		this.activities = activities;
	}

	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
	public void onOutingCreated(OutingCreatedEvent event) {
		record(new ActivityEvent(
				event.creatorId(), null, null, ActivityType.OUTING_CREATED,
				event.outingId(), null, null, null,
				"Created outing “" + event.title() + "”"));
	}

	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
	public void onDebtAdded(DebtAddedEvent event) {
		record(new ActivityEvent(
				event.debtorId(), event.creditorId(), null, ActivityType.DEBT_ADDED,
				event.outingId(), event.debtId(), null, event.amount(),
				"User #" + event.debtorId() + " owes User #" + event.creditorId()
						+ " ৳" + event.amount()));
	}

	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
	public void onPaymentRecorded(PaymentRecordedEvent event) {
		long otherUser = event.recordedById() == event.debtorId() ? event.creditorId() : event.debtorId();
		record(new ActivityEvent(
				event.recordedById(), otherUser, null, ActivityType.PAYMENT_RECORDED,
				event.outingId(), event.debtId(), null, event.amount(),
				"User #" + event.recordedById() + " recorded a ৳" + event.amount() + " payment"));
	}

	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
	public void onPaymentConfirmed(PaymentConfirmedEvent event) {
		record(new ActivityEvent(
				event.confirmedById(), event.debtorId(), null, ActivityType.PAYMENT_CONFIRMED,
				event.outingId(), event.debtId(), null, event.amount(),
				"User #" + event.confirmedById() + " confirmed a ৳" + event.amount() + " payment"));
	}

	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
	public void onDelegationExecuted(DelegationExecutedEvent event) {
		record(new ActivityEvent(
				event.middlemanId(), event.debtorId(), event.creditorId(), ActivityType.DELEGATION_EXECUTED,
				null, event.delegatedDebtId(), event.delegationId(), event.amount(),
				"User #" + event.middlemanId() + " delegated ৳" + event.amount()
						+ " from User #" + event.debtorId() + " to User #" + event.creditorId()));
	}

	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
	public void onDelegationReversed(DelegationReversedEvent event) {
		long otherOuterUser = event.disputedById() == event.debtorId() ? event.creditorId() : event.debtorId();
		record(new ActivityEvent(
				event.disputedById(), otherOuterUser, event.middlemanId(), ActivityType.DELEGATION_REVERSED,
				null, event.delegatedDebtId(), event.delegationId(), event.amount(),
				"User #" + event.disputedById() + " disputed and reversed a ৳" + event.amount()
						+ " delegation: " + event.reason()));
	}

	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
	public void onDebtDisputed(DebtDisputedEvent event) {
		long otherUser = event.disputedById() == event.debtorId() ? event.creditorId() : event.debtorId();
		record(new ActivityEvent(
				event.disputedById(), otherUser, event.outingCreatorId(), ActivityType.DEBT_DISPUTED,
				event.outingId(), event.debtId(), null, null,
				"User #" + event.disputedById() + " disputed debt #" + event.debtId()
						+ ": " + event.reason()));
	}

	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
	public void onGuestDebtSettled(GuestDebtSettledEvent event) {
		record(new ActivityEvent(
				event.ownerId(), null, null, ActivityType.GUEST_DEBT_SETTLED,
				event.outingId(), null, null, event.amount().abs(),
				"Marked Guest #" + event.guestId() + " settled for ৳" + event.amount().abs()));
	}

	private void record(ActivityEvent event) {
		activities.save(event);
	}
}
