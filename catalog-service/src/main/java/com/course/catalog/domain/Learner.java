package com.course.catalog.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "learner")
public class Learner {

    @Id
    @Column(name = "learner_id", length = 40)
    private String learnerId;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "learner_completion", joinColumns = @JoinColumn(name = "learner_id"))
    @Column(name = "course_code", length = 20)
    private Set<String> completedCourseCodes = new HashSet<>();

    protected Learner() {
    }

    public Learner(String learnerId, String displayName, Set<String> completedCourseCodes) {
        this.learnerId = learnerId;
        this.displayName = displayName;
        this.completedCourseCodes = new HashSet<>(completedCourseCodes);
    }

    public String getLearnerId() {
        return learnerId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Set<String> getCompletedCourseCodes() {
        return completedCourseCodes;
    }
}
