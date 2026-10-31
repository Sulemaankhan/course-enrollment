package com.course.enrollment.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestInfrastructure {

    @Bean
    MutableClock clock() {
        return new MutableClock();
    }

    @Bean
    @Primary
    FakeCatalogGateway catalogGateway() {
        return new FakeCatalogGateway();
    }

    @Bean
    UnstableQueue fifoWaitlistQueue() {
        return new UnstableQueue();
    }
}
