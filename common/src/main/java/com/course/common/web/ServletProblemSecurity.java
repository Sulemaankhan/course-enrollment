package com.course.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public final class ServletProblemSecurity {

    private ServletProblemSecurity() {
    }

    public static AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper mapper) {
        return (request, response, exception) -> write(request, response, mapper, HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED", "Authentication is required.");
    }

    public static AccessDeniedHandler accessDeniedHandler(ObjectMapper mapper) {
        return (request, response, exception) -> write(request, response, mapper, HttpStatus.FORBIDDEN,
                "FORBIDDEN", "You do not have access to this resource.");
    }

    private static void write(HttpServletRequest request, HttpServletResponse response, ObjectMapper mapper,
                              HttpStatus status, String outcome, String detail) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        ProblemDetail problem = ApiProblems.of(status, outcome, detail);
        try {
            problem.setInstance(URI.create(request.getRequestURI()));
        } catch (IllegalArgumentException ignored) {
            // The request path is not a URI reference.
        }
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (status == HttpStatus.UNAUTHORIZED) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        mapper.writeValue(response.getOutputStream(), problem);
    }
}
