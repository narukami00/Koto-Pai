package com.kotopai.ledger.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kotopai.ledger.domain.Debt;
import com.kotopai.ledger.domain.GuestDebt;
import com.kotopai.ledger.domain.GuestDebtStatus;
import com.kotopai.ledger.repository.DebtRepository;
import com.kotopai.ledger.repository.GuestDebtRepository;

@Service
public class LedgerService {

	private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

	private final DebtRepository debts;
	private final GuestDebtRepository guestDebts;

	public LedgerService(DebtRepository debts, GuestDebtRepository guestDebts) {
		this.debts = debts;
		this.guestDebts = guestDebts;
	}

	@Transactional(readOnly = true)
	public LedgerSummary summaryFor(long userId) {
		requireUserId(userId);
		Map<CounterpartyKey, BigDecimal> balances = new LinkedHashMap<>();
		BigDecimal receivable = ZERO;
		BigDecimal payable = ZERO;

		for (Debt debt : debts.findAllByDebtorIdOrCreditorIdOrderByCreatedAtDesc(userId, userId)) {
			if (!debt.getStatus().contributesToBalance()) {
				continue;
			}
			if (debt.getCreditorId() == userId) {
				receivable = receivable.add(debt.getRemaining());
				merge(balances, new CounterpartyKey(debt.getDebtorId(), CounterpartyType.USER), debt.getRemaining());
			} else {
				payable = payable.add(debt.getRemaining());
				merge(balances, new CounterpartyKey(debt.getCreditorId(), CounterpartyType.USER), debt.getRemaining().negate());
			}
		}

		for (GuestDebt guestDebt : guestDebts.findAllByOwnerIdAndStatus(userId, GuestDebtStatus.OPEN)) {
			BigDecimal amount = guestDebt.getAmount();
			if (amount.signum() > 0) {
				receivable = receivable.add(amount);
			} else {
				payable = payable.add(amount.abs());
			}
			merge(balances, new CounterpartyKey(guestDebt.getGuestId(), CounterpartyType.GUEST), amount);
		}

		List<LedgerBalance> entries = balances.entrySet().stream()
				.filter(entry -> entry.getValue().signum() != 0)
				.map(entry -> new LedgerBalance(
						entry.getKey().id(), entry.getKey().type(), entry.getValue()))
				.sorted(Comparator.comparing((LedgerBalance balance) -> balance.balance().abs()).reversed()
						.thenComparing(LedgerBalance::counterpartyId))
				.toList();
		return new LedgerSummary(userId, receivable, payable, receivable.subtract(payable), entries);
	}

	@Transactional(readOnly = true)
	public List<DebtHistoryItem> historyBetween(long userId, long friendId) {
		requireUserId(userId);
		requireUserId(friendId);
		if (userId == friendId) {
			throw new IllegalArgumentException("friendId must identify a different user");
		}

		List<DebtHistoryItem> history = new ArrayList<>();
		for (Debt debt : debts.findAllByDebtorIdOrCreditorIdOrderByCreatedAtDesc(userId, userId)) {
			boolean betweenUsers = (debt.getDebtorId() == userId && debt.getCreditorId() == friendId)
					|| (debt.getDebtorId() == friendId && debt.getCreditorId() == userId);
			if (betweenUsers) {
				history.add(toHistoryItem(debt));
			}
		}
		return List.copyOf(history);
	}

	@Transactional(readOnly = true)
	public List<DebtHistoryItem> debtsFor(long userId) {
		requireUserId(userId);
		return debts.findAllByDebtorIdOrCreditorIdOrderByCreatedAtDesc(userId, userId).stream()
				.map(LedgerService::toHistoryItem)
				.toList();
	}

	private static DebtHistoryItem toHistoryItem(Debt debt) {
		return new DebtHistoryItem(
				debt.getId(),
				debt.getOutingId(),
				debt.getDebtorId(),
				debt.getCreditorId(),
				debt.getAmount(),
				debt.getRemaining(),
				debt.getStatus(),
				debt.getOrigin(),
				debt.getCreatedAt());
	}

	private static void merge(
			Map<CounterpartyKey, BigDecimal> balances,
			CounterpartyKey key,
			BigDecimal amount) {
		balances.merge(key, amount, BigDecimal::add);
	}

	private static void requireUserId(long userId) {
		if (userId <= 0) {
			throw new IllegalArgumentException("userId must be positive");
		}
	}

	private record CounterpartyKey(long id, CounterpartyType type) {
	}
}
