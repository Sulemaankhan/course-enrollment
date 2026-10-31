package com.course.enrollment.domain;

import org.springframework.http.HttpStatus;

public enum Outcome {
    ENROLLED(HttpStatus.CREATED),
    WAITLISTED(HttpStatus.ACCEPTED),
    ALREADY_ENROLLED(HttpStatus.OK),
    ALREADY_WAITLISTED(HttpStatus.OK),
    PREREQUISITE_NOT_MET(HttpStatus.UNPROCESSABLE_ENTITY),
    DROPPED(HttpStatus.OK),
    LEFT_WAITLIST(HttpStatus.OK),
    NOT_ENROLLED(HttpStatus.CONFLICT),
    OFFERING_NOT_FOUND(HttpStatus.NOT_FOUND),
    LEARNER_NOT_FOUND(HttpStatus.NOT_FOUND),
    CATALOG_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    Outcome(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
