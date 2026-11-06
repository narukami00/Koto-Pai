package com.kotopai.outing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.kotopai.outing.engine.split.SplitType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Integration test: boots the whole application and saves and reloads an outing in two SEPARATE
 * transactions, so what comes back really was written to and read from the database.
 */
@SpringBootTest
class OutingPersistenceIT {

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager em;

    @Test
    @DisplayName("A full outing survives a real save and a fresh reload")
    void fullOutingRoundTrip() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        Long id = tx.execute(status -> {
            Guest rahim = new Guest(1L, "Rahim");
            em.persist(rahim);
            Outing outing = new Outing("Burger night IT", OutingCategory.EATING, LocalDate.of(2026, 11, 6), 1L);
            OutingParticipant rafsan = outing.addUserParticipant(1L);
            OutingParticipant tanvir = outing.addUserParticipant(2L);
            OutingParticipant guest = outing.addGuestParticipant(rahim);
            Item pizza = outing.addItem("Large pizza", new BigDecimal("1000.00"), SplitType.PERCENTAGE);
            pizza.addShare(rafsan, new BigDecimal("50"));
            pizza.addShare(tanvir, new BigDecimal("30"));
            pizza.addShare(guest, new BigDecimal("20"));
            outing.addContribution(rafsan, new BigDecimal("1000.00"));
            em.persist(outing);
            return outing.getId();
        });

        tx.executeWithoutResult(status -> {
            Outing loaded = em.find(Outing.class, id);
            assertThat(loaded.getParticipants()).hasSize(3);
            assertThat(loaded.getItems()).hasSize(1);
            assertThat(loaded.getItems().get(0).getShares())
                .extracting(ItemShare::getPercentage)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactlyInAnyOrder(new BigDecimal("50"), new BigDecimal("30"), new BigDecimal("20"));
            assertThat(loaded.getContributions()).hasSize(1);
        });
    }
}
