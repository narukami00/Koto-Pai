package com.kotopai.outing.engine.split;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kotopai.common.money.Money;
import com.kotopai.outing.engine.allocation.LargestRemainderAllocator;
import com.kotopai.outing.engine.model.ItemLine;
import com.kotopai.outing.engine.model.ParticipantRef;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EqualSplitStrategyTest {

    private static final ParticipantRef RAFSAN = ParticipantRef.user(1);
    private static final ParticipantRef TANVIR = ParticipantRef.user(2);
    private static final ParticipantRef NABILA = ParticipantRef.user(3);
    private static final ParticipantRef SAMIHA = ParticipantRef.user(4);

    private final EqualSplitStrategy strategy = new EqualSplitStrategy(new LargestRemainderAllocator());

    @Test
    @DisplayName("1000 between four people -> 250 each")
    void evenSplit() {
        ItemLine item = ItemLine.equal("Boat ride", Money.of(1000), RAFSAN, TANVIR, NABILA, SAMIHA);

        assertThat(strategy.split(item).values()).containsOnly(Money.of(250));
    }

    @Test
    @DisplayName("100 between three: the extra poisha goes to the first consumer (33.34 / 33.33 / 33.33)")
    void leftoverPoisha() {
        ItemLine item = ItemLine.equal("Fries", Money.of(100), RAFSAN, TANVIR, NABILA);

        assertThat(strategy.split(item)).containsExactly(
            Map.entry(RAFSAN, Money.of("33.34")),
            Map.entry(TANVIR, Money.of("33.33")),
            Map.entry(NABILA, Money.of("33.33")));
    }

    @Test
    @DisplayName("A single consumer gets the whole amount")
    void singleConsumer() {
        assertThat(strategy.split(ItemLine.equal("Tea", Money.of(30), TANVIR)))
            .containsExactly(Map.entry(TANVIR, Money.of(30)));
    }

    @Test
    @DisplayName("No consumers, duplicates or the wrong split type are rejected")
    void rejectsInvalidItems() {
        ItemLine noConsumers = new ItemLine("Tea", Money.of(30), SplitType.EQUAL, List.of());
        ItemLine duplicate = ItemLine.equal("Tea", Money.of(30), TANVIR, TANVIR);
        ItemLine personal = ItemLine.personal("Tea", Money.of(30), TANVIR);

        assertThatThrownBy(() -> strategy.split(noConsumers)).hasMessageContaining("no consumers");
        assertThatThrownBy(() -> strategy.split(duplicate)).hasMessageContaining("more than once");
        assertThatThrownBy(() -> strategy.split(personal)).hasMessageContaining("cannot split PERSONAL");
    }
}
