package com.kotopai.ledger.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.kotopai.ledger.domain.GuestDebt;
import com.kotopai.ledger.domain.GuestDebtStatus;
import com.kotopai.ledger.repository.GuestDebtRepository;

@ExtendWith(MockitoExtension.class)
class GuestDebtServiceTest {

	@Mock GuestDebtRepository guestDebts;

	private GuestDebtService service;

	@BeforeEach
	void setUp() {
		service = new GuestDebtService(guestDebts);
	}

	@Test
	void ownerSettlesGuestDebtWithoutDeletingHistory() {
		GuestDebt debt = debt();
		when(guestDebts.findById(10L)).thenReturn(Optional.of(debt));

		GuestDebtResult result = service.settle(10L, 1L);

		assertThat(result.status()).isEqualTo(GuestDebtStatus.SETTLED);
		assertThat(result.settledAt()).isNotNull();
	}

	@Test
	void nonOwnerCannotSettleGuestDebt() {
		when(guestDebts.findById(10L)).thenReturn(Optional.of(debt()));

		assertThatThrownBy(() -> service.settle(10L, 2L))
				.isInstanceOf(LedgerOperationException.class)
				.hasMessageContaining("owner");
	}

	private static GuestDebt debt() {
		GuestDebt debt = new GuestDebt(50L, 8L, 1L, new BigDecimal("30.00"));
		ReflectionTestUtils.setField(debt, "id", 10L);
		return debt;
	}
}
