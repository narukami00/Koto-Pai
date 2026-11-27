package com.kotopai.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.kotopai.ledger.domain.Debt;
import com.kotopai.ledger.repository.DebtRepository;
import com.kotopai.ledger.repository.PaymentRepository;
import com.kotopai.ledger.service.PaymentService;

@SpringBootTest
class ConcurrentPaymentIT {

	@Autowired DebtRepository debts;
	@Autowired PaymentRepository payments;
	@Autowired PaymentService paymentService;

	private ExecutorService executor;

	@BeforeEach
	void setUp() {
		payments.deleteAll();
		debts.deleteAll();
		executor = Executors.newFixedThreadPool(2);
	}

	@AfterEach
	void tearDown() throws InterruptedException {
		executor.shutdownNow();
		executor.awaitTermination(5, TimeUnit.SECONDS);
	}

	@Test
	void simultaneousCreditorPaymentsCannotOverpayTheDebt() throws Exception {
		Debt debt = debts.saveAndFlush(Debt.delegated(2L, 1L, money("100.00")));
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);

		Future<Boolean> first = executor.submit(() -> recordAfterBarrier(debt.getId(), ready, start));
		Future<Boolean> second = executor.submit(() -> recordAfterBarrier(debt.getId(), ready, start));
		assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
		start.countDown();

		assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
				.containsExactlyInAnyOrder(true, false);
		Debt persisted = debts.findById(debt.getId()).orElseThrow();
		assertThat(persisted.getRemaining()).isEqualByComparingTo("40.00");
		assertThat(payments.findAllByDebtIdOrderByCreatedAtDesc(debt.getId())).hasSize(1);
	}

	private boolean recordAfterBarrier(long debtId, CountDownLatch ready, CountDownLatch start)
			throws InterruptedException {
		ready.countDown();
		if (!start.await(5, TimeUnit.SECONDS)) {
			return false;
		}
		try {
			paymentService.record(debtId, 1L, money("60.00"));
			return true;
		} catch (RuntimeException rejectedConcurrentAttempt) {
			return false;
		}
	}

	private static BigDecimal money(String amount) {
		return new BigDecimal(amount);
	}
}
