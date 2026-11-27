package com.kotopai.outing.adapter;

import com.kotopai.ledger.spi.GuestNetPosition;
import com.kotopai.ledger.spi.OutingSettlementProvider;
import com.kotopai.ledger.spi.OutingSettlementSnapshot;
import com.kotopai.ledger.spi.UserNetPosition;
import com.kotopai.outing.domain.Outing;
import com.kotopai.outing.engine.SplitEngine;
import com.kotopai.outing.engine.balance.ParticipantBalance;
import com.kotopai.outing.engine.model.ParticipantRef;
import com.kotopai.outing.mapping.OutingSnapshotMapper;
import com.kotopai.outing.repository.OutingRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adapter: implements the ledger settlement port with the split engine. The ledger folds guest nets into their
 * owners itself, so this hands over each participant OWN net (the engine pre-fold balances), never the already
 * folded user nets; sending those would count every guest twice. The ledger's "not provided yet" fallback
 * bean is conditional on a missing provider, so it steps aside when this adapter is present.
 */
@Component
public class EngineSettlementProvider implements OutingSettlementProvider {

    private final OutingRepository outings;
    private final OutingSnapshotMapper mapper;
    private final SplitEngine engine;

    public EngineSettlementProvider(OutingRepository outings, OutingSnapshotMapper mapper, SplitEngine engine) {
        this.outings = Objects.requireNonNull(outings);
        this.mapper = Objects.requireNonNull(mapper);
        this.engine = Objects.requireNonNull(engine);
    }

    @Override
    @Transactional(readOnly = true)
    public OutingSettlementSnapshot settlementFor(long outingId) {
        Outing outing = outings.findById(outingId)
            .orElseThrow(() -> new IllegalArgumentException("outing " + outingId + " does not exist"));
        List<UserNetPosition> users = new ArrayList<>();
        List<GuestNetPosition> guestPositions = new ArrayList<>();
        for (ParticipantBalance balance : engine.calculate(mapper.toSnapshot(outing)).balances()) {
            if (balance.participant() instanceof ParticipantRef.User user) {
                users.add(new UserNetPosition(user.userId(), balance.net().amount()));
            } else if (balance.participant() instanceof ParticipantRef.Guest guest) {
                guestPositions.add(new GuestNetPosition(guest.guestId(), guest.ownerUserId(), balance.net().amount()));
            }
        }
        return new OutingSettlementSnapshot(outingId, users, guestPositions);
    }
}
