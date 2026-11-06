package com.kotopai.outing.engine.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kotopai.common.money.Money;
import com.kotopai.outing.engine.split.SplitType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ItemLineTest {

    private static final ParticipantRef RAFSAN = ParticipantRef.user(1);
    private static final ParticipantRef TANVIR = ParticipantRef.user(2);

    @Test
    @DisplayName("Factory methods set the split type and consumers")
    void factories() {
        assertThat(ItemLine.personal("Burger", Money.of(200), RAFSAN).splitType()).isEqualTo(SplitType.PERSONAL);
        assertThat(ItemLine.equal("Tea", Money.of(30), RAFSAN, TANVIR).shares())
            .extracting(ShareLine::participant).containsExactly(RAFSAN, TANVIR);
        assertThat(ItemLine.percentage("Pizza", Money.of(100), ShareLine.of(RAFSAN, "100")).splitType())
            .isEqualTo(SplitType.PERCENTAGE);
    }

    @Test
    @DisplayName("Blank names and negative amounts are rejected")
    void rejectsInvalid() {
        assertThatThrownBy(() -> ItemLine.personal(" ", Money.of(10), RAFSAN))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ItemLine.personal("Burger", Money.of(-10), RAFSAN))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Shares are copied, so changing the original list does not change the item")
    void defensiveCopy() {
        List<ShareLine> shares = new ArrayList<>(List.of(ShareLine.of(RAFSAN)));
        ItemLine item = new ItemLine("Burger", Money.of(200), SplitType.PERSONAL, shares);

        shares.add(ShareLine.of(TANVIR));

        assertThat(item.shares()).hasSize(1);
        assertThatThrownBy(() -> item.shares().add(ShareLine.of(TANVIR)))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Users and guests are distinct participants; equal IDs mean the same participant")
    void participantIdentity() {
        assertThat(ParticipantRef.user(1)).isEqualTo(ParticipantRef.user(1));
        assertThat(ParticipantRef.user(1)).isNotEqualTo(ParticipantRef.guest(1, 1));
    }
}
