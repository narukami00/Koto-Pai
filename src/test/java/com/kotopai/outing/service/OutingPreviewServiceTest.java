package com.kotopai.outing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kotopai.common.money.Money;
import com.kotopai.outing.domain.Guest;
import com.kotopai.outing.engine.SplitEngine;
import com.kotopai.outing.engine.balance.ParticipantBalance;
import com.kotopai.outing.engine.balance.UserPosition;
import com.kotopai.outing.engine.split.SplitType;
import com.kotopai.outing.engine.validation.ValidationIssue;
import com.kotopai.outing.repository.GuestRepository;
import com.kotopai.outing.service.OutingCommands.ContributionInput;
import com.kotopai.outing.service.OutingCommands.ItemInput;
import com.kotopai.outing.service.OutingCommands.OutingContents;
import com.kotopai.outing.service.OutingCommands.ParticipantKey;
import com.kotopai.outing.service.OutingCommands.ShareInput;
import com.kotopai.outing.service.OutingOperationException.Reason;
import com.kotopai.outing.service.OutingPreviewService.PreviewResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class OutingPreviewServiceTest {

    private static final long RAFSAN = 1L;
    private static final long TANVIR = 2L;
    private static final long NABILA = 3L;

    private final GuestRepository guests = mock(GuestRepository.class);
    private final OutingPreviewService service = new OutingPreviewService(guests, SplitEngine.withDefaults());

    private static OutingContents burgerNight() {
        ParticipantKey rafsan = ParticipantKey.user(RAFSAN);
        ParticipantKey tanvir = ParticipantKey.user(TANVIR);
        ParticipantKey nabila = ParticipantKey.user(NABILA);
        return new OutingContents(
            List.of(rafsan, tanvir, nabila),
            List.of(
                new ItemInput("Burger", new BigDecimal("200.00"), SplitType.PERSONAL, List.of(new ShareInput(rafsan, null))),
                new ItemInput("Pizza", new BigDecimal("500.00"), SplitType.PERSONAL, List.of(new ShareInput(tanvir, null))),
                new ItemInput("Cold coffee", new BigDecimal("200.00"), SplitType.PERSONAL, List.of(new ShareInput(nabila, null))),
                new ItemInput("Large pizza", new BigDecimal("1000.00"), SplitType.PERCENTAGE, List.of(
                    new ShareInput(rafsan, new BigDecimal("50")), new ShareInput(tanvir, new BigDecimal("30")),
                    new ShareInput(nabila, new BigDecimal("20"))))),
            List.of(new ContributionInput(rafsan, new BigDecimal("1400.00")),
                new ContributionInput(tanvir, new BigDecimal("500.00"))));
    }

    @Test
    @DisplayName("Burger night: Rafsan +700, Tanvir -300, Nabila -400, and nothing is saved")
    void burgerNightNumbers() {
        PreviewResult preview = service.preview(RAFSAN, burgerNight());

        assertThat(preview.isValid()).isTrue();
        assertThat(preview.billTotal()).isEqualTo(Money.of(1900));
        assertThat(preview.paidTotal()).isEqualTo(Money.of(1900));
        assertThat(preview.result().balances()).extracting(ParticipantBalance::net)
            .containsExactly(Money.of(700), Money.of(-300), Money.of(-400));
        assertThat(preview.result().balances()).extracting(ParticipantBalance::consumed)
            .containsExactly(Money.of(700), Money.of(800), Money.of(400));
        assertThat(preview.result().folded().users()).extracting(UserPosition::net)
            .containsExactly(Money.of(700), Money.of(-300), Money.of(-400));
        verify(guests, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Incomplete input is a normal answer: every issue, totals that show the gap, no balances")
    void invalidReturnsAllIssues() {
        ParticipantKey rafsan = ParticipantKey.user(RAFSAN);
        OutingContents unpaid = new OutingContents(
            List.of(rafsan),
            List.of(new ItemInput("Burger", new BigDecimal("200.00"), SplitType.PERSONAL,
                List.of(new ShareInput(ParticipantKey.user(TANVIR), null)))),
            List.of(new ContributionInput(rafsan, new BigDecimal("150.00"))));

        PreviewResult preview = service.preview(RAFSAN, unpaid);

        assertThat(preview.isValid()).isFalse();
        assertThat(preview.result()).isNull();
        assertThat(preview.issues()).extracting(ValidationIssue::code).containsExactly("UNKNOWN_CONSUMER", "PAID_NOT_EQUAL_BILL");
        assertThat(preview.billTotal()).isEqualTo(Money.of(200));
        assertThat(preview.paidTotal()).isEqualTo(Money.of(150));
    }

    @Test
    @DisplayName("An empty outing lists the missing pieces instead of failing")
    void emptyOuting() {
        PreviewResult preview = service.preview(RAFSAN, new OutingContents(List.of(), List.of(), List.of()));

        assertThat(preview.issues()).extracting(ValidationIssue::code).containsExactly("NO_PARTICIPANTS", "NO_ITEMS");
    }

    @Test
    @DisplayName("An own guest is folded into the owner; the guest own net is kept")
    void guestsAreFolded() {
        Guest rahim = new Guest(RAFSAN, "Rahim");
        ReflectionTestUtils.setField(rahim, "id", 10L);
        when(guests.findByIdAndOwnerId(10L, RAFSAN)).thenReturn(Optional.of(rahim));
        ParticipantKey rafsan = ParticipantKey.user(RAFSAN);
        ParticipantKey guest = ParticipantKey.guest(10);
        OutingContents contents = new OutingContents(List.of(rafsan, guest),
            List.of(new ItemInput("Steak", new BigDecimal("300.00"), SplitType.PERSONAL, List.of(new ShareInput(guest, null)))),
            List.of(new ContributionInput(rafsan, new BigDecimal("300.00"))));

        PreviewResult preview = service.preview(RAFSAN, contents);

        assertThat(preview.isValid()).isTrue();
        assertThat(preview.result().folded().users()).containsExactly(new UserPosition(1, Money.of(0)));
        assertThat(preview.result().folded().guests()).hasSize(1);
        assertThat(preview.result().folded().guests().get(0).net()).isEqualTo(Money.of(-300));
    }

    @Test
    @DisplayName("A guest that is not the actor own is refused")
    void foreignGuestRefused() {
        when(guests.findByIdAndOwnerId(10L, RAFSAN)).thenReturn(Optional.empty());
        OutingContents contents = new OutingContents(List.of(ParticipantKey.guest(10)), List.of(), List.of());

        assertThatThrownBy(() -> service.preview(RAFSAN, contents))
            .isInstanceOfSatisfying(OutingOperationException.class, e -> assertThat(e.getReason()).isEqualTo(Reason.UNKNOWN_GUEST));
    }

    @Test
    @DisplayName("A participant listed twice and null arguments are refused")
    void badInput() {
        OutingContents twice = new OutingContents(List.of(ParticipantKey.user(1), ParticipantKey.user(1)), List.of(), List.of());

        assertThatThrownBy(() -> service.preview(RAFSAN, twice))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("twice");
        assertThatThrownBy(() -> service.preview(null, burgerNight())).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OutingPreviewService(null, SplitEngine.withDefaults())).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OutingPreviewService(guests, null)).isInstanceOf(NullPointerException.class);
    }
}
