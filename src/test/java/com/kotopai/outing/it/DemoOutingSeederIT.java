package com.kotopai.outing.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.kotopai.ledger.service.LedgerService;
import com.kotopai.ledger.service.LedgerSummary;
import com.kotopai.outing.config.DemoOutingSeeder;
import com.kotopai.outing.domain.Outing;
import com.kotopai.outing.domain.OutingStatus;
import com.kotopai.outing.service.OutingService;
import com.kotopai.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Boots the app with the h2 profile (which runs both seeders) and checks what a fresh local run contains. */
@SpringBootTest
@ActiveProfiles("h2")
class DemoOutingSeederIT {

    @Autowired
    private UserRepository users;

    @Autowired
    private OutingService outings;

    @Autowired
    private LedgerService ledger;

    @Autowired
    private DemoOutingSeeder seeder;

    private long id(String userCode) {
        return users.findByUserCode(userCode).orElseThrow().getId();
    }

    @Test
    @Transactional(readOnly = true)
    @DisplayName("A fresh h2 run has two active outings and one private draft")
    void seedsOutings() {
        List<Outing> rafsan = outings.listVisibleTo(id("rafsan10"), PageRequest.of(0, 20));

        assertThat(rafsan).extracting(Outing::getTitle)
            .containsExactlyInAnyOrder("Burger night", "Cox's Bazar trip", "Movie night");
        assertThat(rafsan).filteredOn(o -> o.getStatus() == OutingStatus.ACTIVE).hasSize(2);
        assertThat(rafsan).filteredOn(o -> o.getStatus() == OutingStatus.DRAFT)
            .extracting(Outing::getTitle).containsExactly("Movie night");

        List<Outing> samiha = outings.listVisibleTo(id("samiha"), PageRequest.of(0, 20));
        assertThat(samiha).extracting(Outing::getTitle).containsExactly("Cox's Bazar trip");
    }

    @Test
    @DisplayName("Submitting the seeded outings produced the expected debts")
    void seedsDebts() {
        // Rafsan: Rahim (guest) owes 550 from burger night, and Rafsan owes Tanvir 2000 for the trip.
        LedgerSummary rafsan = ledger.summaryFor(id("rafsan10"));
        assertThat(rafsan.totalReceivable()).isEqualByComparingTo("550.00");
        assertThat(rafsan.totalPayable()).isEqualByComparingTo("2000.00");

        // Nabila owes Tanvir 200 from burger night; Samiha owes Tanvir 2000 for the trip.
        assertThat(ledger.summaryFor(id("nabila_r")).totalPayable()).isEqualByComparingTo("200.00");
        assertThat(ledger.summaryFor(id("samiha")).totalPayable()).isEqualByComparingTo("2000.00");
    }

    @Test
    @DisplayName("Running the seeder again changes nothing")
    void seederIsIdempotent() {
        int before = outings.listVisibleTo(id("rafsan10"), PageRequest.of(0, 50)).size();

        seeder.run();

        assertThat(outings.listVisibleTo(id("rafsan10"), PageRequest.of(0, 50))).hasSize(before);
    }
}
