package com.kotopai.outing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kotopai.common.events.OutingUpdatedEvent;
import com.kotopai.ledger.event.OutingActivatedEvent;
import com.kotopai.ledger.event.OutingCancelledEvent;
import com.kotopai.outing.domain.Guest;
import com.kotopai.outing.domain.IllegalOutingTransitionException;
import com.kotopai.outing.domain.Outing;
import com.kotopai.outing.domain.OutingCategory;
import com.kotopai.outing.domain.OutingStatus;
import com.kotopai.outing.engine.SplitEngine;
import com.kotopai.outing.engine.split.SplitType;
import com.kotopai.outing.engine.validation.EngineValidationException;
import com.kotopai.outing.mapping.OutingSnapshotMapper;
import com.kotopai.outing.port.FriendshipChecker;
import com.kotopai.outing.port.PaymentLockChecker;
import com.kotopai.outing.repository.GuestRepository;
import com.kotopai.outing.repository.OutingRepository;
import com.kotopai.outing.service.OutingCommands.ContributionInput;
import com.kotopai.outing.service.OutingCommands.CreateOuting;
import com.kotopai.outing.service.OutingCommands.ItemInput;
import com.kotopai.outing.service.OutingCommands.OutingContents;
import com.kotopai.outing.service.OutingCommands.ParticipantKey;
import com.kotopai.outing.service.OutingCommands.ShareInput;
import com.kotopai.outing.service.OutingOperationException.Reason;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

/** The ports and repositories are mocked; the mapper and the engine are the real ones. */
@ExtendWith(MockitoExtension.class)
class OutingServiceTest {

    private static final long RAFSAN = 1L;
    private static final long TANVIR = 2L;
    private static final long NABILA = 3L;
    private static final long OUTING_ID = 100L;

    @Mock
    private OutingRepository outings;
    @Mock
    private GuestRepository guests;
    @Mock
    private FriendshipChecker friendships;
    @Mock
    private PaymentLockChecker paymentLock;
    @Mock
    private ApplicationEventPublisher events;

    private OutingService service;

