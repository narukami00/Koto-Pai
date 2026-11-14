package com.kotopai.outing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kotopai.ledger.domain.Debt;
import com.kotopai.ledger.domain.DebtStatus;
import com.kotopai.ledger.event.OutingActivatedEvent;
import com.kotopai.ledger.event.OutingCancelledEvent;
import com.kotopai.ledger.repository.DebtRepository;
import com.kotopai.outing.domain.Outing;
import com.kotopai.outing.domain.OutingCategory;
import com.kotopai.outing.engine.split.SplitType;
import com.kotopai.outing.engine.validation.EngineValidationException;
import com.kotopai.outing.service.OutingCommands.ContributionInput;
import com.kotopai.outing.service.OutingCommands.CreateOuting;
import com.kotopai.outing.service.OutingCommands.ItemInput;
import com.kotopai.outing.service.OutingCommands.OutingContents;
import com.kotopai.outing.service.OutingCommands.ParticipantKey;
import com.kotopai.outing.service.OutingCommands.ShareInput;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * Whole-application test of the outing to ledger hand-off: submitting really produces debts (written by the ledger
 * in response to the event, using the engine through the settlement adapter), and cancelling voids them.
 */
@SpringBootTest
@RecordApplicationEvents
class OutingEventsIT {

    @Autowired
    private OutingService service;

    @Autowired
    private DebtRepository debts;

    @Autowired
    private ApplicationEvents events;

    private Outing draftWithBurgerNight(long creator, long friendA, long friendB) {
        Outing outing = service.create(creator, new CreateOuting("Burger night", OutingCategory.EATING, LocalDate.of(2026, 11, 14)));
        ParticipantKey c = ParticipantKey.user(creator);
        ParticipantKey a = ParticipantKey.user(friendA);
        ParticipantKey b = ParticipantKey.user(friendB);
        service.updateContents(creator, outing.getId(), new OutingContents(
            List.of(c, a, b),
            List.of(
                new ItemInput("Burger", new BigDecimal("200.00"), SplitType.PERSONAL, List.of(new ShareInput(c, null))),
                new ItemInput("Pizza", new BigDecimal("500.00"), SplitType.PERSONAL, List.of(new ShareInput(a, null))),
                new ItemInput("Cold coffee", new BigDecimal("200.00"), SplitType.PERSONAL, List.of(new ShareInput(b, null))),
                new ItemInput("Large pizza", new BigDecimal("1000.00"), SplitType.PERCENTAGE, List.of(
                    new ShareInput(c, new BigDecimal("50")), new ShareInput(a, new BigDecimal("30")),
                    new ShareInput(b, new BigDecimal("20"))))),
            List.of(new ContributionInput(c, new BigDecimal("1400.00")), new ContributionInput(a, new BigDecimal("500.00")))));
        return outing;
    }

    private long countOf(Class<?> type) {
        return events.stream()
            .filter(e -> e instanceof PayloadApplicationEvent<?> p && type.isInstance(p.getPayload()))
            .count();
    }

    @Test
    @DisplayName("Submit publishes one OutingActivatedEvent and the ledger creates the right debts")
    void submitCreatesDebts() {
        Outing outing = draftWithBurgerNight(11, 12, 13);

        service.submit(11L, outing.getId());

        assertThat(countOf(OutingActivatedEvent.class)).isEqualTo(1);
        List<Debt> created = debts.findAllByOutingId(outing.getId());
        assertThat(created).hasSize(2);
        assertThat(created).allSatisfy(d -> {
            assertThat(d.getCreditorId()).isEqualTo(11L);
            assertThat(d.getStatus()).isEqualTo(DebtStatus.OPEN);
        });
        assertThat(created).extracting(Debt::getDebtorId, Debt::getAmount)
            .containsExactlyInAnyOrder(
                Tuple.tuple(12L, new BigDecimal("300.00")),
                Tuple.tuple(13L, new BigDecimal("400.00")));
    }

    @Test
    @DisplayName("Cancel publishes OutingCancelledEvent and the ledger voids the debts")
    void cancelVoidsDebts() {
        Outing outing = draftWithBurgerNight(21, 22, 23);
        service.submit(21L, outing.getId());

        service.cancel(21L, outing.getId());

        assertThat(countOf(OutingCancelledEvent.class)).isGreaterThanOrEqualTo(1);
        assertThat(debts.findAllByOutingId(outing.getId())).isNotEmpty()
            .allSatisfy(d -> assertThat(d.getStatus()).isEqualTo(DebtStatus.VOID));
    }

    @Test
    @DisplayName("A failed submit publishes nothing and creates no debts")
    void failedSubmitLeavesNoTrace() {
        Outing outing = service.create(31L, new CreateOuting("Empty", OutingCategory.OTHER, LocalDate.of(2026, 11, 14)));
        long activatedBefore = countOf(OutingActivatedEvent.class);

        assertThatThrownBy(() -> service.submit(31L, outing.getId())).isInstanceOf(EngineValidationException.class);

        assertThat(countOf(OutingActivatedEvent.class)).isEqualTo(activatedBefore);
        assertThat(debts.findAllByOutingId(outing.getId())).isEmpty();
    }
}
