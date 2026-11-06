package com.kotopai.outing.engine.allocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kotopai.common.money.Money;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;

class LargestRemainderAllocatorTest {

    private final LargestRemainderAllocator allocator = new LargestRemainderAllocator();

    private static List<BigDecimal> weights(String... values) {
        return Arrays.stream(values).map(BigDecimal::new).toList();
    }

    private static List<Money> money(String... values) {
        return Arrays.stream(values).map(Money::of).toList();
    }

    @Test
    @DisplayName("Burger-night shared pizza: 1000 at 50/30/20 -> 500/300/200")
    void sharedPizza() {
        assertThat(allocator.allocate(Money.of(1000), weights("50", "30", "20")))
            .containsExactlyElementsOf(money("500", "300", "200"));
    }

    @Test
    @DisplayName("100 split three ways keeps every poisha: 33.34 + 33.33 + 33.33 (naive rounding gives 99.99)")
    void noPoishaLost() {
        List<Money> shares = allocator.allocateEvenly(Money.of(100), 3);

        assertThat(shares).containsExactlyElementsOf(money("33.34", "33.33", "33.33"));
        assertThat(Money.sum(shares)).isEqualTo(Money.of(100));
    }

    @Test
    @DisplayName("Leftover poisha go to the largest remainders, not just the first shares")
    void leftoverGoesToLargestRemainder() {
        // 10.00 at 1/2/3 (sum 6): exact 166.67 / 333.33 / 500.00 poisha
        assertThat(allocator.allocate(Money.of(10), weights("1", "2", "3")))
            .containsExactlyElementsOf(money("1.67", "3.33", "5.00"));
    }

    @Test
    @DisplayName("Ties are broken by position, so the result is deterministic")
    void tiesByPosition() {
        assertThat(allocator.allocateEvenly(Money.of("0.02"), 3))
            .containsExactlyElementsOf(money("0.01", "0.01", "0.00"));
    }

    @Test
    @DisplayName("10.00 into 7 parts: six shares of 1.43 and one of 1.42")
    void sevenParts() {
        List<Money> shares = allocator.allocateEvenly(Money.of(10), 7);

        assertThat(Money.sum(shares)).isEqualTo(Money.of(10));
        assertThat(shares).filteredOn(m -> m.equals(Money.of("1.43"))).hasSize(6);
        assertThat(shares).filteredOn(m -> m.equals(Money.of("1.42"))).hasSize(1);
    }

    @Test
    @DisplayName("Fractional percentages such as 33.33/33.33/33.34 work")
    void fractionalWeights() {
        assertThat(allocator.allocate(Money.of(300), weights("33.33", "33.33", "33.34")))
            .containsExactlyElementsOf(money("99.99", "99.99", "100.02"));
    }

    @Test
    @DisplayName("Edge cases: zero total, single share, zero weight")
    void edgeCases() {
        assertThat(allocator.allocateEvenly(Money.ZERO, 3)).containsOnly(Money.ZERO);
        assertThat(allocator.allocate(Money.of(250), weights("7"))).containsExactly(Money.of(250));
        assertThat(allocator.allocate(Money.of(100), weights("1", "0", "1")))
            .containsExactlyElementsOf(money("50", "0", "50"));
    }

    @Test
    @DisplayName("Invalid input is rejected")
    void rejectsInvalidInput() {
        assertThatThrownBy(() -> allocator.allocate(Money.of(10), List.of()))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> allocator.allocate(Money.of(10), weights("1", "-1")))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> allocator.allocate(Money.of(10), weights("0", "0")))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> allocator.allocate(Money.of(-10), weights("1")))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> allocator.allocateEvenly(Money.of(10), 0))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> allocator.allocate(null, weights("1")))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Returned list is unmodifiable")
    void unmodifiable() {
        List<Money> shares = allocator.allocateEvenly(Money.of(10), 2);

        assertThatThrownBy(() -> shares.add(Money.ZERO)).isInstanceOf(UnsupportedOperationException.class);
    }

    @RepeatedTest(200)
    @DisplayName("Random inputs: shares always sum to the total and each is within 1 poisha of its exact share")
    void randomInputsAlwaysBalance(RepetitionInfo info) {
        Random random = new Random(info.getCurrentRepetition()); // seeded, so failures are reproducible
        Money total = Money.ofMinorUnits(random.nextInt(10_000_000));
        int n = 1 + random.nextInt(12);
        List<BigDecimal> weights = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            weights.add(BigDecimal.valueOf(random.nextInt(10_000), 2)); // 0.00 .. 99.99
        }
        weights.set(0, weights.get(0).add(BigDecimal.ONE)); // ensure at least one positive weight

        List<Money> shares = allocator.allocate(total, weights);

        assertThat(shares).hasSize(n);
        assertThat(Money.sum(shares)).isEqualTo(total);
        BigDecimal weightSum = weights.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        for (int i = 0; i < n; i++) {
            BigDecimal exactPoisha = BigDecimal.valueOf(total.toMinorUnits())
                .multiply(weights.get(i)).divide(weightSum, MathContext.DECIMAL128);
            BigDecimal diff = BigDecimal.valueOf(shares.get(i).toMinorUnits()).subtract(exactPoisha).abs();
            assertThat(diff).isLessThan(BigDecimal.ONE);
        }
    }
}
