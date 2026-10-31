package com.course.common.web;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RestControllerAdvice
public class ServletApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ServletApiExceptionHandler.class);

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> constraints(ConstraintViolationException exception, WebRequest request) {
        ProblemDetail problem = ApiProblems.of(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Request validation failed.");
        problem.setProperty("errors", exception.getConstraintViolations().stream().map(ServletApiExceptionHandler::violation).toList());
        return handleExceptionInternal(exception, problem, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> denied(AccessDeniedException exception, WebRequest request) {
        ProblemDetail problem = ApiProblems.of(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have access to this resource.");
        return handleExceptionInternal(exception, problem, new HttpHeaders(), HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> unauthenticated(AuthenticationException exception, WebRequest request) {
        ProblemDetail problem = ApiProblems.of(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required.");
        return handleExceptionInternal(exception, problem, new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> unexpected(Exception exception, WebRequest request) {
        ProblemDetail problem = ApiProblems.of(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "The request could not be completed.");
        return handleExceptionInternal(exception, problem, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ApiProblems.of(status, "INVALID_REQUEST", "Request validation failed.");
        problem.setProperty("errors", exception.getBindingResult().getFieldErrors().stream().map(ApiProblems::fieldError).toList());
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            org.springframework.http.converter.HttpMessageNotReadableException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ApiProblems.of(status, "INVALID_REQUEST", "Request body is missing or malformed.");
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ApiProblems.of(status, "NOT_FOUND", "No endpoint matches this request.");
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, @Nullable Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (status.is5xxServerError()) {
            log.error("Request failed", exception);
        } else if (log.isDebugEnabled()) {
            log.debug("Rejected request with status {}: {}", status.value(), exception.toString());
        }
        return super.handleExceptionInternal(exception, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(
            @Nullable Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = body instanceof ProblemDetail existing
                ? existing
                : ApiProblems.of(status, ApiProblems.outcomeFor(status), ApiProblems.defaultDetail(status));
        ApiProblems.ensureClientFields(problem, status, path(request));
        return new ResponseEntity<>(problem, headers, status);
    }

    private static Map<String, String> violation(ConstraintViolation<?> violation) {
        return Map.of("field", violation.getPropertyPath().toString(), "message", violation.getMessage());
    }

    private static String path(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        return null;
    }
}
