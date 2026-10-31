package com.kotopai.outing.engine.split;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kotopai.common.money.Money;
import com.kotopai.outing.engine.allocation.LargestRemainderAllocator;
import com.kotopai.outing.engine.model.ItemLine;
import com.kotopai.outing.engine.model.ParticipantRef;
import com.kotopai.outing.engine.model.ShareLine;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SplitStrategyFactoryTest {

    private static final ParticipantRef RAFSAN = ParticipantRef.user(1);
    private static final ParticipantRef TANVIR = ParticipantRef.user(2);

    private final SplitStrategyFactory factory = SplitStrategyFactory.withDefaultStrategies();

    @ParameterizedTest(name = "{0}")
    @EnumSource(SplitType.class)
    @DisplayName("Every split type resolves to a strategy for that type")
    void everyTypeResolves(SplitType type) {
        assertThat(factory.forType(type).type()).isEqualTo(type);
    }

    @Test
    @DisplayName("Default mapping: PERSONAL -> sole consumer, EQUAL -> equal, PERCENTAGE -> percentage")
    void defaultMapping() {
        assertThat(factory.forType(SplitType.PERSONAL)).isInstanceOf(SoleConsumerSplitStrategy.class);
        assertThat(factory.forType(SplitType.EQUAL)).isInstanceOf(EqualSplitStrategy.class);
        assertThat(factory.forType(SplitType.PERCENTAGE)).isInstanceOf(PercentageSplitStrategy.class);
    }

    @Test
    @DisplayName("Callers can split any item without knowing the concrete strategy")
    void splitsThroughTheInterface() {
        List<ItemLine> items = List.of(
            ItemLine.personal("Burger", Money.of(200), RAFSAN),
            ItemLine.equal("Fries", Money.of(100), RAFSAN, TANVIR),
            ItemLine.percentage("Pizza", Money.of(1000), ShareLine.of(RAFSAN, "70"), ShareLine.of(TANVIR, "30")));

        Money total = Money.ZERO;
        for (ItemLine item : items) {
            total = total.plus(Money.sum(factory.forType(item.splitType()).split(item).values()));
        }

        assertThat(total).isEqualTo(Money.of(1300));
    }

    @Test
    @DisplayName("A split type without a strategy fails at construction, not at runtime")
    void missingStrategyFailsFast() {
        assertThatThrownBy(() -> new SplitStrategyFactory(List.of(new SoleConsumerSplitStrategy())))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("no strategy registered for")
            .hasMessageContaining("EQUAL")
            .hasMessageContaining("PERCENTAGE");
    }

    @Test
    @DisplayName("Two strategies for the same type are rejected")
    void duplicateStrategyRejected() {
        LargestRemainderAllocator allocator = new LargestRemainderAllocator();

        assertThatThrownBy(() -> new SplitStrategyFactory(List.of(
            new SoleConsumerSplitStrategy(),
            new EqualSplitStrategy(allocator),
            new EqualSplitStrategy(allocator),
            new PercentageSplitStrategy(allocator))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("two strategies registered for EQUAL");
    }

    @Test
    @DisplayName("Null input is rejected")
    void rejectsNull() {
        assertThatThrownBy(() -> factory.forType(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SplitStrategyFactory(null)).isInstanceOf(NullPointerException.class);
    }
}
