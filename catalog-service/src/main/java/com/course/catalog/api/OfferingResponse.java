package com.course.catalog.api;

public record OfferingResponse(
        String offeringId,
        String courseCode,
        String title,
        String term,
        int capacity,
        String prerequisiteCourseCode,
        String description
) {
}
