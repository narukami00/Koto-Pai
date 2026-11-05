package com.kotopai.ledger.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "debts")
public class Debt {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "outing_id")
	private Long outingId;

	@Column(name = "debtor_id", nullable = false)
	private Long debtorId;

	@Column(name = "creditor_id", nullable = false)
	private Long creditorId;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal remaining;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 24)
	private DebtStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private DebtOrigin origin;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Version
	private long version;

	protected Debt() {
	}

	private Debt(
			Long outingId,
			Long debtorId,
			Long creditorId,
			BigDecimal amount,
			DebtOrigin origin) {
		if (outingId != null) {
			LedgerAmounts.positiveId(outingId, "outingId");
		}
		this.debtorId = LedgerAmounts.positiveId(debtorId, "debtorId");
		this.creditorId = LedgerAmounts.positiveId(creditorId, "creditorId");
		if (this.debtorId.equals(this.creditorId)) {
			throw new IllegalArgumentException("debtor and creditor must be different users");
		}
		this.outingId = outingId;
		this.amount = LedgerAmounts.positive(amount, "amount");
		this.remaining = this.amount;
		this.status = DebtStatus.OPEN;
		this.origin = java.util.Objects.requireNonNull(origin, "origin must not be null");
	}

	public static Debt forOuting(Long outingId, Long debtorId, Long creditorId, BigDecimal amount) {
		LedgerAmounts.positiveId(outingId, "outingId");
		return new Debt(outingId, debtorId, creditorId, amount, DebtOrigin.OUTING);
	}

	public static Debt delegated(Long debtorId, Long creditorId, BigDecimal amount) {
		return new Debt(null, debtorId, creditorId, amount, DebtOrigin.DELEGATION);
	}

	@PrePersist
	void initializeCreatedAt() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public void applyConfirmedPayment(BigDecimal paymentAmount) {
		if (!status.contributesToBalance()) {
			throw new IllegalStateException("payments require an open debt");
		}
		BigDecimal payment = LedgerAmounts.positive(paymentAmount, "paymentAmount");
		if (payment.compareTo(remaining) > 0) {
			throw new IllegalArgumentException("payment cannot exceed remaining debt");
		}
		remaining = remaining.subtract(payment);
		if (remaining.signum() == 0) {
			transitionTo(DebtStatus.PAID);
		} else if (status == DebtStatus.OPEN) {
			transitionTo(DebtStatus.PARTIALLY_PAID);
		}
	}

	public void reduceForDelegation(BigDecimal delegatedAmount) {
		if (!status.contributesToBalance()) {
			throw new IllegalStateException("delegation requires an open debt");
		}
		BigDecimal reduction = LedgerAmounts.positive(delegatedAmount, "delegatedAmount");
		if (reduction.compareTo(remaining) > 0) {
			throw new IllegalArgumentException("delegation cannot exceed remaining debt");
		}
		remaining = remaining.subtract(reduction);
		if (remaining.signum() == 0) {
			transitionTo(DebtStatus.PAID);
		} else if (status == DebtStatus.OPEN) {
			transitionTo(DebtStatus.PARTIALLY_PAID);
		}
	}

	public void dispute() {
		transitionTo(DebtStatus.DISPUTED);
	}

	public void reopen() {
		transitionTo(DebtStatus.OPEN);
	}

	public void voidDebt() {
		if (status != DebtStatus.VOID) {
			transitionTo(DebtStatus.VOID);
		}
	}

	private void transitionTo(DebtStatus target) {
		status.requireTransitionTo(target);
		status = target;
	}

	public Long getId() {
		return id;
	}

	public Long getOutingId() {
		return outingId;
	}

	public Long getDebtorId() {
		return debtorId;
	}

	public Long getCreditorId() {
		return creditorId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public BigDecimal getRemaining() {
		return remaining;
	}

	public DebtStatus getStatus() {
		return status;
	}

	public DebtOrigin getOrigin() {
		return origin;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public long getVersion() {
		return version;
	}
}
