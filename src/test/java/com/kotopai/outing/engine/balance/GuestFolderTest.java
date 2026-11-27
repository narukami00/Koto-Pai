package com.kotopai.outing.engine.balance;

import static com.kotopai.outing.engine.EngineFixtures.NABILA;
import static com.kotopai.outing.engine.EngineFixtures.RAFSAN;
import static com.kotopai.outing.engine.EngineFixtures.RAHIM_GUEST_OF_RAFSAN;
import static com.kotopai.outing.engine.EngineFixtures.TANVIR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kotopai.common.money.Money;
import com.kotopai.outing.engine.EngineFixtures;
import com.kotopai.outing.engine.model.OutingSnapshot;
import com.kotopai.outing.engine.model.ParticipantRef;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GuestFolderTest {

    private final BalanceCalculator calculator = BalanceCalculator.withDefaultStrategies();
    private final GuestFolder folder = new GuestFolder();

    @Test
    @DisplayName("Without guests, folding changes nothing")
    void noGuests() {
        FoldedBalances result = folder.fold(calculator.calculate(EngineFixtures.burgerNight()));

        assertThat(result.guests()).isEmpty();
        assertThat(result.users()).containsExactly(
            new UserPosition(1, Money.of(700)), new UserPosition(2, Money.of(-300)), new UserPosition(3, Money.of(-400)));
    }

    @Test
    @DisplayName("A guest net is added to the owner net; the guest own net is kept")
    void guestFoldsIntoOwner() {
        // Rafsan brings Rahim. Rafsan eats 200, Rahim eats 300, Tanvir eats 100; Rafsan pays all 600.
        OutingSnapshot snapshot = OutingSnapshot.builder()
            .participants(RAFSAN, RAHIM_GUEST_OF_RAFSAN, TANVIR)
            .personalItem("Burger", "200", RAFSAN)
            .personalItem("Steak", "300", RAHIM_GUEST_OF_RAFSAN)
            .personalItem("Salad", "100", TANVIR)
            .paid(RAFSAN, "600").build();

        FoldedBalances result = folder.fold(calculator.calculate(snapshot));

        // Rafsan own net 600-200 = +400, minus the 300 Rahim consumed -> +100
        assertThat(result.users()).containsExactly(new UserPosition(1, Money.of(100)), new UserPosition(2, Money.of(-100)));
        assertThat(result.guests()).containsExactly(new GuestPosition(10, 1, Money.of(-300)));
        assertThat(result.usersTotal()).isEqualTo(Money.ZERO);
    }

    @Test
    @DisplayName("Several guests of one owner all fold into that owner")
    void severalGuests() {
        ParticipantRef g1 = ParticipantRef.guest(11, 1);
        ParticipantRef g2 = ParticipantRef.guest(12, 1);
        OutingSnapshot snapshot = OutingSnapshot.builder().participants(RAFSAN, g1, g2, NABILA)
            .personalItem("A", "100", g1).personalItem("B", "50", g2).personalItem("C", "70", NABILA)
            .paid(NABILA, "220").build();

        FoldedBalances result = folder.fold(calculator.calculate(snapshot));

        assertThat(result.users()).containsExactly(new UserPosition(1, Money.of(-150)), new UserPosition(3, Money.of(150)));
        assertThat(result.guests()).hasSize(2);
    }

    @Test
    @DisplayName("A guest whose owner is not on the outing is rejected")
    void ownerMissing() {
        List<ParticipantBalance> balances = List.of(
            ParticipantBalance.of(TANVIR, Money.ZERO, Money.ZERO),
            ParticipantBalance.of(RAHIM_GUEST_OF_RAFSAN, Money.of(10), Money.ZERO));

        assertThatThrownBy(() -> folder.fold(balances))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("guest #10").hasMessageContaining("user #1");
    }

    @Test
    @DisplayName("Null balances and null nets are refused")
    void nullSafe() {
        assertThatThrownBy(() -> folder.fold(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UserPosition(1, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GuestPosition(1, 1, null)).isInstanceOf(NullPointerException.class);
    }
}
