package com.course.enrollment.queue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryFifoWaitlistQueue implements FifoWaitlistQueue {

    private final Map<String, List<QueuedLearner>> queues = new ConcurrentHashMap<>();

    @Override
    public void sync(String offeringId, List<QueuedLearner> ordered) {
        queues.put(offeringId, List.copyOf(ordered));
    }

    @Override
    public Integer position(String offeringId, String learnerId) {
        List<QueuedLearner> ordered = queues.getOrDefault(offeringId, List.of());
        for (int index = 0; index < ordered.size(); index++) {
            if (ordered.get(index).learnerId().equals(learnerId)) {
                return index + 1;
            }
        }
        return null;
    }

    public void clear() {
        queues.clear();
    }
}
