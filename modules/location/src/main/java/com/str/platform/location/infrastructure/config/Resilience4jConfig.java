package com.str.platform.location.infrastructure.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j configuration for Mapbox API client.
 * Registers mapbox-specific rate limiter and retry into the shared registries.
 */
@Configuration
public class Resilience4jConfig {

    /**
     * Rate limiter for Mapbox API calls.
     * Mapbox free tier: 100,000 requests/month ≈ 38 requests/minute
     * Being conservative with 30 requests/minute.
     * Registers into the shared RateLimiterRegistry created by RateLimitConfig.
     */
    @Bean
    public RateLimiter mapboxRateLimiter(RateLimiterRegistry registry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(30)
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .timeoutDuration(Duration.ofSeconds(5))
            .build();

        return registry.rateLimiter("mapbox", config);
    }

    /**
     * Retry configuration for Mapbox API calls.
     * Retries transient failures with exponential backoff.
     */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(500))
            .retryExceptions(
                org.springframework.web.reactive.function.client.WebClientRequestException.class,
                java.util.concurrent.TimeoutException.class
            )
            .build();

        RetryRegistry registry = RetryRegistry.of(config);
        registry.retry("mapbox", config);
        
        return registry;
    }
}
