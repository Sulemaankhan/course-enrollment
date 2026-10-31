package com.course.enrollment.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "waitlist_entry",
        uniqueConstraints = @UniqueConstraint(name = "uk_waitlist_learner", columnNames = {"offering_id", "learner_id"}),
        indexes = @Index(name = "idx_waitlist_fifo", columnList = "offering_id, enqueued_at, learner_id"))
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "offering_id", nullable = false, length = 40)
    private String offeringId;

    @Column(name = "learner_id", nullable = false, length = 40)
    private String learnerId;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(name = "enqueued_at", nullable = false)
    private Instant enqueuedAt;

    protected WaitlistEntry() {
    }

    public WaitlistEntry(String offeringId, String learnerId, String displayName, Instant enqueuedAt) {
        this.offeringId = offeringId;
        this.learnerId = learnerId;
        this.displayName = displayName;
        this.enqueuedAt = enqueuedAt;
    }

    public String getOfferingId() {
        return offeringId;
    }

    public String getLearnerId() {
        return learnerId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Instant getEnqueuedAt() {
        return enqueuedAt;
    }
}
