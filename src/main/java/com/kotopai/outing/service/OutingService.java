package com.kotopai.outing.service;

import com.kotopai.outing.domain.Guest;
import com.kotopai.outing.domain.Item;
import com.kotopai.outing.domain.Outing;
import com.kotopai.outing.domain.OutingParticipant;
import com.kotopai.outing.domain.OutingStatus;
import com.kotopai.outing.engine.SplitEngine;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service: the use cases of an outing (create a draft, edit it, submit, cancel, delete). It
 * orchestrates the pieces and holds no business rules of its own: status rules live in {@link OutingStatus},
 * calculation rules in the engine, and the other teams are reached only through ports. Each public method is
 * one transaction, so a failure anywhere rolls the whole use case back.
 */
@Service
@Transactional
public class OutingService {

    private final OutingRepository outings;
    private final GuestRepository guests;
    private final FriendshipChecker friendships;
    private final PaymentLockChecker paymentLock;
    private final OutingSnapshotMapper mapper;
    private final SplitEngine engine;

    public OutingService(OutingRepository outings, GuestRepository guests, FriendshipChecker friendships,
                         PaymentLockChecker paymentLock, OutingSnapshotMapper mapper, SplitEngine engine) {
        this.outings = Objects.requireNonNull(outings);
        this.guests = Objects.requireNonNull(guests);
        this.friendships = Objects.requireNonNull(friendships);
        this.paymentLock = Objects.requireNonNull(paymentLock);
        this.mapper = Objects.requireNonNull(mapper);
        this.engine = Objects.requireNonNull(engine);
    }

    /** New draft; the creator is always the first participant. */
    public Outing create(Long actorId, CreateOuting command) {
        Objects.requireNonNull(actorId, "actorId must not be null");
        Outing outing = new Outing(command.title(), command.category(), command.date(), actorId);
        outing.addUserParticipant(actorId);
        return outings.save(outing);
    }

    /**
     * Replaces the participants, items and contributions. Allowed on a draft, or on an active outing while no
     * payment has been recorded; an active outing is re-validated so its debts can be recomputed safely.
     */
    public Outing updateContents(Long actorId, Long outingId, OutingContents contents) {
        Outing outing = loadOwned(actorId, outingId);
        boolean paymentRecorded = outing.getStatus() == OutingStatus.ACTIVE && paymentLock.hasRecordedPayments(outingId);
        outing.assertEditable(paymentRecorded);

        Map<ParticipantKey, OutingParticipant> byKey = rebuildParticipants(outing, contents);
        for (ItemInput input : contents.items()) {
            Item item = outing.addItem(input.name(), input.amount(), input.splitType());
            for (ShareInput share : input.shares()) {
                item.addShare(resolve(byKey, share.participant(), "item '" + input.name() + "'"), share.percentage());
            }
        }
        for (ContributionInput input : contents.contributions()) {
            outing.addContribution(resolve(byKey, input.participant(), "a payment"), input.amount());
        }

        if (outing.getStatus() == OutingStatus.ACTIVE) {
            engine.calculate(mapper.toSnapshot(outing));
        }
        return outing;
    }

    /** Validates the whole outing with the engine (every problem is reported at once), then activates it. */
    public Outing submit(Long actorId, Long outingId) {
        Outing outing = loadOwned(actorId, outingId);
        engine.calculate(mapper.toSnapshot(outing));
        outing.submit();
        return outing;
    }

    public Outing cancel(Long actorId, Long outingId) {
        Outing outing = loadOwned(actorId, outingId);
        outing.cancel();
        return outing;
    }

    /** Only drafts can be deleted; anything submitted stays in history (cancel it instead). */
    public void delete(Long actorId, Long outingId) {
        Outing outing = loadOwned(actorId, outingId);
        if (outing.getStatus() != OutingStatus.DRAFT) {
            throw new OutingOperationException(Reason.DRAFT_ONLY, "Only a draft can be deleted; cancel it instead");
        }
        outings.delete(outing);
    }

    private Map<ParticipantKey, OutingParticipant> rebuildParticipants(Outing outing, OutingContents contents) {
        outing.clearContents();
        Map<ParticipantKey, OutingParticipant> byKey = new LinkedHashMap<>();
        for (ParticipantKey key : contents.participants()) {
            if (byKey.containsKey(key)) {
                throw new IllegalArgumentException(key.label() + " is listed twice");
            }
            byKey.put(key, key.userId() != null
                ? addUser(outing, key.userId())
                : outing.addGuestParticipant(ownedGuest(outing.getCreatorId(), key.guestId())));
        }
        return byKey;
    }

    private OutingParticipant addUser(Outing outing, Long userId) {
        boolean isCreator = userId.equals(outing.getCreatorId());
        if (!isCreator && !friendships.areFriends(outing.getCreatorId(), userId)) {
            throw new OutingOperationException(Reason.NOT_A_FRIEND, "user #" + userId + " is not your friend");
        }
        return outing.addUserParticipant(userId);
    }

    private Guest ownedGuest(Long creatorId, Long guestId) {
        return guests.findByIdAndOwnerId(guestId, creatorId).orElseThrow(() ->
            new OutingOperationException(Reason.UNKNOWN_GUEST, "guest #" + guestId + " is not one of your guests"));
    }

    private static OutingParticipant resolve(Map<ParticipantKey, OutingParticipant> byKey, ParticipantKey key, String where) {
        OutingParticipant participant = byKey.get(key);
        if (participant == null) {
            throw new IllegalArgumentException(where + " refers to " + key.label() + ", who is not on the outing");
        }
        return participant;
    }

    private Outing loadOwned(Long actorId, Long outingId) {
        Objects.requireNonNull(actorId, "actorId must not be null");
        Outing outing = outings.findById(outingId).orElseThrow(() ->
            new OutingOperationException(Reason.NOT_FOUND, "Outing not found"));
        if (!outing.getCreatorId().equals(actorId)) {
            throw new OutingOperationException(Reason.FORBIDDEN, "Only the creator can change this outing");
        }
        return outing;
    }
}
