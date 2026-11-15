package com.kotopai.ledger.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import com.kotopai.ledger.domain.Debt;
import com.kotopai.ledger.domain.DebtStatus;
import com.kotopai.ledger.domain.Payment;
import com.kotopai.ledger.domain.PaymentStatus;
import com.kotopai.ledger.event.PaymentConfirmedEvent;
import com.kotopai.ledger.repository.DebtRepository;
import com.kotopai.ledger.repository.PaymentRepository;
import com.kotopai.ledger.spi.OutingLedgerGateway;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

	@Mock DebtRepository debts;
	@Mock PaymentRepository payments;
	@Mock OutingLedgerGateway outings;
	@Mock ApplicationEventPublisher events;

	private PaymentService service;

	@BeforeEach
	void setUp() {
		service = new PaymentService(debts, payments, outings, events);
		lenient().when(payments.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> {
			Payment payment = invocation.getArgument(0);
			ReflectionTestUtils.setField(payment, "id", 99L);
			return payment;
		});
	}

	@Test
	void debtorRecordsPendingPaymentWithoutChangingRemaining() {
		Debt debt = debt("100.00");
		when(debts.findById(10L)).thenReturn(Optional.of(debt));

		PaymentResult result = service.record(10L, 2L, money("40.00"));

		assertThat(result.status()).isEqualTo(PaymentStatus.PENDING_CONFIRMATION);
		assertThat(debt.getRemaining()).isEqualByComparingTo("100.00");
		assertThat(debt.getStatus()).isEqualTo(DebtStatus.OPEN);
		verify(events, never()).publishEvent(any(PaymentConfirmedEvent.class));
		verify(outings, never()).markSettled(anyLong());
	}

	@Test
	void creditorRecordsReceivedPaymentAndLastPaymentSettlesOuting() {
		Debt debt = debt("100.00");
		when(debts.findById(10L)).thenReturn(Optional.of(debt));
		when(debts.findAllByOutingId(50L)).thenReturn(List.of(debt));

		PaymentResult result = service.record(10L, 1L, money("100.00"));

		assertThat(result.status()).isEqualTo(PaymentStatus.CONFIRMED);
		assertThat(debt.getRemaining()).isEqualByComparingTo("0.00");
		assertThat(debt.getStatus()).isEqualTo(DebtStatus.PAID);
		verify(outings).markSettled(50L);
	}

	@Test
	void onlyCreditorCanConfirmPendingPayment() {
		Debt debt = debt("100.00");
		Payment payment = Payment.pending(debt, money("25.00"), 2L);
		ReflectionTestUtils.setField(payment, "id", 20L);
		when(payments.findById(20L)).thenReturn(Optional.of(payment));

		assertThatThrownBy(() -> service.confirm(20L, 2L))
				.isInstanceOf(LedgerOperationException.class)
				.hasMessageContaining("creditor");
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING_CONFIRMATION);
	}

	private static Debt debt(String amount) {
		Debt debt = Debt.forOuting(50L, 2L, 1L, money(amount));
		ReflectionTestUtils.setField(debt, "id", 10L);
		return debt;
	}

	private static BigDecimal money(String value) {
		return new BigDecimal(value);
	}
}
