package com.kotopai.outing.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kotopai.ledger.service.DebtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class PortAdaptersTest {

    @Test
    @DisplayName("Stub friendship: different users are friends, a user is not their own friend")
    void stubFriendship() {
        StubFriendshipChecker stub = new StubFriendshipChecker();

        assertThat(stub.areFriends(1L, 2L)).isTrue();
        assertThat(stub.areFriends(1L, 1L)).isFalse();
        assertThatThrownBy(() -> stub.areFriends(null, 2L)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> stub.areFriends(1L, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Payment lock asks the ledger debt service, resolved lazily at call time")
    @SuppressWarnings("unchecked")
    void paymentLockDelegatesToLedger() {
        DebtService debts = mock(DebtService.class);
        ObjectProvider<DebtService> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(debts);
        when(debts.hasRecordedPayments(7L)).thenReturn(true);
        when(debts.hasRecordedPayments(8L)).thenReturn(false);
        LedgerPaymentLockChecker checker = new LedgerPaymentLockChecker(provider);

        assertThat(checker.hasRecordedPayments(7L)).isTrue();
        assertThat(checker.hasRecordedPayments(8L)).isFalse();
        verify(debts).hasRecordedPayments(7L);
    }

    @Test
    @DisplayName("Payment lock refuses null arguments")
    @SuppressWarnings("unchecked")
    void paymentLockNullSafe() {
        ObjectProvider<DebtService> provider = mock(ObjectProvider.class);

        assertThatThrownBy(() -> new LedgerPaymentLockChecker(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new LedgerPaymentLockChecker(provider).hasRecordedPayments(null))
            .isInstanceOf(NullPointerException.class);
    }
}
