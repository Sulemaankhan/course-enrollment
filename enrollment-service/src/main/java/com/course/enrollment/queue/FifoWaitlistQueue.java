package com.course.enrollment.queue;

import java.util.List;

/**
 * Cache of the durable waitlist. The database order is authoritative.
 * Redis (or the in-memory stand-in) is rewritten from that order after each change.
 */
public interface FifoWaitlistQueue {

    void sync(String offeringId, List<QueuedLearner> ordered);

    /** 1-based position, or null when the learner is not cached. */
    Integer position(String offeringId, String learnerId);
}
