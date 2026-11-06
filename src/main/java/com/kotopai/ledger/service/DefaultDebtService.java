package com.kotopai.ledger.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kotopai.ledger.DebtSimplifier;
import com.kotopai.ledger.NetPosition;
import com.kotopai.ledger.Transfer;
import com.kotopai.ledger.domain.Debt;
import com.kotopai.ledger.domain.GuestDebt;
import com.kotopai.ledger.domain.GuestDebtStatus;
import com.kotopai.ledger.repository.DebtRepository;
import com.kotopai.ledger.repository.GuestDebtRepository;
import com.kotopai.ledger.repository.PaymentRepository;
import com.kotopai.ledger.spi.GuestNetPosition;
import com.kotopai.ledger.spi.OutingSettlementProvider;
import com.kotopai.ledger.spi.OutingSettlementSnapshot;
import com.kotopai.ledger.spi.UserNetPosition;

@Service
public class DefaultDebtService implements DebtService {

	private final DebtRepository debts;
	private final PaymentRepository payments;
	private final GuestDebtRepository guestDebts;
	private final OutingSettlementProvider settlements;
	private final DebtSimplifier simplifier;

	public DefaultDebtService(
			DebtRepository debts,
			PaymentRepository payments,
			GuestDebtRepository guestDebts,
			OutingSettlementProvider settlements,
			DebtSimplifier simplifier) {
		this.debts = debts;
		this.payments = payments;
		this.guestDebts = guestDebts;
		this.settlements = settlements;
		this.simplifier = simplifier;
	}

	@Override
	@Transactional
	public void generateDebts(Long outingId) {
		requireId(outingId, "outingId");
		if (hasRecordedPayments(outingId)) {
			throw new IllegalStateException("cannot regenerate debts after a payment has been recorded");
		}

		OutingSettlementSnapshot snapshot = settlements.settlementFor(outingId);
		if (snapshot == null || snapshot.outingId() != outingId) {
			throw new IllegalArgumentException("settlement snapshot does not match outing " + outingId);
		}

		voidExistingDebts(outingId);
		guestDebts.deleteAllByOutingIdAndStatus(outingId, GuestDebtStatus.OPEN);

		Map<Long, BigDecimal> effectiveNets = new LinkedHashMap<>();
		for (UserNetPosition position : snapshot.userPositions()) {
			effectiveNets.merge(position.userId(), position.net(), BigDecimal::add);
		}
		for (GuestNetPosition guest : snapshot.guestPositions()) {
			effectiveNets.merge(guest.ownerId(), guest.net(), BigDecimal::add);
		}

		List<NetPosition> positions = effectiveNets.entrySet().stream()
				.map(entry -> new NetPosition(entry.getKey().toString(), entry.getValue()))
				.toList();
		List<Transfer> transfers = simplifier.simplify(positions);
		List<Debt> generatedDebts = transfers.stream()
				.map(transfer -> Debt.forOuting(
						outingId,
						Long.valueOf(transfer.debtor()),
						Long.valueOf(transfer.creditor()),
						transfer.amount()))
				.toList();
		debts.saveAll(generatedDebts);

		List<GuestDebt> generatedGuestDebts = new ArrayList<>();
		for (GuestNetPosition guest : snapshot.guestPositions()) {
			BigDecimal ownerPerspectiveAmount = guest.net().negate();
			if (ownerPerspectiveAmount.signum() != 0) {
				generatedGuestDebts.add(new GuestDebt(
						outingId, guest.guestId(), guest.ownerId(), ownerPerspectiveAmount));
			}
		}
		guestDebts.saveAll(generatedGuestDebts);
	}

	@Override
	@Transactional
	public void voidDebts(Long outingId) {
		requireId(outingId, "outingId");
		voidExistingDebts(outingId);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean hasRecordedPayments(Long outingId) {
		requireId(outingId, "outingId");
		return payments.existsByDebtOutingId(outingId);
	}

	private void voidExistingDebts(Long outingId) {
		List<Debt> outingDebts = debts.findAllByOutingId(outingId);
		outingDebts.forEach(Debt::voidDebt);
		debts.saveAll(outingDebts);
	}

	private static void requireId(Long id, String field) {
		if (id == null || id <= 0) {
			throw new IllegalArgumentException(field + " must be positive");
		}
	}
}
