package com.kotopai.ledger;

import java.math.BigDecimal;

/** The before/after remaining amount for one existing debt. */
public record DebtAdjustment(long debtId, BigDecimal before, BigDecimal after) {
}
