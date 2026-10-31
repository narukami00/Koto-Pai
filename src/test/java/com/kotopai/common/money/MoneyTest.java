package com.kotopai.common.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MoneyTest {

    @Test
    @DisplayName("Amounts are normalised to 2 decimals, so 2.0 equals 2.00 (raw BigDecimal does not)")
    void normalisesScale() {
        assertThat(new BigDecimal("2.0")).isNotEqualTo(new BigDecimal("2.00"));

        assertThat(Money.of("2.0")).isEqualTo(Money.of("2.00"));
        assertThat(Money.of("2.0")).hasSameHashCodeAs(Money.of("2.00"));
        assertThat(Money.of(2).toString()).isEqualTo("2.00");
    }

    @Test
    @DisplayName("Input with more than 2 decimal places is rejected, not silently rounded")
    void rejectsTooManyDecimals() {
        assertThatThrownBy(() -> Money.of("10.005"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(Money.of("10.500")).isEqualTo(Money.of("10.50"));
    }

    @Test
    @DisplayName("Null input is rejected")
    void rejectsNull() {
        assertThatThrownBy(() -> Money.of((BigDecimal) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Money.of((String) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Money.of(1).plus(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("0.10 + 0.20 is exactly 0.30 (with double it is 0.30000000000000004)")
    void exactDecimalArithmetic() {
        assertThat(0.1 + 0.2).isNotEqualTo(0.3);

        assertThat(Money.of("0.10").plus(Money.of("0.20"))).isEqualTo(Money.of("0.30"));
    }

    @Test
    @DisplayName("plus, minus, negate and abs")
    void arithmetic() {
        Money paid = Money.of(1400);
        Money consumed = Money.of(700);

        assertThat(paid.minus(consumed)).isEqualTo(Money.of(700));
        assertThat(consumed.minus(paid)).isEqualTo(Money.of(-700));
        assertThat(paid.negate()).isEqualTo(Money.of(-1400));
        assertThat(Money.of(-300).abs()).isEqualTo(Money.of(300));
    }

    @ParameterizedTest(name = "{0} x {1} = {2}")
    @CsvSource({
        "1000.00, 0.5,    500.00",
        "1000.00, 0.3,    300.00",
        "100.00,  0.3333, 33.33",
        "0.05,    0.5,    0.02",   // 0.025 -> half-even rounds down to the even digit
        "0.15,    0.5,    0.08"    // 0.075 -> half-even rounds up to the even digit
    })
    @DisplayName("times() rounds half-even to 2 decimals")
    void timesRoundsHalfEven(String amount, String factor, String expected) {
        assertThat(Money.of(amount).times(new BigDecimal(factor))).isEqualTo(Money.of(expected));
    }

    @ParameterizedTest(name = "{0} poisha <-> {1}")
    @CsvSource({"0, 0.00", "1, 0.01", "150000, 1500.00", "-30000, -300.00"})
    @DisplayName("Minor units (poisha) round-trip exactly")
    void minorUnitsRoundTrip(long poisha, String amount) {
        assertThat(Money.ofMinorUnits(poisha)).isEqualTo(Money.of(amount));
        assertThat(Money.of(amount).toMinorUnits()).isEqualTo(poisha);
    }

    @Test
    @DisplayName("Sign checks and comparison")
    void signAndComparison() {
        assertThat(Money.ZERO.isZero()).isTrue();
        assertThat(Money.of(5).isPositive()).isTrue();
        assertThat(Money.of(-5).isNegative()).isTrue();
        assertThat(Money.of("9.99")).isLessThan(Money.of(10));
    }

    @Test
    @DisplayName("sum() adds a list; an empty list is zero")
    void sums() {
        assertThat(Money.sum(List.of(Money.of(200), Money.of(500), Money.of(200), Money.of(1000))))
            .isEqualTo(Money.of(1900));
        assertThat(Money.sum(List.of())).isEqualTo(Money.ZERO);
    }

    @Test
    @DisplayName("Operations never change the original (immutability)")
    void immutable() {
        Money original = Money.of(100);
        original.plus(Money.of(50));
        original.negate();

        assertThat(original).isEqualTo(Money.of(100));
    }
}
