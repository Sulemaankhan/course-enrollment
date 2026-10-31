package com.course.gateway;

import com.course.gateway.fallback.FallbackController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayAuthTest {

    @Autowired
    private WebTestClient client;

    @Test
    void loginIssuesATokenAndRejectsABadPassword() {
        TokenBody body = client.post().uri("/api/auth/login")
                .bodyValue(Map.of("username", "alice", "password", "password"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(TokenBody.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(body);
        assertEquals("alice", body.learnerId);
        assertEquals("Alice Chen", body.displayName);
        assertEquals("USER", body.role);
        assertNotNull(body.token);

        client.post().uri("/api/auth/login")
                .bodyValue(Map.of("username", "alice", "password", "wrong"))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType("application/problem+json")
                .expectBody()
                .jsonPath("$.outcome").isEqualTo("UNAUTHORIZED")
                .jsonPath("$.message").isEqualTo("Invalid username or password.")
                .jsonPath("$.detail").isEqualTo("Invalid username or password.");
    }

    @Test
    void missingTokenIsUnauthorizedAndAValidTokenIsNotRejectedAsAnonymous() {
        client.get().uri("/api/catalog/offerings")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.outcome").isEqualTo("UNAUTHORIZED")
                .jsonPath("$.message").isEqualTo("Authentication is required.");

        TokenBody body = client.post().uri("/api/auth/login")
                .bodyValue(Map.of("username", "dan", "password", "password"))
                .exchange()
                .expectBody(TokenBody.class)
                .returnResult()
                .getResponseBody();

        client.get().uri("/api/catalog/offerings")
                .header("Authorization", "Bearer " + body.token)
                .exchange()
                .expectStatus().value(status -> assertNotEquals(HttpStatus.UNAUTHORIZED.value(), status));
    }

    @Test
    void registrarAndLearnerAreAuthorizedForDifferentScreens() {
        TokenBody learner = login("alice", "password");
        assertEquals("USER", learner.role);

        TokenBody registrar = login("admin", "password");
        assertEquals("admin", registrar.learnerId);
        assertEquals("Registrar", registrar.displayName);
        assertEquals("ADMIN", registrar.role);

        client.post().uri("/api/auth/login")
                .bodyValue(Map.of("username", "admin", "password", "wrong"))
                .exchange()
                .expectStatus().isUnauthorized();

        client.get().uri("/api/enrollments/offerings/CS-301-2026FA/overview")
                .header("Authorization", "Bearer " + learner.token)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.outcome").isEqualTo("FORBIDDEN")
                .jsonPath("$.message").isEqualTo("You do not have access to this resource.");

        client.post().uri("/api/enrollments/offerings/CS-301-2026FA/enroll")
                .header("Authorization", "Bearer " + registrar.token)
                .exchange()
                .expectStatus().isForbidden();

        client.get().uri("/api/catalog/offerings")
                .header("Authorization", "Bearer " + registrar.token)
                .exchange()
                .expectStatus().value(status -> assertNotEquals(HttpStatus.UNAUTHORIZED.value(), status));
    }

    @Test
    void fallbackResponsesAreExplicit() {
        FallbackController controller = new FallbackController();
        assertEquals(503, controller.catalog().getStatusCode().value());
        assertEquals("SERVICE_UNAVAILABLE", controller.catalog().getBody().getProperties().get("outcome"));
        assertEquals("Enrollment service is temporarily unavailable.",
                controller.enrollment().getBody().getDetail());
    }

    private static final class TokenBody {
        public String token;
        public String learnerId;
        public String displayName;
        public long expiresInSeconds;
        public String role;
    }

    private TokenBody login(String username, String password) {
        TokenBody body = client.post().uri("/api/auth/login")
                .bodyValue(Map.of("username", username, "password", password))
                .exchange()
                .expectStatus().isOk()
                .expectBody(TokenBody.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(body);
        return body;
    }
}
