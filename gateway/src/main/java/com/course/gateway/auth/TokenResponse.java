package com.course.gateway.auth;

public record TokenResponse(String token, String learnerId, String displayName, long expiresInSeconds, String role) {
}
