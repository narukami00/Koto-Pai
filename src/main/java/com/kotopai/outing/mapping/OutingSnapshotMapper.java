package com.kotopai.outing.mapping;

import com.kotopai.common.money.Money;
import com.kotopai.outing.domain.Contribution;
import com.kotopai.outing.domain.Item;
import com.kotopai.outing.domain.ItemShare;
import com.kotopai.outing.domain.Outing;
import com.kotopai.outing.domain.OutingParticipant;
import com.kotopai.outing.engine.model.ItemLine;
import com.kotopai.outing.engine.model.OutingSnapshot;
import com.kotopai.outing.engine.model.ParticipantRef;
import com.kotopai.outing.engine.model.ShareLine;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Adapter pattern: translates the JPA {@link Outing} graph into the engine own {@link OutingSnapshot}, so the
 * pure-Java engine never sees entities, lazy proxies or Hibernate. If the database model changes, only this class
 * changes; the engine and its tests stay untouched.
 *
 * <p>No database access happens here, but lazy associations are read, so call it inside a transaction.
 */
@Component
public class OutingSnapshotMapper {

    public OutingSnapshot toSnapshot(Outing outing) {
        Objects.requireNonNull(outing, "outing must not be null");
        OutingSnapshot.Builder builder = OutingSnapshot.builder();
        outing.getParticipants().forEach(p -> builder.participant(refOf(p)));
        outing.getItems().forEach(item -> builder.item(lineOf(item)));
        for (Contribution c : outing.getContributions()) {
            builder.paid(refOf(c.getParticipant()), Money.of(c.getAmountPaid()));
        }
        return builder.build();
    }

    private static ItemLine lineOf(Item item) {
        List<ShareLine> shares = item.getShares().stream().map(OutingSnapshotMapper::shareOf).toList();
        return new ItemLine(item.getName(), Money.of(item.getAmount()), item.getSplitType(), shares);
    }

    private static ShareLine shareOf(ItemShare share) {
        return new ShareLine(refOf(share.getParticipant()), share.getPercentage());
    }

    private static ParticipantRef refOf(OutingParticipant participant) {
        if (!participant.isGuest()) {
            return ParticipantRef.user(participant.getUserId());
        }
        if (participant.getGuest().getId() == null) {
            throw new IllegalStateException("guest '" + participant.getGuest().getName() + "' must be saved before mapping");
        }
        return ParticipantRef.guest(participant.getGuest().getId(), participant.getGuest().getOwnerId());
    }
}
