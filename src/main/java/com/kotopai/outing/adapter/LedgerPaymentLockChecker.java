package com.kotopai.outing.adapter;

import com.kotopai.ledger.service.DebtService;
import com.kotopai.outing.port.PaymentLockChecker;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Adapter: answers the outing port by asking Member 3 ledger. The debt service is looked up lazily through an
 * {@link ObjectProvider} because the ledger will itself depend on the outing feature (settlement provider, M2-29);
 * resolving it at call time avoids a circular dependency at start-up.
 */
@Component
public class LedgerPaymentLockChecker implements PaymentLockChecker {

    private final ObjectProvider<DebtService> debtService;

    public LedgerPaymentLockChecker(ObjectProvider<DebtService> debtService) {
        this.debtService = Objects.requireNonNull(debtService, "debtService must not be null");
    }

    @Override
    public boolean hasRecordedPayments(Long outingId) {
        Objects.requireNonNull(outingId, "outingId must not be null");
        return debtService.getObject().hasRecordedPayments(outingId);
    }
}
