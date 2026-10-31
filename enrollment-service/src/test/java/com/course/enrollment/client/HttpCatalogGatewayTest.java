package com.course.enrollment.client;

import com.course.common.DemoCourse;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpCatalogGatewayTest {

    private static final String OFFERING_JSON = """
            {"offeringId":"CS-301-2026FA","courseCode":"CS-301","title":"Data Structures","term":"2026FA","capacity":2,"prerequisiteCourseCode":"CS-201"}
            """;
    private static final String LEARNER_JSON = """
            {"learnerId":"alice","displayName":"Alice Chen","completedCourseCodes":["CS-201"]}
            """;

    private HttpServer server;
    private final AtomicInteger offeringHits = new AtomicInteger();
    private volatile int offeringStatus = 200;
    private HttpCatalogGateway gateway;

    @BeforeEach
    void start() throws IOException {
        offeringHits.set(0);
        offeringStatus = 200;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/catalog/offerings/" + DemoCourse.OFFERING_ID, this::offering);
        server.createContext("/api/catalog/offerings/missing", exchange -> write(exchange, 404, "{\"outcome\":\"OFFERING_NOT_FOUND\"}"));
        server.createContext("/api/catalog/learners/alice", exchange -> write(exchange, 200, LEARNER_JSON));
        server.createContext("/api/catalog/learners/ghost", exchange -> write(exchange, 404, "{\"outcome\":\"LEARNER_NOT_FOUND\"}"));
        server.start();
        RestClient client = RestClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .build();
        gateway = new HttpCatalogGateway(client, CircuitBreakerRegistry.ofDefaults());
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void readsOfferingAndLearnerAndFallsBackToCache() {
        OfferingSnapshot offering = gateway.getOffering(DemoCourse.OFFERING_ID);
        assertEquals("Data Structures", offering.title());
        assertEquals(DemoCourse.PREREQUISITE, offering.prerequisiteCourseCode());
        assertEquals("Alice Chen", gateway.getLearner("alice").displayName());

        offeringStatus = 500;
        assertEquals("Data Structures", gateway.getOffering(DemoCourse.OFFERING_ID).title());
        assertTrue(offeringHits.get() >= 2);
    }

    @Test
    void notFoundDoesNotOpenTheCircuitOrReturnACachedOffering() {
        gateway.getOffering(DemoCourse.OFFERING_ID);
        for (int attempt = 0; attempt < 6; attempt++) {
            assertThrows(OfferingNotFoundException.class, () -> gateway.getOffering("missing"));
        }
        assertThrows(LearnerNotFoundException.class, () -> gateway.getLearner("ghost"));
        assertEquals(CircuitBreaker.State.CLOSED, gateway.breaker().getState());
        assertTrue(HttpCatalogGateway.isMissingLearnerOrOffering(
                new IllegalStateException(new OfferingNotFoundException("x"))));
        assertFalse(HttpCatalogGateway.isMissingLearnerOrOffering(new IllegalStateException("other")));
    }

    @Test
    void openCircuitServesTheCachedOfferingWithoutAnotherCall() {
        assertEquals("Data Structures", gateway.getOffering(DemoCourse.OFFERING_ID).title());
        offeringStatus = 500;
        int guard = 0;
        while (gateway.breaker().getState() != CircuitBreaker.State.OPEN && guard < 12) {
            gateway.getOffering(DemoCourse.OFFERING_ID);
            guard++;
        }
        assertEquals(CircuitBreaker.State.OPEN, gateway.breaker().getState());
        int observed = offeringHits.get();
        assertEquals("Data Structures", gateway.getOffering(DemoCourse.OFFERING_ID).title());
        assertEquals(observed, offeringHits.get());
    }

    @Test
    void connectionFailureWithoutCacheIsUnavailable() {
        RestClient client = RestClient.builder().baseUrl("http://127.0.0.1:9").build();
        HttpCatalogGateway offline = new HttpCatalogGateway(client, CircuitBreakerRegistry.ofDefaults());
        assertThrows(CatalogUnavailableException.class, () -> offline.getOffering(DemoCourse.OFFERING_ID));
        assertThrows(CatalogUnavailableException.class, () -> offline.getLearner("alice"));
    }

    private void offering(HttpExchange exchange) throws IOException {
        offeringHits.incrementAndGet();
        if (offeringStatus == 200) {
            write(exchange, 200, OFFERING_JSON);
        } else {
            write(exchange, offeringStatus, "{\"outcome\":\"ERROR\"}");
        }
    }

    private static void write(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream stream = exchange.getResponseBody()) {
            stream.write(bytes);
        }
    }
}
