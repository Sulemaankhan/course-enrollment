package com.course.enrollment.queue;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisFifoWaitlistQueueTest {

    @Test
    void syncRewritesTheSortedSetInFifoOrder() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ZSetOperations<String, String> zset = mock(ZSetOperations.class);
        when(redis.opsForZSet()).thenReturn(zset);
        when(zset.rank("waitlist:CS-301-2026FA", "eve")).thenReturn(1L);
        when(zset.rank("waitlist:CS-301-2026FA", "missing")).thenReturn(null);
        when(zset.range("waitlist:CS-301-2026FA", 0, -1))
                .thenReturn(new LinkedHashSet<>(List.of("carol", "eve")));

        RedisFifoWaitlistQueue queue = new RedisFifoWaitlistQueue(redis);
        Instant first = Instant.parse("2026-08-01T15:00:00Z");
        queue.sync("CS-301-2026FA", List.of(
                new QueuedLearner("carol", first),
                new QueuedLearner("eve", first)));

        verify(redis).delete("waitlist:CS-301-2026FA");
        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<Set<ZSetOperations.TypedTuple<String>>> captor =
                org.mockito.ArgumentCaptor.forClass(Set.class);
        verify(zset).add(eq("waitlist:CS-301-2026FA"), captor.capture());
        assertEquals(2, captor.getValue().size());
        double carol = score(captor.getValue(), "carol");
        double eve = score(captor.getValue(), "eve");
        assertTrue(carol < eve);

        assertEquals(2, queue.position("CS-301-2026FA", "eve"));
        assertNull(queue.position("CS-301-2026FA", "missing"));
        assertEquals(List.of("carol", "eve"), queue.orderedIds("CS-301-2026FA"));
    }

    @Test
    void emptySyncDeletesWithoutAdding() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ZSetOperations<String, String> zset = mock(ZSetOperations.class);
        when(redis.opsForZSet()).thenReturn(zset);
        when(zset.range("waitlist:CS-301-2026FA", 0, -1)).thenReturn(null);

        RedisFifoWaitlistQueue queue = new RedisFifoWaitlistQueue(redis);
        queue.sync("CS-301-2026FA", List.of());

        verify(redis).delete("waitlist:CS-301-2026FA");
        verify(zset, org.mockito.Mockito.never()).add(eq("waitlist:CS-301-2026FA"), anySet());
        assertEquals(List.of(), queue.orderedIds("CS-301-2026FA"));
    }

    private static double score(Set<ZSetOperations.TypedTuple<String>> tuples, String learnerId) {
        return tuples.stream()
                .filter(tuple -> learnerId.equals(tuple.getValue()))
                .findFirst()
                .orElseThrow()
                .getScore();
    }
}
