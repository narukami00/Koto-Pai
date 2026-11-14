package com.kotopai.outing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kotopai.outing.domain.Guest;
import com.kotopai.outing.repository.GuestRepository;
import com.kotopai.outing.service.GuestOperationException.Reason;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GuestServiceTest {

    @Mock
    private GuestRepository repository;

    @InjectMocks
    private GuestService service;

    private static Guest guest(long id, long owner, String name) {
        Guest g = new Guest(owner, name);
        ReflectionTestUtils.setField(g, "id", id);
        return g;
    }

    @Test
    @DisplayName("Creating a guest saves it for the owner with a trimmed name")
    void createSaves() {
        when(repository.existsByOwnerIdAndNameIgnoreCase(1L, "Rahim")).thenReturn(false);
        when(repository.save(any(Guest.class))).thenAnswer(inv -> inv.getArgument(0));

        Guest created = service.create(1L, "  Rahim ");

        assertThat(created.getName()).isEqualTo("Rahim");
        assertThat(created.getOwnerId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Decision: a second guest with the same name (any case) for the same owner is rejected")
    void duplicateNameRejected() {
        when(repository.existsByOwnerIdAndNameIgnoreCase(1L, "rahim")).thenReturn(true);

        assertThatThrownBy(() -> service.create(1L, "rahim"))
            .isInstanceOfSatisfying(GuestOperationException.class, e -> assertThat(e.getReason()).isEqualTo(Reason.DUPLICATE_NAME));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Blank and overlong names are refused; nulls are refused")
    void invalidInput() {
        assertThatThrownBy(() -> service.create(1L, "  ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.create(1L, "x".repeat(81))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.create(null, "Rahim")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> service.list(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GuestService(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Listing returns the owner guests from the repository")
    void listDelegates() {
        List<Guest> expected = List.of(guest(1, 1, "Alvi"));
        when(repository.findByOwnerIdOrderByNameAsc(1L)).thenReturn(expected);

        assertThat(service.list(1L)).isEqualTo(expected);
    }

    @Test
    @DisplayName("An unused guest is deleted by its owner")
    void deleteUnused() {
        Guest g = guest(5, 1, "Rahim");
        when(repository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.of(g));
        when(repository.isUsedInAnyOuting(5L)).thenReturn(false);

        service.delete(1L, 5L);

        verify(repository).delete(g);
    }

    @Test
    @DisplayName("Someone else guest looks exactly like a missing one")
    void deleteForeignGuestIsNotFound() {
        when(repository.findByIdAndOwnerId(5L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(2L, 5L))
            .isInstanceOfSatisfying(GuestOperationException.class, e -> assertThat(e.getReason()).isEqualTo(Reason.NOT_FOUND));
        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("A guest who took part in an outing cannot be deleted")
    void deleteUsedGuestRejected() {
        Guest g = guest(5, 1, "Rahim");
        when(repository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.of(g));
        when(repository.isUsedInAnyOuting(5L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L, 5L))
            .isInstanceOfSatisfying(GuestOperationException.class, e -> {
                assertThat(e.getReason()).isEqualTo(Reason.IN_USE);
                assertThat(e.getMessage()).contains("Rahim");
            });
        verify(repository, never()).delete(any());
    }
}
