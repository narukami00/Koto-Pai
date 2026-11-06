package com.kotopai.outing.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.kotopai.outing.domain.Guest;
import com.kotopai.outing.domain.Outing;
import com.kotopai.outing.domain.OutingCategory;
import com.kotopai.outing.domain.OutingStatus;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class OutingRepositoryTest {

    private static final long RAFSAN = 1L;
    private static final long TANVIR = 2L;
    private static final long STRANGER = 99L;

    @Autowired
    private OutingRepository outings;

    @Autowired
    private EntityManager em;

    private Outing outing(String title, LocalDate date, OutingStatus status, Long... participantUserIds) {
        Outing o = new Outing(title, OutingCategory.EATING, date, RAFSAN);
        for (Long id : participantUserIds) {
            o.addUserParticipant(id);
        }
        Outing saved = outings.saveAndFlush(o);
        if (status != OutingStatus.DRAFT) {
            em.createQuery("update Outing o set o.status = :s where o.id = :id")
                .setParameter("s", status).setParameter("id", saved.getId()).executeUpdate();
        }
        em.clear();
        return saved;
    }

    @Test
    @DisplayName("The creator sees their draft; a participant does not see it until it is active")
    void draftsAreCreatorOnly() {
        outing("Draft trip", LocalDate.of(2026, 11, 1), OutingStatus.DRAFT, RAFSAN, TANVIR);

        assertThat(outings.findVisibleTo(RAFSAN, Pageable.unpaged())).hasSize(1);
        assertThat(outings.findVisibleTo(TANVIR, Pageable.unpaged())).isEmpty();
    }

    @Test
    @DisplayName("Once active, participants see it; strangers never do")
    void activeOutingsAreVisibleToParticipants() {
        outing("Burger night", LocalDate.of(2026, 11, 2), OutingStatus.ACTIVE, RAFSAN, TANVIR);

        assertThat(outings.findVisibleTo(TANVIR, Pageable.unpaged())).extracting(Outing::getTitle)
            .containsExactly("Burger night");
        assertThat(outings.findVisibleTo(STRANGER, Pageable.unpaged())).isEmpty();
    }

    @Test
    @DisplayName("A creator who is also a participant gets the outing once, not twice")
    void noDuplicateRows() {
        outing("Once only", LocalDate.of(2026, 11, 3), OutingStatus.ACTIVE, RAFSAN, TANVIR);

        assertThat(outings.findVisibleTo(RAFSAN, Pageable.unpaged())).hasSize(1);
    }

    @Test
    @DisplayName("Newest first, and a page request returns only the most recent")
    void newestFirstAndPaged() {
        outing("Old", LocalDate.of(2026, 10, 1), OutingStatus.ACTIVE, RAFSAN);
        outing("Newest", LocalDate.of(2026, 11, 5), OutingStatus.ACTIVE, RAFSAN);
        outing("Middle", LocalDate.of(2026, 10, 20), OutingStatus.ACTIVE, RAFSAN);

        assertThat(outings.findVisibleTo(RAFSAN, Pageable.unpaged())).extracting(Outing::getTitle)
            .containsExactly("Newest", "Middle", "Old");
        assertThat(outings.findVisibleTo(RAFSAN, PageRequest.of(0, 2))).extracting(Outing::getTitle)
            .containsExactly("Newest", "Middle");
    }

    @Test
    @DisplayName("Entity graph loads participants and guests in a single SQL statement")
    void entityGraphAvoidsNPlusOne() {
        Guest rahim = em.merge(new Guest(RAFSAN, "Rahim"));
        Outing o = new Outing("Graph", OutingCategory.EATING, LocalDate.of(2026, 11, 4), RAFSAN);
        o.addUserParticipant(RAFSAN);
        o.addUserParticipant(TANVIR);
        o.addGuestParticipant(rahim);
        Long id = outings.saveAndFlush(o).getId();
        em.clear();
        Statistics stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        Outing loaded = outings.findWithParticipantsById(id).orElseThrow();
        String guestName = loaded.getParticipants().get(2).getGuest().getName();

        assertThat(guestName).isEqualTo("Rahim");
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Unknown id gives an empty result")
    void unknownIdIsEmpty() {
        assertThat(outings.findWithParticipantsById(-1L)).isEmpty();
    }
}
