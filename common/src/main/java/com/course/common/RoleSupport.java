package com.course.common;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public final class RoleSupport {

    public static final String USER = "USER";
    public static final String ADMIN = "ADMIN";
    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_DISPLAY_NAME = "Registrar";

    private RoleSupport() {
    }

    public static String normalize(String role) {
        return ADMIN.equals(role) ? ADMIN : USER;
    }

    public static List<GrantedAuthority> authorities(Jwt jwt) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + normalize(jwt.getClaimAsString("role"))));
    }
}
