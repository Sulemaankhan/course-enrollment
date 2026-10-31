package com.course.common;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class JwtSupport {

    public static final long TOKEN_HOURS = 8;

    private JwtSupport() {
    }

    public static SecretKey key(String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    public static JwtDecoder decoder(String secret) {
        return NimbusJwtDecoder.withSecretKey(key(secret)).macAlgorithm(MacAlgorithm.HS256).build();
    }

    public static String issue(String secret, String learnerId, String displayName) {
        return issue(secret, learnerId, displayName, RoleSupport.USER, Instant.now());
    }

    public static String issue(String secret, String learnerId, String displayName, String role) {
        return issue(secret, learnerId, displayName, role, Instant.now());
    }

    public static String issue(String secret, String learnerId, String displayName, Instant now) {
        return issue(secret, learnerId, displayName, RoleSupport.USER, now);
    }

    public static String issue(String secret, String learnerId, String displayName, String role, Instant now) {
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key(secret)));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("course-enrollment")
                .subject(learnerId)
                .issuedAt(now)
                .expiresAt(now.plus(TOKEN_HOURS, ChronoUnit.HOURS))
                .claim("name", displayName)
                .claim("role", RoleSupport.normalize(role))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
