package com.kotopai.outing.engine.balance;

import com.kotopai.common.money.Money;
import com.kotopai.outing.engine.model.ParticipantRef;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Guest proxy rule: a guest has no account, so whatever a guest owes (or is owed) is settled by their owner.
 * Folding adds each guest's net into the owner's net, so debts are generated only between real users, while the
 * guest's own net is returned unchanged for the owner-guest record. Without it, a guest's bill would vanish
 * or someone would be asked to pay a person who cannot log in.
 */
public final class GuestFolder {

    public FoldedBalances fold(List<ParticipantBalance> balances) {
        Objects.requireNonNull(balances, "balances must not be null");
        Map<Long, Money> userNets = new LinkedHashMap<>();
        for (ParticipantBalance b : balances) {
            if (b.participant() instanceof ParticipantRef.User u) {
                userNets.merge(u.userId(), b.net(), Money::plus);
            }
        }
        List<GuestPosition> guests = new ArrayList<>();
        for (ParticipantBalance b : balances) {
            if (b.participant() instanceof ParticipantRef.Guest g) {
                if (!userNets.containsKey(g.ownerUserId())) {
                    throw new IllegalArgumentException(
                        g.label() + " belongs to user #" + g.ownerUserId() + " who is not on the outing");
                }
                userNets.merge(g.ownerUserId(), b.net(), Money::plus);
                guests.add(new GuestPosition(g.guestId(), g.ownerUserId(), b.net()));
            }
        }
        List<UserPosition> users = userNets.entrySet().stream()
            .map(e -> new UserPosition(e.getKey(), e.getValue())).toList();
        return new FoldedBalances(users, guests);
    }
}
