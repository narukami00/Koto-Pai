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

/** Audit record linking the two reduced debts to the direct debt created by delegation. */
@Entity
@Table(name = "delegations")
public class Delegation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "incoming_debt_id", nullable = false)
	private Long incomingDebtId;

	@Column(name = "outgoing_debt_id", nullable = false)
	private Long outgoingDebtId;

	@Column(name = "delegated_debt_id", nullable = false, unique = true)
	private Long delegatedDebtId;

	@Column(name = "debtor_id", nullable = false)
	private Long debtorId;

	@Column(name = "middleman_id", nullable = false)
	private Long middlemanId;

	@Column(name = "creditor_id", nullable = false)
	private Long creditorId;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private DelegationStatus status;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "reversed_at")
	private Instant reversedAt;

	@Column(name = "reversed_by_id")
	private Long reversedById;

	@Column(name = "reversal_reason", length = 500)
	private String reversalReason;

	@Version
	private long version;

	protected Delegation() {
	}

	private Delegation(
			Long incomingDebtId,
			Long outgoingDebtId,
			Long delegatedDebtId,
			Long debtorId,
			Long middlemanId,
			Long creditorId,
			BigDecimal amount) {
		this.incomingDebtId = LedgerAmounts.positiveId(incomingDebtId, "incomingDebtId");
		this.outgoingDebtId = LedgerAmounts.positiveId(outgoingDebtId, "outgoingDebtId");
		this.delegatedDebtId = LedgerAmounts.positiveId(delegatedDebtId, "delegatedDebtId");
		this.debtorId = LedgerAmounts.positiveId(debtorId, "debtorId");
		this.middlemanId = LedgerAmounts.positiveId(middlemanId, "middlemanId");
		this.creditorId = LedgerAmounts.positiveId(creditorId, "creditorId");
		if (this.debtorId.equals(this.middlemanId)
				|| this.middlemanId.equals(this.creditorId)
				|| this.debtorId.equals(this.creditorId)) {
			throw new IllegalArgumentException("delegation users must be different");
		}
		this.amount = LedgerAmounts.positive(amount, "amount");
		this.status = DelegationStatus.EXECUTED;
	}

	public static Delegation executed(
			Long incomingDebtId,
			Long outgoingDebtId,
			Long delegatedDebtId,
			Long debtorId,
			Long middlemanId,
			Long creditorId,
			BigDecimal amount) {
		return new Delegation(
				incomingDebtId, outgoingDebtId, delegatedDebtId,
				debtorId, middlemanId, creditorId, amount);
	}

	@PrePersist
	void initializeCreatedAt() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public void reverse(Long actorId, String reason) {
		if (status != DelegationStatus.EXECUTED) {
			throw new IllegalStateException("delegation is already reversed");
		}
		long validatedActorId = LedgerAmounts.positiveId(actorId, "actorId");
		if (reason == null || reason.isBlank()) {
			throw new IllegalArgumentException("reversal reason must not be blank");
		}
		String normalized = reason.strip();
		if (normalized.length() > 500) {
			throw new IllegalArgumentException("reversal reason must not exceed 500 characters");
		}
		status = DelegationStatus.REVERSED;
		reversedById = validatedActorId;
		reversalReason = normalized;
		reversedAt = Instant.now();
	}

	public Long getId() { return id; }
	public Long getIncomingDebtId() { return incomingDebtId; }
	public Long getOutgoingDebtId() { return outgoingDebtId; }
	public Long getDelegatedDebtId() { return delegatedDebtId; }
	public Long getDebtorId() { return debtorId; }
	public Long getMiddlemanId() { return middlemanId; }
	public Long getCreditorId() { return creditorId; }
	public BigDecimal getAmount() { return amount; }
	public DelegationStatus getStatus() { return status; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getReversedAt() { return reversedAt; }
	public Long getReversedById() { return reversedById; }
	public String getReversalReason() { return reversalReason; }
	public long getVersion() { return version; }
}
