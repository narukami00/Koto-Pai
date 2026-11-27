package com.kotopai.outing.domain;

/** Thrown when an action is not allowed in the outing's current status, e.g. submitting a cancelled outing. */
public class IllegalOutingTransitionException extends IllegalStateException {

    private final OutingStatus from;
    private final String action;

    public IllegalOutingTransitionException(OutingStatus from, String action) {
        super("Cannot " + action + " an outing that is " + from);
        this.from = from;
        this.action = action;
    }

    public OutingStatus getFrom() {
        return from;
    }

    public String getAction() {
        return action;
    }
}
