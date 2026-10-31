package com.course.enrollment.api;

import com.course.enrollment.client.InvalidRequestException;
import com.course.enrollment.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/enrollments")
@Tag(name = "Enrollment and waitlist")
@SecurityRequirement(name = "bearerAuth")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final StatusPublisher publisher;
    private final StatusBroadcaster broadcaster;

    public EnrollmentController(EnrollmentService enrollmentService, StatusPublisher publisher, StatusBroadcaster broadcaster) {
        this.enrollmentService = enrollmentService;
        this.publisher = publisher;
        this.broadcaster = broadcaster;
    }

    @PostMapping("/offerings/{offeringId}/enroll")
    @Operation(summary = "Enroll the authenticated learner, or join the FIFO waitlist when the offering is full")
    public ResponseEntity<CommandResult> enroll(@PathVariable String offeringId,
                                                Authentication authentication,
                                                @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        String learnerId = requiredLearner(authentication);
        CommandResult result = enrollmentService.enroll(offeringId, learnerId, idempotencyKey);
        publisher.publish(offeringId, learnerId);
        return ResponseEntity.status(result.outcome().status()).body(result);
    }

    @PostMapping("/offerings/{offeringId}/drop")
    @Operation(summary = "Drop the authenticated learner and promote the earliest eligible waitlisted learner")
    public ResponseEntity<CommandResult> drop(@PathVariable String offeringId,
                                              Authentication authentication,
                                              @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        String learnerId = requiredLearner(authentication);
        CommandResult result = enrollmentService.drop(offeringId, learnerId, idempotencyKey);
        publisher.publish(offeringId, learnerId);
        return ResponseEntity.status(result.outcome().status()).body(result);
    }

    @GetMapping("/offerings/{offeringId}/status")
    @Operation(summary = "Read roster, FIFO waitlist, and the caller's eligibility")
    public StatusView status(@PathVariable String offeringId, Authentication authentication) {
        return enrollmentService.status(offeringId, requiredLearner(authentication));
    }

    @GetMapping("/offerings/{offeringId}/overview")
    @Operation(summary = "Read the roster and FIFO waitlist for a registrar")
    public StatusView overview(@PathVariable String offeringId, Authentication authentication) {
        return enrollmentService.overview(offeringId, requiredLearner(authentication), displayName(authentication));
    }

    @GetMapping(value = "/offerings/{offeringId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream roster and waitlist changes for this offering")
    public SseEmitter events(@PathVariable String offeringId) {
        return broadcaster.subscribe(offeringId);
    }

    private static String requiredLearner(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()
                || "anonymousUser".equals(authentication.getName())) {
            throw new InvalidRequestException("Authentication is required.");
        }
        return authentication.getName();
    }

    private static String displayName(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            String name = jwt.getClaimAsString("name");
            if (name != null && !name.isBlank()) {
                return name;
            }
        }
        return authentication.getName();
    }
}
