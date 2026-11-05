package com.kotopai.ledger.service;

public interface DebtService {

	void generateDebts(Long outingId);

	void voidDebts(Long outingId);

	boolean hasRecordedPayments(Long outingId);
}
