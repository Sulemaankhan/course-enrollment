package com.course.enrollment.queue;

import java.time.Instant;

public record QueuedLearner(String learnerId, Instant enqueuedAt) {
}
