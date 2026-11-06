package com.kotopai.outing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

/** A person without an account, brought by a registered user who settles for them. */
@Entity
@Table(name = "guests")
public class Guest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 80)
    private String name;

    protected Guest() {
    }

    public Guest(Long ownerId, String name) {
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId must not be null");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("guest name must not be blank");
        }
        this.name = name.strip();
    }

    public Long getId() {
        return id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }
}
