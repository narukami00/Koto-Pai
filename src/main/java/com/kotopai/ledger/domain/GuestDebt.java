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

@Entity
@Table(name = "guest_debts")
public class GuestDebt {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "outing_id", nullable = false)
	private Long outingId;

	@Column(name = "guest_id", nullable = false)
	private Long guestId;

	@Column(name = "owner_id", nullable = false)
	private Long ownerId;

	/** Positive means the guest owes the owner; negative means the owner owes the guest. */
	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private GuestDebtStatus status;

	private Instant settledAt;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected GuestDebt() {
	}

	public GuestDebt(Long outingId, Long guestId, Long ownerId, BigDecimal amount) {
		this.outingId = LedgerAmounts.positiveId(outingId, "outingId");
		this.guestId = LedgerAmounts.positiveId(guestId, "guestId");
		this.ownerId = LedgerAmounts.positiveId(ownerId, "ownerId");
		this.amount = LedgerAmounts.exact(amount, "amount");
		if (this.amount.signum() == 0) {
			throw new IllegalArgumentException("guest debt amount must not be zero");
		}
		this.status = GuestDebtStatus.OPEN;
	}

	@PrePersist
	void initializeCreatedAt() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public void settle() {
		if (status != GuestDebtStatus.OPEN) {
			throw new IllegalStateException("only open guest debts can be settled");
		}
		status = GuestDebtStatus.SETTLED;
		settledAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public Long getOutingId() {
		return outingId;
	}

	public Long getGuestId() {
		return guestId;
	}

	public Long getOwnerId() {
		return ownerId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public GuestDebtStatus getStatus() {
		return status;
	}

	public Instant getSettledAt() {
		return settledAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
