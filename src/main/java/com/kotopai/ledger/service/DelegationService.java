package com.kotopai.ledger.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kotopai.friend.service.FriendService;
import com.kotopai.ledger.DelegationPlan;
import com.kotopai.ledger.DelegationRules;
import com.kotopai.ledger.DelegationSuggestion;
import com.kotopai.ledger.DelegationSuggester;
import com.kotopai.ledger.OpenDebt;
import com.kotopai.ledger.domain.Debt;
import com.kotopai.ledger.domain.Delegation;
import com.kotopai.ledger.domain.DelegationStatus;
import com.kotopai.ledger.event.DelegationExecutedEvent;
import com.kotopai.ledger.event.DelegationReversedEvent;
import com.kotopai.ledger.repository.DebtRepository;
import com.kotopai.ledger.repository.DelegationRepository;
import com.kotopai.ledger.repository.PaymentRepository;
import com.kotopai.ledger.service.LedgerOperationException.Reason;

@Service
public class DelegationService {

	private final DebtRepository debts;
	private final DelegationRepository delegations;
	private final PaymentRepository payments;
	private final FriendService friends;
	private final ApplicationEventPublisher events;

	public DelegationService(
			DebtRepository debts,
			DelegationRepository delegations,
			PaymentRepository payments,
			FriendService friends,
			ApplicationEventPublisher events) {
		this.debts = debts;
		this.delegations = delegations;
		this.payments = payments;
		this.friends = friends;
		this.events = events;
	}

	@Transactional(readOnly = true)
	public List<DelegationSuggestion> suggestions(long actorId) {
		Map<Long, Debt> unique = new LinkedHashMap<>();
		for (Debt debt : debts.findAllByDebtorIdOrCreditorIdOrderByCreatedAtDesc(actorId, actorId)) {
			if (debt.getStatus().contributesToBalance()) {
				unique.put(debt.getId(), debt);
			}
		}
		List<OpenDebt> openDebts = unique.values().stream().map(DelegationService::openDebt).toList();
		return new DelegationSuggester((userA, userB) -> friends.areFriends(userA, userB))
				.suggest(openDebts).stream()
				.filter(suggestion -> suggestion.middlemanId() == actorId)
				.toList();
	}

	@Transactional
	public DelegationResult execute(
			long actorId,
			long incomingDebtId,
			long outgoingDebtId,
			BigDecimal amount) {
		Debt incoming = debt(incomingDebtId);
		Debt outgoing = debt(outgoingDebtId);
		if (!incoming.getCreditorId().equals(actorId) || !outgoing.getDebtorId().equals(actorId)) {
			throw new LedgerOperationException(
					Reason.FORBIDDEN, "only the middleman owed by one user and owing the other can delegate");
		}
		if (!incoming.getStatus().contributesToBalance() || !outgoing.getStatus().contributesToBalance()) {
			throw new LedgerOperationException(Reason.CONFLICT, "delegation requires two open debts");
		}

		DelegationPlan plan = new DelegationRules((userA, userB) -> friends.areFriends(userA, userB))
				.plan(openDebt(incoming), openDebt(outgoing), actorId, amount);
		incoming.reduceForDelegation(plan.incomingDebt().before().subtract(plan.incomingDebt().after()));
		outgoing.reduceForDelegation(plan.outgoingDebt().before().subtract(plan.outgoingDebt().after()));

		Debt delegatedDebt = debts.saveAndFlush(Debt.delegated(
				plan.delegatedDebt().debtorId(),
				plan.delegatedDebt().creditorId(),
				plan.delegatedDebt().amount()));
		Delegation delegation = delegations.saveAndFlush(Delegation.executed(
				incoming.getId(), outgoing.getId(), delegatedDebt.getId(),
				delegatedDebt.getDebtorId(), actorId, delegatedDebt.getCreditorId(), delegatedDebt.getAmount()));
		events.publishEvent(new DelegationExecutedEvent(
				delegation.getId(), incoming.getId(), outgoing.getId(), delegatedDebt.getId(),
				delegation.getDebtorId(), actorId, delegation.getCreditorId(), delegation.getAmount()));
		return DelegationResult.from(delegation);
	}

	@Transactional
	public DebtHistoryItem reverseDelegatedDebt(Debt delegatedDebt, long actorId, String reason) {
		Delegation delegation = delegations.findByDelegatedDebtId(delegatedDebt.getId())
				.orElseThrow(() -> new LedgerOperationException(Reason.NOT_FOUND, "Delegation record not found"));
		if (!delegation.getDebtorId().equals(actorId) && !delegation.getCreditorId().equals(actorId)) {
			throw new LedgerOperationException(
					Reason.FORBIDDEN, "only the delegated debtor or creditor can dispute this delegation");
		}
		if (delegation.getStatus() != DelegationStatus.EXECUTED) {
			throw new LedgerOperationException(Reason.CONFLICT, "delegation is already reversed");
		}
		if (payments.existsByDebtId(delegatedDebt.getId())
				|| !delegatedDebt.getStatus().contributesToBalance()
				|| delegatedDebt.getRemaining().compareTo(delegation.getAmount()) != 0) {
			throw new LedgerOperationException(
					Reason.CONFLICT, "delegation cannot be reversed after a payment or another state change");
		}

		Debt incoming = debt(delegation.getIncomingDebtId());
		Debt outgoing = debt(delegation.getOutgoingDebtId());
		try {
			incoming.restoreFromDelegation(delegation.getAmount());
			outgoing.restoreFromDelegation(delegation.getAmount());
			delegatedDebt.voidDebt();
			delegation.reverse(actorId, reason);
		} catch (IllegalStateException | IllegalArgumentException exception) {
			throw new LedgerOperationException(Reason.CONFLICT, exception.getMessage());
		}
		debts.flush();
		delegations.flush();
		events.publishEvent(new DelegationReversedEvent(
				delegation.getId(), delegatedDebt.getId(), actorId,
				delegation.getDebtorId(), delegation.getMiddlemanId(), delegation.getCreditorId(),
				delegation.getAmount(), delegation.getReversalReason()));
		return LedgerService.toHistoryItem(delegatedDebt);
	}

	private Debt debt(long debtId) {
		return debts.findById(debtId)
				.orElseThrow(() -> new LedgerOperationException(Reason.NOT_FOUND, "Debt not found"));
	}

	private static OpenDebt openDebt(Debt debt) {
		return new OpenDebt(debt.getId(), debt.getDebtorId(), debt.getCreditorId(), debt.getRemaining());
	}
}
