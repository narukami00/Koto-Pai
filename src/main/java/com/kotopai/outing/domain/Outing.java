package com.kotopai.outing.domain;

import com.kotopai.outing.engine.split.SplitType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root: participants, items (with their shares) and contributions are only created through
 * this class, so the whole outing is always loaded, saved and deleted as one consistent unit.
 */
@Entity
@Table(name = "outings")
public class Outing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OutingCategory category;

    @Column(name = "outing_date", nullable = false)
    private LocalDate date;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OutingStatus status = OutingStatus.DRAFT;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private long version;

    @OneToMany(mappedBy = "outing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OutingParticipant> participants = new ArrayList<>();

    @OneToMany(mappedBy = "outing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Item> items = new ArrayList<>();

    @OneToMany(mappedBy = "outing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Contribution> contributions = new ArrayList<>();

    protected Outing() {
    }

    public Outing(String title, OutingCategory category, LocalDate date, Long creatorId) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        this.title = title.strip();
        this.category = Objects.requireNonNull(category, "category must not be null");
        this.date = Objects.requireNonNull(date, "date must not be null");
        this.creatorId = Objects.requireNonNull(creatorId, "creatorId must not be null");
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public OutingParticipant addUserParticipant(Long userId) {
        OutingParticipant p = new OutingParticipant(this, Objects.requireNonNull(userId, "userId must not be null"), null);
        participants.add(p);
        return p;
    }

    public OutingParticipant addGuestParticipant(Guest guest) {
        OutingParticipant p = new OutingParticipant(this, null, Objects.requireNonNull(guest, "guest must not be null"));
        participants.add(p);
        return p;
    }

    public Item addItem(String name, BigDecimal amount, SplitType splitType) {
        Item item = new Item(this, name, amount, splitType);
        items.add(item);
        return item;
    }

    public Contribution addContribution(OutingParticipant participant, BigDecimal amountPaid) {
        Contribution c = new Contribution(this, participant, amountPaid);
        contributions.add(c);
        return c;
    }

    public void removeItem(Item item) {
        items.remove(item);
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public OutingCategory getCategory() {
        return category;
    }

    public LocalDate getDate() {
        return date;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public OutingStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public long getVersion() {
        return version;
    }

    public List<OutingParticipant> getParticipants() {
        return Collections.unmodifiableList(participants);
    }

    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    public List<Contribution> getContributions() {
        return Collections.unmodifiableList(contributions);
    }
}
