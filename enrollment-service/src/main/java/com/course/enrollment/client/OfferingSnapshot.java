package com.course.enrollment.client;

public record OfferingSnapshot(
        String offeringId,
        String courseCode,
        String title,
        String term,
        int capacity,
        String prerequisiteCourseCode
) {
}
