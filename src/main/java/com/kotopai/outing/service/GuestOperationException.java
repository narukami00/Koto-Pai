package com.kotopai.outing.service;

/** A guest operation was refused; the reason lets the REST layer pick the right HTTP status later. */
public class GuestOperationException extends RuntimeException {

    public enum Reason {
        /** No such guest, or it belongs to someone else (deliberately indistinguishable). */
        NOT_FOUND,
        /** The owner already has a guest with this name. */
        DUPLICATE_NAME,
        /** The guest took part in an outing, so deleting it would corrupt that outing. */
        IN_USE
    }

    private final Reason reason;

    public GuestOperationException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
