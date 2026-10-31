package com.course.common;

/**
 * The single course offering this slice registers. Capacity is small on purpose
 * so a full section and the waitlist are visible without a data load.
 */
public final class DemoCourse {

    public static final String OFFERING_ID = "CS-301-2026FA";
    public static final String COURSE_CODE = "CS-301";
    public static final String TITLE = "Data Structures";
    public static final String TERM = "2026FA";
    public static final String PREREQUISITE = "CS-201";
    public static final int CAPACITY = 2;
    public static final String DESCRIPTION =
            "Queues, trees, and graphs. Eligibility requires completion of CS-201 Programming Fundamentals.";

    private DemoCourse() {
    }
}
