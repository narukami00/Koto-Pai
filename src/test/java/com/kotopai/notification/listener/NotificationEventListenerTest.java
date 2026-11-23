package com.kotopai.notification.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kotopai.ledger.domain.PaymentStatus;
import com.kotopai.ledger.event.DebtDisputedEvent;
import com.kotopai.ledger.event.DelegationExecutedEvent;
import com.kotopai.ledger.event.OutingActivatedEvent;
import com.kotopai.ledger.event.PaymentConfirmedEvent;
import com.kotopai.ledger.event.PaymentRecordedEvent;
import com.kotopai.notification.domain.NotificationType;
import com.kotopai.notification.service.NotificationService;
import com.kotopai.outing.domain.Outing;
import com.kotopai.outing.domain.OutingParticipant;
import com.kotopai.outing.repository.OutingRepository;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private OutingRepository outingRepository;

    @InjectMocks
    private NotificationEventListener listener;

    @Test
    @DisplayName("onOutingActivated notifies non-creator participants")
    void onOutingActivated_success() {
        Outing outing = org.mockito.Mockito.mock(Outing.class);
        when(outing.getId()).thenReturn(100L);
        when(outing.getTitle()).thenReturn("Burger Night");

        OutingParticipant creator = org.mockito.Mockito.mock(OutingParticipant.class);
        when(creator.getUserId()).thenReturn(1L);

        OutingParticipant friend1 = org.mockito.Mockito.mock(OutingParticipant.class);
        when(friend1.getUserId()).thenReturn(2L);

        OutingParticipant friend2 = org.mockito.Mockito.mock(OutingParticipant.class);
        when(friend2.getUserId()).thenReturn(3L);

        when(outing.getParticipants()).thenReturn(List.of(creator, friend1, friend2));
        when(outingRepository.findWithParticipantsById(100L)).thenReturn(Optional.of(outing));

        listener.onOutingActivated(new OutingActivatedEvent(100L, 1L));

        verify(notificationService).notify(
                eq(2L),
                eq(NotificationType.OUTING_ACTIVATED),
                eq("Outing 'Burger Night' was activated! Check your split."),
                eq("/outings/100")
        );
        verify(notificationService).notify(
                eq(3L),
                eq(NotificationType.OUTING_ACTIVATED),
                eq("Outing 'Burger Night' was activated! Check your split."),
                eq("/outings/100")
        );
        verify(notificationService, never()).notify(eq(1L), any(), any(), any());
    }

    @Test
    @DisplayName("onPaymentRecorded notifies creditor")
    void onPaymentRecorded_success() {
        PaymentRecordedEvent event = new PaymentRecordedEvent(
                10L, 5L, 100L, 2L, 2L, 1L, BigDecimal.valueOf(300), PaymentStatus.PENDING_CONFIRMATION);

        listener.onPaymentRecorded(event);

        verify(notificationService).notify(
                eq(1L),
                eq(NotificationType.PAYMENT_RECORDED),
                eq("Payment of ৳300 recorded (pending your confirmation)."),
                eq("/settle")
        );
    }

    @Test
    @DisplayName("onPaymentConfirmed notifies debtor")
    void onPaymentConfirmed_success() {
        PaymentConfirmedEvent event = new PaymentConfirmedEvent(
                10L, 5L, 100L, 1L, 2L, 1L, BigDecimal.valueOf(300));

        listener.onPaymentConfirmed(event);

        verify(notificationService).notify(
                eq(2L),
                eq(NotificationType.PAYMENT_CONFIRMED),
                eq("Your payment of ৳300 was confirmed! Debt settled."),
                eq("/ledger")
        );
    }

    @Test
    @DisplayName("onDebtDisputed notifies counterparty and creator")
    void onDebtDisputed_success() {
        DebtDisputedEvent event = new DebtDisputedEvent(
                5L, 100L, 2L, 1L, 2L, 3L, "Wrong item assigned");

        listener.onDebtDisputed(event);

        // Counterparty is 3L
        verify(notificationService).notify(
                eq(3L),
                eq(NotificationType.DEBT_DISPUTED),
                eq("A debt was disputed: Wrong item assigned"),
                eq("/ledger")
        );
        // Creator is 1L
        verify(notificationService).notify(
                eq(1L),
                eq(NotificationType.DEBT_DISPUTED),
                eq("A debt in your outing was disputed: Wrong item assigned"),
                eq("/ledger")
        );
    }

    @Test
    @DisplayName("onDelegationExecuted notifies both debtor and creditor")
    void onDelegationExecuted_success() {
        DelegationExecutedEvent event = new DelegationExecutedEvent(
                1L, 10L, 11L, 12L, 2L, 3L, 4L, BigDecimal.valueOf(350));

        listener.onDelegationExecuted(event);

        verify(notificationService).notify(
                eq(2L),
                eq(NotificationType.DEBT_DELEGATED),
                eq("A debt of ৳350 was delegated to you."),
                eq("/ledger")
        );
        verify(notificationService).notify(
                eq(4L),
                eq(NotificationType.DEBT_DELEGATED),
                eq("A debt of ৳350 was delegated in your favor."),
                eq("/ledger")
        );
    }
}

