package com.course.catalog.config;

import com.course.common.JwtSupport;
import com.course.common.RoleSupport;
import com.course.common.web.ServletProblemSecurity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Profile("!test")
    SecurityFilterChain apiSecurity(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(ServletProblemSecurity.authenticationEntryPoint(objectMapper))
                        .accessDeniedHandler(ServletProblemSecurity.accessDeniedHandler(objectMapper)))
                .oauth2ResourceServer(oauth -> oauth
                        .authenticationEntryPoint(ServletProblemSecurity.authenticationEntryPoint(objectMapper))
                        .accessDeniedHandler(ServletProblemSecurity.accessDeniedHandler(objectMapper))
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(roleConverter())));
        return http.build();
    }

    private static JwtAuthenticationConverter roleConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(RoleSupport::authorities);
        return converter;
    }

    @Bean
    @Profile("test")
    SecurityFilterChain testSecurity(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Profile("!test")
    JwtDecoder jwtDecoder(@Value("${security.jwt.secret}") String secret) {
        return JwtSupport.decoder(secret);
    }
}
