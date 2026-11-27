package com.kotopai.outing.engine.balance;

import static com.kotopai.outing.engine.EngineFixtures.NABILA;
import static com.kotopai.outing.engine.EngineFixtures.RAFSAN;
import static com.kotopai.outing.engine.EngineFixtures.TANVIR;
import static com.kotopai.outing.engine.model.ShareLine.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kotopai.common.money.Money;
import com.kotopai.outing.engine.EngineFixtures;
import com.kotopai.outing.engine.model.OutingSnapshot;
import com.kotopai.outing.engine.model.ParticipantRef;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BalanceCalculatorTest {

    private final BalanceCalculator calculator = BalanceCalculator.withDefaultStrategies();

    private static ParticipantBalance of(List<ParticipantBalance> balances, ParticipantRef who) {
        return balances.stream().filter(b -> b.participant().equals(who)).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("Burger night: Rafsan +700, Tanvir -300, Nabila -400 (the worked example in ARCHITECTURE.md)")
    void burgerNight() {
        List<ParticipantBalance> balances = calculator.calculate(EngineFixtures.burgerNight());

        assertThat(balances).extracting(ParticipantBalance::participant).containsExactly(RAFSAN, TANVIR, NABILA);
        assertThat(of(balances, RAFSAN)).isEqualTo(new ParticipantBalance(RAFSAN, Money.of(700), Money.of(1400), Money.of(700)));
        assertThat(of(balances, TANVIR)).isEqualTo(new ParticipantBalance(TANVIR, Money.of(800), Money.of(500), Money.of(-300)));
        assertThat(of(balances, NABILA)).isEqualTo(new ParticipantBalance(NABILA, Money.of(400), Money.ZERO, Money.of(-400)));
    }

    @Test
    @DisplayName("Nets always add up to zero when paid equals the bill")
    void netsSumToZero() {
        Money total = Money.sum(calculator.calculate(EngineFixtures.burgerNight()).stream().map(ParticipantBalance::net).toList());

        assertThat(total).isEqualTo(Money.ZERO);
    }

    @Test
    @DisplayName("Awkward amounts: a 100.00 item split three ways loses no poisha")
    void roundingKeepsTheBooksBalanced() {
        OutingSnapshot snapshot = OutingSnapshot.builder().participants(RAFSAN, TANVIR, NABILA)
            .equalItem("Tea", "100", RAFSAN, TANVIR, NABILA)
            .paid(RAFSAN, "100").build();

        List<ParticipantBalance> balances = calculator.calculate(snapshot);

        assertThat(Money.sum(balances.stream().map(ParticipantBalance::consumed).toList())).isEqualTo(Money.of(100));
        assertThat(Money.sum(balances.stream().map(ParticipantBalance::net).toList())).isEqualTo(Money.ZERO);
    }

    @Test
    @DisplayName("People with no items and no payments are listed with zeros")
    void bystanderHasZeroBalance() {
        OutingSnapshot snapshot = OutingSnapshot.builder().participants(RAFSAN, TANVIR)
            .personalItem("Burger", "200", RAFSAN).paid(RAFSAN, "200").build();

        assertThat(of(calculator.calculate(snapshot), TANVIR).net()).isEqualTo(Money.ZERO);
    }

    @Test
    @DisplayName("An unpaid bill leaves nets that do not sum to zero (validation catches this earlier)")
    void unpaidBillIsVisibleInNets() {
        OutingSnapshot snapshot = OutingSnapshot.builder().participants(RAFSAN)
            .personalItem("Burger", "200", RAFSAN).build();

        assertThat(of(calculator.calculate(snapshot), RAFSAN).net()).isEqualTo(Money.of(-200));
    }

    @Test
    @DisplayName("A consumer who is not on the outing is rejected, not silently added")
    void rejectsUnknownConsumer() {
        OutingSnapshot snapshot = OutingSnapshot.builder().participants(RAFSAN)
            .personalItem("Burger", "200", TANVIR).build();

        assertThatThrownBy(() -> calculator.calculate(snapshot))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("user #2").hasMessageContaining("Burger");
    }

    @Test
    @DisplayName("A payer who is not on the outing is rejected")
    void rejectsUnknownPayer() {
        OutingSnapshot snapshot = OutingSnapshot.builder().participants(RAFSAN)
            .personalItem("Burger", "200", RAFSAN).paid(NABILA, "200").build();

        assertThatThrownBy(() -> calculator.calculate(snapshot))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("user #3").hasMessageContaining("paid");
    }

    @Test
    @DisplayName("Null arguments are refused")
    void nullSafe() {
        assertThatThrownBy(() -> calculator.calculate(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new BalanceCalculator(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ParticipantBalance(null, Money.ZERO, Money.ZERO, Money.ZERO))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ParticipantBalance(RAFSAN, null, Money.ZERO, Money.ZERO))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ParticipantBalance(RAFSAN, Money.ZERO, null, Money.ZERO))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ParticipantBalance(RAFSAN, Money.ZERO, Money.ZERO, null))
            .isInstanceOf(NullPointerException.class);
    }
}
