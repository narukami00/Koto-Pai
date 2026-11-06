package com.kotopai.outing.domain;

import com.kotopai.outing.engine.split.SplitType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** One line on the bill. */
@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "outing_id", nullable = false)
    private Outing outing;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", nullable = false, length = 16)
    private SplitType splitType;

    @OneToMany(mappedBy = "item")
    private List<ItemShare> shares = new ArrayList<>();

    protected Item() {
    }

    Item(Outing outing, String name, BigDecimal amount, SplitType splitType) {
        this.outing = Objects.requireNonNull(outing, "outing must not be null");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("item name must not be blank");
        }
        this.name = name.strip();
        this.amount = Objects.requireNonNull(amount, "amount must not be null");
        this.splitType = Objects.requireNonNull(splitType, "splitType must not be null");
    }

    /** Adds a consumer; use a null percentage unless the item is split by percentage. */
    public ItemShare addShare(OutingParticipant participant, BigDecimal percentage) {
        ItemShare share = new ItemShare(this, participant, percentage);
        shares.add(share);
        return share;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public List<ItemShare> getShares() {
        return Collections.unmodifiableList(shares);
    }
}
