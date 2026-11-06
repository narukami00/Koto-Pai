package com.kotopai.outing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.kotopai.outing.engine.split.SplitType;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

/** Fast mapping test: runs in the unit job against in-memory H2, rolled back after each test. */
@DataJpaTest
class OutingMappingTest {

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("A full outing can be saved and read back")
    void savesFullOuting() {
        Guest rahim = new Guest(1L, "Rahim");
        em.persist(rahim);

        Outing outing = new Outing("Burger night", OutingCategory.EATING, LocalDate.of(2026, 11, 6), 1L);
        OutingParticipant rafsan = outing.addUserParticipant(1L);
        OutingParticipant tanvir = outing.addUserParticipant(2L);
        OutingParticipant guest = outing.addGuestParticipant(rahim);
        Item pizza = outing.addItem("Large pizza", new BigDecimal("1000.00"), SplitType.PERCENTAGE);
        pizza.addShare(rafsan, new BigDecimal("50"));
        pizza.addShare(tanvir, new BigDecimal("30"));
        pizza.addShare(guest, new BigDecimal("20"));
        outing.addContribution(rafsan, new BigDecimal("1000.00"));
        em.persist(outing);
        em.flush();
        em.clear(); // forget everything in memory so the next read really comes from the database

        Outing loaded = em.find(Outing.class, outing.getId());

        assertThat(loaded.getStatus()).isEqualTo(OutingStatus.DRAFT);
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getParticipants()).hasSize(3);
        assertThat(loaded.getItems()).hasSize(1);
        assertThat(loaded.getItems().get(0).getShares()).hasSize(3);
        assertThat(loaded.getContributions()).extracting(Contribution::getAmountPaid)
            .containsExactly(new BigDecimal("1000.00"));
        assertThat(loaded.getParticipants().get(2).isGuest()).isTrue();
    }

    @Test
    @DisplayName("Removing an item deletes its shares too")
    void removingItemDeletesItsShares() {
        Outing outing = new Outing("Cleanup", OutingCategory.EATING, LocalDate.of(2026, 11, 6), 1L);
        OutingParticipant rafsan = outing.addUserParticipant(1L);
        OutingParticipant tanvir = outing.addUserParticipant(2L);
        Item pizza = outing.addItem("Pizza", new BigDecimal("500.00"), SplitType.EQUAL);
        pizza.addShare(rafsan, null);
        pizza.addShare(tanvir, null);
        em.persist(outing);
        em.flush();
        assertThat(shareCount()).isEqualTo(2);

        outing.removeItem(pizza);
        em.flush();

        assertThat(shareCount()).isZero();
    }

    private long shareCount() {
        return em.createQuery("select count(s) from ItemShare s", Long.class).getSingleResult();
    }
}
