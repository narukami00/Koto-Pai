package com.kotopai.outing.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kotopai.ledger.domain.Debt;
import com.kotopai.ledger.domain.DebtStatus;
import com.kotopai.ledger.domain.GuestDebt;
import com.kotopai.ledger.event.OutingActivatedEvent;
import com.kotopai.ledger.event.OutingCancelledEvent;
import com.kotopai.ledger.repository.DebtRepository;
import com.kotopai.ledger.repository.GuestDebtRepository;
import com.kotopai.outing.domain.OutingStatus;
import com.kotopai.outing.repository.OutingRepository;
import java.math.BigDecimal;
import java.util.List;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * The whole story of an outing through the real application: create a draft, add friends, a guest, items,
 * shares and payments, preview, submit (debts appear in the ledger), read it back, cancel (debts are voided).
 */
@RecordApplicationEvents
class OutingLifecycleIT extends OutingItSupport {

    private static final long RAFSAN = 101;
    private static final long TANVIR = 102;
    private static final long NABILA = 103;

    @Autowired
    private OutingRepository outings;
    @Autowired
    private DebtRepository debts;
    @Autowired
    private GuestDebtRepository guestDebts;
    @Autowired
    private ApplicationEvents events;

    private long countOf(Class<?> type) {
        return events.stream().filter(e -> e instanceof PayloadApplicationEvent<?> p && type.isInstance(p.getPayload())).count();
    }

    /** Burger night plus Rafsan guest Rahim, the same golden scenario as the engine tests. */
    private String goldenBody(long rahim) {
        String large = "{\"name\":\"Large pizza\",\"amount\":1000.00,\"splitType\":\"PERCENTAGE\",\"shares\":["
            + "{\"participant\":" + userRef(RAFSAN) + ",\"percentage\":40},"
            + "{\"participant\":" + userRef(TANVIR) + ",\"percentage\":20},"
            + "{\"participant\":" + userRef(NABILA) + ",\"percentage\":10},"
            + "{\"participant\":" + guestRef(rahim) + ",\"percentage\":30}]}";
        return contents(
            userRef(RAFSAN) + "," + userRef(TANVIR) + "," + userRef(NABILA) + "," + guestRef(rahim),
            personalItem("Burger", "200.00", userRef(RAFSAN)) + "," + personalItem("Pizza", "500.00", userRef(TANVIR)) + ","
                + personalItem("Cold coffee", "200.00", userRef(NABILA)) + "," + large + ","
                + personalItem("Shake", "150.00", guestRef(rahim)),
            payment(userRef(RAFSAN), "1550.00") + "," + payment(userRef(TANVIR), "500.00"));
    }

    @Test
    @DisplayName("Draft to active to cancelled: the database, the events and the ledger all follow")
    void fullLifecycle() throws Exception {
        long rahim = createGuest(RAFSAN, "Rahim");
        long outingId = createDraft(RAFSAN, "Burger night");

        // fill the draft; nothing exists in the ledger yet
        putAs(RAFSAN, "/api/outings/" + outingId, goldenBody(rahim))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.billTotal").value(2050.00))
            .andExpect(jsonPath("$.participants.length()").value(4));
        assertThat(debts.findAllByOutingId(outingId)).isEmpty();

        // the live preview agrees with what will be saved
        postAs(RAFSAN, "/api/outings/preview", goldenBody(rahim))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.userNets.length()").value(3));

        // submit: the outing becomes ACTIVE and the ledger creates the debts, in one transaction
        postAs(RAFSAN, "/api/outings/" + outingId + "/submit", "")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));
        assertThat(outings.findById(outingId).orElseThrow().getStatus()).isEqualTo(OutingStatus.ACTIVE);
        assertThat(countOf(OutingActivatedEvent.class)).isEqualTo(1);

        List<Debt> created = debts.findAllByOutingId(outingId);
        assertThat(created).extracting(Debt::getDebtorId, Debt::getCreditorId, Debt::getAmount)
            .containsExactlyInAnyOrder(
                Tuple.tuple(TANVIR, RAFSAN, new BigDecimal("200.00")),
                Tuple.tuple(NABILA, RAFSAN, new BigDecimal("300.00")));
        List<GuestDebt> guestOwes = guestDebts.findAllByOutingId(outingId);
        assertThat(guestOwes).hasSize(1);
        assertThat(guestOwes.get(0).getOwnerId()).isEqualTo(RAFSAN);
        assertThat(guestOwes.get(0).getAmount()).isEqualByComparingTo("450.00");

        // read it back
        getAs(RAFSAN, "/api/outings/" + outingId)
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
        getAs(RAFSAN, "/api/outings")
            .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == " + outingId + ")].status").value("ACTIVE"));

        // cancel: status, event and ledger follow
        postAs(RAFSAN, "/api/outings/" + outingId + "/cancel", "")
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(countOf(OutingCancelledEvent.class)).isEqualTo(1);
        assertThat(debts.findAllByOutingId(outingId)).hasSize(2)
            .allSatisfy(d -> assertThat(d.getStatus()).isEqualTo(DebtStatus.VOID));

        // a cancelled outing cannot be cancelled again or edited
        postAs(RAFSAN, "/api/outings/" + outingId + "/cancel", "").andExpect(status().isConflict());
        putAs(RAFSAN, "/api/outings/" + outingId, goldenBody(rahim)).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Submitting an incomplete draft is refused with every problem and leaves no trace")
    void incompleteDraftCannotBeSubmitted() throws Exception {
        long outingId = createDraft(RAFSAN + 10, "Half done");
        putAs(RAFSAN + 10, "/api/outings/" + outingId, contents(userRef(RAFSAN + 10),
            personalItem("Burger", "200.00", userRef(RAFSAN + 10)), payment(userRef(RAFSAN + 10), "150.00")))
            .andExpect(status().isOk());

        postAs(RAFSAN + 10, "/api/outings/" + outingId + "/submit", "")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.issues[0].code").value("PAID_NOT_EQUAL_BILL"));

        assertThat(outings.findById(outingId).orElseThrow().getStatus()).isEqualTo(OutingStatus.DRAFT);
        assertThat(debts.findAllByOutingId(outingId)).isEmpty();
    }
}
