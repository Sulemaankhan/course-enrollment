package com.course.common.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.net.URI;

@Order(-2)
public class ReactiveApiExceptionHandler implements WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ReactiveApiExceptionHandler.class);

    private final ObjectMapper mapper;

    public ReactiveApiExceptionHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable exception) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(exception);
        }
        ProblemDetail problem = ApiProblems.from(exception);
        String path = exchange.getRequest().getPath().value();
        if (path != null && !path.isBlank()) {
            try {
                problem.setInstance(URI.create(path));
            } catch (IllegalArgumentException ignored) {
                // The request path is not a URI reference.
            }
        }
        if (problem.getStatus() >= 500) {
            log.error("Request failed", exception);
        } else if (log.isDebugEnabled()) {
            log.debug("Rejected request with status {}: {}", problem.getStatus(), exception.toString());
        }
        byte[] body;
        try {
            body = mapper.writeValueAsBytes(problem);
        } catch (JsonProcessingException writeFailure) {
            log.error("Could not write the error response", writeFailure);
            return Mono.error(exception);
        }
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatusCode.valueOf(problem.getStatus()));
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }
}
