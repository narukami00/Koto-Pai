package com.kotopai.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kotopai.outing.domain.Guest;
import com.kotopai.outing.repository.GuestRepository;
import com.kotopai.user.dto.UserSummary;
import com.kotopai.user.service.UserService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PersonNamesTest {

    private UserService users;
    private GuestRepository guests;
    private PersonNames names;

    @BeforeEach
    void setUp() {
        users = mock(UserService.class);
        guests = mock(GuestRepository.class);
        names = new PersonNames(users, guests);
    }

    @Test
    @DisplayName("A known user shows their display name")
    void knownUser() {
        when(users.summariesOf(anyCollection()))
            .thenReturn(Map.of(2L, new UserSummary(2L, "tanvir_h", "Tanvir", "#fff")));

        assertThat(names.nameOf(2L)).isEqualTo("Tanvir");
    }

    @Test
    @DisplayName("An unknown or missing user id falls back to a neutral label")
    void unknownUser() {
        when(users.summariesOf(anyCollection())).thenReturn(Map.of());

        assertThat(names.nameOf(99L)).isEqualTo(PersonNames.UNKNOWN_USER);
        assertThat(names.nameOf(null)).isEqualTo(PersonNames.UNKNOWN_USER);
    }

    @Test
    @DisplayName("A blank display name is treated as unknown")
    void blankName() {
        when(users.summariesOf(anyCollection()))
            .thenReturn(Map.of(3L, new UserSummary(3L, "x", "  ", "#fff")));

        assertThat(names.nameOf(3L)).isEqualTo(PersonNames.UNKNOWN_USER);
    }

    @Test
    @DisplayName("A guest shows the name the host gave them, with a fallback")
    void guestNames() {
        Guest rahim = mock(Guest.class);
        when(rahim.getName()).thenReturn("Rahim");
        when(guests.findById(5L)).thenReturn(Optional.of(rahim));
        when(guests.findById(6L)).thenReturn(Optional.empty());

        assertThat(names.guestNameOf(5L)).isEqualTo("Rahim");
        assertThat(names.guestNameOf(6L)).isEqualTo(PersonNames.UNKNOWN_GUEST);
        assertThat(names.guestNameOf(null)).isEqualTo(PersonNames.UNKNOWN_GUEST);
    }
}
