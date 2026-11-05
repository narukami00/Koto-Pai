package com.kotopai.ledger.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.kotopai.ledger.DebtSimplifier;
import com.kotopai.ledger.DelegationPlan;
import com.kotopai.ledger.DelegationRules;
import com.kotopai.ledger.FriendshipLookup;
import com.kotopai.ledger.NetPosition;
import com.kotopai.ledger.OpenDebt;
import com.kotopai.ledger.Transfer;

/** Facade over simplification, persisted generation, and delegation calculation. */
@Service
public class SettlementFacade {

	private final DebtSimplifier simplifier;
	private final DebtService debtService;

	public SettlementFacade(DebtSimplifier simplifier, DebtService debtService) {
		this.simplifier = simplifier;
		this.debtService = debtService;
	}

	public <T> List<Transfer<T>> simplify(List<NetPosition<T>> positions) {
		return simplifier.simplify(positions);
	}

	public void generateDebts(Long outingId) {
		debtService.generateDebts(outingId);
	}

	public DelegationPlan planDelegation(
			OpenDebt incoming,
			OpenDebt outgoing,
			long middlemanId,
			BigDecimal amount,
			FriendshipLookup friendships) {
		return new DelegationRules(friendships).plan(incoming, outgoing, middlemanId, amount);
	}
}
