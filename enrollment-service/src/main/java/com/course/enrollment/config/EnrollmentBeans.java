package com.course.enrollment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class EnrollmentBeans {

    @Bean
    @Profile("!test")
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean(name = "catalogRestClient")
    @Profile("!test")
    RestClient catalogRestClient(@Value("${catalog.base-url}") String baseUrl) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofMillis(800));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }
}
