package com.str.platform.application.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for API rate limiting.
 * Protects endpoints from abuse and ensures fair usage across clients.
 */
@Configuration
public class RateLimitConfig {

    /**
     * Default rate limiter for public API endpoints.
     * Allows 100 requests per minute per IP address.
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(100) // 100 requests
            .limitRefreshPeriod(Duration.ofMinutes(1)) // per minute
            .timeoutDuration(Duration.ofSeconds(5)) // wait up to 5s for permission
            .build();
        
        return RateLimiterRegistry.of(config);
    }

    /**
     * Stricter rate limiter for expensive operations like scraping.
     * Allows 10 requests per minute per IP address.
     */
    @Bean
    public RateLimiter scrapingRateLimiter(RateLimiterRegistry registry) {
        RateLimiterConfig strictConfig = RateLimiterConfig.custom()
            .limitForPeriod(10) // 10 requests
            .limitRefreshPeriod(Duration.ofMinutes(1)) // per minute
            .timeoutDuration(Duration.ofSeconds(3))
            .build();
        
        return registry.rateLimiter("scraping", strictConfig);
    }

    /**
     * Moderate rate limiter for analysis endpoints.
     * Allows 30 requests per minute per IP address.
     */
    @Bean
    public RateLimiter analysisRateLimiter(RateLimiterRegistry registry) {
        RateLimiterConfig moderateConfig = RateLimiterConfig.custom()
            .limitForPeriod(30) // 30 requests
            .limitRefreshPeriod(Duration.ofMinutes(1)) // per minute
            .timeoutDuration(Duration.ofSeconds(5))
            .build();
        
        return registry.rateLimiter("analysis", moderateConfig);
    }
}
