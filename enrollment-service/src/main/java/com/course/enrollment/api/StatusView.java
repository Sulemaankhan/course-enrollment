package com.course.enrollment.api;

import java.time.Instant;
import java.util.List;

public record StatusView(
        String offeringId,
        String courseCode,
        String title,
        String term,
        int capacity,
        int enrolledCount,
        int seatsRemaining,
        String prerequisiteCourseCode,
        String eligibilityPolicy,
        List<RosterSeat> roster,
        List<WaitlistSeat> waitlist,
        Viewer viewer
) {
    public record RosterSeat(String learnerId, String displayName, Instant enrolledAt) {
    }

    public record WaitlistSeat(String learnerId, String displayName, int position, Instant enqueuedAt, boolean eligible) {
    }

    public record Viewer(String learnerId, String displayName, boolean eligible, String state, Integer waitlistPosition) {
    }
}
