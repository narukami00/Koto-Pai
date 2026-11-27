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

/** Money a participant actually paid towards the bill. */
@Entity
@Table(name = "contributions")
public class Contribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "outing_id", nullable = false)
    private Outing outing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private OutingParticipant participant;

    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPaid;

    protected Contribution() {
    }

    Contribution(Outing outing, OutingParticipant participant, BigDecimal amountPaid) {
        this.outing = Objects.requireNonNull(outing, "outing must not be null");
        this.participant = Objects.requireNonNull(participant, "participant must not be null");
        this.amountPaid = Objects.requireNonNull(amountPaid, "amountPaid must not be null");
    }

    public Long getId() {
        return id;
    }

    public OutingParticipant getParticipant() {
        return participant;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }
}
