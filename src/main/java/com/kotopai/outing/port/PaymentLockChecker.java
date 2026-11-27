package com.kotopai.outing.port;

/**
 * Port: an active outing may be edited only while nobody has recorded a payment on its debts. That fact lives in
 * the ledger, so the outing feature asks through this interface instead of touching ledger classes.
 */
public interface PaymentLockChecker {

    /** True if any payment has been recorded against the debts of this outing. */
    boolean hasRecordedPayments(Long outingId);
}
