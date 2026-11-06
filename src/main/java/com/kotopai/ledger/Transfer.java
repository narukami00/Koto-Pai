package com.kotopai.ledger;

import java.math.BigDecimal;

/** A payment from a debtor to a creditor. */
public record Transfer(String debtor, String creditor, BigDecimal amount) {
}
