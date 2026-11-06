package com.kotopai.outing.engine.allocation;

import com.kotopai.common.money.Money;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Splits an amount into shares proportional to weights so the shares always
 * add up to exactly the original amount.
 *
 * <p>Largest remainder method, working in poisha: every share first gets the
 * whole-poisha part of its exact proportion; the poisha left over are handed
 * out one each to the shares with the largest fractional remainders (ties go
 * to the earlier position, so the result is deterministic).
 *
 * <p>Why not round each share? 100.00 split three ways rounds to
 * 33.33 + 33.33 + 33.33 = 99.99 and a poisha disappears from the ledger.
 */
public final class LargestRemainderAllocator {

    /**
     * @param total   non-negative amount to split
     * @param weights one non-negative weight per share (e.g. percentages); at least one must be positive
     * @return shares in the same order as {@code weights}, summing exactly to {@code total}
     */
    public List<Money> allocate(Money total, List<BigDecimal> weights) {
        Objects.requireNonNull(total, "total must not be null");
        Objects.requireNonNull(weights, "weights must not be null");
        if (total.isNegative()) {
            throw new IllegalArgumentException("total must not be negative: " + total);
        }
        if (weights.isEmpty()) {
            throw new IllegalArgumentException("at least one weight is required");
        }
        BigDecimal weightSum = BigDecimal.ZERO;
        for (BigDecimal w : weights) {
            Objects.requireNonNull(w, "weights must not contain null");
            if (w.signum() < 0) {
                throw new IllegalArgumentException("weights must not be negative: " + w);
            }
            weightSum = weightSum.add(w);
        }
        if (weightSum.signum() == 0) {
            throw new IllegalArgumentException("at least one weight must be positive");
        }

        BigDecimal totalPoisha = BigDecimal.valueOf(total.toMinorUnits());
        int n = weights.size();
        long[] shares = new long[n];
        // remainder numerators: all share the same denominator (weightSum), so they compare directly
        BigDecimal[] remainders = new BigDecimal[n];
        long allocated = 0;
        for (int i = 0; i < n; i++) {
            BigDecimal numerator = totalPoisha.multiply(weights.get(i));
            BigDecimal whole = numerator.divideToIntegralValue(weightSum);
            shares[i] = whole.longValueExact();
            remainders[i] = numerator.subtract(whole.multiply(weightSum));
            allocated += shares[i];
        }

        long leftover = total.toMinorUnits() - allocated;
        List<Integer> byRemainder = IntStream.range(0, n).boxed()
            .sorted(Comparator.comparing((Integer i) -> remainders[i]).reversed()
                .thenComparing(Comparator.naturalOrder()))
            .toList();
        for (int k = 0; k < leftover; k++) {
            shares[byRemainder.get(k)]++;
        }

        List<Money> result = new ArrayList<>(n);
        for (long poisha : shares) {
            result.add(Money.ofMinorUnits(poisha));
        }
        return Collections.unmodifiableList(result);
    }

    /** Equal split into {@code parts} shares (e.g. 100.00 / 3 = 33.34, 33.33, 33.33). */
    public List<Money> allocateEvenly(Money total, int parts) {
        if (parts <= 0) {
            throw new IllegalArgumentException("parts must be positive: " + parts);
        }
        return allocate(total, Collections.nCopies(parts, BigDecimal.ONE));
    }
}
