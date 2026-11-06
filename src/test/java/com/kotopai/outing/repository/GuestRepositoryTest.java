package com.kotopai.outing.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.kotopai.outing.domain.Guest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class GuestRepositoryTest {

    @Autowired
    private GuestRepository guests;

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
}
