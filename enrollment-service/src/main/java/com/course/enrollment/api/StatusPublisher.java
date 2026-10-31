package com.course.enrollment.api;

import com.course.enrollment.service.EnrollmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StatusPublisher {

    private static final Logger log = LoggerFactory.getLogger(StatusPublisher.class);

    private final EnrollmentService enrollmentService;
    private final StatusBroadcaster broadcaster;

    public StatusPublisher(EnrollmentService enrollmentService, StatusBroadcaster broadcaster) {
        this.enrollmentService = enrollmentService;
        this.broadcaster = broadcaster;
    }

    public void publish(String offeringId, String learnerId) {
        try {
            broadcaster.publish(offeringId, enrollmentService.status(offeringId, learnerId));
        } catch (RuntimeException ex) {
            log.warn("Live status push skipped: {}", ex.toString());
        }
    }
}
