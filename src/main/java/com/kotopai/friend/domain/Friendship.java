package com.kotopai.friend.domain;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.kotopai.common.BaseEntity;
import com.kotopai.user.domain.User;

/**
 * A directional request or accepted relationship between two users.
 *
 * <p>Stored as a single record in the database. When accepted, both users
 * consider each other friends symmetrically.
 */
@Entity
@Table(
        name = "friendships",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_friendship_pair",
                columnNames = {"requester_id", "addressee_id"}
        )
)
public class Friendship extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "addressee_id", nullable = false)
    private User addressee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FriendshipStatus status;

    protected Friendship() {
    }

    public Friendship(User requester, User addressee) {
        this.requester = Objects.requireNonNull(requester, "requester must not be null");
        this.addressee = Objects.requireNonNull(addressee, "addressee must not be null");
        if (requester.equals(addressee)
                || (requester.getId() != null && requester.getId().equals(addressee.getId()))) {
            throw new IllegalArgumentException("cannot create friendship with oneself");
        }
        this.status = FriendshipStatus.PENDING;
    }

    public void accept() {
        if (this.status == FriendshipStatus.ACCEPTED) {
            throw new IllegalStateException("friendship is already accepted");
        }
        this.status = FriendshipStatus.ACCEPTED;
    }

    public User getRequester() {
        return requester;
    }

    public User getAddressee() {
        return addressee;
    }

    public FriendshipStatus getStatus() {
        return status;
    }
}

