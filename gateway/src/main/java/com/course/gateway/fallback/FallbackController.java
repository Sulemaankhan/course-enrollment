package com.course.gateway.fallback;

import com.course.common.web.ApiProblems;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FallbackController {

    @RequestMapping("/fallback/catalog")
    public ResponseEntity<ProblemDetail> catalog() {
        return unavailable("Course catalog is temporarily unavailable.");
    }

    @RequestMapping("/fallback/enrollment")
    public ResponseEntity<ProblemDetail> enrollment() {
        return unavailable("Enrollment service is temporarily unavailable.");
    }

    private static ResponseEntity<ProblemDetail> unavailable(String message) {
        ProblemDetail problem = ApiProblems.of(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", message);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