    @BeforeEach
    void setUp() {
        service = new OutingService(outings, guests, friendships, paymentLock, new OutingSnapshotMapper(),
            SplitEngine.withDefaults(), events, new OutingAccessPolicy(outings));
        lenient().when(friendships.areFriends(anyLong(), anyLong())).thenReturn(true);
        lenient().when(outings.save(any(Outing.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Outing draft() {
        Outing o = service.create(RAFSAN, new CreateOuting("Burger night", OutingCategory.EATING, LocalDate.of(2026, 11, 6)));
        ReflectionTestUtils.setField(o, "id", OUTING_ID);
        lenient().when(outings.findById(OUTING_ID)).thenReturn(Optional.of(o));
        return o;
    }

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
    @DisplayName("Create makes a draft with the creator as first participant")
    void createDraft() {
        Outing o = draft();

        assertThat(o.getStatus()).isEqualTo(OutingStatus.DRAFT);
        assertThat(o.getCreatorId()).isEqualTo(RAFSAN);
        assertThat(o.getParticipants()).hasSize(1);
    }

    @Test
    @DisplayName("Updating a draft replaces participants, items, shares and contributions")
    void updateReplacesContents() {
        Outing o = draft();

        service.updateContents(RAFSAN, OUTING_ID, burgerNight());
        service.updateContents(RAFSAN, OUTING_ID, burgerNight());

        assertThat(o.getParticipants()).hasSize(3);
        assertThat(o.getItems()).hasSize(4);
        assertThat(o.getItems().get(3).getShares()).hasSize(3);
        assertThat(o.getContributions()).hasSize(2);
        verify(friendships, org.mockito.Mockito.atLeastOnce()).areFriends(RAFSAN, TANVIR);
    }

    @Test
    @DisplayName("A participant who is not a friend of the creator is rejected")
    void nonFriendRejected() {
        draft();
        when(friendships.areFriends(RAFSAN, NABILA)).thenReturn(false);

        assertThatThrownBy(() -> service.updateContents(RAFSAN, OUTING_ID, burgerNight()))
            .isInstanceOfSatisfying(OutingOperationException.class, e -> assertThat(e.getReason()).isEqualTo(Reason.NOT_A_FRIEND));
    }

    @Test
    @DisplayName("A guest owned by somebody else is rejected")
    void foreignGuestRejected() {
        draft();
        when(guests.findByIdAndOwnerId(10L, RAFSAN)).thenReturn(Optional.empty());
        OutingContents withGuest = new OutingContents(List.of(ParticipantKey.guest(10)), List.of(), List.of());

        assertThatThrownBy(() -> service.updateContents(RAFSAN, OUTING_ID, withGuest))
            .isInstanceOfSatisfying(OutingOperationException.class, e -> assertThat(e.getReason()).isEqualTo(Reason.UNKNOWN_GUEST));
    }

    @Test
    @DisplayName("An own guest can be added to the outing")
    void ownGuestAccepted() {
        Outing o = draft();
        Guest rahim = new Guest(RAFSAN, "Rahim");
        ReflectionTestUtils.setField(rahim, "id", 10L);
        when(guests.findByIdAndOwnerId(10L, RAFSAN)).thenReturn(Optional.of(rahim));

        service.updateContents(RAFSAN, OUTING_ID,
            new OutingContents(List.of(ParticipantKey.user(RAFSAN), ParticipantKey.guest(10)), List.of(), List.of()));

        assertThat(o.getParticipants()).hasSize(2);
        assertThat(o.getParticipants().get(1).isGuest()).isTrue();
    }

    @Test
    @DisplayName("Unknown ids and outings the actor cannot see are both not found")
    void notFoundAndInvisible() {
        draft();

        assertThatThrownBy(() -> service.submit(RAFSAN, 999L))
            .isInstanceOfSatisfying(OutingOperationException.class, e -> assertThat(e.getReason()).isEqualTo(Reason.NOT_FOUND));
        // a stranger cannot see a draft, so it does not exist for them
        assertThatThrownBy(() -> service.submit(TANVIR, OUTING_ID))
            .isInstanceOfSatisfying(OutingOperationException.class, e -> assertThat(e.getReason()).isEqualTo(Reason.NOT_FOUND));
    }

    @Test
    @DisplayName("A participant can see an active outing but cannot change it")
    void participantCanSeeButNotChange() {
        draft();
        service.updateContents(RAFSAN, OUTING_ID, burgerNight());
        service.submit(RAFSAN, OUTING_ID);

        assertThat(service.view(TANVIR, OUTING_ID).getTitle()).isEqualTo("Burger night");
        assertThatThrownBy(() -> service.cancel(TANVIR, OUTING_ID))
            .isInstanceOfSatisfying(OutingOperationException.class, e -> assertThat(e.getReason()).isEqualTo(Reason.FORBIDDEN));
    }

    @Test
    @DisplayName("view hides drafts from participants and returns them to the creator")
    void viewRespectsPolicy() {
        draft();
        service.updateContents(RAFSAN, OUTING_ID, burgerNight());

        assertThat(service.view(RAFSAN, OUTING_ID)).isNotNull();
        assertThatThrownBy(() -> service.view(TANVIR, OUTING_ID))
            .isInstanceOfSatisfying(OutingOperationException.class, e -> assertThat(e.getReason()).isEqualTo(Reason.NOT_FOUND));
    }

    @Test
    @DisplayName("listVisibleTo asks the repository visibility query")
    void listDelegates() {
        Outing o = draft();
        org.springframework.data.domain.Pageable page = org.springframework.data.domain.PageRequest.of(0, 5);
        when(outings.findVisibleTo(RAFSAN, page)).thenReturn(List.of(o));

        assertThat(service.listVisibleTo(RAFSAN, page)).containsExactly(o);
        assertThatThrownBy(() -> service.listVisibleTo(null, page)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Submit validates with the engine and activates a correct outing")
    void submitHappyPath() {
        Outing o = draft();
        service.updateContents(RAFSAN, OUTING_ID, burgerNight());

        service.submit(RAFSAN, OUTING_ID);

        assertThat(o.getStatus()).isEqualTo(OutingStatus.ACTIVE);
    }

    @Test
    @DisplayName("Submit of an invalid outing surfaces every engine problem and stays a draft")
    void submitInvalid() {
        Outing o = draft();
        OutingContents unpaid = new OutingContents(List.of(ParticipantKey.user(RAFSAN)),
            List.of(new ItemInput("Burger", new BigDecimal("200.00"), SplitType.PERSONAL,
                List.of(new ShareInput(ParticipantKey.user(RAFSAN), null)))),
            List.of(new ContributionInput(ParticipantKey.user(RAFSAN), new BigDecimal("150.00"))));
        service.updateContents(RAFSAN, OUTING_ID, unpaid);

        assertThatThrownBy(() -> service.submit(RAFSAN, OUTING_ID))
            .isInstanceOfSatisfying(EngineValidationException.class,
                e -> assertThat(e.getResult().codes()).containsExactly("PAID_NOT_EQUAL_BILL"));
        assertThat(o.getStatus()).isEqualTo(OutingStatus.DRAFT);
    }

    @Test
    @DisplayName("Edit is locked once a payment was recorded on an active outing")
    void editLockedAfterPayment() {
        draft();
        service.updateContents(RAFSAN, OUTING_ID, burgerNight());
        service.submit(RAFSAN, OUTING_ID);
        when(paymentLock.hasRecordedPayments(OUTING_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.updateContents(RAFSAN, OUTING_ID, burgerNight()))
            .isInstanceOf(IllegalOutingTransitionException.class).hasMessageContaining("edit");
    }

    @Test
    @DisplayName("An active outing can be edited while unpaid, but an edit that breaks the books is refused")
    void editActiveWhileUnpaid() {
        draft();
        service.updateContents(RAFSAN, OUTING_ID, burgerNight());
        service.submit(RAFSAN, OUTING_ID);
        when(paymentLock.hasRecordedPayments(OUTING_ID)).thenReturn(false);

        service.updateContents(RAFSAN, OUTING_ID, burgerNight());

        OutingContents broken = new OutingContents(List.of(ParticipantKey.user(RAFSAN)), List.of(), List.of());
        assertThatThrownBy(() -> service.updateContents(RAFSAN, OUTING_ID, broken))
            .isInstanceOf(EngineValidationException.class);
    }

    @Test
    @DisplayName("Drafts never consult the payment lock")
    void draftSkipsPaymentLock() {
        draft();

        service.updateContents(RAFSAN, OUTING_ID, burgerNight());

        verify(paymentLock, never()).hasRecordedPayments(anyLong());
    }

    @Test
    @DisplayName("Cancel works from draft and active, not from cancelled")
    void cancelFromEachStatus() {
        Outing fromDraft = draft();
        service.cancel(RAFSAN, OUTING_ID);
        assertThat(fromDraft.getStatus()).isEqualTo(OutingStatus.CANCELLED);
        assertThatThrownBy(() -> service.cancel(RAFSAN, OUTING_ID)).isInstanceOf(IllegalOutingTransitionException.class);

        Outing fromActive = draft();
        service.updateContents(RAFSAN, OUTING_ID, burgerNight());
        service.submit(RAFSAN, OUTING_ID);
        service.cancel(RAFSAN, OUTING_ID);
        assertThat(fromActive.getStatus()).isEqualTo(OutingStatus.CANCELLED);
    }

    @Test
    @DisplayName("Only drafts can be deleted")
    void deleteOnlyDrafts() {
        Outing o = draft();
        service.delete(RAFSAN, OUTING_ID);
        verify(outings).delete(o);

        service.updateContents(RAFSAN, OUTING_ID, burgerNight());
        service.submit(RAFSAN, OUTING_ID);
        assertThatThrownBy(() -> service.delete(RAFSAN, OUTING_ID))
            .isInstanceOfSatisfying(OutingOperationException.class, e -> assertThat(e.getReason()).isEqualTo(Reason.DRAFT_ONLY));
    }

    @Test
    @DisplayName("Bad references in the command are refused clearly")
    void badReferences() {
        draft();
        ParticipantKey stranger = ParticipantKey.user(99);
        OutingContents unknownInShare = new OutingContents(List.of(ParticipantKey.user(RAFSAN)),
            List.of(new ItemInput("Tea", new BigDecimal("30.00"), SplitType.EQUAL, List.of(new ShareInput(stranger, null)))),
            List.of());
        OutingContents duplicate = new OutingContents(List.of(ParticipantKey.user(RAFSAN), ParticipantKey.user(RAFSAN)),
            List.of(), List.of());

        assertThatThrownBy(() -> service.updateContents(RAFSAN, OUTING_ID, unknownInShare))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Tea").hasMessageContaining("user #99");
        assertThatThrownBy(() -> service.updateContents(RAFSAN, OUTING_ID, duplicate))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("twice");
        assertThatThrownBy(() -> new ParticipantKey(null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ParticipantKey(1L, 2L)).isInstanceOf(IllegalArgumentException.class);
    }

    // --- domain events (Observer) ---

    @Test
    @DisplayName("Submit publishes exactly one OutingActivatedEvent")
    void submitPublishesActivated() {
        draft();
        service.updateContents(RAFSAN, OUTING_ID, burgerNight());

        service.submit(RAFSAN, OUTING_ID);

        verify(events).publishEvent((Object) new OutingActivatedEvent(OUTING_ID, RAFSAN));
        verify(events, org.mockito.Mockito.times(1)).publishEvent(any(OutingActivatedEvent.class));
    }

    @Test
    @DisplayName("A failed submit publishes nothing")
    void failedSubmitPublishesNothing() {
        draft();
        clearInvocations(events);

        assertThatThrownBy(() -> service.submit(RAFSAN, OUTING_ID)).isInstanceOf(EngineValidationException.class);

        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("Editing an active outing publishes OutingUpdatedEvent; editing a draft does not")
    void updatePublishesOnlyForActive() {
        draft();
        clearInvocations(events);
        service.updateContents(RAFSAN, OUTING_ID, burgerNight());
        verify(events, never()).publishEvent(any(Object.class));

        service.submit(RAFSAN, OUTING_ID);
        service.updateContents(RAFSAN, OUTING_ID, burgerNight());

        verify(events).publishEvent((Object) new OutingUpdatedEvent(OUTING_ID, RAFSAN));
    }

    @Test
    @DisplayName("Cancelling an active outing publishes OutingCancelledEvent; cancelling a draft does not")
    void cancelPublishesOnlyForActive() {
        draft();
        clearInvocations(events);
        service.cancel(RAFSAN, OUTING_ID);
        verify(events, never()).publishEvent(any(Object.class));

        draft();
        clearInvocations(events);
        service.updateContents(RAFSAN, OUTING_ID, burgerNight());
        service.submit(RAFSAN, OUTING_ID);
        service.cancel(RAFSAN, OUTING_ID);

        verify(events).publishEvent((Object) new OutingCancelledEvent(OUTING_ID));
    }
}
