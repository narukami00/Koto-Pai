package com.kotopai.ledger.service;

public class LedgerOperationException extends RuntimeException {

	private final Reason reason;

	public LedgerOperationException(Reason reason, String message) {
		super(message);
		this.reason = reason;
	}

	public Reason getReason() {
		return reason;
	}

	public enum Reason {
		NOT_FOUND,
		FORBIDDEN,
		CONFLICT
	}
}
