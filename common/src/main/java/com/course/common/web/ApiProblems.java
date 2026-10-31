package com.course.common.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

public final class ApiProblems {

    private static final URI BLANK = URI.create("about:blank");

    private ApiProblems() {
    }

    public static ProblemDetail of(HttpStatusCode status, String outcome, String detail) {
        String safe = detail == null || detail.isBlank() ? defaultDetail(status) : detail;
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, safe);
        problem.setTitle(reason(status));
        problem.setType(BLANK);
        problem.setProperty("outcome", outcome);
        problem.setProperty("message", safe);
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    public static ProblemDetail from(Throwable exception) {
        if (exception instanceof ResponseStatusException responseStatus) {
            return fromStatus(responseStatus);
        }
        if (exception instanceof AccessDeniedException) {
            return of(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have access to this resource.");
        }
        if (exception instanceof AuthenticationException) {
            return of(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required.");
        }
        return of(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "The request could not be completed.");
    }

    public static void ensureClientFields(ProblemDetail problem, HttpStatusCode status, String path) {
        int code = problem.getStatus();
        if (code <= 0) {
            code = status.value();
        }
        problem.setStatus(code);
        HttpStatusCode resolved = HttpStatusCode.valueOf(code);
        if (problem.getTitle() == null || problem.getTitle().isBlank()) {
            problem.setTitle(reason(resolved));
        }
        if (problem.getType() == null) {
            problem.setType(BLANK);
        }
        if (problem.getInstance() == null && path != null && !path.isBlank()) {
            try {
                problem.setInstance(URI.create(path));
            } catch (IllegalArgumentException ignored) {
                // The request path is not a URI reference.
            }
        }
        if (!has(problem, "outcome")) {
            problem.setProperty("outcome", outcomeFor(resolved));
        }
        if (!has(problem, "timestamp")) {
            problem.setProperty("timestamp", Instant.now().toString());
        }
        if (resolved.is5xxServerError()) {
            String safe = code == HttpStatus.SERVICE_UNAVAILABLE.value()
                    ? "The service is temporarily unavailable."
                    : "The request could not be completed.";
            problem.setDetail(safe);
            problem.setProperty("message", safe);
            return;
        }
        if (problem.getDetail() == null || problem.getDetail().isBlank()) {
            problem.setDetail(defaultDetail(resolved));
        }
        if (!has(problem, "message")) {
            problem.setProperty("message", problem.getDetail());
        }
    }

    public static String outcomeFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 400, 422 -> "INVALID_REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 409 -> "CONFLICT";
            case 413 -> "PAYLOAD_TOO_LARGE";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            case 429 -> "TOO_MANY_REQUESTS";
            case 503 -> "SERVICE_UNAVAILABLE";
            default -> status.is5xxServerError() ? "INTERNAL_ERROR" : "REQUEST_FAILED";
        };
    }

    public static Map<String, String> fieldError(FieldError error) {
        String message = error.getDefaultMessage() == null ? "Invalid value." : error.getDefaultMessage();
        return Map.of("field", error.getField(), "message", message);
    }

    public static String reason(HttpStatusCode status) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        return resolved == null ? "Error" : resolved.getReasonPhrase();
    }

    public static String defaultDetail(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "The request is invalid.";
            case 401 -> "Authentication is required.";
            case 403 -> "You do not have access to this resource.";
            case 404 -> "No endpoint matches this request.";
            case 405 -> "This method is not allowed for the requested resource.";
            case 409 -> "The request conflicted with an existing record.";
            case 415 -> "The request media type is not supported.";
            case 503 -> "The service is temporarily unavailable.";
            default -> status.is5xxServerError()
                    ? "The request could not be completed."
                    : reason(status);
        };
    }

    private static ProblemDetail fromStatus(ResponseStatusException exception) {
        HttpStatusCode status = exception.getStatusCode();
        if (exception instanceof WebExchangeBindException binding) {
            ProblemDetail problem = of(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Request validation failed.");
            problem.setProperty("errors", binding.getFieldErrors().stream().map(ApiProblems::fieldError).toList());
            return problem;
        }
        if (exception instanceof ServerWebInputException) {
            return of(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Request body is missing or malformed.");
        }
        if (status.is5xxServerError()) {
            return of(status, outcomeFor(status), defaultDetail(status));
        }
        String detail = exception.getReason();
        return of(status, outcomeFor(status), detail == null || detail.isBlank() ? defaultDetail(status) : detail);
    }

    private static boolean has(ProblemDetail problem, String key) {
        return problem.getProperties() != null && problem.getProperties().containsKey(key);
    }
}
