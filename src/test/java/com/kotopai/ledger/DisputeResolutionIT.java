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
import com.kotopai.outing.domain.Outing;
import com.kotopai.outing.domain.OutingCategory;
import com.kotopai.outing.engine.split.SplitType;
import com.kotopai.outing.service.OutingCommands.ContributionInput;
import com.kotopai.outing.service.OutingCommands.CreateOuting;
import com.kotopai.outing.service.OutingCommands.ItemInput;
import com.kotopai.outing.service.OutingCommands.OutingContents;
import com.kotopai.outing.service.OutingCommands.ParticipantKey;
import com.kotopai.outing.service.OutingCommands.ShareInput;
import com.kotopai.outing.service.OutingService;

@SpringBootTest
class DisputeResolutionIT {

	private static final long CREATOR = 701L;
	private static final long PARTICIPANT = 702L;

	@Autowired OutingService outings;
	@Autowired DisputeService disputes;
	@Autowired DebtRepository debts;

	@Test
	void creatorEditAndParticipantRemovalRecomputeDisputedDebt() {
		ParticipantKey creator = ParticipantKey.user(CREATOR);
		ParticipantKey participant = ParticipantKey.user(PARTICIPANT);
		Outing outing = outings.create(CREATOR,
				new CreateOuting("Disputed dinner", OutingCategory.EATING, LocalDate.of(2026, 11, 15)));
		outings.updateContents(CREATOR, outing.getId(), new OutingContents(
				List.of(creator, participant),
				List.of(personalItem("Dinner", participant)),
				List.of(new ContributionInput(creator, money("100.00")))));
		outings.submit(CREATOR, outing.getId());

		Debt disputed = debts.findAllByOutingId(outing.getId()).get(0);
		disputes.dispute(disputed.getId(), PARTICIPANT, "I did not attend");
		assertThat(debts.findById(disputed.getId()).orElseThrow().getStatus()).isEqualTo(DebtStatus.DISPUTED);

		outings.updateContents(CREATOR, outing.getId(), new OutingContents(
				List.of(creator),
				List.of(personalItem("Dinner", creator)),
				List.of(new ContributionInput(creator, money("100.00")))));

		assertThat(debts.findAllByOutingId(outing.getId()))
				.singleElement()
				.extracting(Debt::getStatus)
				.isEqualTo(DebtStatus.VOID);
	}

	private static ItemInput personalItem(String name, ParticipantKey consumer) {
		return new ItemInput(name, money("100.00"), SplitType.PERSONAL,
				List.of(new ShareInput(consumer, null)));
	}

	private static BigDecimal money(String amount) {
		return new BigDecimal(amount);
	}
}
