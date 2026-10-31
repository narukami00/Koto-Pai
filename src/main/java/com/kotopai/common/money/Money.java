package com.kotopai.common.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * An amount of money in BDT.
 *
 * <p>Value Object: immutable, always scale 2, compared by value. All money in
 * the application goes through this type so rounding and scale rules live in
 * one place and {@code double} never touches an amount.
 */
public final class Money implements Comparable<Money> {

    public static final String CURRENCY = "BDT";
    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;
    public static final Money ZERO = new Money(BigDecimal.ZERO);

    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        this.amount = amount.setScale(SCALE, ROUNDING);
    }

    /** Exact amount; rejects more than 2 decimal places instead of silently rounding user input. */
    public static Money of(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        if (amount.stripTrailingZeros().scale() > SCALE) {
            throw new IllegalArgumentException("Money has at most 2 decimal places: " + amount);
        }
        return new Money(amount);
    }

    public static Money of(String amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        return of(new BigDecimal(amount.trim()));
    }

    public static Money of(long wholeTaka) {
        return new Money(BigDecimal.valueOf(wholeTaka));
    }

    /** From poisha (1 taka = 100 poisha). */
    public static Money ofMinorUnits(long poisha) {
        return new Money(BigDecimal.valueOf(poisha, SCALE));
    }

    /** Amount in poisha; exact because the scale is always 2. */
    public long toMinorUnits() {
        return amount.movePointRight(SCALE).longValueExact();
    }

    public BigDecimal amount() {
        return amount;
    }

    public Money plus(Money other) {
        return new Money(amount.add(require(other).amount));
    }

    public Money minus(Money other) {
        return new Money(amount.subtract(require(other).amount));
    }

    public Money negate() {
        return new Money(amount.negate());
    }

    /** Multiplies and rounds half-even to 2 decimals, e.g. a percentage share. */
    public Money times(BigDecimal factor) {
        Objects.requireNonNull(factor, "factor must not be null");
        return new Money(amount.multiply(factor));
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public Money abs() {
        return isNegative() ? negate() : this;
    }

    public static Money sum(Iterable<Money> amounts) {
        Money total = ZERO;
        for (Money m : amounts) {
            total = total.plus(m);
        }
        return total;
    }

    @Override
    public int compareTo(Money other) {
        return amount.compareTo(require(other).amount);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Money other && amount.equals(other.amount));
    }

    @Override
    public int hashCode() {
        return amount.hashCode();
    }

    /** Plain amount with 2 decimals, e.g. {@code 1400.00}. */
    @Override
    public String toString() {
        return amount.toPlainString();
    }

    private static Money require(Money other) {
        return Objects.requireNonNull(other, "other amount must not be null");
    }
}
