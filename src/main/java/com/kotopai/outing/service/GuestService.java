package com.kotopai.outing.service;

import com.kotopai.outing.domain.Guest;
import com.kotopai.outing.repository.GuestRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guests are people without accounts, owned by one user. Every operation takes the acting owner and only ever
 * touches that owner own guests.
 */
@Service
@Transactional
public class GuestService {

    private final GuestRepository guests;

    public GuestService(GuestRepository guests) {
        this.guests = Objects.requireNonNull(guests, "guests must not be null");
    }

    /**
     * Names must be unique per owner, ignoring case: two guests both called "Rahim" would be
     * indistinguishable in the wizard, and a bill could be assigned to the wrong one.
     */
    public Guest create(Long ownerId, String name) {
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        Guest guest = new Guest(ownerId, name); // validates blank / too long
        if (guests.existsByOwnerIdAndNameIgnoreCase(ownerId, guest.getName())) {
            throw new GuestOperationException(GuestOperationException.Reason.DUPLICATE_NAME,
                "You already have a guest named " + guest.getName());
        }
        return guests.save(guest);
    }

    @Transactional(readOnly = true)
    public List<Guest> list(Long ownerId) {
        return guests.findByOwnerIdOrderByNameAsc(Objects.requireNonNull(ownerId, "ownerId must not be null"));
    }

    /** A guest who took part in an outing cannot be deleted: their consumption is part of that bill. */
    public void delete(Long ownerId, Long guestId) {
        Guest guest = guests.findByIdAndOwnerId(guestId, ownerId).orElseThrow(() ->
            new GuestOperationException(GuestOperationException.Reason.NOT_FOUND, "Guest not found"));
        if (guests.isUsedInAnyOuting(guest.getId())) {
            throw new GuestOperationException(GuestOperationException.Reason.IN_USE,
                guest.getName() + " took part in an outing and cannot be deleted");
        }
        guests.delete(guest);
    }
}
