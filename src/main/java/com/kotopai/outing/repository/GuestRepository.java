package com.kotopai.outing.repository;

import com.kotopai.outing.domain.Guest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestRepository extends JpaRepository<Guest, Long> {

    /** A user guest list for the wizard, alphabetical. */
    List<Guest> findByOwnerIdOrderByNameAsc(Long ownerId);

    /** Ownership-checked lookup: a user can only fetch their own guests. */
    Optional<Guest> findByIdAndOwnerId(Long id, Long ownerId);
}
