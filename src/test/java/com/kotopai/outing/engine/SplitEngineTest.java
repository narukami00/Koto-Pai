package com.kotopai.outing.engine;

import static com.kotopai.outing.engine.EngineFixtures.NABILA;
import static com.kotopai.outing.engine.EngineFixtures.RAFSAN;
import static com.kotopai.outing.engine.EngineFixtures.RAHIM_GUEST_OF_RAFSAN;
import static com.kotopai.outing.engine.EngineFixtures.TANVIR;
import static com.kotopai.outing.engine.model.ShareLine.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kotopai.common.money.Money;
import com.kotopai.outing.engine.balance.BalanceCalculator;
import com.kotopai.outing.engine.balance.GuestFolder;
import com.kotopai.outing.engine.balance.GuestPosition;
import com.kotopai.outing.engine.balance.ParticipantBalance;
import com.kotopai.outing.engine.balance.UserPosition;
import com.kotopai.outing.engine.model.OutingSnapshot;
import com.kotopai.outing.engine.validation.EngineValidationException;
import com.kotopai.outing.engine.validation.SnapshotValidator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SplitEngineTest {

    private final SplitEngine engine = SplitEngine.withDefaults();

    @Test
    @DisplayName("Burger night end to end: Rafsan +700, Tanvir -300, Nabila -400")
    void burgerNight() {
        EngineResult result = engine.calculate(EngineFixtures.burgerNight());

        assertThat(result.billTotal()).isEqualTo(Money.of(1900));
        assertThat(result.balances()).extracting(ParticipantBalance::net)
            .containsExactly(Money.of(700), Money.of(-300), Money.of(-400));
        assertThat(result.folded().users()).containsExactly(
            new UserPosition(1, Money.of(700)), new UserPosition(2, Money.of(-300)), new UserPosition(3, Money.of(-400)));
        assertThat(result.folded().guests()).isEmpty();
    }

    /**
     * Golden scenario: burger night plus Rafsan guest Rahim, who shares the big pizza (30%) and has a
     * 150 shake of his own. Every number below was worked out by hand; if the engine changes them, review the change.
     *
     * Items: Burger 200 Rafsan, Pizza 500 Tanvir, Coffee 200 Nabila, Large pizza 1000 split Rafsan 40 / Tanvir 20 /
     * Nabila 10 / Rahim 30, Shake 150 Rahim. Bill 2050. Rafsan paid 1550, Tanvir 500.
     * Consumed: Rafsan 200+400=600, Tanvir 500+200=700, Nabila 200+100=300, Rahim 300+150=450 (total 2050).
     * Nets: Rafsan +950, Tanvir -200, Nabila -300, Rahim -450. Folded: Rafsan +500.
     */
    @Test
    @DisplayName("Golden scenario with a guest folds Rahim into Rafsan")
    void goldenScenarioWithGuest() {
        OutingSnapshot snapshot = OutingSnapshot.builder()
            .participants(RAFSAN, TANVIR, NABILA, RAHIM_GUEST_OF_RAFSAN)
            .personalItem("Burger", "200", RAFSAN)
            .personalItem("Pizza", "500", TANVIR)
            .personalItem("Coffee", "200", NABILA)
            .percentageItem("Large pizza", "1000",
                of(RAFSAN, "40"), of(TANVIR, "20"), of(NABILA, "10"), of(RAHIM_GUEST_OF_RAFSAN, "30"))
            .personalItem("Shake", "150", RAHIM_GUEST_OF_RAFSAN)
            .paid(RAFSAN, "1550")
            .paid(TANVIR, "500")
            .build();

        EngineResult result = engine.calculate(snapshot);

        assertThat(result.billTotal()).isEqualTo(Money.of(2050));
        assertThat(result.balances()).extracting(ParticipantBalance::consumed)
            .containsExactly(Money.of(600), Money.of(700), Money.of(300), Money.of(450));
        assertThat(result.folded().users()).containsExactly(
            new UserPosition(1, Money.of(500)), new UserPosition(2, Money.of(-200)), new UserPosition(3, Money.of(-300)));
        assertThat(result.folded().guests()).containsExactly(new GuestPosition(10, 1, Money.of(-450)));
        assertThat(result.folded().usersTotal()).isEqualTo(Money.ZERO);
    }

    @Test
    @DisplayName("An invalid outing is refused with every problem, before any calculation")
    void invalidSnapshotThrows() {
        OutingSnapshot broken = OutingSnapshot.builder().participants(RAFSAN)
            .personalItem("Burger", "200", TANVIR).paid(RAFSAN, "150").build();

        assertThatThrownBy(() -> engine.calculate(broken))
            .isInstanceOfSatisfying(EngineValidationException.class, e ->
                assertThat(e.getResult().codes()).containsExactly("UNKNOWN_CONSUMER", "PAID_NOT_EQUAL_BILL"));
    }

    @Test
    @DisplayName("Constructors refuse null collaborators")
    void nullSafe() {
        SnapshotValidator v = SnapshotValidator.withDefaultRules();
        BalanceCalculator c = BalanceCalculator.withDefaultStrategies();
        GuestFolder g = new GuestFolder();
        var folded = engine.calculate(EngineFixtures.burgerNight()).folded();

        assertThatThrownBy(() -> new SplitEngine(null, c, g)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SplitEngine(v, null, g)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SplitEngine(v, c, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new EngineResult(null, List.of(), folded)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new EngineResult(Money.ZERO, List.of(), null)).isInstanceOf(NullPointerException.class);
    }
}
