package com.kotopai.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.kotopai.ledger.domain.Debt;
import com.kotopai.ledger.domain.DebtStatus;
import com.kotopai.ledger.repository.DebtRepository;
import com.kotopai.ledger.service.DisputeService;
import com.kotopai.ledger.service.LedgerService;
import com.kotopai.ledger.service.PaymentResult;
import com.kotopai.ledger.service.PaymentService;
import com.kotopai.outing.domain.Outing;
import com.kotopai.outing.domain.OutingCategory;
import com.kotopai.outing.domain.OutingStatus;
import com.kotopai.outing.engine.split.SplitType;
import com.kotopai.outing.service.OutingCommands.ContributionInput;
import com.kotopai.outing.service.OutingCommands.CreateOuting;
import com.kotopai.outing.service.OutingCommands.ItemInput;
import com.kotopai.outing.service.OutingCommands.OutingContents;
import com.kotopai.outing.service.OutingCommands.ParticipantKey;
import com.kotopai.outing.service.OutingCommands.ShareInput;
import com.kotopai.outing.service.OutingService;

@SpringBootTest
class LedgerFullFlowIT {

	private static final long CREATOR = 81001L;
	private static final long PARTICIPANT = 81002L;

	@Autowired OutingService outings;
	@Autowired DisputeService disputes;
	@Autowired PaymentService payments;
	@Autowired LedgerService ledger;
	@Autowired DebtRepository debts;

	@Test
	void outingDebtCanBeDisputedRegeneratedPaidAndSettled() {
		ParticipantKey creator = ParticipantKey.user(CREATOR);
		ParticipantKey participant = ParticipantKey.user(PARTICIPANT);
		OutingContents burgerNight = new OutingContents(
				List.of(creator, participant),
				List.of(new ItemInput("Burger", money("100.00"), SplitType.PERSONAL,
						List.of(new ShareInput(participant, null)))),
				List.of(new ContributionInput(creator, money("100.00"))));

		Outing outing = outings.create(CREATOR,
				new CreateOuting("Phase 4 burger night", OutingCategory.EATING, LocalDate.of(2026, 11, 25)));
		outings.updateContents(CREATOR, outing.getId(), burgerNight);
		outings.submit(CREATOR, outing.getId());

		Debt original = activeDebt(outing.getId());
		assertThat(original.getDebtorId()).isEqualTo(PARTICIPANT);
		assertThat(original.getCreditorId()).isEqualTo(CREATOR);
		assertThat(original.getRemaining()).isEqualByComparingTo("100.00");

		disputes.dispute(original.getId(), PARTICIPANT, "Please recalculate my share");
		assertThat(ledger.summaryFor(CREATOR).totalReceivable()).isEqualByComparingTo("0.00");

		outings.updateContents(CREATOR, outing.getId(), burgerNight);
		assertThat(debts.findById(original.getId()).orElseThrow().getStatus()).isEqualTo(DebtStatus.VOID);
		Debt regenerated = activeDebt(outing.getId());
		assertThat(regenerated.getId()).isNotEqualTo(original.getId());
		assertThat(ledger.summaryFor(CREATOR).totalReceivable()).isEqualByComparingTo("100.00");

		PaymentResult pending = payments.record(regenerated.getId(), PARTICIPANT, money("40.00"));
		payments.confirm(pending.id(), CREATOR);
		assertThat(debts.findById(regenerated.getId()).orElseThrow().getStatus())
				.isEqualTo(DebtStatus.PARTIALLY_PAID);

		payments.record(regenerated.getId(), CREATOR, money("60.00"));
		Debt paid = debts.findById(regenerated.getId()).orElseThrow();
		assertThat(paid.getStatus()).isEqualTo(DebtStatus.PAID);
		assertThat(paid.getRemaining()).isEqualByComparingTo("0.00");
		assertThat(ledger.summaryFor(CREATOR).net()).isEqualByComparingTo("0.00");
		assertThat(outings.view(CREATOR, outing.getId()).getStatus()).isEqualTo(OutingStatus.SETTLED);
	}

	private Debt activeDebt(long outingId) {
		return debts.findAllByOutingId(outingId).stream()
				.filter(debt -> debt.getStatus().contributesToBalance())
				.reduce((first, second) -> {
					throw new AssertionError("Expected one active debt for outing " + outingId);
				})
				.orElseThrow();
	}

	private static BigDecimal money(String amount) {
		return new BigDecimal(amount);
	}
}
