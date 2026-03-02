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
     * 10 requests per second per IP.
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        RateLimiterConfig defaultConfig = RateLimiterConfig.custom()
            .limitForPeriod(10)
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .timeoutDuration(Duration.ZERO)
            .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(defaultConfig);

        // Outbound Mapbox calls: free tier = 600/min = 10/s. Use 9/s to stay safely under.
        RateLimiterConfig mapboxConfig = RateLimiterConfig.custom()
            .limitForPeriod(9)
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .timeoutDuration(Duration.ZERO)
            .build();
        registry.rateLimiter("mapbox", mapboxConfig);

        return registry;
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
