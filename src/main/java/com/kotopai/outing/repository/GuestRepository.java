package com.kotopai.outing.repository;

import com.kotopai.outing.domain.Guest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GuestRepository extends JpaRepository<Guest, Long> {

    /** A user guest list for the wizard, alphabetical. */
    List<Guest> findByOwnerIdOrderByNameAsc(Long ownerId);

    /** Ownership-checked lookup: a user can only fetch their own guests. */
    Optional<Guest> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByOwnerIdAndNameIgnoreCase(Long ownerId, String name);

    /** True if the guest is a participant of any outing. */
    @Query("select count(p) > 0 from OutingParticipant p where p.guest.id = :guestId")
    boolean isUsedInAnyOuting(@Param("guestId") Long guestId);
}
