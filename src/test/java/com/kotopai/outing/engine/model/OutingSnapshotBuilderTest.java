package com.kotopai.outing.engine.model;

import static com.kotopai.outing.engine.EngineFixtures.NABILA;
import static com.kotopai.outing.engine.EngineFixtures.RAFSAN;
import static com.kotopai.outing.engine.EngineFixtures.RAHIM_GUEST_OF_RAFSAN;
import static com.kotopai.outing.engine.EngineFixtures.TANVIR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kotopai.common.money.Money;
import com.kotopai.outing.engine.EngineFixtures;
import com.kotopai.outing.engine.split.SplitType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OutingSnapshotBuilderTest {

    @Test
    @DisplayName("Builds the burger-night outing fluently: 3 people, 4 items, 2 payments, bill 1900")
    void buildsBurgerNight() {
        OutingSnapshot snapshot = EngineFixtures.burgerNight();

        assertThat(snapshot.participants()).containsExactly(RAFSAN, TANVIR, NABILA);
        assertThat(snapshot.items()).extracting(ItemLine::name)
            .containsExactly("Burger", "Pizza", "Cold coffee", "Large pizza");
        assertThat(snapshot.items()).extracting(ItemLine::splitType)
            .containsExactly(SplitType.PERSONAL, SplitType.PERSONAL, SplitType.PERSONAL, SplitType.PERCENTAGE);
        assertThat(snapshot.contributions()).hasSize(2);
        assertThat(snapshot.billTotal()).isEqualTo(Money.of(1900));
        assertThat(snapshot.paidTotal()).isEqualTo(Money.of(1900));
    }

    @Test
    @DisplayName("Guests are participants like anyone else; equal items list every consumer")
    void guestsAndEqualItems() {
        OutingSnapshot snapshot = OutingSnapshot.builder()
            .participants(RAFSAN, RAHIM_GUEST_OF_RAFSAN)
            .equalItem("Nachos", "300", RAFSAN, RAHIM_GUEST_OF_RAFSAN)
            .paid(RAFSAN, "300")
            .build();

        assertThat(snapshot.participants()).contains(RAHIM_GUEST_OF_RAFSAN);
        assertThat(snapshot.items().get(0).shares()).extracting(ShareLine::participant)
            .containsExactly(RAFSAN, RAHIM_GUEST_OF_RAFSAN);
    }

    @Test
    @DisplayName("The built snapshot is immutable")
    void immutable() {
        OutingSnapshot snapshot = EngineFixtures.burgerNight();

        assertThatThrownBy(() -> snapshot.participants().add(ParticipantRef.user(99)))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> snapshot.items().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> snapshot.contributions().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Using the builder after build() does not change an already-built snapshot")
    void builderReuseDoesNotLeak() {
        OutingSnapshot.Builder builder = OutingSnapshot.builder().participants(RAFSAN).paid(RAFSAN, "100");
        OutingSnapshot first = builder.build();

        builder.participant(TANVIR).paid(TANVIR, "50");
        OutingSnapshot second = builder.build();

        assertThat(first.participants()).containsExactly(RAFSAN);
        assertThat(first.paidTotal()).isEqualTo(Money.of(100));
        assertThat(second.participants()).containsExactly(RAFSAN, TANVIR);
        assertThat(second.paidTotal()).isEqualTo(Money.of(150));
    }

    @Test
    @DisplayName("Structural mistakes are rejected: duplicate participant, null, negative payment")
    void rejectsStructuralMistakes() {
        assertThatThrownBy(() -> OutingSnapshot.builder().participants(RAFSAN, RAFSAN))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("added twice");
        assertThatThrownBy(() -> new OutingSnapshot(List.of(RAFSAN, RAFSAN), List.of(), List.of()))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OutingSnapshot.builder().participant(null))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> OutingSnapshot.builder().paid(RAFSAN, "-5"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Business rules are left to the validator: an unbalanced outing can still be built")
    void businessRulesAreNotTheBuildersJob() {
        OutingSnapshot unbalanced = OutingSnapshot.builder()
            .participants(RAFSAN)
            .personalItem("Burger", "200", RAFSAN)
            .paid(RAFSAN, "150")
            .build();

        assertThat(unbalanced.billTotal()).isNotEqualTo(unbalanced.paidTotal());
    }
}
