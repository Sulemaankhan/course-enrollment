package com.course.common;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtSupportTest {

    private static final String SECRET = "enrollment-platform-demo-signing-key-32";

    @Test
    void roundTripsLearnerIdentity() {
        String token = JwtSupport.issue(SECRET, "alice", "Alice Chen");
        Jwt jwt = JwtSupport.decoder(SECRET).decode(token);
        assertEquals("alice", jwt.getSubject());
        assertEquals("Alice Chen", jwt.getClaimAsString("name"));
        assertEquals(RoleSupport.USER, jwt.getClaimAsString("role"));
        assertEquals("ROLE_USER", RoleSupport.authorities(jwt).get(0).getAuthority());
    }

    @Test
    void registrarTokenCarriesTheAdminRole() {
        String token = JwtSupport.issue(SECRET, "admin", "Registrar", RoleSupport.ADMIN);
        Jwt jwt = JwtSupport.decoder(SECRET).decode(token);
        assertEquals("ROLE_ADMIN", RoleSupport.authorities(jwt).get(0).getAuthority());

        Jwt unsigned = Jwt.withTokenValue("unsigned")
                .header("alg", "none")
                .subject("alice")
                .build();
        assertEquals("ROLE_USER", RoleSupport.authorities(unsigned).get(0).getAuthority());
    }

    @Test
    void rejectsShortSecret() {
        assertThrows(IllegalArgumentException.class, () -> JwtSupport.key("too-short"));
    }
}
