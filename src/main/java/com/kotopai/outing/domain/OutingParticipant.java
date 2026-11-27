package com.kotopai.outing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;

/** Someone on an outing: exactly one of a registered user id or a guest. */
@Entity
@Table(name = "outing_participants")
public class OutingParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "outing_id", nullable = false)
    private Outing outing;

    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id")
    private Guest guest;

    protected OutingParticipant() {
    }

    OutingParticipant(Outing outing, Long userId, Guest guest) {
        if ((userId == null) == (guest == null)) {
            throw new IllegalArgumentException("a participant is exactly one of: a user or a guest");
        }
        this.outing = Objects.requireNonNull(outing, "outing must not be null");
        this.userId = userId;
        this.guest = guest;
    }

    public Long getId() {
        return id;
    }

    public Outing getOuting() {
        return outing;
    }

    public Long getUserId() {
        return userId;
    }

    public Guest getGuest() {
        return guest;
    }

    public boolean isGuest() {
        return guest != null;
    }
}
