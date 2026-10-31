package com.kotopai.ledger;

import java.math.BigDecimal;

/**
 * A participant's net position after an outing is balanced.
 * A positive amount is money to receive; a negative amount is money owed.
 */
public record NetPosition(String participant, BigDecimal amount) {
}
