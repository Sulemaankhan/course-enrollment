package com.course.enrollment.support;

import com.course.enrollment.queue.FifoWaitlistQueue;
import com.course.enrollment.queue.InMemoryFifoWaitlistQueue;
import com.course.enrollment.queue.QueuedLearner;

import java.util.List;

public final class UnstableQueue implements FifoWaitlistQueue {

    private final InMemoryFifoWaitlistQueue delegate = new InMemoryFifoWaitlistQueue();
    private volatile boolean fail;

    public void reset() {
        fail = false;
        delegate.clear();
    }

    public void setFail(boolean fail) {
        this.fail = fail;
    }

    @Override
    public void sync(String offeringId, List<QueuedLearner> ordered) {
        if (fail) {
            throw new IllegalStateException("redis down");
        }
        delegate.sync(offeringId, ordered);
    }

    @Override
    public Integer position(String offeringId, String learnerId) {
        if (fail) {
            throw new IllegalStateException("redis down");
        }
        return delegate.position(offeringId, learnerId);
    }
}
