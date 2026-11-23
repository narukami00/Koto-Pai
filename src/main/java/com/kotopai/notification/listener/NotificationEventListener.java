package com.kotopai.notification.listener;

import java.util.Objects;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.kotopai.common.events.OutingUpdatedEvent;
import com.kotopai.ledger.event.DebtDisputedEvent;
import com.kotopai.ledger.event.DelegationExecutedEvent;
import com.kotopai.ledger.event.DelegationReversedEvent;
import com.kotopai.ledger.event.OutingActivatedEvent;
import com.kotopai.ledger.event.OutingCancelledEvent;
import com.kotopai.ledger.event.PaymentConfirmedEvent;
import com.kotopai.ledger.event.PaymentRecordedEvent;
import com.kotopai.notification.domain.NotificationType;
import com.kotopai.notification.service.NotificationService;
import com.kotopai.outing.domain.OutingParticipant;
import com.kotopai.outing.repository.OutingRepository;

/**
 * Observer listener reacting to domain events from Outings, Ledger, and Friends.
 *
 * <p>Translates business lifecycle events into persistent notifications for affected users.
 */
@Component
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final OutingRepository outingRepository;

    public NotificationEventListener(NotificationService notificationService, OutingRepository outingRepository) {
        this.notificationService = notificationService;
        this.outingRepository = outingRepository;
    }

    @EventListener
    public void onOutingActivated(OutingActivatedEvent event) {
        if (event == null || event.outingId() == null) {
            return;
        }
        outingRepository.findWithParticipantsById(event.outingId()).ifPresent(outing -> {
            String title = outing.getTitle() != null ? outing.getTitle() : "Outing";
            for (OutingParticipant p : outing.getParticipants()) {
                if (p.getUserId() != null && !Objects.equals(p.getUserId(), event.creatorId())) {
                    notificationService.notify(
                            p.getUserId(),
                            NotificationType.OUTING_ACTIVATED,
                            "Outing '" + title + "' was activated! Check your split.",
                            "/outings/" + outing.getId()
                    );
                }
            }
        });
    }

    @EventListener
    public void onOutingUpdated(OutingUpdatedEvent event) {
        if (event == null || event.outingId() == null) {
            return;
        }
        outingRepository.findWithParticipantsById(event.outingId()).ifPresent(outing -> {
            String title = outing.getTitle() != null ? outing.getTitle() : "Outing";
            for (OutingParticipant p : outing.getParticipants()) {
                if (p.getUserId() != null && !Objects.equals(p.getUserId(), event.editorId())) {
                    notificationService.notify(
                            p.getUserId(),
                            NotificationType.OUTING_UPDATED,
                            "Outing '" + title + "' was updated. Numbers recalculated.",
                            "/outings/" + outing.getId()
                    );
                }
            }
        });
    }

    @EventListener
    public void onOutingCancelled(OutingCancelledEvent event) {
        if (event == null || event.outingId() == null) {
            return;
        }
        outingRepository.findWithParticipantsById(event.outingId()).ifPresent(outing -> {
            String title = outing.getTitle() != null ? outing.getTitle() : "Outing";
            for (OutingParticipant p : outing.getParticipants()) {
                if (p.getUserId() != null && !Objects.equals(p.getUserId(), outing.getCreatorId())) {
                    notificationService.notify(
                            p.getUserId(),
                            NotificationType.OUTING_CANCELLED,
                            "Outing '" + title + "' was cancelled.",
                            "/"
                    );
                }
            }
        });
    }

    @EventListener
    public void onPaymentRecorded(PaymentRecordedEvent event) {
        if (event == null) {
            return;
        }
        notificationService.notify(
                event.creditorId(),
                NotificationType.PAYMENT_RECORDED,
                "Payment of ৳" + event.amount() + " recorded (pending your confirmation).",
                "/settle"
        );
    }

    @EventListener
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        if (event == null) {
            return;
        }
        notificationService.notify(
                event.debtorId(),
                NotificationType.PAYMENT_CONFIRMED,
                "Your payment of ৳" + event.amount() + " was confirmed! Debt settled.",
                "/ledger"
        );
    }

    @EventListener
    public void onDebtDisputed(DebtDisputedEvent event) {
        if (event == null) {
            return;
        }
        long otherParty = (event.disputedById() == event.debtorId()) ? event.creditorId() : event.debtorId();
        String reasonStr = (event.reason() != null && !event.reason().isBlank()) ? ": " + event.reason() : "";

        notificationService.notify(
                otherParty,
                NotificationType.DEBT_DISPUTED,
                "A debt was disputed" + reasonStr,
                "/ledger"
        );

        if (event.outingCreatorId() != event.disputedById() && event.outingCreatorId() != otherParty) {
            notificationService.notify(
                    event.outingCreatorId(),
                    NotificationType.DEBT_DISPUTED,
                    "A debt in your outing was disputed" + reasonStr,
                    "/ledger"
            );
        }
    }

    @EventListener
    public void onDelegationExecuted(DelegationExecutedEvent event) {
        if (event == null) {
            return;
        }
        notificationService.notify(
                event.debtorId(),
                NotificationType.DEBT_DELEGATED,
                "A debt of ৳" + event.amount() + " was delegated to you.",
                "/ledger"
        );
        notificationService.notify(
                event.creditorId(),
                NotificationType.DEBT_DELEGATED,
                "A debt of ৳" + event.amount() + " was delegated in your favor.",
                "/ledger"
        );
    }

    @EventListener
    public void onDelegationReversed(DelegationReversedEvent event) {
        if (event == null) {
            return;
        }
        String reasonStr = (event.reason() != null && !event.reason().isBlank()) ? ": " + event.reason() : "";
        notificationService.notify(
                event.debtorId(),
                NotificationType.DEBT_DISPUTED,
                "Delegation of ৳" + event.amount() + " was reversed" + reasonStr,
                "/ledger"
        );
        notificationService.notify(
                event.creditorId(),
                NotificationType.DEBT_DISPUTED,
                "Delegation of ৳" + event.amount() + " was reversed" + reasonStr,
                "/ledger"
        );
    }
}

