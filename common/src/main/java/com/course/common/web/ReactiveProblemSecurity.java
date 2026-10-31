package com.course.common.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

public final class ReactiveProblemSecurity {

    private ReactiveProblemSecurity() {
    }

    public static ServerAuthenticationEntryPoint authenticationEntryPoint(ObjectMapper mapper) {
        return (exchange, exception) -> write(exchange, mapper, HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED", "Authentication is required.");
    }

    public static ServerAccessDeniedHandler accessDeniedHandler(ObjectMapper mapper) {
        return (exchange, exception) -> write(exchange, mapper, HttpStatus.FORBIDDEN,
                "FORBIDDEN", "You do not have access to this resource.");
    }

    private static Mono<Void> write(ServerWebExchange exchange, ObjectMapper mapper, HttpStatus status,
                                    String outcome, String detail) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.empty();
        }
        ProblemDetail problem = ApiProblems.of(status, outcome, detail);
        String path = exchange.getRequest().getPath().value();
        if (path != null && !path.isBlank()) {
            try {
                problem.setInstance(URI.create(path));
            } catch (IllegalArgumentException ignored) {
                // The request path is not a URI reference.
            }
        }
        byte[] body;
        try {
            body = mapper.writeValueAsBytes(problem);
        } catch (JsonProcessingException exception) {
            return Mono.error(exception);
        }
        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        if (status == HttpStatus.UNAUTHORIZED) {
            response.getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }
}
