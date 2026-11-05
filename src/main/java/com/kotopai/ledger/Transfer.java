package com.kotopai.ledger;

import java.math.BigDecimal;

/** A payment from a debtor to a creditor. */
public record Transfer<T>(T debtor, T creditor, BigDecimal amount) {
}
