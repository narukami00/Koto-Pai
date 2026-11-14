package com.kotopai.outing.service;

import com.kotopai.common.money.Money;
import com.kotopai.outing.domain.Guest;
import com.kotopai.outing.engine.EngineResult;
import com.kotopai.outing.engine.SplitEngine;
import com.kotopai.outing.engine.model.ItemLine;
import com.kotopai.outing.engine.model.OutingSnapshot;
import com.kotopai.outing.engine.model.ParticipantRef;
import com.kotopai.outing.engine.model.ShareLine;
import com.kotopai.outing.engine.validation.EngineValidationException;
import com.kotopai.outing.engine.validation.ValidationIssue;
import com.kotopai.outing.repository.GuestRepository;
import com.kotopai.outing.service.OutingCommands.ContributionInput;
import com.kotopai.outing.service.OutingCommands.ItemInput;
import com.kotopai.outing.service.OutingCommands.OutingContents;
import com.kotopai.outing.service.OutingCommands.ParticipantKey;
import com.kotopai.outing.service.OutingOperationException.Reason;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Dry run of the split engine on input that has not been saved: nothing is persisted and nobody is notified. It
 * powers the live split preview in the wizard, so an incomplete outing is a normal answer (valid = false plus every
 * issue), not an error.
 */
@Service
public class OutingPreviewService {

    /** The preview outcome; {@code result} is present only when there are no issues. */
    public record PreviewResult(Money billTotal, Money paidTotal, List<ValidationIssue> issues, EngineResult result) {

        public boolean isValid() {
            return issues.isEmpty();
        }
    }

    private final GuestRepository guests;
    private final SplitEngine engine;

    public OutingPreviewService(GuestRepository guests, SplitEngine engine) {
        this.guests = Objects.requireNonNull(guests, "guests must not be null");
        this.engine = Objects.requireNonNull(engine, "engine must not be null");
    }

    public PreviewResult preview(Long actorId, OutingContents contents) {
        Objects.requireNonNull(actorId, "actorId must not be null");
        Map<Long, ParticipantRef> guestRefs = new HashMap<>();
        OutingSnapshot.Builder builder = OutingSnapshot.builder();
        Set<ParticipantKey> seen = new HashSet<>();
        for (ParticipantKey key : contents.participants()) {
            if (!seen.add(key)) {
                throw new IllegalArgumentException(key.label() + " is listed twice");
            }
            builder.participant(ref(actorId, key, guestRefs));
        }
        for (ItemInput item : contents.items()) {
            List<ShareLine> shares = item.shares().stream()
                .map(s -> new ShareLine(ref(actorId, s.participant(), guestRefs), s.percentage())).toList();
            builder.item(new ItemLine(item.name(), Money.of(item.amount()), item.splitType(), shares));
        }
        for (ContributionInput payment : contents.contributions()) {
            builder.paid(ref(actorId, payment.participant(), guestRefs), Money.of(payment.amount()));
        }
        OutingSnapshot snapshot = builder.build();

        try {
            EngineResult result = engine.calculate(snapshot);
            return new PreviewResult(snapshot.billTotal(), snapshot.paidTotal(), List.of(), result);
        } catch (EngineValidationException e) {
            return new PreviewResult(snapshot.billTotal(), snapshot.paidTotal(), e.getResult().issues(), null);
        }
    }

    /** A user id is taken as is; a guest must be one of the actor own guests (the engine needs the owner). */
    private ParticipantRef ref(Long actorId, ParticipantKey key, Map<Long, ParticipantRef> guestRefs) {
        if (key.userId() != null) {
            return ParticipantRef.user(key.userId());
        }
        return guestRefs.computeIfAbsent(key.guestId(), id -> {
            Guest guest = guests.findByIdAndOwnerId(id, actorId).orElseThrow(() ->
                new OutingOperationException(Reason.UNKNOWN_GUEST, "guest #" + id + " is not one of your guests"));
            return ParticipantRef.guest(guest.getId(), guest.getOwnerId());
        });
    }
}
