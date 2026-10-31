package com.course.enrollment.config;

import com.course.enrollment.queue.FifoWaitlistQueue;
import com.course.enrollment.queue.InMemoryFifoWaitlistQueue;
import com.course.enrollment.queue.RedisFifoWaitlistQueue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class QueueConfig {

    @Bean
    @ConditionalOnProperty(name = "enrollment.queue", havingValue = "redis")
    FifoWaitlistQueue redisFifoWaitlistQueue(StringRedisTemplate redis) {
        return new RedisFifoWaitlistQueue(redis);
    }

    @Bean
    @ConditionalOnProperty(name = "enrollment.queue", havingValue = "memory")
    FifoWaitlistQueue inMemoryFifoWaitlistQueue() {
        return new InMemoryFifoWaitlistQueue();
    }
}
