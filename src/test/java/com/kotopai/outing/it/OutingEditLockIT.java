package com.kotopai.outing.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kotopai.ledger.domain.Debt;
import com.kotopai.ledger.domain.Payment;
import com.kotopai.ledger.repository.DebtRepository;
import com.kotopai.ledger.repository.PaymentRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An active outing may be edited only until somebody records a payment. The payment here is a real ledger record,
 * so this checks the whole chain: ledger payment, payment-lock adapter, service rule, HTTP status.
 */
class OutingEditLockIT extends OutingItSupport {

    private static final long CREATOR = 301;
    private static final long FRIEND = 302;

    @Autowired
    private DebtRepository debts;
    @Autowired
    private PaymentRepository payments;

    private static String body(long creator, long friend, String amount) {
        return contents(userRef(creator) + "," + userRef(friend),
            personalItem("Pizza", amount, userRef(friend)),
            payment(userRef(creator), amount));
    }

    @Test
    @DisplayName("Editing an active outing works until a payment is recorded, then it is locked")
    void editIsLockedAfterThePaymentIsRecorded() throws Exception {
        long outingId = createDraft(CREATOR, "Locked later");
        putAs(CREATOR, "/api/outings/" + outingId, body(CREATOR, FRIEND, "100.00")).andExpect(status().isOk());
        postAs(CREATOR, "/api/outings/" + outingId + "/submit", "").andExpect(status().isOk());

        // nobody has paid yet: an edit is accepted
        putAs(CREATOR, "/api/outings/" + outingId, body(CREATOR, FRIEND, "120.00"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.billTotal").value(120.00));

        // the friend pays the creator: a real payment in the ledger
        Debt debt = debts.findAllByOutingId(outingId).get(0);
        payments.save(Payment.confirmed(debt, new BigDecimal("50.00"), CREATOR));

        // now the outing is locked: 409 with a clear message, and the stored amounts are unchanged
        putAs(CREATOR, "/api/outings/" + outingId, body(CREATOR, FRIEND, "200.00"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(containsString("edit")));
        getAs(CREATOR, "/api/outings/" + outingId)
            .andExpect(status().isOk()).andExpect(jsonPath("$.billTotal").value(120.00));
    }

    @Test
    @DisplayName("A draft stays editable no matter what: the payment lock only applies to active outings")
    void draftsAreAlwaysEditable() throws Exception {
        long creator = CREATOR + 10;
        long friend = FRIEND + 10;
        long outingId = createDraft(creator, "Still a draft");

        putAs(creator, "/api/outings/" + outingId, body(creator, friend, "10.00")).andExpect(status().isOk());
        putAs(creator, "/api/outings/" + outingId, body(creator, friend, "20.00"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.billTotal").value(20.00));
        assertThat(debts.findAllByOutingId(outingId)).isEmpty();
    }
}
