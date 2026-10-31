package com.course.enrollment.api;

import com.course.common.web.ApiProblems;
import com.course.enrollment.client.CatalogUnavailableException;
import com.course.enrollment.client.InvalidRequestException;
import com.course.enrollment.client.LearnerNotFoundException;
import com.course.enrollment.client.OfferingNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(OfferingNotFoundException.class)
    public ResponseEntity<ProblemDetail> offering(OfferingNotFoundException exception) {
        ProblemDetail problem = ApiProblems.of(HttpStatus.NOT_FOUND, "OFFERING_NOT_FOUND", exception.getMessage());
        problem.setProperty("offeringId", exception.getOfferingId());
        return respond(problem);
    }

    @ExceptionHandler(LearnerNotFoundException.class)
    public ResponseEntity<ProblemDetail> learner(LearnerNotFoundException exception) {
        ProblemDetail problem = ApiProblems.of(HttpStatus.NOT_FOUND, "LEARNER_NOT_FOUND", exception.getMessage());
        problem.setProperty("learnerId", exception.getLearnerId());
        return respond(problem);
    }

    @ExceptionHandler(CatalogUnavailableException.class)
    public ResponseEntity<ProblemDetail> catalog(CatalogUnavailableException exception) {
        return respond(ApiProblems.of(HttpStatus.SERVICE_UNAVAILABLE, "CATALOG_UNAVAILABLE", exception.getMessage()));
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ProblemDetail> invalid(InvalidRequestException exception) {
        return respond(ApiProblems.of(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> conflict(DataIntegrityViolationException exception) {
        log.warn("Rejected a write that would have duplicated enrollment state: {}", exception.getMostSpecificCause().getMessage());
        return respond(ApiProblems.of(HttpStatus.CONFLICT, "NOT_ENROLLED",
                "The request conflicted with an existing record. Enrollment, capacity, and waitlist were not changed."));
    }

    private static ResponseEntity<ProblemDetail> respond(ProblemDetail problem) {
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }
}
