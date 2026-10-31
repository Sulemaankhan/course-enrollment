package com.course.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiProblemsTest {

    @Test
    void statusCodesKeepAStableOutcomeAndAClientSafeMessage() throws Exception {
        ProblemDetail missing = ApiProblems.from(new ResponseStatusException(HttpStatus.NOT_FOUND, "Offerings only."));
        assertEquals("NOT_FOUND", missing.getProperties().get("outcome"));
        assertEquals("Offerings only.", missing.getDetail());
        assertEquals("Offerings only.", missing.getProperties().get("message"));

        ProblemDetail broken = ApiProblems.from(new IllegalStateException("password=secret"));
        assertEquals(500, broken.getStatus());
        assertEquals("INTERNAL_ERROR", broken.getProperties().get("outcome"));
        assertFalse(broken.getDetail().contains("secret"));

        ProblemDetail denied = ApiProblems.from(new AccessDeniedException("hidden"));
        assertEquals("FORBIDDEN", denied.getProperties().get("outcome"));
        assertFalse(String.valueOf(denied.getDetail()).contains("hidden"));

        ObjectMapper mapper = new ObjectMapper().addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class);
        String json = mapper.writeValueAsString(missing);
        assertTrue(json.contains("\"outcome\":\"NOT_FOUND\""));
        assertTrue(json.contains("\"message\":\"Offerings only.\""));
    }

    @Test
    void servletHandlerHidesUnexpectedFailuresAndValidationDetails() throws Exception {
        ServletApiExceptionHandler handler = new ServletApiExceptionHandler();
        ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest("GET", "/api/catalog/offerings"));

        ProblemDetail unexpected = (ProblemDetail) handler.unexpected(new IllegalStateException("jdbc password"), request).getBody();
        assertEquals(500, unexpected.getStatus());
        assertEquals("INTERNAL_ERROR", unexpected.getProperties().get("outcome"));
        assertEquals("The request could not be completed.", unexpected.getDetail());
        assertFalse(unexpected.getDetail().contains("password"));
        assertEquals("/api/catalog/offerings", unexpected.getInstance().toString());

        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.addError(new FieldError("request", "username", "must not be blank"));
        Method method = Sample.class.getDeclaredMethod("login", String.class);
        MethodArgumentNotValidException invalid = new MethodArgumentNotValidException(
                new org.springframework.core.MethodParameter(method, 0), binding);
        ProblemDetail validation = (ProblemDetail) handler.handleMethodArgumentNotValid(
                invalid, new org.springframework.http.HttpHeaders(), HttpStatus.BAD_REQUEST, request).getBody();
        assertEquals("INVALID_REQUEST", validation.getProperties().get("outcome"));
        assertTrue(validation.getProperties().get("errors").toString().contains("username"));

        ProblemDetail malformed = (ProblemDetail) handler.handleHttpMessageNotReadable(
                new HttpMessageNotReadableException("JSON parse error: secret"),
                new org.springframework.http.HttpHeaders(), HttpStatus.BAD_REQUEST, request).getBody();
        assertEquals("Request body is missing or malformed.", malformed.getDetail());
        assertFalse(malformed.getDetail().contains("secret"));

        ProblemDetail constraints = (ProblemDetail) handler.constraints(
                new ConstraintViolationException("nope", Set.of()), request).getBody();
        assertEquals(400, constraints.getStatus());
        assertEquals("INVALID_REQUEST", constraints.getProperties().get("outcome"));
    }

    @Test
    void securityResponsesUseTheSameProblemBody() throws Exception {
        ObjectMapper mapper = new ObjectMapper().addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/catalog/offerings");
        MockHttpServletResponse response = new MockHttpServletResponse();
        ServletProblemSecurity.authenticationEntryPoint(mapper).commence(request, response, null);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/problem+json"));
        assertTrue(response.getContentAsString().contains("\"outcome\":\"UNAUTHORIZED\""));
        assertEquals("Bearer", response.getHeader("WWW-Authenticate"));
    }

    private static final class Sample {
        @SuppressWarnings("unused")
        void login(String username) {
        }
    }
}
