package com.course.enrollment.persistence;

import com.course.enrollment.client.OfferingSnapshot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "offering_capacity")
public class OfferingCapacity {

    @Id
    @Column(name = "offering_id", length = 40)
    private String offeringId;

    @Column(name = "course_code", nullable = false, length = 20)
    private String courseCode;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 20)
    private String term;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "prerequisite_course_code", length = 20)
    private String prerequisiteCourseCode;

    protected OfferingCapacity() {
    }

    public static OfferingCapacity from(OfferingSnapshot snapshot) {
        OfferingCapacity row = new OfferingCapacity();
        row.offeringId = snapshot.offeringId();
        row.refreshFrom(snapshot);
        return row;
    }

    public void refreshFrom(OfferingSnapshot snapshot) {
        this.courseCode = snapshot.courseCode();
        this.title = snapshot.title();
        this.term = snapshot.term();
        this.capacity = snapshot.capacity();
        this.prerequisiteCourseCode = snapshot.prerequisiteCourseCode();
    }

    public String getOfferingId() {
        return offeringId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getTitle() {
        return title;
    }

    public String getTerm() {
        return term;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getPrerequisiteCourseCode() {
        return prerequisiteCourseCode;
    }
}
