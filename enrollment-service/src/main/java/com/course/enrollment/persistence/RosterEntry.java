package com.course.enrollment.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "roster_entry", uniqueConstraints = @UniqueConstraint(
        name = "uk_roster_learner", columnNames = {"offering_id", "learner_id"}))
public class RosterEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "offering_id", nullable = false, length = 40)
    private String offeringId;

    @Column(name = "learner_id", nullable = false, length = 40)
    private String learnerId;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(name = "enrolled_at", nullable = false)
    private Instant enrolledAt;

    protected RosterEntry() {
    }

    public RosterEntry(String offeringId, String learnerId, String displayName, Instant enrolledAt) {
        this.offeringId = offeringId;
        this.learnerId = learnerId;
        this.displayName = displayName;
        this.enrolledAt = enrolledAt;
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

    public Instant getEnrolledAt() {
        return enrolledAt;
    }
}
