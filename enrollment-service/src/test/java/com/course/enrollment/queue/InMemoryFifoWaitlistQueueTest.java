package com.course.enrollment.queue;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InMemoryFifoWaitlistQueueTest {

    @Test
    void replacesTheCachedOrder() {
        InMemoryFifoWaitlistQueue queue = new InMemoryFifoWaitlistQueue();
        Instant start = Instant.parse("2026-08-01T15:00:00Z");
        queue.sync("CS", List.of(
                new QueuedLearner("carol", start),
                new QueuedLearner("eve", start.plusMillis(5))));
        assertEquals(1, queue.position("CS", "carol"));
        assertEquals(2, queue.position("CS", "eve"));

        queue.sync("CS", List.of(new QueuedLearner("eve", start.plusMillis(5))));
        assertNull(queue.position("CS", "carol"));
        assertEquals(1, queue.position("CS", "eve"));
        assertNull(queue.position("other", "eve"));

        queue.clear();
        assertNull(queue.position("CS", "eve"));
    }
}
