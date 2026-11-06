package com.kotopai.ledger.service;

import java.math.BigDecimal;

/** Positive balance means the counterparty owes the current user. */
public record LedgerBalance(long counterpartyId, CounterpartyType type, BigDecimal balance) {
}
