package com.kotopai.outing.engine.split;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kotopai.common.money.Money;
import com.kotopai.outing.engine.allocation.LargestRemainderAllocator;
import com.kotopai.outing.engine.model.ItemLine;
import com.kotopai.outing.engine.model.ParticipantRef;
import com.kotopai.outing.engine.model.ShareLine;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PercentageSplitStrategyTest {

    private static final ParticipantRef RAFSAN = ParticipantRef.user(1);
    private static final ParticipantRef TANVIR = ParticipantRef.user(2);
    private static final ParticipantRef NABILA = ParticipantRef.user(3);
    private static final ParticipantRef RAHIM_GUEST = ParticipantRef.guest(10, 1);

    private final PercentageSplitStrategy strategy = new PercentageSplitStrategy(new LargestRemainderAllocator());

    @Test
    @DisplayName("Burger-night large pizza: 1000 at 50/30/20 -> 500/300/200")
    void sharedPizza() {
        ItemLine pizza = ItemLine.percentage("Large pizza", Money.of(1000),
            ShareLine.of(RAFSAN, "50"), ShareLine.of(TANVIR, "30"), ShareLine.of(NABILA, "20"));

        assertThat(strategy.split(pizza)).containsExactly(
            Map.entry(RAFSAN, Money.of(500)),
            Map.entry(TANVIR, Money.of(300)),
            Map.entry(NABILA, Money.of(200)));
    }

    @Test
    @DisplayName("Percentages written with decimals (50.00 / 30.00 / 20.00) are accepted")
    void decimalPercentagesAccepted() {
        ItemLine pizza = ItemLine.percentage("Large pizza", Money.of(1000),
            ShareLine.of(RAFSAN, "50.00"), ShareLine.of(TANVIR, "30.00"), ShareLine.of(NABILA, "20.00"));

        assertThat(strategy.split(pizza).values())
            .containsExactly(Money.of(500), Money.of(300), Money.of(200));
    }

    @Test
    @DisplayName("Fractional percentages 33.33 / 33.33 / 33.34 split 100 exactly")
    void fractionalPercentages() {
        ItemLine item = ItemLine.percentage("Fries", Money.of(100),
            ShareLine.of(RAFSAN, "33.33"), ShareLine.of(TANVIR, "33.33"), ShareLine.of(NABILA, "33.34"));

        Map<ParticipantRef, Money> shares = strategy.split(item);

        assertThat(shares.values()).containsExactly(Money.of("33.33"), Money.of("33.33"), Money.of("33.34"));
        assertThat(Money.sum(shares.values())).isEqualTo(Money.of(100));
    }

    @Test
    @DisplayName("A consumer at 0% gets nothing; guests are split like anyone else")
    void zeroPercentAndGuest() {
        ItemLine item = ItemLine.percentage("Nachos", Money.of(300),
            ShareLine.of(RAFSAN, "0"), ShareLine.of(RAHIM_GUEST, "100"));

        assertThat(strategy.split(item)).containsExactly(
            Map.entry(RAFSAN, Money.ZERO),
            Map.entry(RAHIM_GUEST, Money.of(300)));
    }

    @Test
    @DisplayName("Percentages that don't add up to 100 are rejected")
    void mustAddUpTo100() {
        ItemLine item = ItemLine.percentage("Pizza", Money.of(1000),
            ShareLine.of(RAFSAN, "50"), ShareLine.of(TANVIR, "49"));

        assertThatThrownBy(() -> strategy.split(item))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("add up to 100");
    }

    @Test
    @DisplayName("Missing or negative percentages are rejected")
    void missingOrNegative() {
        ItemLine missing = new ItemLine("Pizza", Money.of(100), SplitType.PERCENTAGE,
            java.util.List.of(ShareLine.of(RAFSAN, "100"), ShareLine.of(TANVIR)));
        ItemLine negative = ItemLine.percentage("Pizza", Money.of(100),
            ShareLine.of(RAFSAN, "120"), ShareLine.of(TANVIR, "-20"));

        assertThatThrownBy(() -> strategy.split(missing)).hasMessageContaining("no percentage");
        assertThatThrownBy(() -> strategy.split(negative)).hasMessageContaining("negative");
    }

    @Test
    @DisplayName("Common checks: wrong split type, duplicate consumer")
    void commonChecks() {
        ItemLine equalItem = ItemLine.equal("Pizza", Money.of(100), RAFSAN, TANVIR);
        ItemLine duplicate = ItemLine.percentage("Pizza", Money.of(100),
            ShareLine.of(RAFSAN, "50"), ShareLine.of(RAFSAN, "50"));

        assertThatThrownBy(() -> strategy.split(equalItem)).hasMessageContaining("cannot split EQUAL");
        assertThatThrownBy(() -> strategy.split(duplicate)).hasMessageContaining("more than once");
    }

    @Test
    @DisplayName("Result is unmodifiable")
    void unmodifiable() {
        ItemLine item = ItemLine.percentage("Pizza", Money.of(100), ShareLine.of(RAFSAN, "100"));

        assertThatThrownBy(() -> strategy.split(item).put(TANVIR, Money.ZERO))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
