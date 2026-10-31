package com.course.enrollment.support;

import com.course.common.DemoCourse;
import com.course.common.DemoLearners;
import com.course.enrollment.client.CatalogGateway;
import com.course.enrollment.client.CatalogUnavailableException;
import com.course.enrollment.client.LearnerNotFoundException;
import com.course.enrollment.client.LearnerSnapshot;
import com.course.enrollment.client.OfferingNotFoundException;
import com.course.enrollment.client.OfferingSnapshot;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class FakeCatalogGateway implements CatalogGateway {

    private final ConcurrentHashMap<String, LearnerSnapshot> learners = new ConcurrentHashMap<>();
    private volatile int capacity = DemoCourse.CAPACITY;
    private volatile RuntimeException failure;
    private volatile int remainingSuccesses = Integer.MAX_VALUE;

    public FakeCatalogGateway() {
        reset();
    }

    public void reset() {
        failure = null;
        capacity = DemoCourse.CAPACITY;
        remainingSuccesses = Integer.MAX_VALUE;
        learners.clear();
        for (DemoLearners.Person person : DemoLearners.ALL) {
            Set<String> completed = person.completedPrerequisite() ? Set.of(DemoCourse.PREREQUISITE) : Set.of();
            learners.put(person.learnerId(), new LearnerSnapshot(person.learnerId(), person.displayName(), completed));
        }
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public void setCompleted(String learnerId, Set<String> completed) {
        LearnerSnapshot current = learners.get(learnerId);
        if (current == null) {
            throw new LearnerNotFoundException(learnerId);
        }
        learners.put(learnerId, new LearnerSnapshot(learnerId, current.displayName(), Set.copyOf(completed)));
    }

    public void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    public void succeedTimes(int times) {
        this.remainingSuccesses = times;
    }

    @Override
    public OfferingSnapshot getOffering(String offeringId) {
        tick();
        if (!DemoCourse.OFFERING_ID.equals(offeringId)) {
            throw new OfferingNotFoundException(offeringId);
        }
        return new OfferingSnapshot(
                DemoCourse.OFFERING_ID,
                DemoCourse.COURSE_CODE,
                DemoCourse.TITLE,
                DemoCourse.TERM,
                capacity,
                DemoCourse.PREREQUISITE);
    }

    @Override
    public LearnerSnapshot getLearner(String learnerId) {
        tick();
        LearnerSnapshot learner = learners.get(learnerId);
        if (learner == null) {
            throw new LearnerNotFoundException(learnerId);
        }
        return learner;
    }

    private void tick() {
        if (failure != null) {
            throw failure;
        }
        if (remainingSuccesses == Integer.MAX_VALUE) {
            return;
        }
        if (remainingSuccesses <= 0) {
            throw new CatalogUnavailableException("catalog down");
        }
        remainingSuccesses--;
    }
}
