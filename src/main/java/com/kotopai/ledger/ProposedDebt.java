package com.kotopai.ledger;

import java.math.BigDecimal;

/** A new direct debt created by delegation. */
public record ProposedDebt(long debtorId, long creditorId, BigDecimal amount) {
}
