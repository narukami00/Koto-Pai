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
	private final DelegationService delegations;

	public SettlementFacade(
			DebtSimplifier simplifier,
			DebtService debtService,
			DelegationService delegations) {
		this.simplifier = simplifier;
		this.debtService = debtService;
		this.delegations = delegations;
	}

	public List<com.kotopai.ledger.DelegationSuggestion> delegationSuggestions(long actorId) {
		return delegations.suggestions(actorId);
	}

	public DelegationResult delegate(
			long actorId,
			long incomingDebtId,
			long outgoingDebtId,
			BigDecimal amount) {
		return delegations.execute(actorId, incomingDebtId, outgoingDebtId, amount);
	}

	public List<Transfer> simplify(List<NetPosition> positions) {
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
