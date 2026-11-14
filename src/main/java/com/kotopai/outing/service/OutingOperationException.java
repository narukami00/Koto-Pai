package com.kotopai.outing.service;

/** An outing operation was refused; the reason lets the REST layer pick the right HTTP status later. */
public class OutingOperationException extends RuntimeException {

    public enum Reason {
        /** No outing with this id. */
        NOT_FOUND,
        /** The actor is not allowed to change this outing (only its creator is). */
        FORBIDDEN,
        /** A participant is not a friend of the creator. */
        NOT_A_FRIEND,
        /** A guest does not exist or belongs to someone else. */
        UNKNOWN_GUEST,
        /** The operation is only allowed while the outing is still a draft. */
        DRAFT_ONLY
    }

    private final Reason reason;

    public OutingOperationException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
