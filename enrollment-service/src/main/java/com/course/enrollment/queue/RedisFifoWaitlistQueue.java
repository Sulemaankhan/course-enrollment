package com.course.enrollment.queue;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RedisFifoWaitlistQueue implements FifoWaitlistQueue {

    static final String PREFIX = "waitlist:";

    private final StringRedisTemplate redis;

    public RedisFifoWaitlistQueue(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void sync(String offeringId, List<QueuedLearner> ordered) {
        String key = PREFIX + offeringId;
        redis.delete(key);
        if (ordered.isEmpty()) {
            return;
        }
        Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>();
        for (int index = 0; index < ordered.size(); index++) {
            QueuedLearner learner = ordered.get(index);
            // Milliseconds are scaled so a same-millisecond tie still sorts by insertion index.
            // The scaled value stays inside the range of integers that a double stores exactly.
            double score = (learner.enqueuedAt().toEpochMilli() * 1000d) + index;
            tuples.add(ZSetOperations.TypedTuple.of(learner.learnerId(), score));
        }
        redis.opsForZSet().add(key, tuples);
    }

    @Override
    public Integer position(String offeringId, String learnerId) {
        Long rank = redis.opsForZSet().rank(PREFIX + offeringId, learnerId);
        if (rank == null) {
            return null;
        }
        return rank.intValue() + 1;
    }

    public List<String> orderedIds(String offeringId) {
        Set<String> ids = redis.opsForZSet().range(PREFIX + offeringId, 0, -1);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return List.copyOf(new LinkedHashSet<>(ids));
    }
}
