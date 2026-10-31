package com.course.enrollment;

import com.course.common.DemoCourse;
import com.course.enrollment.domain.Outcome;
import com.course.enrollment.persistence.OfferingCapacity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConcurrentEnrollmentTest extends EnrollmentTestBase {

    @Test
    void onlyOneLearnerWinsTheLastSeat() throws Exception {
        catalog.setCapacity(1);
        capacities.saveAndFlush(OfferingCapacity.from(catalog.getOffering(DemoCourse.OFFERING_ID)));
        List<String> learners = List.of("alice", "bob", "carol", "eve");

        List<Outcome> outcomes = race(learners);

        assertEquals(1, outcomes.stream().filter(outcome -> outcome == Outcome.ENROLLED).count());
        assertEquals(3, outcomes.stream().filter(outcome -> outcome == Outcome.WAITLISTED).count());
        assertEquals(1, roster.count());
        assertEquals(3, waitlist.count());
        Set<String> seated = roster.findAll().stream().map(entry -> entry.getLearnerId()).collect(Collectors.toSet());
        Set<String> waiting = waitlist.findAll().stream().map(entry -> entry.getLearnerId()).collect(Collectors.toSet());
        assertTrue(java.util.Collections.disjoint(seated, waiting));
        assertEquals(4, seated.size() + waiting.size());
    }

    @Test
    void concurrentDuplicateEnrollCreatesOneRosterRow() throws Exception {
        catalog.setCapacity(5);
        capacities.saveAndFlush(OfferingCapacity.from(catalog.getOffering(DemoCourse.OFFERING_ID)));

        List<Outcome> outcomes = race(List.of("alice", "alice", "alice", "alice"));

        assertEquals(1, outcomes.stream().filter(outcome -> outcome == Outcome.ENROLLED).count());
        assertEquals(3, outcomes.stream().filter(outcome -> outcome == Outcome.ALREADY_ENROLLED).count());
        assertEquals(1, roster.count());
        assertEquals(0, waitlist.count());
    }

    private List<Outcome> race(List<String> learners) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(learners.size());
        CyclicBarrier barrier = new CyclicBarrier(learners.size());
        try {
            List<Future<Outcome>> futures = new ArrayList<>();
            for (String learner : learners) {
                futures.add(pool.submit(() -> {
                    barrier.await(5, TimeUnit.SECONDS);
                    return service.enroll(DemoCourse.OFFERING_ID, learner, null).outcome();
                }));
            }
            List<Outcome> outcomes = new ArrayList<>();
            for (Future<Outcome> future : futures) {
                outcomes.add(future.get(15, TimeUnit.SECONDS));
            }
            return outcomes;
        } finally {
            pool.shutdownNow();
        }
    }
}
