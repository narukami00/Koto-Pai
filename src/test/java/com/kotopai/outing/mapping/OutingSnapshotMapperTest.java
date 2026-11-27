package com.kotopai.outing.mapping;

import static com.kotopai.outing.engine.model.ShareLine.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kotopai.common.money.Money;
import com.kotopai.outing.domain.Guest;
import com.kotopai.outing.domain.Item;
import com.kotopai.outing.domain.Outing;
import com.kotopai.outing.domain.OutingCategory;
import com.kotopai.outing.domain.OutingParticipant;
import com.kotopai.outing.engine.EngineResult;
import com.kotopai.outing.engine.SplitEngine;
import com.kotopai.outing.engine.balance.GuestPosition;
import com.kotopai.outing.engine.balance.UserPosition;
import com.kotopai.outing.engine.model.OutingSnapshot;
import com.kotopai.outing.engine.model.ParticipantRef;
import com.kotopai.outing.engine.split.SplitType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Plain unit test: entities are built by hand (ids set by reflection), no database involved. */
class OutingSnapshotMapperTest {

    private final OutingSnapshotMapper mapper = new OutingSnapshotMapper();

    private static Guest savedGuest(long id, long ownerId, String name) {
        Guest guest = new Guest(ownerId, name);
        ReflectionTestUtils.setField(guest, "id", id);
        return guest;
    }

    /** Burger night plus Rafsan guest Rahim, the same golden scenario as SplitEngineTest, as entities. */
    private static Outing goldenOuting() {
        Outing o = new Outing("Burger night", OutingCategory.EATING, LocalDate.of(2026, 11, 6), 1L);
        OutingParticipant rafsan = o.addUserParticipant(1L);
        OutingParticipant tanvir = o.addUserParticipant(2L);
        OutingParticipant nabila = o.addUserParticipant(3L);
        OutingParticipant rahim = o.addGuestParticipant(savedGuest(10, 1, "Rahim"));
        o.addItem("Burger", new BigDecimal("200.00"), SplitType.PERSONAL).addShare(rafsan, null);
        o.addItem("Pizza", new BigDecimal("500.00"), SplitType.PERSONAL).addShare(tanvir, null);
        o.addItem("Coffee", new BigDecimal("200.00"), SplitType.PERSONAL).addShare(nabila, null);
        Item big = o.addItem("Large pizza", new BigDecimal("1000.00"), SplitType.PERCENTAGE);
        big.addShare(rafsan, new BigDecimal("40"));
        big.addShare(tanvir, new BigDecimal("20"));
        big.addShare(nabila, new BigDecimal("10"));
        big.addShare(rahim, new BigDecimal("30"));
        o.addItem("Shake", new BigDecimal("150.00"), SplitType.PERSONAL).addShare(rahim, null);
        o.addContribution(rafsan, new BigDecimal("1550.00"));
        o.addContribution(tanvir, new BigDecimal("500.00"));
        return o;
    }

    @Test
    @DisplayName("Entities map to exactly the snapshot the builder would produce")
    void mapsToTheSameSnapshotAsTheBuilder() {
        ParticipantRef rafsan = ParticipantRef.user(1);
        ParticipantRef tanvir = ParticipantRef.user(2);
        ParticipantRef nabila = ParticipantRef.user(3);
        ParticipantRef rahim = ParticipantRef.guest(10, 1);
        OutingSnapshot expected = OutingSnapshot.builder()
            .participants(rafsan, tanvir, nabila, rahim)
            .personalItem("Burger", "200.00", rafsan)
            .personalItem("Pizza", "500.00", tanvir)
            .personalItem("Coffee", "200.00", nabila)
            .percentageItem("Large pizza", "1000.00", of(rafsan, "40"), of(tanvir, "20"), of(nabila, "10"), of(rahim, "30"))
            .personalItem("Shake", "150.00", rahim)
            .paid(rafsan, "1550.00")
            .paid(tanvir, "500.00")
            .build();

        assertThat(mapper.toSnapshot(goldenOuting())).isEqualTo(expected);
    }

    @Test
    @DisplayName("Mapped snapshot goes through the engine and gives the golden result")
    void engineResultFromEntities() {
        EngineResult result = SplitEngine.withDefaults().calculate(mapper.toSnapshot(goldenOuting()));

        assertThat(result.billTotal()).isEqualTo(Money.of(2050));
        assertThat(result.folded().users()).containsExactly(
            new UserPosition(1, Money.of(500)), new UserPosition(2, Money.of(-200)), new UserPosition(3, Money.of(-300)));
        assertThat(result.folded().guests()).containsExactly(new GuestPosition(10, 1, Money.of(-450)));
    }

    @Test
    @DisplayName("An outing with nothing added maps to an empty snapshot")
    void emptyOuting() {
        Outing o = new Outing("Empty", OutingCategory.OTHER, LocalDate.of(2026, 11, 6), 1L);

        OutingSnapshot snapshot = mapper.toSnapshot(o);

        assertThat(snapshot.participants()).isEmpty();
        assertThat(snapshot.items()).isEmpty();
        assertThat(snapshot.contributions()).isEmpty();
    }

    @Test
    @DisplayName("A guest that was never saved is reported clearly, not as a null pointer")
    void unsavedGuestRejected() {
        Outing o = new Outing("Trip", OutingCategory.TRAVEL, LocalDate.of(2026, 11, 6), 1L);
        o.addGuestParticipant(new Guest(1L, "Rahim"));

        assertThatThrownBy(() -> mapper.toSnapshot(o))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("Rahim").hasMessageContaining("saved");
    }

    @Test
    @DisplayName("Null outing is refused")
    void nullSafe() {
        assertThatThrownBy(() -> mapper.toSnapshot(null)).isInstanceOf(NullPointerException.class);
    }
}
