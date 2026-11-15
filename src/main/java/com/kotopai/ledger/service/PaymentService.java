package com.kotopai.ledger.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kotopai.ledger.domain.Debt;
import com.kotopai.ledger.domain.DebtStatus;
import com.kotopai.ledger.domain.Payment;
import com.kotopai.ledger.domain.PaymentStatus;
import com.kotopai.ledger.event.PaymentConfirmedEvent;
import com.kotopai.ledger.event.PaymentRecordedEvent;
import com.kotopai.ledger.repository.DebtRepository;
import com.kotopai.ledger.repository.PaymentRepository;
import com.kotopai.ledger.spi.OutingLedgerGateway;
import com.kotopai.ledger.service.LedgerOperationException.Reason;

@Service
public class PaymentService {

	private final DebtRepository debts;
	private final PaymentRepository payments;
	private final OutingLedgerGateway outings;
	private final ApplicationEventPublisher events;

	public PaymentService(
			DebtRepository debts,
			PaymentRepository payments,
			OutingLedgerGateway outings,
			ApplicationEventPublisher events) {
		this.debts = debts;
		this.payments = payments;
		this.outings = outings;
		this.events = events;
	}

	@Transactional
	public PaymentResult record(long debtId, long actorId, BigDecimal amount) {
		Debt debt = debt(debtId);
		requirePayable(debt);
		if (amount == null || amount.signum() <= 0) {
			throw new IllegalArgumentException("amount must be positive");
		}
		if (amount.compareTo(debt.getRemaining()) > 0) {
			throw new LedgerOperationException(Reason.CONFLICT, "payment cannot exceed remaining debt");
		}

		Payment payment;
		if (debt.getDebtorId().equals(actorId)) {
			payment = Payment.pending(debt, amount, actorId);
		} else if (debt.getCreditorId().equals(actorId)) {
			payment = Payment.confirmed(debt, amount, actorId);
			debt.applyConfirmedPayment(amount);
		} else {
			throw new LedgerOperationException(Reason.FORBIDDEN, "only the debtor or creditor can record this payment");
		}

		payment = payments.saveAndFlush(payment);
		publishRecorded(payment, debt);
		if (payment.getStatus() == PaymentStatus.CONFIRMED) {
			publishConfirmed(payment, debt, actorId);
			settleOutingWhenComplete(debt.getOutingId());
		}
		return PaymentResult.from(payment);
	}

	@Transactional
	public PaymentResult confirm(long paymentId, long actorId) {
		Payment payment = payments.findById(paymentId)
				.orElseThrow(() -> new LedgerOperationException(Reason.NOT_FOUND, "Payment not found"));
		Debt debt = payment.getDebt();
		if (!debt.getCreditorId().equals(actorId)) {
			throw new LedgerOperationException(Reason.FORBIDDEN, "only the creditor can confirm this payment");
		}
		if (payment.getStatus() != PaymentStatus.PENDING_CONFIRMATION) {
			throw new LedgerOperationException(Reason.CONFLICT, "payment is not pending confirmation");
		}
		requirePayable(debt);
		if (payment.getAmount().compareTo(debt.getRemaining()) > 0) {
			throw new LedgerOperationException(Reason.CONFLICT, "payment exceeds the debt remaining after another settlement");
		}

		debt.applyConfirmedPayment(payment.getAmount());
		payment.confirm(actorId);
		payments.flush();
		publishConfirmed(payment, debt, actorId);
		settleOutingWhenComplete(debt.getOutingId());
		return PaymentResult.from(payment);
	}

	private Debt debt(long debtId) {
		return debts.findById(debtId)
				.orElseThrow(() -> new LedgerOperationException(Reason.NOT_FOUND, "Debt not found"));
	}

	private static void requirePayable(Debt debt) {
		if (!debt.getStatus().contributesToBalance()) {
			throw new LedgerOperationException(Reason.CONFLICT, "payments require an open debt");
		}
	}

	private void settleOutingWhenComplete(Long outingId) {
		if (outingId == null) {
			return;
		}
		List<Debt> activeDebts = debts.findAllByOutingId(outingId).stream()
				.filter(debt -> debt.getStatus() != DebtStatus.VOID)
				.toList();
		if (!activeDebts.isEmpty() && activeDebts.stream().allMatch(debt -> debt.getStatus() == DebtStatus.PAID)) {
			outings.markSettled(outingId);
		}
	}

	private void publishRecorded(Payment payment, Debt debt) {
		events.publishEvent(new PaymentRecordedEvent(
				payment.getId(), debt.getId(), debt.getOutingId(), payment.getRecordedById(),
				debt.getDebtorId(), debt.getCreditorId(), payment.getAmount(), payment.getStatus()));
	}

	private void publishConfirmed(Payment payment, Debt debt, long actorId) {
		events.publishEvent(new PaymentConfirmedEvent(
				payment.getId(), debt.getId(), debt.getOutingId(), actorId,
				debt.getDebtorId(), debt.getCreditorId(), payment.getAmount()));
	}
}
