package com.course.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "course_offering")
public class CourseOffering {

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

    @Column(nullable = false, length = 400)
    private String description;

    protected CourseOffering() {
    }

    public CourseOffering(String offeringId, String courseCode, String title, String term,
                          int capacity, String prerequisiteCourseCode, String description) {
        this.offeringId = offeringId;
        this.courseCode = courseCode;
        this.title = title;
        this.term = term;
        this.capacity = capacity;
        this.prerequisiteCourseCode = prerequisiteCourseCode;
        this.description = description;
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

    public String getDescription() {
        return description;
    }
}
