package com.kotopai.outing.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.kotopai.outing.domain.Guest;
import com.kotopai.outing.domain.Outing;
import com.kotopai.outing.domain.OutingCategory;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class GuestRepositoryTest {

    @Autowired
    private GuestRepository guests;

    @Autowired
    private OutingRepository outings;

    @Test
    @DisplayName("A user sees only their own guests, alphabetically")
    void listsOwnGuestsSorted() {
        guests.save(new Guest(1L, "Rahim"));
        guests.save(new Guest(1L, "Alvi"));
        guests.save(new Guest(2L, "Someone else guest"));

        assertThat(guests.findByOwnerIdOrderByNameAsc(1L)).extracting(Guest::getName).containsExactly("Alvi", "Rahim");
        assertThat(guests.findByOwnerIdOrderByNameAsc(3L)).isEmpty();
    }

    @Test
    @DisplayName("Lookup by id is refused for a different owner")
    void ownershipCheckedLookup() {
        Guest rahim = guests.save(new Guest(1L, "Rahim"));

        assertThat(guests.findByIdAndOwnerId(rahim.getId(), 1L)).isPresent();
        assertThat(guests.findByIdAndOwnerId(rahim.getId(), 2L)).isEmpty();
    }

    @Test
    @DisplayName("Name check ignores case and is per owner")
    void duplicateNameCheck() {
        guests.save(new Guest(1L, "Rahim"));

        assertThat(guests.existsByOwnerIdAndNameIgnoreCase(1L, "RAHIM")).isTrue();
        assertThat(guests.existsByOwnerIdAndNameIgnoreCase(2L, "Rahim")).isFalse();
    }

    @Test
    @DisplayName("A guest is in use only once they are a participant of an outing")
    void usedInAnyOuting() {
        Guest used = guests.save(new Guest(1L, "Used"));
        Guest unused = guests.save(new Guest(1L, "Unused"));
        Outing outing = new Outing("Trip", OutingCategory.TRAVEL, LocalDate.of(2026, 11, 6), 1L);
        outing.addUserParticipant(1L);
        outing.addGuestParticipant(used);
        outings.saveAndFlush(outing);

        assertThat(guests.isUsedInAnyOuting(used.getId())).isTrue();
        assertThat(guests.isUsedInAnyOuting(unused.getId())).isFalse();
    }
}
