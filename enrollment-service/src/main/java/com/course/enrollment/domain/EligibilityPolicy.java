package com.course.enrollment.domain;

public final class EligibilityPolicy {

    public static final String NAME = "SKIP_INELIGIBLE";

    public static final String DESCRIPTION = "SKIP_INELIGIBLE. A learner may hold at most one roster seat and one "
            + "waitlist place for this offering, and may enroll again after dropping. "
            + "The learner is eligible only when the offering has no prerequisite or the learner has completed that "
            + "exact course code. An ineligible learner is not enrolled and is not added to the waitlist. "
            + "When a seat opens, waitlisted learners are considered in FIFO order (earliest enqueue time, then "
            + "learner id). Ineligible learners are removed and do not keep their place; the earliest remaining "
            + "eligible learner is promoted. Status reads do not change the queue.";

    private EligibilityPolicy() {
    }
}
