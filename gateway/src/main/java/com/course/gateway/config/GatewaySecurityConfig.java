package com.course.gateway.config;

import com.course.common.JwtSupport;
import com.course.common.RoleSupport;
import com.course.common.web.ReactiveProblemSecurity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import reactor.core.publisher.Mono;

import java.util.List;

@Configuration
public class GatewaySecurityConfig {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, ObjectMapper objectMapper) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeExchange(auth -> auth
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/api/auth/login", "/actuator/health", "/fallback/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/enrollments/**").hasRole("USER")
                        .pathMatchers(HttpMethod.GET, "/api/enrollments/offerings/*/overview").hasRole("ADMIN")
                        .pathMatchers("/api/**").authenticated()
                        .anyExchange().permitAll())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(ReactiveProblemSecurity.authenticationEntryPoint(objectMapper))
                        .accessDeniedHandler(ReactiveProblemSecurity.accessDeniedHandler(objectMapper)))
                .oauth2ResourceServer(oauth -> oauth
                        .authenticationEntryPoint(ReactiveProblemSecurity.authenticationEntryPoint(objectMapper))
                        .accessDeniedHandler(ReactiveProblemSecurity.accessDeniedHandler(objectMapper))
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(roleConverter())))
                .build();
    }

    private static Converter<Jwt, Mono<AbstractAuthenticationToken>> roleConverter() {
        return jwt -> Mono.just(new JwtAuthenticationToken(jwt, RoleSupport.authorities(jwt)));
    }

    @Bean
    ReactiveJwtDecoder reactiveJwtDecoder(@Value("${security.jwt.secret}") String secret) {
        return NimbusReactiveJwtDecoder.withSecretKey(JwtSupport.key(secret)).build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(4);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
