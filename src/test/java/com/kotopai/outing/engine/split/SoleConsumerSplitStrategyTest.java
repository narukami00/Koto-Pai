package com.kotopai.outing.engine.split;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kotopai.common.money.Money;
import com.kotopai.outing.engine.model.ItemLine;
import com.kotopai.outing.engine.model.ParticipantRef;
import com.kotopai.outing.engine.model.ShareLine;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SoleConsumerSplitStrategyTest {

    private static final ParticipantRef RAFSAN = ParticipantRef.user(1);
    private static final ParticipantRef TANVIR = ParticipantRef.user(2);

    private final SoleConsumerSplitStrategy strategy = new SoleConsumerSplitStrategy();

    @Test
    @DisplayName("Personal item: the consumer gets the whole amount (burger 200 -> Rafsan 200)")
    void wholeAmountToConsumer() {
        assertThat(strategy.split(ItemLine.personal("Burger", Money.of(200), RAFSAN)))
            .containsExactly(Map.entry(RAFSAN, Money.of(200)));
    }

    @Test
    @DisplayName("A personal item with two consumers is rejected")
    void exactlyOneConsumer() {
        ItemLine twoConsumers = new ItemLine("Burger", Money.of(200), SplitType.PERSONAL,
            List.of(ShareLine.of(RAFSAN), ShareLine.of(TANVIR)));

        assertThatThrownBy(() -> strategy.split(twoConsumers)).hasMessageContaining("exactly one consumer");
    }

    @Test
    @DisplayName("Wrong split type is rejected")
    void wrongType() {
        assertThatThrownBy(() -> strategy.split(ItemLine.equal("Burger", Money.of(200), RAFSAN)))
            .hasMessageContaining("cannot split EQUAL");
    }
}
