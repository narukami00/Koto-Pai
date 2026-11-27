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
import java.math.BigDecimal;
import java.util.Objects;

/** One consumer of an item; the percentage is only meaningful for percentage-split items. */
@Entity
@Table(name = "item_shares")
public class ItemShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private OutingParticipant participant;

    @Column(precision = 5, scale = 2)
    private BigDecimal percentage;

    protected ItemShare() {
    }

    ItemShare(Item item, OutingParticipant participant, BigDecimal percentage) {
        this.item = Objects.requireNonNull(item, "item must not be null");
        this.participant = Objects.requireNonNull(participant, "participant must not be null");
        this.percentage = percentage;
    }

    public Long getId() {
        return id;
    }

    public OutingParticipant getParticipant() {
        return participant;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }
}
