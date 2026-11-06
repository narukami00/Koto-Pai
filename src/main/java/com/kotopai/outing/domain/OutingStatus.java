package com.kotopai.outing.domain;

/**
 * State pattern: each status decides which actions are legal and which status follows, so the rules live in one
 * place instead of scattered {@code if (status == ...)} checks in services and controllers.
 *
 * <pre>
 * DRAFT --submit--> ACTIVE --markSettled--> SETTLED
 *   |                 |
 *   +----cancel-------+--cancel--> CANCELLED      (SETTLED and CANCELLED are final)
 * </pre>
 *
 * An action that is not legal in the current status throws {@link IllegalOutingTransitionException}.
 */
public enum OutingStatus {

    DRAFT {
        @Override
        public OutingStatus submit() {
            return ACTIVE;
        }

        @Override
        public OutingStatus cancel() {
            return CANCELLED;
        }

        @Override
        public boolean canEdit(boolean paymentRecorded) {
            return true;
        }
    },

    ACTIVE {
        @Override
        public OutingStatus cancel() {
            return CANCELLED;
        }

        @Override
        public OutingStatus markSettled() {
            return SETTLED;
        }

        /** An active outing may be edited (debts are recomputed) only while no payment has been recorded. */
        @Override
        public boolean canEdit(boolean paymentRecorded) {
            return !paymentRecorded;
        }
    },

    SETTLED,

    CANCELLED;

    public OutingStatus submit() {
        throw new IllegalOutingTransitionException(this, "submit");
    }

    public OutingStatus cancel() {
        throw new IllegalOutingTransitionException(this, "cancel");
    }

    public OutingStatus markSettled() {
        throw new IllegalOutingTransitionException(this, "settle");
    }

    public boolean canEdit(boolean paymentRecorded) {
        return false;
    }
}
