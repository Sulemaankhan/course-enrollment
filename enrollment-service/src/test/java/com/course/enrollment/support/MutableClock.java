package com.course.enrollment.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

public final class MutableClock extends Clock {

    private volatile Instant now = Instant.parse("2026-08-01T15:00:00Z");

    public void reset() {
        now = Instant.parse("2026-08-01T15:00:00Z");
    }

    public void advanceMillis(long millis) {
        now = now.plusMillis(millis);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return now;
    }
}
