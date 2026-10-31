package com.course.enrollment.api;

import com.course.enrollment.client.CatalogUnavailableException;
import com.course.enrollment.client.InvalidRequestException;
import com.course.enrollment.client.LearnerNotFoundException;
import com.course.enrollment.client.OfferingNotFoundException;
import com.course.enrollment.service.EnrollmentService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiComponentsTest {

    @Test
    void handlerMapsEachFailureToAClearResult() {
        ApiExceptionHandler handler = new ApiExceptionHandler();
        assertEquals("OFFERING_NOT_FOUND", outcome(handler.offering(new OfferingNotFoundException("CS")).getBody()));
        assertEquals("LEARNER_NOT_FOUND", outcome(handler.learner(new LearnerNotFoundException("ghost")).getBody()));
        assertEquals("CATALOG_UNAVAILABLE", outcome(handler.catalog(new CatalogUnavailableException("down")).getBody()));
        assertEquals("INVALID_REQUEST", outcome(handler.invalid(new InvalidRequestException("no")).getBody()));
        assertEquals("no", handler.invalid(new InvalidRequestException("no")).getBody().getProperties().get("message"));
        assertEquals("down", handler.catalog(new CatalogUnavailableException("down")).getBody().getDetail());
        assertEquals("NOT_ENROLLED", outcome(handler.conflict(new DataIntegrityViolationException("dup")).getBody()));
    }

    private static String outcome(ProblemDetail problem) {
        return String.valueOf(problem.getProperties().get("outcome"));
    }

    @Test
    void publisherSwallowsAFailedStatusRead() {
        EnrollmentService service = mock(EnrollmentService.class);
        StatusBroadcaster broadcaster = mock(StatusBroadcaster.class);
        StatusPublisher publisher = new StatusPublisher(service, broadcaster);
        StatusView view = mock(StatusView.class);
        when(service.status("CS", "alice")).thenReturn(view);
        publisher.publish("CS", "alice");
        verify(broadcaster).publish("CS", view);

        when(service.status("CS", "alice")).thenThrow(new CatalogUnavailableException("down"));
        publisher.publish("CS", "alice");
    }

    @Test
    void broadcasterDropsEmittersThatCannotAcceptEvents() {
        StatusBroadcaster broadcaster = new StatusBroadcaster();
        broadcaster.publish("CS", null);
        SseEmitter emitter = broadcaster.subscribe("CS");
        broadcaster.publish("CS", null);
        emitter.complete();
        broadcaster.publish("CS", null);
    }
}
