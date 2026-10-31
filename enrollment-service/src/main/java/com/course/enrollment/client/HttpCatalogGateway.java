package com.course.enrollment.client;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
@Profile("!test")
public class HttpCatalogGateway implements CatalogGateway {

    static final int WINDOW_SIZE = 8;
    static final int MINIMUM_CALLS = 4;

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;
    private final Map<String, OfferingSnapshot> offerings = new ConcurrentHashMap<>();
    private final Map<String, LearnerSnapshot> learners = new ConcurrentHashMap<>();

    public HttpCatalogGateway(@Qualifier("catalogRestClient") RestClient restClient, CircuitBreakerRegistry registry) {
        this.restClient = restClient;
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(WINDOW_SIZE)
                .minimumNumberOfCalls(MINIMUM_CALLS)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(2)
                .ignoreException(HttpCatalogGateway::isMissingLearnerOrOffering)
                .build();
        this.circuitBreaker = registry.circuitBreaker("catalog", config);
    }

    @Override
    public OfferingSnapshot getOffering(String offeringId) {
        return resolve(offerings, offeringId, () -> fetchOffering(offeringId));
    }

    @Override
    public LearnerSnapshot getLearner(String learnerId) {
        return resolve(learners, learnerId, () -> fetchLearner(learnerId));
    }

    CircuitBreaker breaker() {
        return circuitBreaker;
    }

    private <T> T resolve(Map<String, T> cache, String key, Supplier<T> fetch) {
        try {
            T value = circuitBreaker.executeSupplier(fetch::get);
            cache.put(key, value);
            return value;
        } catch (RuntimeException ex) {
            RuntimeException unwrapped = unwrap(ex);
            if (unwrapped instanceof OfferingNotFoundException || unwrapped instanceof LearnerNotFoundException) {
                throw unwrapped;
            }
            T cached = cache.get(key);
            if (cached != null) {
                return cached;
            }
            if (unwrapped instanceof CatalogUnavailableException unavailable) {
                throw unavailable;
            }
            throw new CatalogUnavailableException("Course catalog is unavailable.", unwrapped);
        }
    }

    private OfferingSnapshot fetchOffering(String offeringId) {
        return restClient.get()
                .uri("/api/catalog/offerings/{id}", offeringId)
                .retrieve()
                .onStatus(status -> status.value() == 404, (request, response) -> {
                    throw new OfferingNotFoundException(offeringId);
                })
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new CatalogUnavailableException("Catalog returned HTTP " + response.getStatusCode().value());
                })
                .body(OfferingSnapshot.class);
    }

    private LearnerSnapshot fetchLearner(String learnerId) {
        return restClient.get()
                .uri("/api/catalog/learners/{id}", learnerId)
                .retrieve()
                .onStatus(status -> status.value() == 404, (request, response) -> {
                    throw new LearnerNotFoundException(learnerId);
                })
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new CatalogUnavailableException("Catalog returned HTTP " + response.getStatusCode().value());
                })
                .body(LearnerSnapshot.class);
    }

    static boolean isMissingLearnerOrOffering(Throwable throwable) {
        return unwrap(throwable) instanceof OfferingNotFoundException
                || unwrap(throwable) instanceof LearnerNotFoundException;
    }

    private static RuntimeException unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof OfferingNotFoundException
                    || current instanceof LearnerNotFoundException
                    || current instanceof CatalogUnavailableException) {
                return (RuntimeException) current;
            }
            if (current instanceof CallNotPermittedException) {
                return new CatalogUnavailableException("Course catalog circuit is open.", current);
            }
            current = current.getCause();
        }
        if (throwable instanceof RuntimeException runtime) {
            return runtime;
        }
        return new CatalogUnavailableException("Course catalog is unavailable.", throwable);
    }
}
