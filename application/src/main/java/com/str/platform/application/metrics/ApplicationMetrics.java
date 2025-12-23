package com.str.platform.application.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Custom metrics for monitoring application performance and behavior.
 * Integrates with Micrometer/Prometheus for observability.
 */
@Component
@RequiredArgsConstructor
public class ApplicationMetrics {

    private final MeterRegistry meterRegistry;

    // ===== Scraping Metrics =====

    /**
     * Record a scraping job creation event.
     */
    public void recordScrapingJobCreated(String platform) {
        Counter.builder("scraping.jobs.created")
            .description("Total number of scraping jobs created")
            .tag("platform", platform)
            .register(meterRegistry)
            .increment();
    }

    /**
     * Record a scraping job completion event.
     */
    public void recordScrapingJobCompleted(String platform, int propertiesFound, Duration duration) {
        Counter.builder("scraping.jobs.completed")
            .description("Total number of scraping jobs completed successfully")
            .tag("platform", platform)
            .register(meterRegistry)
            .increment();

        meterRegistry.summary("scraping.properties.found", "platform", platform)
            .record(propertiesFound);

        Timer.builder("scraping.job.duration")
            .description("Duration of scraping job execution")
            .tag("platform", platform)
            .register(meterRegistry)
            .record(duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Record a scraping job failure event.
     */
    public void recordScrapingJobFailed(String platform, String errorType) {
        Counter.builder("scraping.jobs.failed")
            .description("Total number of scraping jobs that failed")
            .tag("platform", platform)
            .tag("error_type", errorType)
            .register(meterRegistry)
            .increment();
    }

    /**
     * Record a scraping job timeout event.
     */
    public void recordScrapingJobTimeout(String platform) {
        Counter.builder("scraping.jobs.timeout")
            .description("Total number of scraping jobs that timed out")
            .tag("platform", platform)
            .register(meterRegistry)
            .increment();
    }

    // ===== Analysis Metrics =====

    /**
     * Record an analysis request event.
     */
    public void recordAnalysisRequested(String investmentType) {
        Counter.builder("analysis.requests")
            .description("Total number of analysis requests")
            .tag("investment_type", investmentType)
            .register(meterRegistry)
            .increment();
    }

    /**
     * Record analysis execution time.
     */
    public void recordAnalysisDuration(String investmentType, Duration duration) {
        Timer.builder("analysis.duration")
            .description("Time taken to complete investment analysis")
            .tag("investment_type", investmentType)
            .register(meterRegistry)
            .record(duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Record data quality of analysis.
     */
    public void recordAnalysisDataQuality(String dataQuality) {
        Counter.builder("analysis.data.quality")
            .description("Distribution of analysis data quality")
            .tag("quality", dataQuality)
            .register(meterRegistry)
            .increment();
    }

    // ===== Cache Metrics =====

    /**
     * Record a cache hit.
     */
    public void recordCacheHit(String cacheName) {
        Counter.builder("cache.hits")
            .description("Total number of cache hits")
            .tag("cache", cacheName)
            .register(meterRegistry)
            .increment();
    }

    /**
     * Record a cache miss.
     */
    public void recordCacheMiss(String cacheName) {
        Counter.builder("cache.misses")
            .description("Total number of cache misses")
            .tag("cache", cacheName)
            .register(meterRegistry)
            .increment();
    }

    /**
     * Record cache eviction.
     */
    public void recordCacheEviction(String cacheName) {
        Counter.builder("cache.evictions")
            .description("Total number of cache evictions")
            .tag("cache", cacheName)
            .register(meterRegistry)
            .increment();
    }

    // ===== API Metrics =====

    /**
     * Record an external API call (Mapbox).
     */
    public void recordExternalApiCall(String apiName, String operation, boolean success, Duration duration) {
        Counter.builder("external.api.calls")
            .description("Total number of external API calls")
            .tag("api", apiName)
            .tag("operation", operation)
            .tag("success", String.valueOf(success))
            .register(meterRegistry)
            .increment();

        Timer.builder("external.api.duration")
            .description("Duration of external API calls")
            .tag("api", apiName)
            .tag("operation", operation)
            .register(meterRegistry)
            .record(duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Record rate limit hit.
     */
    public void recordRateLimitHit(String endpoint, String clientIp) {
        Counter.builder("ratelimit.hits")
            .description("Total number of rate limit violations")
            .tag("endpoint", endpoint)
            .register(meterRegistry)
            .increment();
    }

    // ===== Location Metrics =====

    /**
     * Record a location search request.
     */
    public void recordLocationSearch(int resultCount) {
        Counter.builder("location.searches")
            .description("Total number of location search requests")
            .register(meterRegistry)
            .increment();

        meterRegistry.summary("location.search.results")
            .record(resultCount);
    }

    /**
     * Record driving time calculation.
     */
    public void recordDrivingTimeCalculation(Duration duration) {
        Counter.builder("drivingtime.calculations")
            .description("Total number of driving time calculations")
            .register(meterRegistry)
            .increment();

        Timer.builder("drivingtime.duration")
            .description("Time taken to calculate driving time")
            .register(meterRegistry)
            .record(duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    // ===== Business Metrics =====

    /**
     * Record property count in database.
     */
    public void recordPropertyCount(long count) {
        meterRegistry.gauge("properties.total", count);
    }

    /**
     * Record active scraping jobs.
     */
    public void recordActiveScrapingJobs(long count) {
        meterRegistry.gauge("scraping.jobs.active", count);
    }

    /**
     * Record total locations tracked.
     */
    public void recordLocationCount(long count) {
        meterRegistry.gauge("locations.total", count);
    }
}
